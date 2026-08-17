package cn.iocoder.yudao.module.agent.service.agentmcp;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.agent.controller.admin.agentmcp.vo.AgentMcpPageReqVO;
import cn.iocoder.yudao.module.agent.controller.admin.agentmcp.vo.AgentMcpRespVO;
import cn.iocoder.yudao.module.agent.controller.admin.agentmcp.vo.AgentMcpSaveReqVO;
import cn.iocoder.yudao.module.agent.dal.dataobject.agent.AiAgentDO;
import cn.iocoder.yudao.module.agent.dal.dataobject.agentmcp.AiAgentMcpDO;
import cn.iocoder.yudao.module.agent.dal.dataobject.mcpmeta.AiMcpMetaDO;
import cn.iocoder.yudao.module.agent.dal.mysql.agent.AiAgentMapper;
import cn.iocoder.yudao.module.agent.dal.mysql.agentmcp.AiAgentMcpMapper;
import cn.iocoder.yudao.module.agent.dal.mysql.mcpmeta.AiMcpMetaMapper;
import cn.iocoder.yudao.module.agent.framework.config.QwenPawClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.agent.enums.ErrorCodeConstants.*;

/**
 * 智能体-MCP 绑定 Service 实现类
 *
 * @author 吴皓
 */
@Service
@Validated
@Slf4j
public class AiAgentMcpServiceImpl implements AiAgentMcpService {

    @Resource
    private AiAgentMcpMapper agentMcpMapper;
    @Resource
    private AiAgentMapper agentMapper;
    @Resource
    private AiMcpMetaMapper mcpMetaMapper;
    @Resource
    private QwenPawClient qwenPawClient;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createAgentMcp(AgentMcpSaveReqVO createReqVO) {
        AiAgentDO agent = validateAgent(createReqVO.getAgentId());
        AiMcpMetaDO meta = validateMcpMeta(createReqVO.getMcpMetaId());
        // 重复绑定校验
        if (agentMcpMapper.selectByAgentIdAndMcpMetaId(agent.getId(), meta.getId()) != null) {
            throw exception(AGENT_MCP_DUPLICATE);
        }

        AiAgentMcpDO bind = BeanUtils.toBean(createReqVO, AiAgentMcpDO.class);
        if (bind.getClientKey() == null || bind.getClientKey().isEmpty()) {
            bind.setClientKey(meta.getCode());
        }
        if (bind.getEnabled() == null) {
            bind.setEnabled(1);
        }
        if (bind.getSortOrder() == null) {
            bind.setSortOrder(0);
        }
        bind.setTenantId(agent.getTenantId());
        agentMcpMapper.insert(bind);

        // 下发到 QwenPaw（启用状态下）
        if (Integer.valueOf(1).equals(bind.getEnabled())) {
            dispatchToQwenPaw(agent, meta, bind);
        }
        return bind.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAgentMcp(AgentMcpSaveReqVO updateReqVO) {
        AiAgentMcpDO bind = validateExists(updateReqVO.getId());
        AiAgentDO agent = validateAgent(bind.getAgentId());
        AiMcpMetaDO meta = validateMcpMeta(bind.getMcpMetaId());

        // 更新本地
        AiAgentMcpDO updateObj = BeanUtils.toBean(updateReqVO, AiAgentMcpDO.class);
        agentMcpMapper.updateById(updateObj);

        // 重新下发：先删后建（幂等）
        try {
            qwenPawClient.deleteMcp(agent.getQwenpawAgentId(), bind.getClientKey());
        } catch (Exception e) {
            log.warn("[updateAgentMcp] 删除旧 MCP 失败，clientKey={}", bind.getClientKey(), e);
        }
        AiAgentMcpDO latest = validateExists(updateReqVO.getId());
        if (Integer.valueOf(1).equals(latest.getEnabled())) {
            dispatchToQwenPaw(agent, meta, latest);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAgentMcp(Long id) {
        AiAgentMcpDO bind = validateExists(id);
        AiAgentDO agent = validateAgent(bind.getAgentId());
        agentMcpMapper.deleteById(id);
        // 同步删除 QwenPaw MCP client
        try {
            qwenPawClient.deleteMcp(agent.getQwenpawAgentId(), bind.getClientKey());
        } catch (Exception e) {
            log.warn("[deleteAgentMcp] QwenPaw 删除 MCP 失败，clientKey={}", bind.getClientKey(), e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleAgentMcp(Long id) {
        AiAgentMcpDO bind = validateExists(id);
        AiAgentDO agent = validateAgent(bind.getAgentId());
        AiMcpMetaDO meta = validateMcpMeta(bind.getMcpMetaId());

        boolean enabled = !Integer.valueOf(1).equals(bind.getEnabled());
        AiAgentMcpDO updateObj = new AiAgentMcpDO();
        updateObj.setId(id);
        updateObj.setEnabled(enabled ? 1 : 0);
        agentMcpMapper.updateById(updateObj);

        if (enabled) {
            bind.setEnabled(1);
            dispatchToQwenPaw(agent, meta, bind);
        } else {
            try {
                qwenPawClient.deleteMcp(agent.getQwenpawAgentId(), bind.getClientKey());
            } catch (Exception e) {
                log.warn("[toggleAgentMcp] QwenPaw 停用 MCP 失败，clientKey={}", bind.getClientKey(), e);
            }
        }
    }

    @Override
    public AiAgentMcpDO getAgentMcp(Long id) {
        return validateExists(id);
    }

    @Override
    public PageResult<AgentMcpRespVO> getAgentMcpPage(AgentMcpPageReqVO pageReqVO) {
        PageResult<AiAgentMcpDO> pageResult = agentMcpMapper.selectPage(pageReqVO);
        return BeanUtils.toBean(pageResult, AgentMcpRespVO.class, this::fillMeta);
    }

    @Override
    public List<AgentMcpRespVO> getAgentMcpListByAgentId(Long agentId) {
        List<AiAgentMcpDO> list = agentMcpMapper.selectListByAgentId(agentId);
        return BeanUtils.toBean(list, AgentMcpRespVO.class, this::fillMeta);
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 填充 MCP 商店项冗余展示字段（名称/编码/传输协议）
     */
    private void fillMeta(AgentMcpRespVO respVO) {
        if (respVO == null || respVO.getMcpMetaId() == null) {
            return;
        }
        AiMcpMetaDO meta = mcpMetaMapper.selectById(respVO.getMcpMetaId());
        if (meta != null) {
            respVO.setMcpName(meta.getName());
            respVO.setMcpCode(meta.getCode());
            respVO.setTransport(meta.getTransport());
        }
    }

    private AiAgentMcpDO validateExists(Long id) {
        AiAgentMcpDO bind = agentMcpMapper.selectById(id);
        if (bind == null) {
            throw exception(AGENT_MCP_NOT_EXISTS);
        }
        return bind;
    }

    private AiAgentDO validateAgent(Long agentId) {
        AiAgentDO agent = agentMapper.selectById(agentId);
        if (agent == null) {
            throw exception(AGENT_NOT_EXISTS);
        }
        return agent;
    }

    private AiMcpMetaDO validateMcpMeta(Long mcpMetaId) {
        AiMcpMetaDO meta = mcpMetaMapper.selectById(mcpMetaId);
        if (meta == null) {
            throw exception(MCP_META_NOT_EXISTS);
        }
        return meta;
    }

    /**
     * 组装有效配置并下发到 QwenPaw：
     * 商店模板（url/command/headers）+ 用户级覆盖（config_override，JSON）合并，
     * 取覆盖优先；headers 可由上层注入用户级鉴权凭证。
     */
    private void dispatchToQwenPaw(AiAgentDO agent, AiMcpMetaDO meta, AiAgentMcpDO bind) {
        // 用户级覆盖：允许覆盖 url/command，示例 JSON：{"url":"https://kb-mcp.internal/kb/v1/mcp"}
        String overrideJson = bind.getConfigOverride();
        String url = meta.getUrl();
        String command = meta.getCommand();
        if (overrideJson != null && !overrideJson.isEmpty()) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
                java.util.Map<?, ?> override = om.readValue(overrideJson, java.util.Map.class);
                if (override.get("url") != null) {
                    url = String.valueOf(override.get("url"));
                }
                if (override.get("command") != null) {
                    command = String.valueOf(override.get("command"));
                }
            } catch (Exception e) {
                log.warn("[dispatchToQwenPaw] config_override 解析失败，忽略覆盖。json={}", overrideJson, e);
            }
        }
        // 工具白名单：优先取绑定级，其次取商店级
        String toolsWhitelist = bind.getToolsWhitelist();
        if ((toolsWhitelist == null || toolsWhitelist.isEmpty())
                && meta.getToolsWhitelist() != null && !meta.getToolsWhitelist().isEmpty()) {
            toolsWhitelist = meta.getToolsWhitelist();
        }
        qwenPawClient.registerMcp(agent.getQwenpawAgentId(), bind.getClientKey(),
                meta.getTransport(), url, command, null, meta.getHeaders(), toolsWhitelist);
    }

}
