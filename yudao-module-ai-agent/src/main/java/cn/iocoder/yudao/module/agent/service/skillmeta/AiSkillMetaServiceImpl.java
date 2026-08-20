package cn.iocoder.yudao.module.agent.service.skillmeta;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.security.core.service.SecurityFrameworkService;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.agent.controller.admin.skillmeta.vo.SkillMetaPageReqVO;
import cn.iocoder.yudao.module.agent.controller.admin.skillmeta.vo.SkillMetaSaveReqVO;
import cn.iocoder.yudao.module.agent.dal.dataobject.skillmeta.AiSkillMetaDO;
import cn.iocoder.yudao.module.agent.dal.mysql.skillmeta.AiSkillMetaMapper;
import cn.iocoder.yudao.module.agent.framework.config.QwenPawClient;
import cn.iocoder.yudao.module.system.enums.permission.RoleCodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.agent.enums.ErrorCodeConstants.SKILL_META_NOT_EXISTS;
import static cn.iocoder.yudao.module.agent.enums.ErrorCodeConstants.SKILL_META_NAME_EXISTS;
import static cn.iocoder.yudao.module.agent.enums.ErrorCodeConstants.SKILL_META_UPLOAD_FAILED;

/**
 * 技能商店 Service 实现类（QwenPaw 技能池元数据管理）
 *
 * @author 吴皓
 */
@Service
@Validated
@Slf4j
public class AiSkillMetaServiceImpl implements AiSkillMetaService {

    @Resource
    private AiSkillMetaMapper skillMetaMapper;

    @Resource
    private QwenPawClient qwenPawClient;

    @Resource
    private SecurityFrameworkService securityFrameworkService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSkillMeta(SkillMetaSaveReqVO createReqVO) {
        validateSkillNameUnique(createReqVO.getSkillName(), null);
        AiSkillMetaDO meta = BeanUtils.toBean(createReqVO, AiSkillMetaDO.class);
        if (meta.getVisibility() == null) {
            meta.setVisibility(1); // 默认公开
        }
        if (meta.getStatus() == null) {
            meta.setStatus(1);
        }
        if (meta.getSource() == null || meta.getSource().isEmpty()) {
            meta.setSource("customized");
        }
        skillMetaMapper.insert(meta);
        return meta.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSkillMeta(SkillMetaSaveReqVO updateReqVO) {
        validateExists(updateReqVO.getId());
        validateSkillNameUnique(updateReqVO.getSkillName(), updateReqVO.getId());
        AiSkillMetaDO updateObj = BeanUtils.toBean(updateReqVO, AiSkillMetaDO.class);
        skillMetaMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSkillMeta(Long id) {
        AiSkillMetaDO meta = validateExists(id);
        // 同时从 QwenPaw 技能池删除
        try {
            qwenPawClient.deletePoolSkill(meta.getSkillName());
        } catch (Exception e) {
            log.warn("[deleteSkillMeta] QwenPaw 技能池删除失败，skillName={}", meta.getSkillName(), e);
        }
        skillMetaMapper.deleteById(id);
    }

    @Override
    public AiSkillMetaDO getSkillMeta(Long id) {
        return validateExists(id);
    }

    @Override
    public PageResult<AiSkillMetaDO> getSkillMetaPage(SkillMetaPageReqVO pageReqVO) {
        // 用户级隔离：超管/租户管理员看全部，普通用户仅看公开 + 自己的
        if (isSuperAdmin()) {
            return skillMetaMapper.selectPage(pageReqVO);
        }
        return skillMetaMapper.selectVisiblePage(pageReqVO, SecurityFrameworkUtils.getLoginUserId());
    }

    @Override
    public List<AiSkillMetaDO> getVisibleSkillMetaList(Long userId) {
        // 用户级隔离：超管/租户管理员看全部启用的，普通用户仅看公开 + 自己的
        if (isSuperAdmin()) {
            return skillMetaMapper.selectEnabledList();
        }
        return skillMetaMapper.selectVisibleList(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long uploadSkillToPool(byte[] data, String fileName, String targetName,
                                  String displayName, String description, String icon,
                                  Integer visibility, Long ownerUserId, String tags) {
        // 1. 上传到 QwenPaw 技能池
        Map<String, Object> result;
        try {
            result = qwenPawClient.uploadToPoolZip(data, fileName, targetName);
        } catch (Exception e) {
            log.error("[uploadSkillToPool] QwenPaw 上传失败", e);
            throw exception(SKILL_META_UPLOAD_FAILED);
        }

        // 2. 解析上传结果，获取实际技能名
        String actualSkillName = targetName;
        if (actualSkillName == null || actualSkillName.isEmpty()) {
            // 从返回结果中提取
            Object imported = result.get("imported");
            if (imported instanceof List && !((List<?>) imported).isEmpty()) {
                actualSkillName = String.valueOf(((List<?>) imported).get(0));
            }
        }
        if (actualSkillName == null || actualSkillName.isEmpty()) {
            log.error("[uploadSkillToPool] 无法确定上传后的技能名，result={}", result);
            throw exception(SKILL_META_UPLOAD_FAILED);
        }

        // 3. 创建 Java 侧元数据记录
        AiSkillMetaDO meta = AiSkillMetaDO.builder()
                .skillName(actualSkillName)
                .displayName(displayName != null && !displayName.isEmpty() ? displayName : actualSkillName)
                .description(description)
                .icon(icon)
                .source("customized")
                .visibility(visibility != null ? visibility : 1)
                .ownerUserId(ownerUserId)
                .tags(tags)
                .status(1)
                .build();
        skillMetaMapper.insert(meta);
        return meta.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int syncFromQwenPaw() {
        // 1. 获取 QwenPaw 技能池列表
        List<Map<String, Object>> poolSkills = qwenPawClient.listSkillPool();
        int synced = 0;

        for (Map<String, Object> poolSkill : poolSkills) {
            String skillName = String.valueOf(poolSkill.getOrDefault("name", ""));
            if (skillName.isEmpty()) {
                continue;
            }

            // 2. 检查 Java 侧是否已有记录
            AiSkillMetaDO existing = skillMetaMapper.selectBySkillName(skillName);
            if (existing != null) {
                // 更新 source/version 等同步字段
                AiSkillMetaDO updateObj = new AiSkillMetaDO();
                updateObj.setId(existing.getId());
                updateObj.setSource(String.valueOf(poolSkill.getOrDefault("source", "customized")));
                Object versionText = poolSkill.get("version_text");
                if (versionText != null) {
                    updateObj.setVersion(String.valueOf(versionText));
                }
                skillMetaMapper.updateById(updateObj);
                continue;
            }

            // 3. 创建默认记录（builtin 技能默认公开，ownerUserId=null 表示系统）
            AiSkillMetaDO meta = AiSkillMetaDO.builder()
                    .skillName(skillName)
                    .displayName(String.valueOf(poolSkill.getOrDefault("name", skillName)))
                    .description(String.valueOf(poolSkill.getOrDefault("description", "")).isEmpty()
                            ? "" : String.valueOf(poolSkill.get("description")))
                    .icon(String.valueOf(poolSkill.getOrDefault("emoji", "")))
                    .source(String.valueOf(poolSkill.getOrDefault("source", "customized")))
                    .visibility("builtin".equals(poolSkill.get("source")) ? 1 : 1) // 默认公开
                    .status(1)
                    .build();
            Object versionText = poolSkill.get("version_text");
            if (versionText != null) {
                meta.setVersion(String.valueOf(versionText));
            }
            skillMetaMapper.insert(meta);
            synced++;
        }

        return synced;
    }

    @Override
    public void installSkillFromPool(String skillName, String qwenpawAgentId) {
        // 通过 QwenPaw pool/download 将技能安装到智能体工作区
        try {
            qwenPawClient.downloadPoolSkillToWorkspace(skillName, qwenpawAgentId, false);
        } catch (Exception e) {
            log.warn("[installSkillFromPool] 池下载安装失败，尝试直接安装，skillName={}, agentId={}",
                    skillName, qwenpawAgentId, e);
            // 降级：直接调用 installSkill
            qwenPawClient.installSkill(qwenpawAgentId, skillName);
        }
    }

    // ==================== 私有辅助方法 ====================

    private AiSkillMetaDO validateExists(Long id) {
        AiSkillMetaDO meta = skillMetaMapper.selectById(id);
        if (meta == null) {
            throw exception(SKILL_META_NOT_EXISTS);
        }
        return meta;
    }

    /**
     * 是否超管/租户管理员（可见全部数据）
     */
    private boolean isSuperAdmin() {
        return securityFrameworkService.hasAnyRoles(
                RoleCodeEnum.SUPER_ADMIN.getCode(),
                RoleCodeEnum.TENANT_ADMIN.getCode());
    }

    private void validateSkillNameUnique(String skillName, Long excludeId) {
        AiSkillMetaDO existing = skillMetaMapper.selectBySkillName(skillName);
        if (existing != null && !existing.getId().equals(excludeId)) {
            throw exception(SKILL_META_NAME_EXISTS);
        }
    }

}
