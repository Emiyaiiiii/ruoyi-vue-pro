package cn.iocoder.yudao.module.kb.service.library;

import cn.hutool.core.collection.CollUtil;
import lombok.extern.slf4j.Slf4j;
import cn.iocoder.yudao.framework.security.core.service.SecurityFrameworkService;
import cn.iocoder.yudao.module.kb.dal.dataobject.category.CategoryDO;
import cn.iocoder.yudao.module.kb.dal.dataobject.levelconfig.LevelConfigDO;
import cn.iocoder.yudao.module.kb.dal.dataobject.library.LibraryDO;
import cn.iocoder.yudao.module.kb.dal.dataobject.sharedept.ShareDeptDO;
import cn.iocoder.yudao.module.kb.dal.mysql.category.CategoryMapper;
import cn.iocoder.yudao.module.kb.dal.mysql.levelconfig.LevelConfigMapper;
import cn.iocoder.yudao.module.kb.dal.mysql.sharedept.ShareDeptMapper;
import cn.iocoder.yudao.module.kb.service.projectmember.ProjectMemberService;
import cn.iocoder.yudao.module.kb.service.userdept.KbUserDeptService;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.system.enums.permission.RoleCodeEnum;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import cn.iocoder.yudao.module.kb.controller.admin.library.vo.*;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;

import cn.iocoder.yudao.module.kb.dal.mysql.library.LibraryMapper;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.kb.enums.ErrorCodeConstants.*;

/**
 * 知识库 Service 实现类
 *
 * @author 吴皓
 */
@Service
@Validated
@Slf4j
public class LibraryServiceImpl implements LibraryService {

    @Resource
    private LibraryMapper libraryMapper;

    @Resource
    private ShareDeptMapper shareDeptMapper;

    @Resource
    private CategoryMapper categoryMapper;

    @Resource
    private LevelConfigMapper levelConfigMapper;

    @Resource
    private ProjectMemberService projectMemberService;

    @Resource
    private KbUserDeptService kbUserDeptService;

    @Resource
    private SecurityFrameworkService securityFrameworkService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createLibrary(LibrarySaveReqVO createReqVO) {
        // 校验权限：非超管用户只能创建自己有操作权限的知识库
        LevelConfigDO cfg = null;
        if (createReqVO.getKbLevelId() != null) {
            cfg = levelConfigMapper.selectById(createReqVO.getKbLevelId());
        }
        validateManagementPermission(cfg, createReqVO.getOwnerId());

        // 插入
        LibraryDO library = BeanUtils.toBean(createReqVO, LibraryDO.class);
        libraryMapper.insert(library);

        // 保存共享部门关联（仅共享知识库需要）
        if (createReqVO.getShareDeptIds() != null) {
            shareDeptMapper.delete(new LambdaQueryWrapper<ShareDeptDO>()
                    .eq(ShareDeptDO::getKbId, library.getId()));

            if (CollUtil.isNotEmpty(createReqVO.getShareDeptIds())) {
                List<ShareDeptDO> list = createReqVO.getShareDeptIds().stream()
                        .map(deptId -> new ShareDeptDO()
                                .setKbId(library.getId())
                                .setDeptId(deptId))
                        .collect(Collectors.toList());
                shareDeptMapper.insertBatch(list);
            }
        }
        return library.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateLibrary(LibrarySaveReqVO updateReqVO) {
        // 校验存在
        LibraryDO library = validateLibraryExists(updateReqVO.getId());
        // 校验管理权限（统一走 validateManagementPermission）
        LevelConfigDO cfg = levelConfigMapper.selectById(library.getKbLevelId());
        validateManagementPermission(cfg, library.getOwnerId());

        // 更新
        LibraryDO updateObj = BeanUtils.toBean(updateReqVO, LibraryDO.class);
        libraryMapper.updateById(updateObj);

        // 保存共享部门关联
        if (updateReqVO.getShareDeptIds() != null) {
            shareDeptMapper.delete(new LambdaQueryWrapper<ShareDeptDO>()
                    .eq(ShareDeptDO::getKbId, updateObj.getId()));

            if (CollUtil.isNotEmpty(updateReqVO.getShareDeptIds())) {
                List<ShareDeptDO> list = updateReqVO.getShareDeptIds().stream()
                        .map(deptId -> new ShareDeptDO()
                                .setKbId(updateObj.getId())
                                .setDeptId(deptId))
                        .collect(Collectors.toList());
                shareDeptMapper.insertBatch(list);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteLibrary(Long id) {
        // 校验存在
        LibraryDO library = validateLibraryExists(id);
        // 校验管理权限（统一走 validateManagementPermission）
        LevelConfigDO cfg = levelConfigMapper.selectById(library.getKbLevelId());
        validateManagementPermission(cfg, library.getOwnerId());

        // 删除关联的共享部门记录
        shareDeptMapper.delete(new LambdaQueryWrapper<ShareDeptDO>()
                .eq(ShareDeptDO::getKbId, id));
        // 删除关联的项目成员记录
        projectMemberService.removeAllByKbId(id);
        // 删除
        libraryMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteLibraryListByIds(List<Long> ids) {
        // 先全部校验权限
        for (Long id : ids) {
            LibraryDO library = libraryMapper.selectById(id);
            if (library != null) {
                LevelConfigDO cfg = levelConfigMapper.selectById(library.getKbLevelId());
                validateManagementPermission(cfg, library.getOwnerId());
            }
        }
        // 再统一删除关联的共享部门记录和项目成员记录
        for (Long id : ids) {
            shareDeptMapper.delete(new LambdaQueryWrapper<ShareDeptDO>()
                    .eq(ShareDeptDO::getKbId, id));
            projectMemberService.removeAllByKbId(id);
        }
        // 最后批量删除知识库
        libraryMapper.deleteByIds(ids);
    }

    /**
     * 统一的权限校验入口（所有知识库操作都用此函数）
     * 个人知识库(visibilityRule=1)：仅所有者本人
     * 院级/咨询评估(visibilityRule=2)：该部门（或其祖先部门）管理员
     * 公司级(visibilityRule=3)：公司部门管理员
     * 指定部门(visibilityRule=5)：按 ownerDim 判断，部门归部门管理员，用户归所有者
     */
    private void validateManagementPermission(LevelConfigDO cfg, Long ownerId) {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        if (userId == null) {
            log.debug("[管理权限] 用户未登录 → 拒绝");
            throw exception(LIBRARY_PERMISSION_DENIED);
        }

        // 超级管理员/租户管理员 → 拥有所有权限
        if (securityFrameworkService.hasAnyRoles(
                RoleCodeEnum.SUPER_ADMIN.getCode(),
                RoleCodeEnum.TENANT_ADMIN.getCode())) {
            log.debug("[管理权限] userId={} 是超管/租管 → 放行", userId);
            return;
        }

        // 配置不存在则不做额外校验（由 API 层权限控制兜底）
        if (cfg == null) {
            log.debug("[管理权限] userId={} 配置为空 → 放行（API层兜底）", userId);
            return;
        }

        Integer rule = cfg.getVisibilityRule();
        if (rule == null) {
            log.debug("[管理权限] userId={} visibilityRule为空 → 放行", userId);
            return;
        }

        switch (rule) {
            case 1:   // 个人知识库 → 仅所有者本人
                if (ownerId == null || !ownerId.equals(userId)) {
                    log.debug("[管理权限] userId={} rule=1(个人) ownerId={} 非所有者 → 拒绝", userId, ownerId);
                    throw exception(LIBRARY_PERMISSION_DENIED);
                }
                log.debug("[管理权限] userId={} rule=1(个人) ownerId={} 是所有者 → 放行", userId, ownerId);
                break;
            case 2:   // 院级/咨询评估 → 管理员在 ownerId 或其祖先部门上
            case 3:   // 公司级
                if (ownerId == null) {
                    log.debug("[管理权限] userId={} rule={}(院级/公司) ownerId为空 → 拒绝", userId, rule);
                    throw exception(LIBRARY_PERMISSION_DENIED);
                }
                // 精确匹配
                if (kbUserDeptService.isAdmin(userId, ownerId)) {
                    log.debug("[管理权限] userId={} rule={}(院级/公司) ownerId={} 精确匹配管理员 → 放行", userId, rule, ownerId);
                    break;
                }
                // 向上查找：管理员在父部门可管理子部门库（如水利院管理员可管理规划处的库）
                boolean found = false;
                for (Long ancestorId : kbUserDeptService.getDeptAncestorIds(ownerId)) {
                    if (kbUserDeptService.isAdmin(userId, ancestorId)) {
                        found = true;
                        log.debug("[管理权限] userId={} rule={}(院级/公司) ownerId={} 祖先部门{}是管理员 → 放行", userId, rule, ownerId, ancestorId);
                        break;
                    }
                }
                if (!found) {
                    log.debug("[管理权限] userId={} rule={}(院级/公司) ownerId={} 非管理员 → 拒绝", userId, rule, ownerId);
                    throw exception(LIBRARY_PERMISSION_DENIED);
                }
                break;
            case 5:   // 指定部门列表 → 根据 ownerDim 决定
                if (cfg.getOwnerDim() != null && cfg.getOwnerDim() == 1) {
                    // 按用户归属 → 仅所有者
                    if (ownerId == null || !ownerId.equals(userId)) {
                        log.debug("[管理权限] userId={} rule=5 ownerDim=1(用户) ownerId={} 非所有者 → 拒绝", userId, ownerId);
                        throw exception(LIBRARY_PERMISSION_DENIED);
                    }
                    log.debug("[管理权限] userId={} rule=5 ownerDim=1(用户) ownerId={} 是所有者 → 放行", userId, ownerId);
                } else {
                    // 按部门归属 → 该部门管理员（含祖先部门）
                    if (ownerId == null) {
                        log.debug("[管理权限] userId={} rule=5 ownerDim={}(部门) ownerId为空 → 拒绝", userId, cfg.getOwnerDim());
                        throw exception(LIBRARY_PERMISSION_DENIED);
                    }
                    // 精确匹配
                    if (kbUserDeptService.isAdmin(userId, ownerId)) {
                        log.debug("[管理权限] userId={} rule=5 ownerDim={}(部门) ownerId={} 精确匹配管理员 → 放行", userId, cfg.getOwnerDim(), ownerId);
                        break;
                    }
                    // 向上查找祖先部门管理员
                    boolean foundDept = false;
                    for (Long ancestorId : kbUserDeptService.getDeptAncestorIds(ownerId)) {
                        if (kbUserDeptService.isAdmin(userId, ancestorId)) {
                            foundDept = true;
                            log.debug("[管理权限] userId={} rule=5 ownerDim={}(部门) ownerId={} 祖先部门{}是管理员 → 放行", userId, cfg.getOwnerDim(), ownerId, ancestorId);
                            break;
                        }
                    }
                    if (!foundDept) {
                        log.debug("[管理权限] userId={} rule=5 ownerDim={}(部门) ownerId={} 非管理员 → 拒绝", userId, cfg.getOwnerDim(), ownerId);
                        throw exception(LIBRARY_PERMISSION_DENIED);
                    }
                }
                break;
        }
    }

    /**
     * 可见性过滤：筛选出当前用户有权限看到的知识库
     * 个人知识库(rule=1)：仅所有者可见
     * 院级/咨询评估(rule=2)：用户所属部门+祖先部门范围内可见
     * 公司级(rule=3)：全员可见
     * 指定部门(rule=5)：共享部门列表中任一部门的成员可见
     *
     * 特殊处理：公司级项目库视图（categoryId 对应的分类 visibilityRule=3）时，
     * 所有 isProject=1 的知识库都可见，聚合下级部门创建的项目库
     */
    private List<LibraryDO> filterVisible(List<LibraryDO> allLibs, Long userId, Long categoryId) {
        if (CollUtil.isEmpty(allLibs)) return allLibs;

        // 超级管理员/租户管理员 → 可见所有知识库
        if (userId != null && securityFrameworkService.hasAnyRoles(
                RoleCodeEnum.SUPER_ADMIN.getCode(),
                RoleCodeEnum.TENANT_ADMIN.getCode())) {
            log.debug("[可见性] userId={} 是超管/租管 → 全部可见", userId);
            return allLibs;
        }

        // 加载所有层级配置到 Map
        Map<Long, LevelConfigDO> configMap = levelConfigMapper.selectList()
                .stream()
                .collect(Collectors.toMap(LevelConfigDO::getId, Function.identity()));

        // 判断当前视图是否为公司级分类（用于项目库聚合）
        final boolean isCompanyLevelView = isCompanyLevelProjectView(categoryId);
        // 判断当前视图是否为知识库广场
        final boolean isSquareView = isSquareCategoryView(categoryId);

        // 预加载用户关联的所有部门ID（系统部门 + kb_user_dept 扩展部门）
        Set<Long> userDeptIds = kbUserDeptService.getDeptIdsByUserId(userId);

        // 预计算用户可见的部门范围 = 用户所属部门 + 所有祖先部门（三级部门可见其父院创建的库）
        Set<Long> visibleDeptIds = new HashSet<>(userDeptIds);
        for (Long deptId : userDeptIds) {
            visibleDeptIds.addAll(kbUserDeptService.getDeptAncestorIds(deptId));
        }

        log.debug("[可见性] userId={} categoryId={} 公司视图={} 广场视图={} 待过滤总数={} userDeptIds={} visibleDeptIds={}",
                userId, categoryId, isCompanyLevelView, isSquareView, allLibs.size(), userDeptIds, visibleDeptIds);

        return allLibs.stream()
                .filter(lib -> {
                    // 公司级项目库视图：聚合所有项目成果库（含下级部门创建的）
                    if (isCompanyLevelView && lib.getIsProject() != null && lib.getIsProject() == 1) {
                        log.debug("[可见性] libId={} name={} 公司项目库视图 isProject=1 → 可见", lib.getId(), lib.getName());
                        return true;
                    }

                    // 知识库广场视图：所有 isPublic=1 的知识库全员可见
                    if (isSquareView) {
                        boolean visible = lib.getIsPublic() != null && lib.getIsPublic() == 1;
                        log.debug("[可见性] libId={} name={} 广场视图 isPublic={} → {}", lib.getId(), lib.getName(), lib.getIsPublic(), visible ? "可见" : "不可见");
                        return visible;
                    }

                    LevelConfigDO cfg = configMap.get(lib.getKbLevelId());
                    if (cfg == null) {
                        log.debug("[可见性] libId={} name={} kbLevelId={} 配置不存在 → 不可见", lib.getId(), lib.getName(), lib.getKbLevelId());
                        return false;
                    }

                    int rule = cfg.getVisibilityRule() != null ? cfg.getVisibilityRule() : 0;
                    switch (rule) {
                        case 1:   // 个人知识库 → 只有所有者可见
                            if (lib.getOwnerId() != null && lib.getOwnerId().equals(userId)) {
                                log.debug("[可见性] libId={} name={} rule=1(个人) ownerId={} 是所有者 → 可见", lib.getId(), lib.getName(), lib.getOwnerId());
                                return true;
                            }
                            log.debug("[可见性] libId={} name={} rule=1(个人) ownerId={} 非所有者(userId={}) → 不可见", lib.getId(), lib.getName(), lib.getOwnerId(), userId);
                            return false;

                        case 2:   // 院级 / 咨询评估 → 用户所属部门+祖先部门范围内可见
                            if (lib.getOwnerId() != null && visibleDeptIds.contains(lib.getOwnerId())) {
                                log.debug("[可见性] libId={} name={} rule=2(院级) ownerId={} 在可见部门范围 → 可见", lib.getId(), lib.getName(), lib.getOwnerId());
                                return true;
                            }
                            log.debug("[可见性] libId={} name={} rule=2(院级) ownerId={} 不在可见部门范围 → 不可见", lib.getId(), lib.getName(), lib.getOwnerId());
                            return false;

                        case 3:   // 公司知识库 → 全员可见
                            log.debug("[可见性] libId={} name={} rule=3(公司) → 可见", lib.getId(), lib.getName());
                            return true;

                        case 5:   // 指定部门列表 → 用户任一部门在共享列表中
                            if (CollUtil.isEmpty(userDeptIds)) {
                                log.debug("[可见性] libId={} name={} rule=5(指定部门) userDeptIds为空 → 不可见", lib.getId(), lib.getName());
                                return false;
                            }
                            long shareCount = shareDeptMapper.selectCount(
                                    new LambdaQueryWrapper<ShareDeptDO>()
                                            .eq(ShareDeptDO::getKbId, lib.getId())
                                            .in(ShareDeptDO::getDeptId, userDeptIds)
                            );
                            if (shareCount > 0) {
                                log.debug("[可见性] libId={} name={} rule=5(指定部门) 共享匹配数={} → 可见", lib.getId(), lib.getName(), shareCount);
                                return true;
                            }
                            log.debug("[可见性] libId={} name={} rule=5(指定部门) 无共享匹配 → 不可见", lib.getId(), lib.getName());
                            return false;

                        default:
                            log.debug("[可见性] libId={} name={} rule={} 未知规则 → 不可见", lib.getId(), lib.getName(), rule);
                            return false;
                    }
                })
                .collect(Collectors.toList());
    }

    /**
     * 判断当前分类是否为公司级项目库视图
     * 公司级项目库需要聚合所有下级部门的项目成果库
     */
    private boolean isCompanyLevelProjectView(Long categoryId) {
        Integer rule = getCategoryViewRule(categoryId);
        return rule != null && rule == 3;
    }

    /**
     * 判断当前分类是否为知识库广场视图
     * 广场视图下所有 isPublic=1 的知识库全员可见
     */
    private boolean isSquareCategoryView(Long categoryId) {
        Integer rule = getCategoryViewRule(categoryId);
        return rule != null && rule == 6;
    }

    private LibraryDO validateLibraryExists(Long id) {
        LibraryDO library = libraryMapper.selectById(id);
        if (library == null) {
            throw exception(LIBRARY_NOT_EXISTS);
        }
        return library;
    }

    @Override
    public LibraryDO getLibrary(Long id) {
        return libraryMapper.selectById(id);
    }

    @Override
    public PageResult<LibraryDO> getLibraryPage(LibraryPageReqVO pageReqVO) {
        // 保存原始 categoryId，广场分类需要置空 categoryId 查全量 isPublic=1
        Long originalCategoryId = pageReqVO.getCategoryId();
        if (isSquareCategoryView(originalCategoryId)) {
            pageReqVO.setCategoryId(null);
            pageReqVO.setIsPublic(1);
        }

        PageResult<LibraryDO> pageResult = libraryMapper.selectPage(pageReqVO);

        // 可见性过滤：非超管/租户管理员只能看到自己有权限的知识库
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        if (userId != null && !securityFrameworkService.hasAnyRoles(
                RoleCodeEnum.SUPER_ADMIN.getCode(),
                RoleCodeEnum.TENANT_ADMIN.getCode())) {
            List<LibraryDO> visible = filterVisible(pageResult.getList(), userId, originalCategoryId);
            log.debug("[分页查询] userId={} categoryId={} SQL总数={} 可见后={}", userId, originalCategoryId, pageResult.getTotal(), visible.size());
            pageResult.setList(visible);
        }

        return pageResult;
    }

    /**
     * 获取分类对应的可见规则（用于视图模式判断）
     *
     * @param categoryId 分类ID，null 则返回 null
     * @return 可见规则值，或 null
     */
    private Integer getCategoryViewRule(Long categoryId) {
        if (categoryId == null) return null;
        CategoryDO category = categoryMapper.selectById(categoryId);
        if (category == null || category.getKbLevelId() == null) return null;
        LevelConfigDO cfg = levelConfigMapper.selectById(category.getKbLevelId());
        return cfg != null ? cfg.getVisibilityRule() : null;
    }

    @Override
    public void togglePublic(Long id) {
        LibraryDO library = libraryMapper.selectById(id);
        if (library == null) {
            throw exception(LIBRARY_NOT_EXISTS);
        }
        // 只有个人知识库（rule=1）才能设置公开
        LevelConfigDO cfg = levelConfigMapper.selectById(library.getKbLevelId());
        if (cfg == null || cfg.getVisibilityRule() == null || cfg.getVisibilityRule() != 1) {
            throw exception(LIBRARY_PERMISSION_DENIED);
        }
        // 统一走 validateManagementPermission 校验所有者权限
        validateManagementPermission(cfg, library.getOwnerId());
        library.setIsPublic(library.getIsPublic() != null && library.getIsPublic() == 1 ? 0 : 1);
        libraryMapper.updateById(library);
    }

    @Override
    public PageResult<LibraryDO> getPublicPage(PageParam pageParam) {
        return libraryMapper.selectPublicPage(pageParam, null);
    }

    @Override
    public PageResult<LibraryDO> getMyPublicPage(PageParam pageParam, Long userId) {
        return libraryMapper.selectMyPublicPage(pageParam, userId);
    }

    @Override
    public List<LibraryDO> getSimpleLibraryList(Integer isProject) {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        List<LibraryDO> list = libraryMapper.selectSimpleList(isProject);
        if (userId != null && !securityFrameworkService.hasAnyRoles(
                RoleCodeEnum.SUPER_ADMIN.getCode(),
                RoleCodeEnum.TENANT_ADMIN.getCode())) {
            list = filterVisible(list, userId, null);
        }
        return list;
    }

    @Override
    public boolean canManage(Long kbId) {
        if (kbId == null) return false;
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        LibraryDO library = libraryMapper.selectById(kbId);
        if (library == null) {
            log.debug("[管理权限校验] userId={} kbId={} 库不存在 → false", userId, kbId);
            return false;
        }
        LevelConfigDO cfg = levelConfigMapper.selectById(library.getKbLevelId());
        try {
            validateManagementPermission(cfg, library.getOwnerId());
            log.debug("[管理权限校验] userId={} kbId={} name={} → true", userId, kbId, library.getName());
            return true;
        } catch (Exception e) {
            log.debug("[管理权限校验] userId={} kbId={} name={} → false ({})", userId, kbId, library.getName(), e.getMessage());
            return false;
        }
    }
}