package cn.iocoder.yudao.module.kb.service.library;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import cn.iocoder.yudao.module.kb.dal.dataobject.category.CategoryDO;
import cn.iocoder.yudao.module.kb.dal.dataobject.levelconfig.LevelConfigDO;
import cn.iocoder.yudao.module.kb.dal.dataobject.library.LibraryDO;
import cn.iocoder.yudao.module.kb.dal.dataobject.sharedept.ShareDeptDO;
import cn.iocoder.yudao.module.kb.dal.mysql.category.CategoryMapper;
import cn.iocoder.yudao.module.kb.dal.mysql.levelconfig.LevelConfigMapper;
import cn.iocoder.yudao.module.kb.dal.mysql.sharedept.ShareDeptMapper;
import cn.iocoder.yudao.module.kb.service.library.rule.VisibilityContext;
import cn.iocoder.yudao.module.kb.service.library.rule.VisibilityRuleEngine;
import cn.iocoder.yudao.module.kb.service.libraryext.LibraryExtService;
import cn.iocoder.yudao.module.kb.service.userdept.KbUserDeptService;
import cn.iocoder.yudao.framework.security.core.service.SecurityFrameworkService;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.system.enums.permission.RoleCodeEnum;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import cn.iocoder.yudao.module.kb.controller.admin.library.vo.*;
import cn.iocoder.yudao.module.kb.service.kbconfig.KbProcessConfigResolver;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;

import cn.iocoder.yudao.module.kb.dal.mysql.library.LibraryMapper;
import cn.iocoder.yudao.module.kb.dal.mysql.folder.FolderMapper;
import cn.iocoder.yudao.module.kb.service.document.DocumentService;

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
    private SecurityFrameworkService securityFrameworkService;

    @Resource
    private LibraryExtService libraryExtService;

    @Resource
    private VisibilityRuleEngine ruleEngine;

    @Resource
    private KbUserDeptService kbUserDeptService;

    @Resource
    private DocumentService documentService;

    @Resource
    private FolderMapper folderMapper;

    @Resource
    private KbProcessConfigResolver kbProcessConfigResolver;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createLibrary(LibrarySaveReqVO createReqVO) {
        // 校验权限：非超管用户只能创建自己有操作权限的知识库
        LevelConfigDO cfg = resolveLevelConfig(createReqVO.getKbLevelId(), createReqVO.getCategoryId());
        if (cfg != null && createReqVO.getKbLevelId() == null) {
            createReqVO.setKbLevelId(cfg.getId());
        }
        boolean instituteDefault = applyInstituteDefaultOwner(createReqVO, cfg);
        if (instituteDefault) {
            validateInstituteCreatePermission(cfg);
        } else {
            validateManagementPermission(cfg, createReqVO.getOwnerId());
        }

        // 院级/公司「项目成果库」下创建的知识库自动纳入项目成员管理
        applyProjectFlag(createReqVO);

        // 插入
        LibraryDO library = BeanUtils.toBean(createReqVO, LibraryDO.class);
        // processConfig Map -> JSON 字符串落库（覆盖 BeanUtils 的类型拷贝）
        if (createReqVO.getProcessConfig() != null) {
            library.setProcessConfig(JsonUtils.toJsonString(createReqVO.getProcessConfig()));
        }
        libraryMapper.insert(library);
        // 发布 KB 级配置变更（python-vector 检索侧按 kb_id 读取覆盖租户级配置）
        kbProcessConfigResolver.publishKbConfig(library.getId());

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
        // 保存自定义字段值
        if (createReqVO.getExtValues() != null) {
            libraryExtService.replaceExtValues(library.getId(), createReqVO.getExtValues());
        }
        // 项目库：将分类及其子分类标记为项目成果库（打开权限实时读成员字段 ext 值，无需同步成员表）
        if (ProjectCategorySupport.isMarkedProject(library.getIsProject())) {
            libraryMapper.updateIsProjectByCategoryIds(Collections.singleton(library.getCategoryId()), 1);
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

        // 分类变更或仍挂在项目成果库下时，同步项目库标记
        applyProjectFlag(updateReqVO);

        // 更新
        LibraryDO updateObj = BeanUtils.toBean(updateReqVO, LibraryDO.class);
        // processConfig：前端传 null 表示不改动（MyBatis-Plus 不更新 null 字段，保留原值）；
        // 传空对象 {} 表示清空 KB 级配置（全部回退租户级）
        if (updateReqVO.getProcessConfig() != null) {
            updateObj.setProcessConfig(JsonUtils.toJsonString(updateReqVO.getProcessConfig()));
        } else {
            updateObj.setProcessConfig(null);
        }
        libraryMapper.updateById(updateObj);
        // 发布 KB 级配置变更
        kbProcessConfigResolver.publishKbConfig(updateObj.getId());

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
        // 保存自定义字段值（项目库打开权限实时读成员字段 ext 值，无需同步成员表）
        if (updateReqVO.getExtValues() != null) {
            libraryExtService.replaceExtValues(updateReqVO.getId(), updateReqVO.getExtValues());
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
        // 删除自定义字段值
        libraryExtService.removeAllByKbId(id);
        // 级联删除库下全部文档与文件夹（含源文件/向量/向量任务清理；wiki 由 documentService.deleteByKbId 联动，避免孤儿）
        documentService.deleteByKbId(id);
        folderMapper.deleteByKbId(id);
        // 删除
        libraryMapper.deleteById(id);
        // 发布 KB 配置删除（python-vector 删除 Redis kb:process:config:{kb_id}，与 create/update 对称）
        kbProcessConfigResolver.publishKbConfigDelete(id);
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
        // 再统一删除关联的共享部门记录
        for (Long id : ids) {
            shareDeptMapper.delete(new LambdaQueryWrapper<ShareDeptDO>()
                    .eq(ShareDeptDO::getKbId, id));
            libraryExtService.removeAllByKbId(id);
            // 级联删除库下全部文档与文件夹（含源文件/向量/向量任务清理；wiki 由 documentService.deleteByKbId 联动，避免孤儿）
            documentService.deleteByKbId(id);
            folderMapper.deleteByKbId(id);
        }
        // 最后批量删除知识库
        libraryMapper.deleteByIds(ids);
        // 批量发布 KB 配置删除（python-vector 清理对应 Redis 缓存）
        for (Long id : ids) {
            kbProcessConfigResolver.publishKbConfigDelete(id);
        }
    }

    /**
     * 统一的权限校验入口（委托给 VisibilityRuleEngine）
     * <p>
     * 具体规则逻辑由各 VisibilityRuleHandler 实现，此方法仅做统一调度。
     */
    private LevelConfigDO resolveLevelConfig(Long kbLevelId, Long categoryId) {
        if (kbLevelId != null) {
            return levelConfigMapper.selectById(kbLevelId);
        }
        if (categoryId == null) {
            return null;
        }
        CategoryDO category = categoryMapper.selectById(categoryId);
        if (category == null || category.getKbLevelId() == null) {
            return null;
        }
        return levelConfigMapper.selectById(category.getKbLevelId());
    }

    private boolean isInstituteDeptOwned(LevelConfigDO cfg) {
        return cfg != null
                && Integer.valueOf(2).equals(cfg.getVisibilityRule())
                && Integer.valueOf(2).equals(cfg.getOwnerDim());
    }

    /**
     * C 端没有部门下拉，会把 ownerId 写成当前用户。院级库应改为用户部门链上的第二级。
     * 后台已手选部门时（ownerId 是部门而非当前用户）不覆盖。
     *
     * @return true=本次按院级默认补了所属部门
     */
    private boolean applyInstituteDefaultOwner(LibrarySaveReqVO reqVO, LevelConfigDO cfg) {
        if (!isInstituteDeptOwned(cfg)) {
            return false;
        }
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        Long ownerId = reqVO.getOwnerId();
        if (ownerId != null && !Objects.equals(ownerId, loginUserId)) {
            return false;
        }
        Long secondLevelDeptId = kbUserDeptService.getSecondLevelDeptId(loginUserId);
        if (secondLevelDeptId == null) {
            return false;
        }
        reqVO.setOwnerId(secondLevelDeptId);
        log.debug("[院级默认部门] userId={} ownerId={} → 二级部门 {}", loginUserId, ownerId, secondLevelDeptId);
        return true;
    }

    /**
     * 院级自动归属时：超管或任意部门管理员可创建，与总览/大屏「新建」按钮一致。
     * 不按二级部门做 canManage，避免研发中心管理员因归属落到上级院而被拒。
     */
    private void validateInstituteCreatePermission(LevelConfigDO cfg) {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        if (userId == null) {
            throw exception(LIBRARY_PERMISSION_DENIED);
        }
        VisibilityContext ctx = ruleEngine.buildContext(userId);
        if (ctx.isSuperAdmin()) {
            return;
        }
        Set<Long> adminDeptIds = kbUserDeptService.getAdminDeptIds(userId);
        if (CollUtil.isNotEmpty(adminDeptIds)) {
            return;
        }
        validateManagementPermission(cfg, null);
    }

    private void validateManagementPermission(LevelConfigDO cfg, Long ownerId) {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        if (userId == null) {
            log.debug("[管理权限] 用户未登录 → 拒绝");
            throw exception(LIBRARY_PERMISSION_DENIED);
        }

        // 构建临时 LibraryDO 用于权限判断（只需要 kbLevelId 和 ownerId）
        LibraryDO tempLib = LibraryDO.builder()
                .kbLevelId(cfg != null ? cfg.getId() : null)
                .ownerId(ownerId)
                .build();

        VisibilityContext ctx = ruleEngine.buildContext(userId);
        if (!ruleEngine.canManage(ctx, tempLib)) {
            throw exception(LIBRARY_PERMISSION_DENIED);
        }
    }

    /**
     * 可见性过滤（委托给 VisibilityRuleEngine，保留视图级特殊处理）
     * <p>
     * 视图级特殊处理（公司项目库视图、广场视图）在此处理，
     * 规则级判断（rule=1/2/3/5）委托给 ruleEngine.filterVisible。
     */
    private List<LibraryDO> filterVisible(List<LibraryDO> allLibs, Long userId, Long categoryId) {
        if (CollUtil.isEmpty(allLibs)) return allLibs;

        VisibilityContext ctx = ruleEngine.buildContext(userId);
        if (ctx.isSuperAdmin()) {
            return allLibs;
        }

        // 视图级特殊处理
        final boolean isCompanyLevelView = isCompanyLevelProjectView(categoryId);
        final boolean isSquareView = isSquareCategoryView(categoryId);

        log.debug("[可见性] userId={} categoryId={} 公司视图={} 广场视图={} 待过滤总数={}",
                userId, categoryId, isCompanyLevelView, isSquareView, allLibs.size());

        return allLibs.stream()
                .filter(lib -> {
                    // 公司级项目库视图：聚合所有项目成果库
                    if (isCompanyLevelView && lib.getIsProject() != null && lib.getIsProject() == 1) {
                        return true;
                    }
                    // 广场视图：所有 isPublic=1 的知识库全员可见
                    if (isSquareView) {
                        return lib.getIsPublic() != null && lib.getIsPublic() == 1;
                    }
                    // 规则级判断委托给引擎
                    return ruleEngine.canSee(ctx, lib);
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

        Long userId = SecurityFrameworkUtils.getLoginUserId();
        VisibilityContext ctx = ruleEngine.buildContext(userId);

        // 超管/租户管理员 → 直接查询，无需可见性过滤
        if (ctx.isSuperAdmin()) {
            return libraryMapper.selectPage(pageReqVO);
        }

        // 广场视图：所有 isPublic=1 的知识库全员可见，不走规则级过滤
        // （isPublic=1 已通过 pageReqVO 设置，无需额外条件）
        if (isSquareCategoryView(originalCategoryId)) {
            return libraryMapper.selectPage(pageReqVO);
        }

        // 非管理员用户：通过 RuleEngine 生成 SQL 可见性条件，下推到 SQL 层
        List<String> conditions = ruleEngine.buildSqlConditions(ctx);

        // 公司级项目库视图：聚合所有 is_project=1 的知识库
        boolean isCompanyProjectView = isCompanyLevelProjectView(originalCategoryId);
        if (isCompanyProjectView) {
            conditions.add("is_project = 1");
        }

        log.debug("[分页查询] userId={} categoryId={} conditions={} isCompanyProjectView={}",
                userId, originalCategoryId, conditions, isCompanyProjectView);

        return libraryMapper.selectPageWithVisibility(pageReqVO, conditions);
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
        // 项目成员管理只查项目库：顺带把院级/公司项目成果分类下尚未打标的旧库补上
        if (Integer.valueOf(1).equals(isProject)) {
            syncUnmarkedProjectLibraries();
        }
        List<LibraryDO> list = libraryMapper.selectSimpleList(isProject);
        // filterVisible 内部通过 ruleEngine 处理超管短路和规则判断
        List<LibraryDO> visible = filterVisible(list, userId, null);
        if (CollUtil.isEmpty(visible)) {
            return visible;
        }
        // 项目成果库按成员收敛：非成员的项目库不进精简列表（与 canOpen 语义一致），
        // 保证检索范围/下拉选择只能选到「能打开」的库。
        Set<Long> openable = filterOpenProjectLibraries(
                visible.stream().map(LibraryDO::getId).collect(Collectors.toList()));
        return visible.stream()
                .filter(lib -> openable.contains(lib.getId()))
                .collect(Collectors.toList());
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
        VisibilityContext ctx = ruleEngine.buildContext(userId);
        boolean result = ruleEngine.canManage(ctx, library);
        log.debug("[管理权限校验] userId={} kbId={} name={} → {}", userId, kbId, library.getName(), result);
        return result;
    }

    @Override
    public Map<Long, Boolean> canManageMap(List<LibraryDO> libraries) {
        if (CollUtil.isEmpty(libraries)) {
            return Collections.emptyMap();
        }
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        VisibilityContext ctx = ruleEngine.buildContext(userId);
        Map<Long, Boolean> result = new HashMap<>(libraries.size());
        for (LibraryDO lib : libraries) {
            if (lib == null || lib.getId() == null) {
                continue;
            }
            result.put(lib.getId(), ruleEngine.canManage(ctx, lib));
        }
        return result;
    }

    @Override
    public boolean canOpenProjectLibrary(Long kbId) {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        if (userId == null) {
            return false;
        }
        boolean admin = isAdminProjectShortcut();
        log.debug("[项目库打开] userId={} kbId={} adminShortcut={}", userId, kbId, admin);
        // 租户管理员/超管短路：可打开全部项目库
        if (admin) {
            return true;
        }
        LibraryDO lib = kbId == null ? null : libraryMapper.selectById(kbId);
        if (lib == null) {
            return false;
        }
        // 非项目库直接放行
        if (lib.getIsProject() == null || lib.getIsProject() != 1) {
            return true;
        }
        boolean member = isProjectMemberByField(lib, userId, libraryExtService.getExtValues(kbId));
        log.debug("[项目库打开] userId={} kbId={} isProject=1 memberField={}", userId, kbId, member);
        return member;
    }

    @Override
    public Set<Long> filterOpenProjectLibraries(Collection<Long> kbIds) {
        Set<Long> result = new HashSet<>();
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        if (userId == null || CollUtil.isEmpty(kbIds)) {
            return result;
        }
        if (isAdminProjectShortcut()) {
            result.addAll(kbIds);
            return result;
        }
        List<LibraryDO> libs = libraryMapper.selectBatchIds(kbIds);
        Map<Long, Map<String, String>> extMap = libraryExtService.getExtValuesMap(kbIds);
        for (LibraryDO lib : libs) {
            if (lib == null || lib.getId() == null) {
                continue;
            }
            if (lib.getIsProject() != null && lib.getIsProject() == 1) {
                if (isProjectMemberByField(lib, userId, extMap.get(lib.getId()))) {
                    result.add(lib.getId());
                }
            } else {
                result.add(lib.getId());
            }
        }
        return result;
    }

    private boolean isAdminProjectShortcut() {
        return securityFrameworkService.hasAnyRoles(
                RoleCodeEnum.SUPER_ADMIN.getCode(),
                RoleCodeEnum.TENANT_ADMIN.getCode());
    }

    /**
     * 实时判断用户是否在「分类显式指定的打开依据成员字段」的 ext 值中。
     * 打开依据字段未在分类管理指定时 → 一律无权限（项目成果库只认显式指定；除租户管理员/超管短路）。
     */
    private boolean isProjectMemberByField(LibraryDO lib, Long userId, Map<String, String> ext) {
        CategoryDO category = lib.getCategoryId() == null ? null : categoryMapper.selectById(lib.getCategoryId());
        String key = (category == null || StrUtil.isBlank(category.getProjectMemberFieldKey()))
                ? null : category.getProjectMemberFieldKey();
        if (key == null) {
            return false;
        }
        String raw = ext == null ? null : ext.get(key);
        if (StrUtil.isBlank(raw)) {
            return false;
        }
        return parseUserIds(raw).contains(userId);
    }

    @Override
    public void markLibrariesAsProjectByCategory(Long categoryId) {
        if (categoryId == null) {
            return;
        }
        Set<Long> categoryIds = collectSelfAndDescendantIds(categoryId);
        libraryMapper.updateIsProjectByCategoryIds(categoryIds, 1);
    }

    private List<Long> parseUserIds(String raw) {
        if (StrUtil.isBlank(raw)) {
            return Collections.emptyList();
        }
        String text = raw.trim();
        List<Long> ids = new ArrayList<>();
        try {
            if (text.startsWith("[")) {
                List<Object> arr = JsonUtils.parseArray(text, Object.class);
                if (arr != null) {
                    for (Object item : arr) {
                        Long id = toUserId(item);
                        if (id != null) {
                            ids.add(id);
                        }
                    }
                }
                return ids;
            }
        } catch (Exception ignored) {
            // 再按逗号拆
        }
        for (String part : text.split("[,，、\\s]+")) {
            Long id = toUserId(part);
            if (id != null) {
                ids.add(id);
            }
        }
        return ids;
    }

    private Long toUserId(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof Number) {
            return ((Number) raw).longValue();
        }
        String text = String.valueOf(raw).trim();
        if (text.isEmpty() || !text.matches("\\d+")) {
            return null;
        }
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /**
     * 按分类树决定是否项目成果库，不依赖前端是否勾选。
     */
    private void applyProjectFlag(LibrarySaveReqVO reqVO) {
        boolean project = isProjectCategory(reqVO.getCategoryId());
        reqVO.setIsProject(project ? 1 : 0);
    }

    private boolean isProjectCategory(Long categoryId) {
        if (categoryId == null) {
            return false;
        }
        return ProjectCategorySupport.isProjectCategory(
                categoryMapper.selectById(categoryId), categoryMapper::selectById);
    }

    /**
     * 把院级/公司「项目成果」分类下、尚未打标的知识库补成项目库，便于旧数据进入项目成员管理。
     */
    private void syncUnmarkedProjectLibraries() {
        Set<Long> projectCategoryIds = findProjectCategoryIds();
        if (projectCategoryIds.isEmpty()) {
            return;
        }
        libraryMapper.updateIsProjectByCategoryIds(projectCategoryIds, 1);
    }

    private Set<Long> findProjectCategoryIds() {
        List<CategoryDO> all = categoryMapper.selectList();
        if (CollUtil.isEmpty(all)) {
            return Collections.emptySet();
        }
        Map<Long, CategoryDO> byId = all.stream()
                .filter(c -> c.getId() != null)
                .collect(Collectors.toMap(CategoryDO::getId, c -> c, (a, b) -> a));
        Set<Long> ids = new HashSet<>();
        for (CategoryDO cat : all) {
            if (ProjectCategorySupport.isProjectCategory(cat, byId::get)) {
                ids.add(cat.getId());
            }
        }
        return ids;
    }

    private Set<Long> collectSelfAndDescendantIds(Long categoryId) {
        List<CategoryDO> all = categoryMapper.selectList();
        Map<Long, List<Long>> childrenMap = new HashMap<>();
        for (CategoryDO cat : all) {
            if (cat.getParentId() == null) {
                continue;
            }
            childrenMap.computeIfAbsent(cat.getParentId(), k -> new ArrayList<>()).add(cat.getId());
        }
        Set<Long> ids = new LinkedHashSet<>();
        Deque<Long> queue = new ArrayDeque<>();
        queue.add(categoryId);
        while (!queue.isEmpty()) {
            Long id = queue.poll();
            if (id == null || !ids.add(id)) {
                continue;
            }
            List<Long> children = childrenMap.get(id);
            if (children != null) {
                queue.addAll(children);
            }
        }
        return ids;
    }
}