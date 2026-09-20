package cn.iocoder.yudao.module.kb.controller.admin.library;

import org.springframework.web.bind.annotation.*;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Value;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;

import jakarta.validation.constraints.*;
import jakarta.validation.*;
import jakarta.servlet.http.*;
import java.util.*;
import java.util.stream.Collectors;
import java.io.IOException;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.StrUtil;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.kb.enums.ErrorCodeConstants.*;

import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.*;

import cn.iocoder.yudao.module.kb.controller.admin.library.vo.*;
import cn.iocoder.yudao.module.kb.dal.dataobject.library.LibraryDO;
import cn.iocoder.yudao.module.kb.service.library.LibraryService;
import cn.iocoder.yudao.module.kb.service.libraryext.LibraryExtService;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;

@Tag(name = "管理后台 - 知识库")
@RestController
@RequestMapping("/kb/library")
@Validated
public class LibraryController {

    @Resource
    private LibraryService libraryService;

    @Resource
    private LibraryExtService libraryExtService;

    /**
     * Neo4j 基础设施启用开关（N1，默认关闭=先注释不启用）。
     * 置为 true 且环境变量 NEO4J_URI 非空时，前端 GraphEnabled 开关才可勾选。
     */
    @Value("${kb.postprocess.neo4j-enabled:false}")
    private boolean neo4jEnabled;

    @PostMapping("/create")
    @Operation(summary = "创建知识库")
    @PreAuthorize("@ss.hasPermission('kb:library:create')")
    public CommonResult<Long> createLibrary(@Valid @RequestBody LibrarySaveReqVO createReqVO) {
        return success(libraryService.createLibrary(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新知识库")
    @PreAuthorize("@ss.hasPermission('kb:library:update')")
    public CommonResult<Boolean> updateLibrary(@Valid @RequestBody LibrarySaveReqVO updateReqVO) {
        libraryService.updateLibrary(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除知识库")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('kb:library:delete')")
    public CommonResult<Boolean> deleteLibrary(@RequestParam("id") Long id) {
        libraryService.deleteLibrary(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号", required = true)
    @Operation(summary = "批量删除知识库")
                @PreAuthorize("@ss.hasPermission('kb:library:delete')")
    public CommonResult<Boolean> deleteLibraryList(@RequestParam("ids") List<Long> ids) {
        libraryService.deleteLibraryListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得知识库")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('kb:library:query')")
    public CommonResult<LibraryRespVO> getLibrary(@RequestParam("id") Long id) {
        LibraryDO library = libraryService.getLibrary(id);
        LibraryRespVO respVO = copyToResp(library);
        respVO.setExtValues(libraryExtService.getExtValues(id));
        fillProcessConfig(respVO, library);
        return success(respVO);
    }

    @GetMapping("/page")
    @Operation(summary = "获得知识库分页")
    @PreAuthorize("@ss.hasPermission('kb:library:query')")
    public CommonResult<PageResult<LibraryRespVO>> getLibraryPage(@Valid LibraryPageReqVO pageReqVO) {
        // 分页查询，Service 层会根据用户角色做可见性过滤（非超管只能看到自己有权限的知识库）
        PageResult<LibraryDO> pageResult = libraryService.getLibraryPage(pageReqVO);
        PageResult<LibraryRespVO> result = copyToResp(pageResult);
        // 附带自定义字段值 + KB级处理配置
        List<Long> kbIds = result.getList().stream().map(LibraryRespVO::getId).collect(Collectors.toList());
        Map<Long, Map<String, String>> extMap = libraryExtService.getExtValuesMap(kbIds);
        Map<Long, LibraryDO> libraryMap = pageResult.getList().stream()
                .collect(Collectors.toMap(LibraryDO::getId, lib -> lib));
        result.getList().forEach(vo -> {
            vo.setExtValues(extMap.get(vo.getId()));
            fillProcessConfig(vo, libraryMap.get(vo.getId()));
        });
        return success(result);
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出知识库 Excel")
    @PreAuthorize("@ss.hasPermission('kb:library:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportLibraryExcel(@Valid LibraryPageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<LibraryDO> list = libraryService.getLibraryPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "知识库.xls", "数据", LibraryRespVO.class,
                        copyToResp(list));
    }

    @PutMapping("/toggle-public")
    @Operation(summary = "切换知识库公开状态")
    @PreAuthorize("@ss.hasPermission('kb:library:update')")
    public CommonResult<Boolean> togglePublic(@RequestParam("id") Long id) {
        libraryService.togglePublic(id);
        return success(true);
    }

    @GetMapping("/public-page")
    @Operation(summary = "获得广场公开知识库分页（全部公开）")
    @PreAuthorize("@ss.hasPermission('kb:library:query')")
    public CommonResult<PageResult<LibraryRespVO>> getPublicPage(@Valid PageParam pageParam) {
        PageResult<LibraryDO> pageResult = libraryService.getPublicPage(pageParam);
        PageResult<LibraryRespVO> result = copyToResp(pageResult);
        fillProcessConfigList(result, pageResult);
        return success(result);
    }

    @GetMapping("/simple-list")
    @Operation(summary = "获得知识库精简列表（用于下拉选择）")
    @Parameter(name = "isProject", description = "是否项目成果库（可选，不传=全部）")
    @PreAuthorize("@ss.hasPermission('kb:library:query')")
    public CommonResult<List<LibrarySimpleVO>> getSimpleLibraryList(
            @RequestParam(value = "isProject", required = false) Integer isProject) {
        List<LibraryDO> list = libraryService.getSimpleLibraryList(isProject);
        return success(BeanUtils.toBean(list, LibrarySimpleVO.class));
    }

    @GetMapping("/my-public-page")
    @Operation(summary = "获得我公开的知识库分页")
    @PreAuthorize("@ss.hasPermission('kb:library:query')")
    public CommonResult<PageResult<LibraryRespVO>> getMyPublicPage(@Valid PageParam pageParam) {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        PageResult<LibraryDO> pageResult = libraryService.getMyPublicPage(pageParam, userId);
        PageResult<LibraryRespVO> result = copyToResp(pageResult);
        fillProcessConfigList(result, pageResult);
        return success(result);
    }

    @GetMapping("/can-manage")
    @Operation(summary = "检查当前用户是否有该知识库的管理权限")
    @Parameter(name = "kbId", description = "知识库ID", required = true)
    @PreAuthorize("@ss.hasPermission('kb:library:query')")
    public CommonResult<Boolean> canManage(@RequestParam("kbId") Long kbId) {
        return success(libraryService.canManage(kbId));
    }

    @GetMapping("/can-open")
    @Operation(summary = "检查当前用户能否打开该知识库（项目成果库：租户管理员短路或为项目成员，实时读成员字段 ext 值）")
    @Parameter(name = "kbId", description = "知识库ID", required = true)
    @PreAuthorize("@ss.hasPermission('kb:library:query')")
    public CommonResult<Boolean> canOpen(@RequestParam("kbId") Long kbId) {
        return success(libraryService.canOpenProjectLibrary(kbId));
    }

    @GetMapping("/postprocess-infra")
    @Operation(summary = "获取后处理基础设施就绪状态（Neo4j 是否已配置，供前端置灰 GraphEnabled 开关）")
    @PreAuthorize("@ss.hasPermission('kb:library:query')")
    public CommonResult<Map<String, Object>> getPostprocessInfra() {
        // Neo4j 是否就绪：配置项 kb.postprocess.neo4j-enabled 且环境变量 NEO4J_URI 非空（N1 基础设施未启用时默认 false）
        boolean neo4jReady = neo4jEnabled && StrUtil.isNotBlank(System.getenv("NEO4J_URI"));
        Map<String, Object> result = new HashMap<>();
        result.put("neo4jReady", neo4jReady);
        result.put("neo4jEnabledConfig", neo4jEnabled); // 配置项是否开启（诊断用）
        return success(result);
    }

    // ==================== 私有辅助 ====================

    /**
     * DO → RespVO 拷贝：{@code processConfig} 在 DO 中是 JSON 字符串、在 RespVO 中是 {@code Map}，
     * hutool 的 convertField 无法完成 String→Map 转换（抛 {@code UnsupportedOperationException}，且
     * ignoreError 只保护 setValue、保护不了 convertField），故显式忽略该字段，
     * 由 {@link #fillProcessConfig} 反序列化统一填充。
     */
    private LibraryRespVO copyToResp(LibraryDO library) {
        LibraryRespVO respVO = new LibraryRespVO();
        CopyOptions options = CopyOptions.create().setIgnoreProperties("processConfig");
        BeanUtil.copyProperties(library, respVO, options);
        return respVO;
    }

    private List<LibraryRespVO> copyToResp(List<LibraryDO> libraries) {
        return libraries.stream().map(this::copyToResp).collect(Collectors.toList());
    }

    private PageResult<LibraryRespVO> copyToResp(PageResult<LibraryDO> pageResult) {
        return new PageResult<>(copyToResp(pageResult.getList()), pageResult.getTotal());
    }

    /**
     * 把 DO 的 process_config(JSON字符串) 反序列化为 Map 填充到 RespVO（键名与系统配置一致）。
     */
    private void fillProcessConfig(LibraryRespVO respVO, LibraryDO library) {
        if (respVO == null || library == null || StrUtil.isBlank(library.getProcessConfig())) {
            return;
        }
        Map<String, Object> config = JsonUtils.parseMap(library.getProcessConfig());
        if (config != null) {
            respVO.setProcessConfig(config);
        }
    }

    /**
     * 分页响应批量填充 processConfig（按 id 配对 DO 与 VO）。
     */
    private void fillProcessConfigList(PageResult<LibraryRespVO> result, PageResult<LibraryDO> source) {
        if (result == null || source == null || result.getList() == null || source.getList() == null
                || result.getList().isEmpty()) {
            return;
        }
        Map<Long, LibraryDO> libraryMap = source.getList().stream()
                .collect(Collectors.toMap(LibraryDO::getId, lib -> lib));
        result.getList().forEach(vo -> fillProcessConfig(vo, libraryMap.get(vo.getId())));
    }
}
