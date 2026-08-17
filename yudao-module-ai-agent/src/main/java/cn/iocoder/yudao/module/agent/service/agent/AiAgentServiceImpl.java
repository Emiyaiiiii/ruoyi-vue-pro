package cn.iocoder.yudao.module.agent.service.agent;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.agent.controller.admin.agent.vo.AgentPageReqVO;
import cn.iocoder.yudao.module.agent.controller.admin.agent.vo.AgentSaveReqVO;
import cn.iocoder.yudao.module.agent.dal.dataobject.agent.AiAgentDO;
import cn.iocoder.yudao.module.agent.dal.dataobject.agentmcp.AiAgentMcpDO;
import cn.iocoder.yudao.module.agent.dal.dataobject.chatmessage.AiChatMessageDO;
import cn.iocoder.yudao.module.agent.dal.dataobject.chatsession.AiChatSessionDO;
import cn.iocoder.yudao.module.agent.dal.mysql.agent.AiAgentMapper;
import cn.iocoder.yudao.module.agent.dal.mysql.agentmcp.AiAgentMcpMapper;
import cn.iocoder.yudao.module.agent.dal.mysql.chatmessage.AiChatMessageMapper;
import cn.iocoder.yudao.module.agent.dal.mysql.chatsession.AiChatSessionMapper;
import cn.iocoder.yudao.module.agent.framework.config.QwenPawClient;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.agent.enums.ErrorCodeConstants.*;

/**
 * 智能体实例 Service 实现类
 *
 * @author 吴皓
 */
@Service
@Validated
@Slf4j
public class AiAgentServiceImpl implements AiAgentService {

    @Resource
    private AiAgentMapper agentMapper;
    @Resource
    private AiAgentMcpMapper agentMcpMapper;
    @Resource
    private AiChatSessionMapper chatSessionMapper;
    @Resource
    private AiChatMessageMapper chatMessageMapper;
    @Resource
    private QwenPawClient qwenPawClient;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createAgent(AgentSaveReqVO createReqVO) {
        // 重名校验（同用户下唯一）
        validateNameUnique(createReqVO.getUserId(), createReqVO.getName(), null);

        // 生成 QwenPaw agent id：u{userId}_{随机串}
        String qwenpawAgentId = buildQwenpawAgentId(createReqVO.getUserId());

        // 先落库（失败回滚，避免孤儿记录）
        AiAgentDO agent = BeanUtils.toBean(createReqVO, AiAgentDO.class);
        if (agent.getStatus() == null) {
            agent.setStatus(1);
        }
        if (agent.getEnableKbTool() == null) {
            agent.setEnableKbTool(Boolean.TRUE);
        }
        if (agent.getSortOrder() == null) {
            agent.setSortOrder(0);
        }
        agent.setQwenpawAgentId(qwenpawAgentId);
        agentMapper.insert(agent);

        // 同步创建 QwenPaw agent
        try {
            String createdId = qwenPawClient.createAgent(qwenpawAgentId, agent.getName(),
                    agent.getDescription(), agent.getWorkspaceDir(),
                    agent.getModelProvider(), agent.getModelName());
            if (createdId == null || createdId.isEmpty()) {
                throw new IllegalStateException("QwenPaw 未返回 agent id");
            }
            agent.setQwenpawAgentId(createdId);
            agentMapper.updateById(agent);
        } catch (Exception e) {
            log.error("[createAgent] QwenPaw 智能体创建失败，agentId={}", agent.getId(), e);
            throw exception(AGENT_QWENPAW_CREATE_FAILED);
        }

        // 创建后安装初始技能（从 QwenPaw 技能池按 name 安装）
        List<String> initialSkills = createReqVO.getInitialSkills();
        if (initialSkills != null && !initialSkills.isEmpty()) {
            for (String skillName : initialSkills) {
                if (skillName != null && !skillName.trim().isEmpty()) {
                    try {
                        installQwenpawSkill(agent.getId(), skillName.trim());
                    } catch (Exception e) {
                        log.warn("[createAgent] 初始技能安装失败，skill={}", skillName, e);
                    }
                }
            }
        }
        return agent.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAgent(AgentSaveReqVO updateReqVO) {
        AiAgentDO agent = validateExists(updateReqVO.getId());
        // 重名校验（排除自身）
        validateNameUnique(updateReqVO.getUserId() != null ? updateReqVO.getUserId() : agent.getUserId(),
                updateReqVO.getName(), updateReqVO.getId());

        AiAgentDO updateObj = BeanUtils.toBean(updateReqVO, AiAgentDO.class);
        agentMapper.updateById(updateObj);

        // 同步更新 QwenPaw（名称/描述/模型变更；QwenPaw 为部分更新，仅传变更字段）
        try {
            Map<String, Object> fields = new LinkedHashMap<>();
            fields.put("name", updateReqVO.getName() != null ? updateReqVO.getName() : agent.getName());
            if (updateReqVO.getDescription() != null) {
                fields.put("description", updateReqVO.getDescription());
            }
            // active_model：仅当模型供应商或模型名有变更时下发，避免空对象覆盖
            String provider = updateReqVO.getModelProvider() != null
                    ? updateReqVO.getModelProvider() : agent.getModelProvider();
            String model = updateReqVO.getModelName() != null
                    ? updateReqVO.getModelName() : agent.getModelName();
            if (updateReqVO.getModelProvider() != null || updateReqVO.getModelName() != null) {
                Map<String, Object> activeModel = new LinkedHashMap<>();
                activeModel.put("provider_id", provider == null ? "" : provider);
                activeModel.put("model", model == null ? "" : model);
                fields.put("active_model", activeModel);
            }
            qwenPawClient.updateAgent(agent.getQwenpawAgentId(), fields);
        } catch (Exception e) {
            log.warn("[updateAgent] QwenPaw 同步失败，agentId={}", agent.getQwenpawAgentId(), e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAgent(Long id) {
        AiAgentDO agent = validateExists(id);
        // 级联删除绑定、会话与消息
        agentMcpMapper.delete(new LambdaQueryWrapperX<AiAgentMcpDO>()
                .eq(AiAgentMcpDO::getAgentId, id));
        chatSessionMapper.delete(new LambdaQueryWrapperX<AiChatSessionDO>()
                .eq(AiChatSessionDO::getAgentId, id));
        chatMessageMapper.delete(new LambdaQueryWrapperX<AiChatMessageDO>()
                .eq(AiChatMessageDO::getAgentId, id));
        agentMapper.deleteById(id);

        // 同步删除 QwenPaw agent
        try {
            qwenPawClient.deleteAgent(agent.getQwenpawAgentId());
        } catch (Exception e) {
            log.warn("[deleteAgent] QwenPaw 删除失败，agentId={}", agent.getQwenpawAgentId(), e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleAgent(Long id) {
        AiAgentDO agent = validateExists(id);
        boolean enabled = !Integer.valueOf(1).equals(agent.getStatus());
        // 同步 QwenPaw 后再更新本地
        qwenPawClient.toggleAgent(agent.getQwenpawAgentId(), enabled);
        AiAgentDO updateObj = new AiAgentDO();
        updateObj.setId(id);
        updateObj.setStatus(enabled ? 1 : 0);
        agentMapper.updateById(updateObj);
    }

    @Override
    public AiAgentDO getAgent(Long id) {
        return validateExists(id);
    }

    @Override
    public PageResult<AiAgentDO> getAgentPage(AgentPageReqVO pageReqVO) {
        return agentMapper.selectPage(pageReqVO);
    }

    @Override
    public List<AiAgentDO> getMyAgents() {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        if (userId == null) {
            return agentMapper.selectEnabledByUserId(0L);
        }
        return agentMapper.selectEnabledByUserId(userId);
    }

    @Override
    public AiAgentDO getAgentByQwenpawAgentId(String qwenpawAgentId) {
        return agentMapper.selectByQwenpawAgentId(qwenpawAgentId);
    }

    @Override
    public List<Map<String, Object>> listAgentTools(Long agentId) {
        AiAgentDO agent = validateExists(agentId);
        List<Map<String, Object>> tools = qwenPawClient.listTools(agent.getQwenpawAgentId());
        // QwenPaw 返回 snake_case，转换为前端 camelCase
        for (Map<String, Object> tool : tools) {
            normalizeToolMap(tool);
        }
        return tools;
    }

    @Override
    public Map<String, Object> toggleAgentTool(Long agentId, String toolName) {
        AiAgentDO agent = validateExists(agentId);
        Map<String, Object> result = qwenPawClient.toggleTool(agent.getQwenpawAgentId(), toolName);
        normalizeToolMap(result);
        return result;
    }

    @Override
    public Map<String, Object> getAgentToolConfig(Long agentId, String toolName) {
        AiAgentDO agent = validateExists(agentId);
        return qwenPawClient.getToolConfig(agent.getQwenpawAgentId(), toolName);
    }

    @Override
    public Map<String, Object> updateAgentToolConfig(Long agentId, String toolName, Map<String, Object> config) {
        AiAgentDO agent = validateExists(agentId);
        return qwenPawClient.updateToolConfig(agent.getQwenpawAgentId(), toolName, config);
    }

    @Override
    public Map<String, Object> getAgentStatus(Long agentId) {
        AiAgentDO agent = validateExists(agentId);
        return qwenPawClient.getAgentStatus(agent.getQwenpawAgentId());
    }

    @Override
    public List<Map<String, Object>> listAgentQwenpawMcps(Long agentId) {
        AiAgentDO agent = validateExists(agentId);
        return qwenPawClient.listAgentMcps(agent.getQwenpawAgentId());
    }

    @Override
    public Map<String, Object> toggleAgentQwenpawMcp(Long agentId, String clientKey) {
        AiAgentDO agent = validateExists(agentId);
        return qwenPawClient.toggleAgentMcp(agent.getQwenpawAgentId(), clientKey);
    }

    @Override
    public List<Map<String, Object>> listAgentMcpTools(Long agentId, String clientKey) {
        AiAgentDO agent = validateExists(agentId);
        return qwenPawClient.listMcpTools(agent.getQwenpawAgentId(), clientKey);
    }

    @Override
    public List<Map<String, Object>> listAgentQwenpawSkills(Long agentId) {
        AiAgentDO agent = validateExists(agentId);
        return qwenPawClient.listAgentSkills(agent.getQwenpawAgentId());
    }

    @Override
    public List<Map<String, Object>> listQwenpawSkillPool() {
        return qwenPawClient.listSkillPool();
    }

    @Override
    public Map<String, Object> getQwenpawSkillPoolDetail(String skillName) {
        return qwenPawClient.getSkillPoolDetail(skillName);
    }

    @Override
    public void installQwenpawSkill(Long agentId, String skillName) {
        AiAgentDO agent = validateExists(agentId);
        // 优先使用池下载安装，失败则降级为直接安装
        try {
            qwenPawClient.downloadPoolSkillToWorkspace(skillName, agent.getQwenpawAgentId(), false);
        } catch (Exception e) {
            log.warn("[installQwenpawSkill] 池下载安装失败，降级为直接安装，skillName={}, agentId={}",
                    skillName, agentId, e);
            qwenPawClient.installSkill(agent.getQwenpawAgentId(), skillName);
        }
    }

    @Override
    public void uninstallQwenpawSkill(Long agentId, String skillName) {
        AiAgentDO agent = validateExists(agentId);
        qwenPawClient.uninstallSkill(agent.getQwenpawAgentId(), skillName);
    }

    @Override
    public Map<String, Object> registerQwenpawMcp(Long agentId, String clientKey, String transport, String url,
                                                  String command, String commandArgs, String headersJson, String toolsJson) {
        AiAgentDO agent = validateExists(agentId);
        qwenPawClient.registerMcp(agent.getQwenpawAgentId(), clientKey, transport, url,
                command, commandArgs, headersJson, toolsJson);
        // 返回注册后的最新 MCP 列表，便于前端刷新
        return qwenPawClient.listAgentMcps(agent.getQwenpawAgentId()).stream()
                .filter(m -> clientKey.equals(m.get("client_key")))
                .findFirst().orElse(null);
    }

    @Override
    public void deleteQwenpawMcp(Long agentId, String clientKey) {
        AiAgentDO agent = validateExists(agentId);
        qwenPawClient.deleteMcp(agent.getQwenpawAgentId(), clientKey);
    }

    // ==================== 私有辅助方法 ====================

    private AiAgentDO validateExists(Long id) {
        AiAgentDO agent = agentMapper.selectById(id);
        if (agent == null) {
            throw exception(AGENT_NOT_EXISTS);
        }
        return agent;
    }

    private void validateNameUnique(Long userId, String name, Long excludeId) {
        AiAgentDO existing = agentMapper.selectByUserIdAndName(userId, name);
        if (existing != null && !existing.getId().equals(excludeId)) {
            throw exception(AGENT_NAME_EXISTS);
        }
    }

    private String buildQwenpawAgentId(Long userId) {
        return "u" + userId + "_" + java.util.UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * 将 QwenPaw 内置工具返回的 snake_case 字段归一化为 camelCase
     */
    private void normalizeToolMap(Map<String, Object> tool) {
        if (tool == null) {
            return;
        }
        Object asyncExecution = tool.remove("async_execution");
        if (asyncExecution != null) {
            tool.put("asyncExecution", asyncExecution);
        }
        Object requiresConfig = tool.remove("requires_config");
        if (requiresConfig != null) {
            tool.put("requiresConfig", requiresConfig);
        }
        Object configFields = tool.remove("config_fields");
        if (configFields != null) {
            tool.put("configFields", configFields);
        }
        Object configValues = tool.remove("config_values");
        if (configValues != null) {
            tool.put("configValues", configValues);
        }
    }

}
