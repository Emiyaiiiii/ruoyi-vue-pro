package cn.iocoder.yudao.module.kb.controller.admin.library;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.kb.service.export.KbExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - 知识库完整导出
 *
 * <p>与「导出 Excel」（列表字段）区分：本接口导出的是**单个知识库的完整数据快照**，
 * 包含库配置、文档元数据与源文件、切片、向量、Wiki、向量任务，打包为 ZIP 下载。
 * 全程只读，不影响线上数据。</p>
 */
@Tag(name = "管理后台 - 知识库完整导出")
@RestController
@RequestMapping("/kb/library")
@Validated
public class LibraryExportController {

    @Resource
    private KbExportService kbExportService;

    @GetMapping("/export-package")
    @Operation(summary = "完整导出知识库（配置/文档/切片/向量/Wiki，ZIP 下载）")
    @Parameter(name = "id", description = "知识库编号", required = true, example = "1024")
    @Parameter(name = "includeFiles", description = "是否包含原始文档文件", example = "true")
    @PreAuthorize("@ss.hasPermission('kb:library:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportPackage(@RequestParam("id") @NotNull Long id,
                              @RequestParam(value = "includeFiles", required = false, defaultValue = "true")
                              Boolean includeFiles,
                              HttpServletResponse response) throws IOException {
        kbExportService.exportLibrary(id, !Boolean.FALSE.equals(includeFiles), response);
    }

    @GetMapping("/export-all")
    @Operation(summary = "完整导出全部可见知识库（ZIP 下载，每个库一个子目录）")
    @Parameter(name = "includeFiles", description = "是否包含原始文档文件", example = "true")
    @PreAuthorize("@ss.hasPermission('kb:library:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportAll(@RequestParam(value = "includeFiles", required = false, defaultValue = "true")
                          Boolean includeFiles,
                          HttpServletResponse response) throws IOException {
        kbExportService.exportAllLibraries(!Boolean.FALSE.equals(includeFiles), response);
    }

    @GetMapping("/export-manifest")
    @Operation(summary = "预览知识库导出数据量（文档/切片/向量/Wiki 条数）")
    @Parameter(name = "id", description = "知识库编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('kb:library:export')")
    public CommonResult<Map<String, Object>> getExportManifest(@RequestParam("id") @NotNull Long id) {
        return success(kbExportService.getExportManifest(id));
    }
}
