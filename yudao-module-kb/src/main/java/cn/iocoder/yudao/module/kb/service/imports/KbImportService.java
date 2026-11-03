package cn.iocoder.yudao.module.kb.service.imports;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * 知识库完整导入 Service
 *
 * <p>与「知识库完整导出」配套：解析导出包 → 模型/维度预检 → 新建知识库（新 ID）→
 * 重建文档/文件夹/源文件 → 导入图片/切片/向量/Wiki（ID 重映射 + 图片 URL 改写）。</p>
 *
 * <p><b>安全边界</b>：导入只创建新库，不覆盖、不删除任何已有知识库；预检失败时
 * 不落任何数据。</p>
 */
public interface KbImportService {

    /**
     * 导入导出包（ZIP，小包走 multipart）。
     *
     * @param file 上传的 ZIP
     * @return 导入结果：{success, kbId, kbName, documents, chunks, vectors, wiki, images, warnings}
     */
    default Map<String, Object> importLibrary(MultipartFile file) throws IOException {
        return importLibrary(file, VECTOR_MODE_REUSE);
    }

    /**
     * 导入导出包（ZIP），可指定向量导入方式。
     *
     * @param file       上传的 ZIP
     * @param vectorMode reuse=沿用包内向量；reembed=用切片重跑向量；skip=不导入向量
     */
    Map<String, Object> importLibrary(MultipartFile file, String vectorMode) throws IOException;

    /**
     * 初始化分片上传会话（大包导入用：前端按 chunkSize 切片后逐片上传）。
     *
     * @param fileName  原文件名（仅用于报告）
     * @param totalSize 包总字节数
     * @return {uploadId, chunkSize, totalChunks}
     */
    Map<String, Object> initUpload(String fileName, long totalSize);

    /**
     * 上传一个分片（原始字节）。
     *
     * @param uploadId 会话 ID
     * @param index    分片序号（从 0 开始）
     * @param data     分片字节
     */
    Map<String, Object> uploadChunk(String uploadId, int index, byte[] data) throws IOException;

    /**
     * 查询分片上传进度（用于断点续传）。
     */
    Map<String, Object> getUploadStatus(String uploadId);

    /**
     * 合并分片并**异步**执行导入，立即返回 `{taskId, uploadId}`。
     *
     * <p>导入耗时可能从几十秒到数十分钟（reembed 尤其），接口不再同步等待，
     * 前端通过 {@link #getTaskProgress(String)} 轮询阶段与百分比。</p>
     *
     * <p>预检失败（向量维度不一致）时任务 `status=failed` 且 `result.retryable=true`，
     * 分片会话会被保留，前端可用同一 uploadId 以 `vectorMode=reembed` 重新 complete。</p>
     *
     * @param uploadId   会话 ID
     * @param vectorMode reuse / reembed / skip
     */
    Map<String, Object> completeUpload(String uploadId, String vectorMode) throws IOException;

    /**
     * 查询导入任务进度。
     *
     * @param taskId complete 返回的任务 ID
     * @return {status: running/done/failed, stage, progress, message, kbId, error, result}
     */
    Map<String, Object> getTaskProgress(String taskId);

    /** 沿用包内向量 */
    String VECTOR_MODE_REUSE = "reuse";
    /** 用包内切片重跑向量 */
    String VECTOR_MODE_REEMBED = "reembed";
    /** 不导入向量（仅切片/Wiki，后续可手动重建） */
    String VECTOR_MODE_SKIP = "skip";
}
