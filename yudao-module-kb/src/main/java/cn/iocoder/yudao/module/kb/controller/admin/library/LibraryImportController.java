package cn.iocoder.yudao.module.kb.controller.admin.library;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.kb.service.imports.KbImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.IMPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - 知识库完整导入
 *
 * <p>上传「知识库完整导出」生成的 ZIP，新建一个知识库并恢复其文档/源文件/图片/切片/向量/Wiki。
 * 导入前自动校验 embedding 模型与向量维度：维度不一致直接中止，不落任何数据。</p>
 */
@Tag(name = "管理后台 - 知识库完整导入")
@RestController
@RequestMapping("/kb/library")
@Validated
public class LibraryImportController {

    @Resource
    private KbImportService kbImportService;

    @PostMapping("/import-package")
    @Operation(summary = "导入知识库数据包（ZIP，小包直传；大包请用分片上传）")
    @Parameter(name = "file", description = "导出包（.zip）", required = true)
    @PreAuthorize("@ss.hasPermission('kb:library:create')")
    @ApiAccessLog(operateType = IMPORT)
    public CommonResult<Map<String, Object>> importPackage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "vectorMode", required = false, defaultValue = "reuse") String vectorMode)
            throws IOException {
        return success(kbImportService.importLibrary(file, vectorMode));
    }

    // ==================== 分片上传（规避 multipart/网关体积限制，支持几 GB 的包） ====================

    @PostMapping("/import-package/init")
    @Operation(summary = "初始化导入包分片上传")
    @Parameter(name = "fileName", description = "文件名", required = true)
    @Parameter(name = "totalSize", description = "包总字节数", required = true)
    @PreAuthorize("@ss.hasPermission('kb:library:create')")
    public CommonResult<Map<String, Object>> initUpload(@RequestParam("fileName") String fileName,
                                                        @RequestParam("totalSize") Long totalSize) {
        return success(kbImportService.initUpload(fileName, totalSize == null ? 0L : totalSize));
    }

    @PutMapping("/import-package/chunk")
    @Operation(summary = "上传导入包分片（原始字节）")
    @Parameter(name = "uploadId", description = "上传会话 ID", required = true)
    @Parameter(name = "index", description = "分片序号（从 0 开始）", required = true)
    @PreAuthorize("@ss.hasPermission('kb:library:create')")
    public CommonResult<Map<String, Object>> uploadChunk(@RequestParam("uploadId") String uploadId,
                                                         @RequestParam("index") Integer index,
                                                         @RequestBody(required = false) byte[] data)
            throws IOException {
        return success(kbImportService.uploadChunk(uploadId, index == null ? -1 : index, data));
    }

    @GetMapping("/import-package/status")
    @Operation(summary = "查询导入包分片上传进度（断点续传）")
    @Parameter(name = "uploadId", description = "上传会话 ID", required = true)
    @PreAuthorize("@ss.hasPermission('kb:library:create')")
    public CommonResult<Map<String, Object>> uploadStatus(@RequestParam("uploadId") String uploadId) {
        return success(kbImportService.getUploadStatus(uploadId));
    }

    @PostMapping("/import-package/complete")
    @Operation(summary = "合并分片并异步执行导入（立即返回 taskId，进度用 task 接口轮询）")
    @Parameter(name = "uploadId", description = "上传会话 ID", required = true)
    @Parameter(name = "vectorMode", description = "向量导入方式：reuse / reembed / skip", example = "reuse")
    @PreAuthorize("@ss.hasPermission('kb:library:create')")
    @ApiAccessLog(operateType = IMPORT)
    public CommonResult<Map<String, Object>> completeUpload(
            @RequestParam("uploadId") String uploadId,
            @RequestParam(value = "vectorMode", required = false, defaultValue = "reuse") String vectorMode)
            throws IOException {
        return success(kbImportService.completeUpload(uploadId, vectorMode));
    }

    @GetMapping("/import-package/task/{taskId}")
    @Operation(summary = "查询导入任务进度（阶段 / 百分比 / 结果）")
    @Parameter(name = "taskId", description = "complete 返回的任务 ID", required = true)
    @PreAuthorize("@ss.hasPermission('kb:library:create')")
    public CommonResult<Map<String, Object>> importTaskProgress(@PathVariable("taskId") String taskId) {
        return success(kbImportService.getTaskProgress(taskId));
    }
}
