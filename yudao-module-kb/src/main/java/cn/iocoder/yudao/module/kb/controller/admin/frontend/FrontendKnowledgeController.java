package cn.iocoder.yudao.module.kb.controller.admin.frontend;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.kb.controller.admin.frontend.vo.FrontendKnowledgeBaseSaveReqVO;
import cn.iocoder.yudao.module.kb.controller.admin.frontend.vo.FrontendKnowledgeBaseVO;
import cn.iocoder.yudao.module.kb.controller.admin.frontend.vo.FrontendPermissionsVO;
import cn.iocoder.yudao.module.kb.controller.admin.frontend.vo.FrontendResult;
import cn.iocoder.yudao.module.kb.controller.admin.library.vo.LibraryPageReqVO;
import cn.iocoder.yudao.module.kb.controller.admin.library.vo.LibrarySaveReqVO;
import cn.iocoder.yudao.module.kb.dal.dataobject.category.CategoryDO;
import cn.iocoder.yudao.module.kb.dal.dataobject.levelconfig.LevelConfigDO;
import cn.iocoder.yudao.module.kb.dal.dataobject.library.LibraryDO;
import cn.iocoder.yudao.module.kb.dal.mysql.category.CategoryMapper;
import cn.iocoder.yudao.module.kb.dal.mysql.levelconfig.LevelConfigMapper;
import cn.iocoder.yudao.module.kb.dal.mysql.library.LibraryMapper;
import cn.iocoder.yudao.module.kb.service.follow.FollowService;
import cn.iocoder.yudao.module.kb.service.library.LibraryService;
import cn.iocoder.yudao.module.kb.service.libraryext.LibraryExtService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 前端 C 端 - 知识库列表兼容层
 *
 * <p>将 Python 端 /knowledge/bases/ 等列表接口映射到 Java kb 模块现有 Service/Mapper，
 * 返回前端约定的扁平结构 {@code {code:0, data:[...], total:N}}。
 *
 * @author 吴皓
 */
@Tag(name = "前端 C 端 - 知识库列表兼容层")
@RestController
public class FrontendKnowledgeController {

    private static final DateTimeFormatter DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Resource
    private LibraryMapper libraryMapper;

    @Resource
    private CategoryMapper categoryMapper;

    @Resource
    private FollowService followService;

    @Resource
    private LibraryService libraryService;

    @Resource
    private LibraryExtService libraryExtService;

    @Resource
    private LevelConfigMapper levelConfigMapper;

    @Resource
    private AdminUserApi adminUserApi;

    @Resource
    private DeptApi deptApi;

    /**
     * 知识库广场 / 全部可访问列表（对应 /knowledge/bases/）
     */
    @GetMapping({"/knowledge/bases/", "/knowledge/bases"})
    @Operation(summary = "前端知识库列表（默认=广场公开库）")
    public FrontendResult<List<FrontendKnowledgeBaseVO>> bases(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "page_size", defaultValue = "10") Integer pageSize,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "sortField", required = false) String sortField,
            @RequestParam(value = "sortOrder", required = false) String sortOrder,
            @RequestParam(value = "ordering", required = false) String ordering) {
        PageParam pageParam = buildPageParam(page, pageSize);
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        return toListResult(queryByCategory(category, pageParam, search, userId, sortField, sortOrder, ordering));
    }

    /**
     * 按分类获取知识库（对应 /knowledge/bases/{category}/）
     * 分类取值：public / my-public / my-follows / personal-kbase 等
     */
    @GetMapping({"/knowledge/bases/{category}", "/knowledge/bases/{category}/"})
    @Operation(summary = "前端知识库分类列表")
    public FrontendResult<List<FrontendKnowledgeBaseVO>> basesByCategory(
            @PathVariable String category,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "page_size", defaultValue = "10") Integer pageSize,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "sortField", required = false) String sortField,
            @RequestParam(value = "sortOrder", required = false) String sortOrder,
            @RequestParam(value = "ordering", required = false) String ordering) {
        PageParam pageParam = buildPageParam(page, pageSize);
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        return toListResult(queryByCategory(category, pageParam, search, userId, sortField, sortOrder, ordering));
    }

    /**
     * 我加入的知识库（对应 /knowledge/follows/my-follows/）
     */
    @GetMapping({"/knowledge/follows/my-follows", "/knowledge/follows/my-follows/"})
    @Operation(summary = "前端我加入的知识库列表")
    public FrontendResult<List<FrontendKnowledgeBaseVO>> myFollows(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "page_size", defaultValue = "10") Integer pageSize) {
        return toListResult(queryMyFollows(buildPageParam(page, pageSize), SecurityFrameworkUtils.getLoginUserId()));
    }

    // ---------- 新增 / 修改 / 删除 ----------

    /**
     * 新建知识库（对应 /knowledge/bases/）
     */
    @PostMapping({"/knowledge/bases/", "/knowledge/bases"})
    @Operation(summary = "前端新建知识库")
    public FrontendResult<Long> create(@RequestBody FrontendKnowledgeBaseSaveReqVO req) {
        return FrontendResult.ok(libraryService.createLibrary(buildSaveVO(req, null)));
    }

    /**
     * 更新知识库（对应 /knowledge/bases/{id}/）
     */
    @PutMapping({"/knowledge/bases/{id}", "/knowledge/bases/{id}/"})
    @Operation(summary = "前端更新知识库")
    public FrontendResult<Boolean> update(@PathVariable("id") Long id, @RequestBody FrontendKnowledgeBaseSaveReqVO req) {
        libraryService.updateLibrary(buildSaveVO(req, id));
        return FrontendResult.ok(true);
    }

    /**
     * 删除知识库（对应 /knowledge/bases/{id}/destroy-with-data/）
     */
    @DeleteMapping({"/knowledge/bases/{id}/destroy-with-data", "/knowledge/bases/{id}/destroy-with-data/"})
    @Operation(summary = "前端删除知识库")
    public FrontendResult<Boolean> delete(@PathVariable("id") Long id) {
        libraryService.deleteLibrary(id);
        return FrontendResult.ok(true);
    }

    /**
     * 切换知识库公开状态（对应 /knowledge/bases/{id}/toggle-public/）
     */
    @PatchMapping({"/knowledge/bases/{id}/toggle-public", "/knowledge/bases/{id}/toggle-public/"})
    @Operation(summary = "前端切换知识库公开状态")
    public FrontendResult<Boolean> togglePublic(@PathVariable("id") Long id) {
        libraryService.togglePublic(id);
        return FrontendResult.ok(true);
    }

    /**
     * 当前用户对知识库的权限（对应 /knowledge/bases/{id}/my-permissions/）。
     * 写权限与后台知识库大屏一致：{@link LibraryService#canManage(Long)}。
     */
    @GetMapping({"/knowledge/bases/{id}/my-permissions", "/knowledge/bases/{id}/my-permissions/"})
    @Operation(summary = "前端当前用户对知识库的权限")
    public FrontendResult<FrontendPermissionsVO> myPermissions(@PathVariable("id") Long id) {
        LibraryDO lib = libraryService.getLibrary(id);
        if (lib == null) {
            return FrontendResult.error("知识库不存在");
        }
        boolean manage = libraryService.canManage(id);
        FrontendPermissionsVO vo = new FrontendPermissionsVO();
        vo.setCanView(true);
        vo.setCanGet(true);
        vo.setCanDownload(true);
        vo.setCanEdit(manage);
        vo.setCanManage(manage);
        vo.setCanUpload(manage);
        vo.setCanDelete(manage);
        vo.setCanShare(manage);
        vo.setCanApprove(manage);
        return FrontendResult.ok(vo);
    }

    // ---------- 查询 ----------

    private PageParam buildPageParam(Integer page, Integer pageSize) {
        PageParam pageParam = new PageParam();
        pageParam.setPageNo(page == null || page < 1 ? 1 : page);
        pageParam.setPageSize(pageSize == null || pageSize < 1 ? 10 : pageSize);
        return pageParam;
    }

    /**
     * 按前端分类值路由到对应查询。
     *
     * <p>分类取值有两类：
     * <ul>
     *   <li>分类主键（kb_category.id 的字符串）→ 走知识库大屏的 {@link LibraryService#getLibraryPage}，含可见性过滤；</li>
     *   <li>兼容 slug（personal-kbase / my-public / my-follows 等）→ 保持原逻辑。</li>
     * </ul>
     */
    private PageResult<LibraryDO> queryByCategory(String category, PageParam pageParam, String search, Long userId,
                                                  String sortField, String sortOrder, String ordering) {
        if (category == null || category.isEmpty()) {
            return queryPublic(pageParam, search, sortField, sortOrder, ordering);
        }
        // 分类主键 → 复用知识库大屏的 getLibraryPage（含可见性规则）
        if (category.matches("\\d+")) {
            return queryByCategoryId(Long.parseLong(category), pageParam, search, sortField, sortOrder, ordering);
        }
        // 个人知识库 → 我名下的全部知识库（含私密）
        if (category.startsWith("personal")) {
            return queryMyAll(pageParam, search, userId, sortField, sortOrder, ordering);
        }
        // 我创建的 → 我名下的全部知识库
        if ("my-public".equals(category)) {
            return queryMyAll(pageParam, search, userId, sortField, sortOrder, ordering);
        }
        // 我加入的
        if ("my-follows".equals(category)) {
            return queryMyFollows(pageParam, userId);
        }
        // 知识库广场 / 院级 / 公司 / 咨询评估 等 → 广场公开库
        return queryPublic(pageParam, search, sortField, sortOrder, ordering);
    }

    /** 按分类主键查询知识库列表（复用知识库大屏逻辑，含可见性过滤与排序） */
    private PageResult<LibraryDO> queryByCategoryId(Long categoryId, PageParam pageParam, String search,
                                                    String sortField, String sortOrder, String ordering) {
        LibraryPageReqVO reqVO = new LibraryPageReqVO();
        reqVO.setPageNo(pageParam.getPageNo());
        reqVO.setPageSize(pageParam.getPageSize());
        reqVO.setCategoryId(categoryId);
        reqVO.setName(search);
        applySort(reqVO, sortField, sortOrder, ordering);
        return libraryService.getLibraryPage(reqVO);
    }

    private PageResult<LibraryDO> queryPublic(PageParam pageParam, String search,
                                              String sortField, String sortOrder, String ordering) {
        LibraryPageReqVO reqVO = new LibraryPageReqVO();
        reqVO.setPageNo(pageParam.getPageNo());
        reqVO.setPageSize(pageParam.getPageSize());
        reqVO.setIsPublic(1);
        reqVO.setStatus(0);
        reqVO.setName(search);
        applySort(reqVO, sortField, sortOrder, ordering);
        return libraryMapper.selectPage(reqVO);
    }

    private PageResult<LibraryDO> queryMyAll(PageParam pageParam, String search, Long userId,
                                             String sortField, String sortOrder, String ordering) {
        LibraryPageReqVO reqVO = new LibraryPageReqVO();
        reqVO.setPageNo(pageParam.getPageNo());
        reqVO.setPageSize(pageParam.getPageSize());
        reqVO.setOwnerId(userId);
        reqVO.setStatus(0);
        reqVO.setName(search);
        applySort(reqVO, sortField, sortOrder, ordering);
        return libraryMapper.selectPage(reqVO);
    }

    private PageResult<LibraryDO> queryMyFollows(PageParam pageParam, Long userId) {
        List<Long> kbIds = followService.getFollowedKbIds(userId);
        if (kbIds.isEmpty()) {
            return new PageResult<>(Collections.emptyList(), 0L);
        }
        List<LibraryDO> list = libraryMapper.selectBatchIds(kbIds);
        int total = list.size();
        int fromIndex = (pageParam.getPageNo() - 1) * pageParam.getPageSize();
        int toIndex = Math.min(fromIndex + pageParam.getPageSize(), total);
        if (fromIndex >= total) {
            return new PageResult<>(Collections.emptyList(), (long) total);
        }
        return new PageResult<>(list.subList(fromIndex, toIndex), (long) total);
    }

    /**
     * 与知识库大屏一致：sortField=name/docCount/createTime，sortOrder=ascending/descending。
     * 同时兼容旧版 Python ordering（name / documents_count / created_at）。
     */
    private void applySort(LibraryPageReqVO reqVO, String sortField, String sortOrder, String ordering) {
        if (sortField != null && !sortField.isEmpty()) {
            reqVO.setSortField(sortField);
            reqVO.setSortOrder(sortOrder);
            return;
        }
        if (ordering != null && ordering.contains(":")) {
            String[] parts = ordering.split(":");
            reqVO.setSortField(parts[0]);
            reqVO.setSortOrder(parts.length > 1 ? parts[1] : "ascending");
            return;
        }
        if (ordering == null || ordering.isEmpty()) {
            reqVO.setSortField("createTime");
            reqVO.setSortOrder("descending");
            return;
        }
        switch (ordering) {
            case "name":
                reqVO.setSortField("name");
                reqVO.setSortOrder("ascending");
                break;
            case "documents_count":
                reqVO.setSortField("docCount");
                reqVO.setSortOrder("ascending");
                break;
            case "-documents_count":
                reqVO.setSortField("docCount");
                reqVO.setSortOrder("descending");
                break;
            case "created_at":
                reqVO.setSortField("createTime");
                reqVO.setSortOrder("descending");
                break;
            case "-created_at":
                reqVO.setSortField("createTime");
                reqVO.setSortOrder("ascending");
                break;
            default:
                reqVO.setSortField("createTime");
                reqVO.setSortOrder("descending");
        }
    }

    // ---------- 新增 / 修改 VO 构建 ----------

    /**
     * 将前端入参映射为后端 {@link LibrarySaveReqVO}。
     *
     * <ul>
     *   <li>新建：归属默认为当前登录用户，公开/状态默认关闭；</li>
     *   <li>更新：以现有库为基础，仅覆盖前端传入字段，避免 {@code BeanUtils.toBean} 空值覆盖。</li>
     * </ul>
     */
    private LibrarySaveReqVO buildSaveVO(FrontendKnowledgeBaseSaveReqVO req, Long id) {
        LibrarySaveReqVO save = new LibrarySaveReqVO();
        save.setId(id);

        // 更新时先回填现有值，保证未传入字段不被置空
        if (id != null) {
            LibraryDO existing = libraryService.getLibrary(id);
            if (existing != null) {
                save.setCategoryId(existing.getCategoryId());
                save.setKbLevelId(existing.getKbLevelId());
                save.setOwnerId(existing.getOwnerId());
                save.setDescription(existing.getDescription());
                save.setCoverUrl(existing.getCoverUrl());
                save.setDocCount(existing.getDocCount());
                save.setStatus(existing.getStatus());
                save.setIsPublic(existing.getIsPublic());
                save.setIsProject(existing.getIsProject());
            }
        }

        // 覆盖前端传入字段
        if (req.getName() != null) {
            save.setName(req.getName());
        }
        if (req.getDescription() != null) {
            save.setDescription(req.getDescription());
        }
        if (req.getCoverUrl() != null) {
            save.setCoverUrl(req.getCoverUrl());
        }
        if (req.getExtValues() != null) {
            save.setExtValues(req.getExtValues());
        }
        if (req.getMemberIds() != null) {
            save.setMemberIds(req.getMemberIds().stream()
                    .filter(s -> s != null && s.matches("\\d+"))
                    .map(Long::parseLong)
                    .collect(Collectors.toList()));
        }
        if (req.getIsPublic() != null) {
            save.setIsPublic(req.getIsPublic() ? 1 : 0);
        }

        // 分类：解析主键/名称 → categoryId，并据此补全层级配置 kbLevelId
        Long categoryId = resolveCategoryId(req.getCategory());
        if (categoryId != null) {
            save.setCategoryId(categoryId);
            CategoryDO category = categoryMapper.selectById(categoryId);
            if (category != null) {
                save.setKbLevelId(category.getKbLevelId());
            }
        }

        // 新建：个人库归属当前用户；院级库不传部门，由 LibraryService 落到用户二级部门
        if (id == null) {
            LevelConfigDO cfg = save.getKbLevelId() != null
                    ? levelConfigMapper.selectById(save.getKbLevelId()) : null;
            boolean instituteDeptOwned = cfg != null
                    && Integer.valueOf(2).equals(cfg.getVisibilityRule())
                    && Integer.valueOf(2).equals(cfg.getOwnerDim());
            if (!instituteDeptOwned) {
                save.setOwnerId(SecurityFrameworkUtils.getLoginUserId());
            }
            if (save.getIsPublic() == null) {
                save.setIsPublic(0);
            }
            if (save.getStatus() == null) {
                save.setStatus(0);
            }
        }
        return save;
    }

    /**
     * 解析前端分类值：分类主键字符串 → id；名称 → 按分类名匹配；无法识别返回 null。
     */
    private Long resolveCategoryId(String category) {
        if (category == null || category.isEmpty()) {
            return null;
        }
        if (category.matches("\\d+")) {
            return Long.parseLong(category);
        }
        CategoryDO byName = categoryMapper.selectByName(category);
        return byName != null ? byName.getId() : null;
    }

    // ---------- 映射 ----------

    private FrontendResult<List<FrontendKnowledgeBaseVO>> toListResult(PageResult<LibraryDO> page) {
        List<LibraryDO> libs = page.getList();
        // 批量加载自定义字段值 + 所有者显示名，避免 N+1 查询
        List<Long> kbIds = libs.stream().map(LibraryDO::getId).collect(Collectors.toList());
        Map<Long, Map<String, String>> extMap = kbIds.isEmpty()
                ? Collections.emptyMap()
                : libraryExtService.getExtValuesMap(kbIds);
        Map<Long, String> ownerNames = resolveOwnerNames(libs);
        Map<Long, Boolean> canManageMap = libraryService.canManageMap(libs);

        List<FrontendKnowledgeBaseVO> list = libs.stream()
                .map(lib -> toVO(lib, extMap.get(lib.getId()), ownerNames.get(lib.getOwnerId()),
                        Boolean.TRUE.equals(canManageMap.get(lib.getId()))))
                .collect(Collectors.toList());
        return FrontendResult.okList(list, page.getTotal());
    }

    /**
     * 批量解析所有者显示名，按层级配置 ownerDim 区分归属维度：
     * <ul>
     *   <li>ownerDim=2（部门）→ 部门名称</li>
     *   <li>ownerDim=1（用户）或缺省 → 用户昵称</li>
     * </ul>
     * 解析失败返回 null，前端回退显示 owner id。
     */
    private Map<Long, String> resolveOwnerNames(List<LibraryDO> libs) {
        // 归集每个库的归属维度（kbLevelId → ownerDim）
        Map<Long, Integer> ownerDimMap = resolveOwnerDimMap(libs);

        Set<Long> userIds = new HashSet<>();
        Set<Long> deptIds = new HashSet<>();
        for (LibraryDO lib : libs) {
            Long ownerId = lib.getOwnerId();
            if (ownerId == null) {
                continue;
            }
            Integer ownerDim = ownerDimMap.get(lib.getKbLevelId());
            if (ownerDim != null && ownerDim == 2) {
                deptIds.add(ownerId);
            } else {
                userIds.add(ownerId);
            }
        }

        Map<Long, String> result = new HashMap<>();
        // 用户维度 → 昵称
        if (!userIds.isEmpty()) {
            try {
                Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(userIds);
                for (Long id : userIds) {
                    AdminUserRespDTO user = userMap.get(id);
                    if (user != null) {
                        result.put(id, user.getNickname());
                    }
                }
            } catch (Exception e) {
                // 解析失败忽略
            }
        }
        // 部门维度 → 部门名
        if (!deptIds.isEmpty()) {
            try {
                Map<Long, DeptRespDTO> deptMap = deptApi.getDeptMap(deptIds);
                for (Long id : deptIds) {
                    DeptRespDTO dept = deptMap.get(id);
                    if (dept != null) {
                        result.put(id, dept.getName());
                    }
                }
            } catch (Exception e) {
                // 解析失败忽略
            }
        }
        return result;
    }

    /** 批量加载库对应的层级配置归属维度（kbLevelId → ownerDim） */
    private Map<Long, Integer> resolveOwnerDimMap(List<LibraryDO> libs) {
        Set<Long> kbLevelIds = libs.stream()
                .map(LibraryDO::getKbLevelId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (kbLevelIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, Integer> result = new HashMap<>();
        for (LevelConfigDO cfg : levelConfigMapper.selectBatchIds(kbLevelIds)) {
            result.put(cfg.getId(), cfg.getOwnerDim());
        }
        return result;
    }

    private FrontendKnowledgeBaseVO toVO(LibraryDO lib, Map<String, String> extValues, String ownerName,
                                         boolean canManage) {
        FrontendKnowledgeBaseVO vo = new FrontendKnowledgeBaseVO();
        vo.setId(lib.getId());
        vo.setName(lib.getName());
        vo.setDescription(lib.getDescription());
        vo.setCategory(lib.getCategoryId() != null ? String.valueOf(lib.getCategoryId()) : null);
        vo.setOwner(lib.getOwnerId());
        vo.setOwnerName(ownerName);
        vo.setIsPublic(lib.getIsPublic() != null && lib.getIsPublic() == 1);
        vo.setDocumentCount(lib.getDocCount());
        vo.setStatus(lib.getStatus());
        vo.setCreator(lib.getCreator());
        vo.setCanView(true);
        vo.setCanManage(canManage);
        vo.setCoverUrl(lib.getCoverUrl());
        vo.setExtValues(extValues);
        if (lib.getCreateTime() != null) {
            vo.setCreatedAt(lib.getCreateTime().format(DATETIME));
        }
        return vo;
    }
}
