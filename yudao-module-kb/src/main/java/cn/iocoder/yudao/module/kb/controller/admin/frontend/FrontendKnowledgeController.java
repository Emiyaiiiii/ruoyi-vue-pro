package cn.iocoder.yudao.module.kb.controller.admin.frontend;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.kb.controller.admin.frontend.vo.FrontendKnowledgeBaseSaveReqVO;
import cn.iocoder.yudao.module.kb.controller.admin.frontend.vo.FrontendKnowledgeBaseVO;
import cn.iocoder.yudao.module.kb.controller.admin.frontend.vo.FrontendPermissionsVO;
import cn.iocoder.yudao.module.kb.controller.admin.frontend.vo.FrontendResult;
import cn.iocoder.yudao.module.kb.controller.admin.library.vo.LibraryPageReqVO;
import cn.iocoder.yudao.module.kb.controller.admin.library.vo.LibrarySaveReqVO;
import cn.iocoder.yudao.module.kb.dal.dataobject.category.CategoryDO;
import cn.iocoder.yudao.module.kb.dal.dataobject.chunkmethod.ChunkMethodDO;
import cn.iocoder.yudao.module.kb.dal.dataobject.levelconfig.LevelConfigDO;
import cn.iocoder.yudao.module.kb.dal.dataobject.library.LibraryDO;
import cn.iocoder.yudao.module.kb.dal.mysql.category.CategoryMapper;
import cn.iocoder.yudao.module.kb.dal.mysql.chunkmethod.ChunkMethodMapper;
import cn.iocoder.yudao.module.kb.dal.mysql.document.DocumentMapper;
import cn.iocoder.yudao.module.kb.dal.mysql.folder.FolderMapper;
import cn.iocoder.yudao.module.kb.dal.mysql.follow.FollowMapper;
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
import java.util.ArrayList;
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

    /** ext_values 保留 key：标签（JSON 数组字符串） */
    private static final String EXT_TAGS = "__tags";
    /** ext_values 保留 key：分类类型 */
    private static final String EXT_CATEGORY_TYPE = "__category_type";
    /** ext_values 保留 key：所属部门 */
    private static final String EXT_ORGANIZATION = "__organization";

    @Resource
    private LibraryMapper libraryMapper;

    @Resource
    private DocumentMapper documentMapper;

    @Resource
    private FolderMapper folderMapper;

    @Resource
    private FollowMapper followMapper;

    @Resource
    private CategoryMapper categoryMapper;

    @Resource
    private ChunkMethodMapper chunkMethodMapper;

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
            @RequestParam(value = "ordering", required = false) String ordering,
            @RequestParam(value = "is_public", required = false) Boolean isPublic) {
        PageParam pageParam = buildPageParam(page, pageSize);
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        return toListResult(queryByCategory(category, pageParam, search, userId, sortField, sortOrder, ordering, isPublic));
    }

    /**
     * 按分类获取知识库（对应 /knowledge/bases/{category}，不带尾斜杠，避免与详情 /{id}/ 冲突）
     * 分类取值：public / my-public / my-follows / personal-kbase 等
     */
    @GetMapping({"/knowledge/bases/{category}"})
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
        return toListResult(queryByCategory(category, pageParam, search, userId, sortField, sortOrder, ordering, null));
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
    public FrontendResult<Map<String, Object>> create(@RequestBody FrontendKnowledgeBaseSaveReqVO req) {
        Long id = libraryService.createLibrary(buildSaveVO(req, null));
        Map<String, Object> data = new HashMap<>();
        data.put("id", id);
        data.put("owner", SecurityFrameworkUtils.getLoginUserId());
        return FrontendResult.ok(data);
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
     * 知识库详情（对应 /knowledge/bases/{id}/，与分类列表路径区分：详情带尾斜杠、分类不带）
     */
    @GetMapping({"/knowledge/bases/{id}/"})
    @Operation(summary = "前端知识库详情")
    public FrontendResult<FrontendKnowledgeBaseVO> detail(@PathVariable("id") Long id) {
        LibraryDO lib = libraryService.getLibrary(id);
        if (lib == null) {
            return FrontendResult.error("知识库不存在");
        }
        Map<String, String> extValues = libraryExtService.getExtValuesMap(Collections.singletonList(id))
                .getOrDefault(id, Collections.emptyMap());
        String ownerName = resolveOwnerName(lib);
        boolean canManage = libraryService.canManage(id);
        Set<Long> openableIds = libraryService.filterOpenProjectLibraries(Collections.singletonList(id));
        FrontendKnowledgeBaseVO vo = toVO(lib, extValues, ownerName, canManage, openableIds);
        enrichStats(Collections.singletonList(lib), Collections.singletonList(vo));
        enrichChunkConfig(lib, vo);
        return FrontendResult.ok(vo);
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
        // 项目成果库：非项目成员（含创建者，租户管理员/超管除外）无查看/获取/下载权限（只能看列表，不能打开库）
        boolean canView = true;
        if (lib.getIsProject() != null && lib.getIsProject() == 1) {
            canView = libraryService.canOpenProjectLibrary(id);
        }
        FrontendPermissionsVO vo = new FrontendPermissionsVO();
        vo.setCanView(canView);
        vo.setCanGet(canView);
        vo.setCanDownload(canView);
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
                                                  String sortField, String sortOrder, String ordering, Boolean isPublic) {
        if (category == null || category.isEmpty()) {
            return queryPublic(pageParam, search, sortField, sortOrder, ordering, isPublic);
        }
        // 分类主键 → 复用知识库大屏的 getLibraryPage（含可见性规则）
        if (category.matches("\\d+")) {
            return queryByCategoryId(Long.parseLong(category), pageParam, search, sortField, sortOrder, ordering, isPublic);
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
        return queryPublic(pageParam, search, sortField, sortOrder, ordering, isPublic);
    }

    /** 按分类主键查询知识库列表（复用知识库大屏逻辑，含可见性过滤与排序） */
    private PageResult<LibraryDO> queryByCategoryId(Long categoryId, PageParam pageParam, String search,
                                                    String sortField, String sortOrder, String ordering, Boolean isPublic) {
        LibraryPageReqVO reqVO = new LibraryPageReqVO();
        reqVO.setPageNo(pageParam.getPageNo());
        reqVO.setPageSize(pageParam.getPageSize());
        reqVO.setCategoryId(categoryId);
        reqVO.setName(search);
        if (isPublic != null) {
            reqVO.setIsPublic(isPublic ? 1 : 0);
        }
        applySort(reqVO, sortField, sortOrder, ordering);
        return libraryService.getLibraryPage(reqVO);
    }

    private PageResult<LibraryDO> queryPublic(PageParam pageParam, String search,
                                              String sortField, String sortOrder, String ordering, Boolean isPublic) {
        LibraryPageReqVO reqVO = new LibraryPageReqVO();
        reqVO.setPageNo(pageParam.getPageNo());
        reqVO.setPageSize(pageParam.getPageSize());
        // is_public 参数存在时以其为准（前端"公开/私有"筛选），缺省只看公开
        // 三目表达式一边为基本类型一边为 null 时会按基本类型推导，false 分支拆箱抛 NPE，禁止写成 (isPublic ? 1 : null)
        if (isPublic == null) {
            reqVO.setIsPublic(1);
        } else {
            reqVO.setIsPublic(isPublic ? 1 : 0);
        }
        reqVO.setStatus(0);
        reqVO.setName(search);
        applySort(reqVO, sortField, sortOrder, ordering);
        // “非公开”筛选必须叠加 VisibilityRuleEngine 的可见性条件：
        // 直接 selectPage 会把同租户下他人的私有库也查出来（越权）。
        // “公开/缺省”保持广场语义：is_public=1 本身即全员可见，无需再按规则过滤。
        if (Boolean.FALSE.equals(isPublic)) {
            return libraryService.getLibraryPage(reqVO);
        }
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
                save.setName(existing.getName());
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

        // 业务字段（标签/分类类型/所属部门）落到 ext_values 保留 key，
        // 详情/列表回显时再解析回顶层字段，避免与自定义字段冲突
        Map<String, String> ext = new HashMap<>();
        if (save.getExtValues() != null) {
            ext.putAll(save.getExtValues());
        }
        if (req.getTags() != null) {
            ext.put(EXT_TAGS, JsonUtils.toJsonString(req.getTags()));
        }
        if (req.getCategoryType() != null) {
            ext.put(EXT_CATEGORY_TYPE, String.valueOf(req.getCategoryType()));
        }
        if (req.getOrganization() != null) {
            ext.put(EXT_ORGANIZATION, String.valueOf(req.getOrganization()));
        }
        if (!ext.isEmpty()) {
            save.setExtValues(ext);
        }

        // KB 级处理配置：优先采用前端全量 process_config（含 chunking/retrieval/rerank/model/postprocess），
        // 再以 chunking_method_id / chunking_params 覆盖 chunking 组（兼容旧版 chunk-config 接口语义）。
        // 更新时以现有库 processConfig 为基础，避免未传入的组被清空。
        if (req.getProcessConfig() != null || (req.getChunkingParams() != null && !req.getChunkingParams().isEmpty())
                || req.getChunkingMethodId() != null) {
            Map<String, Object> process = new HashMap<>();
            if (id != null) {
                LibraryDO existing = libraryService.getLibrary(id);
                if (existing != null && existing.getProcessConfig() != null
                        && !existing.getProcessConfig().isEmpty()) {
                    try {
                        process = JsonUtils.parseObject(existing.getProcessConfig(), Map.class);
                    } catch (Exception ignored) {
                    }
                }
            }
            if (process == null) {
                process = new HashMap<>();
            }
            // 全量覆盖：前端显式传入的组直接替换
            if (req.getProcessConfig() != null) {
                process.putAll(req.getProcessConfig());
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> chunking = (Map<String, Object>) process.computeIfAbsent("chunking", k -> new HashMap<String, Object>());
            if (req.getChunkingMethodId() != null) {
                ChunkMethodDO method = chunkMethodMapper.selectById(req.getChunkingMethodId());
                if (method != null) {
                    chunking.put("strategy", method.getCode());
                }
            }
            if (req.getChunkingParams() != null) {
                // 按所选切片方法重建切片参数：清除旧方法的残留参数，但保留 strategy（方法码）
                // 与 image_strategy（图片处理方案，独立于切片方法，不随切片参数重建而清除）
                if (req.getChunkingMethodId() != null) {
                    chunking.keySet().removeIf(k -> !"strategy".equals(k) && !"image_strategy".equals(k));
                }
                chunking.putAll(req.getChunkingParams());
            }
            save.setProcessConfig(process);
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

    /** 解析单个知识库的所有者显示名（按 ownerDim 区分用户昵称/部门名） */
    private String resolveOwnerName(LibraryDO lib) {
        return resolveOwnerNames(Collections.singletonList(lib)).get(lib.getOwnerId());
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
        Set<Long> openableIds = libraryService.filterOpenProjectLibraries(kbIds);

        List<FrontendKnowledgeBaseVO> list = libs.stream()
                .map(lib -> toVO(lib, extMap.get(lib.getId()), ownerNames.get(lib.getOwnerId()),
                        Boolean.TRUE.equals(canManageMap.get(lib.getId())), openableIds))
                .collect(Collectors.toList());
        enrichStats(libs, list);
        return FrontendResult.okList(list, page.getTotal());
    }

    /**
     * 批量填充卡片统计字段：关注数 / 文件数 / 存储用量（MB）/ 更新时间。
     * 用一次 groupBy 聚合替代逐库 N+1 查询。
     */
    private void enrichStats(List<LibraryDO> libs, List<FrontendKnowledgeBaseVO> vos) {
        if (libs == null || libs.isEmpty()) {
            return;
        }
        List<Long> kbIds = libs.stream().map(LibraryDO::getId).collect(Collectors.toList());
        Map<Long, Long> followCountMap = followMapper.selectFollowCountMap(kbIds);
        Map<Long, Long> storageBytesMap = documentMapper.selectStorageBytesMap(kbIds);
        // 文件夹数按 KB 聚合（kb_document_folder），与 document_count（文档数）为不同指标
        Map<Long, Long> folderCountMap = folderMapper.selectFolderCountMap(kbIds);
        for (int i = 0; i < libs.size(); i++) {
            LibraryDO lib = libs.get(i);
            FrontendKnowledgeBaseVO vo = vos.get(i);
            Long kbId = lib.getId();
            vo.setFollowersCount(followCountMap.getOrDefault(kbId, 0L));
            vo.setFolderCount(folderCountMap.getOrDefault(kbId, 0L));
            long bytes = storageBytesMap.getOrDefault(kbId, 0L);
            // 保留两位小数，避免前端 MB 分支显示长浮点
            vo.setUsedStorageMb(Math.round(bytes / 1024.0 / 1024.0 * 100.0) / 100.0);
            if (lib.getUpdateTime() != null) {
                vo.setUpdatedAt(lib.getUpdateTime().format(DATETIME));
            }
        }
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
                                         boolean canManage, Set<Long> openableIds) {
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
        // 项目成果库：非项目成员只能看列表，不能打开（can_view=false，前端据此拦截跳转）
        if (lib.getIsProject() != null && lib.getIsProject() == 1) {
            vo.setCanView(openableIds != null && openableIds.contains(lib.getId()));
        } else {
            vo.setCanView(true);
        }
        vo.setCanManage(canManage);
        vo.setCoverUrl(lib.getCoverUrl());
        vo.setExtValues(extValues);
        if (lib.getCreateTime() != null) {
            vo.setCreatedAt(lib.getCreateTime().format(DATETIME));
        }
        // 从 ext_values 保留 key 解析业务字段
        vo.setTags(parseTagsFromExt(extValues != null ? extValues.get(EXT_TAGS) : null));
        if (extValues != null) {
            if (extValues.get(EXT_CATEGORY_TYPE) != null) {
                vo.setCategoryType(parseLongSafe(extValues.get(EXT_CATEGORY_TYPE)));
            }
            if (extValues.get(EXT_ORGANIZATION) != null) {
                vo.setOrganization(parseLongSafe(extValues.get(EXT_ORGANIZATION)));
            }
        }
        // 是否已关注
        if (lib.getId() != null) {
            vo.setIsFollow(followService.isFollowing(lib.getId(), SecurityFrameworkUtils.getLoginUserId()));
        }
        return vo;
    }

    private List<String> parseTagsFromExt(String raw) {
        if (raw == null || raw.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            List<String> tags = JsonUtils.parseArray(raw, String.class);
            return tags == null ? Collections.emptyList() : tags;
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
    }

    private Long parseLongSafe(String raw) {
        try {
            return Long.parseLong(raw);
        } catch (Exception ignored) {
            return null;
        }
    }

    /** 详情接口补充切片配置：输出 process_config 及 strategy 反查的切片方法信息（前端表单回显用，仅详情调用，无列表 N+1） */
    private void enrichChunkConfig(LibraryDO lib, FrontendKnowledgeBaseVO vo) {
        Map<String, Object> process = parseJsonMap(lib.getProcessConfig());
        vo.setProcessConfig(process);
        Object chunking = process != null ? process.get("chunking") : null;
        if (!(chunking instanceof Map)) {
            return;
        }
        Object strategy = ((Map<?, ?>) chunking).get("strategy");
        if (strategy == null) {
            return;
        }
        ChunkMethodDO method = chunkMethodMapper.selectByCode(String.valueOf(strategy));
        if (method == null) {
            return;
        }
        vo.setChunkingMethodId(method.getId());
        vo.setParametersTemplate(parseJsonMap(method.getParametersTemplate()));
        vo.setDefaultParameters(parseJsonMap(method.getDefaultParameters()));
    }

    private Map<String, Object> parseJsonMap(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return JsonUtils.parseObject(json, Map.class);
        } catch (Exception ignored) {
            return null;
        }
    }
}
