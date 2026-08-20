package cn.iocoder.yudao.module.agent.service.mcpmeta;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.security.core.service.SecurityFrameworkService;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.agent.controller.admin.mcpmeta.vo.McpMetaPageReqVO;
import cn.iocoder.yudao.module.agent.controller.admin.mcpmeta.vo.McpMetaSaveReqVO;
import cn.iocoder.yudao.module.agent.dal.dataobject.mcpmeta.AiMcpMetaDO;
import cn.iocoder.yudao.module.agent.dal.mysql.mcpmeta.AiMcpMetaMapper;
import cn.iocoder.yudao.module.system.enums.permission.RoleCodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.agent.enums.ErrorCodeConstants.MCP_META_CODE_EXISTS;
import static cn.iocoder.yudao.module.agent.enums.ErrorCodeConstants.MCP_META_NOT_EXISTS;

/**
 * MCP 商店 Service 实现类
 *
 * @author 吴皓
 */
@Service
@Validated
@Slf4j
public class AiMcpMetaServiceImpl implements AiMcpMetaService {

    @Resource
    private AiMcpMetaMapper mcpMetaMapper;
    @Resource
    private SecurityFrameworkService securityFrameworkService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createMcpMeta(McpMetaSaveReqVO createReqVO) {
        validateCodeUnique(createReqVO.getCode(), null);
        AiMcpMetaDO meta = BeanUtils.toBean(createReqVO, AiMcpMetaDO.class);
        if (meta.getType() == null) {
            meta.setType(0);
        }
        if (meta.getStatus() == null) {
            meta.setStatus(1);
        }
        if (meta.getSortOrder() == null) {
            meta.setSortOrder(0);
        }
        // 记录创建者用户（个人 MCP 的归属，用于用户级隔离）
        meta.setOwnerUserId(SecurityFrameworkUtils.getLoginUserId());
        mcpMetaMapper.insert(meta);
        return meta.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMcpMeta(McpMetaSaveReqVO updateReqVO) {
        validateExists(updateReqVO.getId());
        validateCodeUnique(updateReqVO.getCode(), updateReqVO.getId());
        AiMcpMetaDO updateObj = BeanUtils.toBean(updateReqVO, AiMcpMetaDO.class);
        mcpMetaMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMcpMeta(Long id) {
        validateExists(id);
        mcpMetaMapper.deleteById(id);
    }

    @Override
    public AiMcpMetaDO getMcpMeta(Long id) {
        return validateExists(id);
    }

    @Override
    public PageResult<AiMcpMetaDO> getMcpMetaPage(McpMetaPageReqVO pageReqVO) {
        // 用户级隔离：超管/租户管理员看全部，普通用户仅看公开 + 自己的
        if (isSuperAdmin()) {
            return mcpMetaMapper.selectPage(pageReqVO);
        }
        return mcpMetaMapper.selectVisiblePage(pageReqVO, SecurityFrameworkUtils.getLoginUserId());
    }

    @Override
    public List<AiMcpMetaDO> getEnabledMcpMetaList() {
        return mcpMetaMapper.selectEnabledList();
    }

    @Override
    public List<AiMcpMetaDO> getVisibleMcpMetaList() {
        // 用户级隔离：超管/租户管理员看全部启用的，普通用户仅看公开 + 自己的
        if (isSuperAdmin()) {
            return mcpMetaMapper.selectEnabledList();
        }
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        return mcpMetaMapper.selectVisibleList(userId);
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 是否超管/租户管理员（可见全部数据）
     */
    private boolean isSuperAdmin() {
        return securityFrameworkService.hasAnyRoles(
                RoleCodeEnum.SUPER_ADMIN.getCode(),
                RoleCodeEnum.TENANT_ADMIN.getCode());
    }

    private AiMcpMetaDO validateExists(Long id) {
        AiMcpMetaDO meta = mcpMetaMapper.selectById(id);
        if (meta == null) {
            throw exception(MCP_META_NOT_EXISTS);
        }
        return meta;
    }

    private void validateCodeUnique(String code, Long excludeId) {
        AiMcpMetaDO existing = mcpMetaMapper.selectByCode(code);
        if (existing != null && !existing.getId().equals(excludeId)) {
            throw exception(MCP_META_CODE_EXISTS);
        }
    }

}
