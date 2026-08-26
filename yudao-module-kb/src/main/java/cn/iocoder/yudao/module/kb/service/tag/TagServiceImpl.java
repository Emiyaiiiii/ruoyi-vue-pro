package cn.iocoder.yudao.module.kb.service.tag;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.security.core.service.SecurityFrameworkService;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.kb.controller.admin.tag.vo.TagPageReqVO;
import cn.iocoder.yudao.module.kb.controller.admin.tag.vo.TagSaveReqVO;
import cn.iocoder.yudao.module.kb.dal.dataobject.tag.TagDO;
import cn.iocoder.yudao.module.kb.dal.mysql.tag.TagMapper;
import cn.iocoder.yudao.module.system.enums.permission.RoleCodeEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.kb.enums.ErrorCodeConstants.*;

/**
 * 标签 Service 实现类
 *
 * <p>可见性规则对齐 Python 端 {@code apis/knowledge/tags/views.py}：
 * <ul>
 *   <li>创建：管理员且 {@code isGlobal=true} → 全局（owner=null），否则 → 本人标签；</li>
 *   <li>更新：管理员可改可见范围（全局 owner=null，个人归原归属人或当前管理员）；非管理员忽略 isGlobal；</li>
 *   <li>删除/更新权限：管理员可操作全部标签，普通用户仅可操作本人标签；</li>
 *   <li>列表：管理员可见全部，普通用户可见「本人 + 全局」。</li>
 * </ul>
 *
 * @author 吴皓
 */
@Service
@Validated
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final TagMapper tagMapper;
    private final SecurityFrameworkService securityFrameworkService;

    @Override
    public Long createTag(TagSaveReqVO reqVO) {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        // 归属：管理员 + isGlobal=true → 全局；否则本人标签（非管理员的 isGlobal 被静默忽略）
        Long ownerId = Boolean.TRUE.equals(reqVO.getIsGlobal()) && isAdmin() ? null : userId;
        validateNameUnique(null, reqVO.getName(), ownerId);

        TagDO tag = BeanUtils.toBean(reqVO, TagDO.class);
        tag.setOwnerId(ownerId);
        if (tag.getColor() == null || tag.getColor().isEmpty()) {
            tag.setColor("#007bff");
        }
        if (tag.getType() == null || tag.getType().isEmpty()) {
            tag.setType("other");
        }
        tagMapper.insert(tag);
        return tag.getId();
    }

    @Override
    public void updateTag(TagSaveReqVO reqVO) {
        TagDO tag = validateExists(reqVO.getId());
        checkManagePermission(tag);

        Long ownerId = resolveOwnerIdOnUpdate(reqVO, tag);
        validateNameUnique(reqVO.getId(), reqVO.getName(), ownerId);

        TagDO updateObj = BeanUtils.toBean(reqVO, TagDO.class);
        updateObj.setOwnerId(ownerId);
        tagMapper.updateById(updateObj);
    }

    @Override
    public void deleteTag(Long id) {
        TagDO tag = validateExists(id);
        checkManagePermission(tag);
        tagMapper.deleteById(id);
    }

    @Override
    public TagDO getTag(Long id) {
        return tagMapper.selectById(id);
    }

    @Override
    public PageResult<TagDO> getTagPage(TagPageReqVO reqVO) {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        boolean admin = isAdmin();
        LambdaQueryWrapperX<TagDO> wrapper = new LambdaQueryWrapperX<TagDO>()
                .likeIfPresent(TagDO::getName, reqVO.getName())
                .eqIfPresent(TagDO::getType, reqVO.getType());
        // 归属范围过滤
        String scope = reqVO.getScope();
        if ("global".equals(scope)) {
            wrapper.isNull(TagDO::getOwnerId);
        } else if ("personal".equals(scope)) {
            wrapper.eq(TagDO::getOwnerId, userId);
        } else if (!admin) {
            // 普通用户默认仅可见「本人 + 全局」
            wrapper.and(w -> w.eq(TagDO::getOwnerId, userId).or().isNull(TagDO::getOwnerId));
        }
        wrapper.orderByDesc(TagDO::getId);
        return tagMapper.selectPage(reqVO, wrapper);
    }

    @Override
    public List<TagDO> getTagList(String type) {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        LambdaQueryWrapperX<TagDO> wrapper = new LambdaQueryWrapperX<TagDO>()
                .eqIfPresent(TagDO::getType, type);
        if (!isAdmin()) {
            wrapper.and(w -> w.eq(TagDO::getOwnerId, userId).or().isNull(TagDO::getOwnerId));
        }
        wrapper.orderByDesc(TagDO::getId);
        return tagMapper.selectList(wrapper);
    }

    // ---------- 内部逻辑 ----------

    private boolean isAdmin() {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        if (userId == null) {
            return false;
        }
        return securityFrameworkService.hasAnyRoles(
                RoleCodeEnum.SUPER_ADMIN.getCode(),
                RoleCodeEnum.TENANT_ADMIN.getCode());
    }

    private TagDO validateExists(Long id) {
        TagDO tag = tagMapper.selectById(id);
        if (tag == null) {
            throw exception(TAG_NOT_EXISTS);
        }
        return tag;
    }

    /**
     * 管理权限校验：管理员可操作全部标签；普通用户仅可操作本人标签（不可改全局标签）。
     */
    private void checkManagePermission(TagDO tag) {
        if (isAdmin()) {
            return;
        }
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        if (tag.getOwnerId() == null || !Objects.equals(tag.getOwnerId(), userId)) {
            throw exception(TAG_PERMISSION_DENIED);
        }
    }

    /**
     * 更新时解析归属。管理员可根据 {@code isGlobal} 在全局/个人之间切换；
     * 未传 isGlobal 或非管理员则保持原归属。
     */
    private Long resolveOwnerIdOnUpdate(TagSaveReqVO reqVO, TagDO existing) {
        Boolean isGlobal = reqVO.getIsGlobal();
        if (isGlobal == null || !isAdmin()) {
            return existing.getOwnerId();
        }
        if (Boolean.TRUE.equals(isGlobal)) {
            return null;
        }
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        return existing.getOwnerId() != null ? existing.getOwnerId() : userId;
    }

    /**
     * 作用域内名称唯一校验（全局 / 个人各自独立，对齐 Python (name, owner) 唯一）
     */
    private void validateNameUnique(Long excludeId, String name, Long ownerId) {
        TagDO exist = tagMapper.selectByNameAndOwner(name, ownerId);
        if (exist == null) {
            return;
        }
        if (excludeId == null || !Objects.equals(exist.getId(), excludeId)) {
            throw exception(TAG_NAME_DUPLICATE);
        }
    }

}