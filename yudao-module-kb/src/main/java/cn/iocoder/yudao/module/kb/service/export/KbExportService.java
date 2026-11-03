package cn.iocoder.yudao.module.kb.service.export;

import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

/**
 * 知识库完整导出 Service
 *
 * <p>把知识库的全部数据（库配置 / 文档元数据 / 源文件 / 切片 / 向量 / Wiki / 向量任务）
 * 打包为 ZIP 流式下载。全链路**只读**：不删除、不修改任何线上数据
 * （MySQL 只查、ES/Milvus 只 scan、MinIO 只取对象）。</p>
 */
public interface KbExportService {

    /**
     * 导出单个知识库为 ZIP（直接写入 HTTP 响应流，不落地临时文件）。
     *
     * @param kbId         知识库 ID
     * @param includeFiles 是否包含原始文档文件
     * @param response     HTTP 响应
     */
    void exportLibrary(Long kbId, boolean includeFiles, HttpServletResponse response) throws IOException;

    /**
     * 导出当前用户可见的全部知识库为单个 ZIP（每个知识库一个子目录）。
     *
     * @param includeFiles 是否包含原始文档文件
     * @param response     HTTP 响应
     */
    void exportAllLibraries(boolean includeFiles, HttpServletResponse response) throws IOException;

    /**
     * 导出前预览单库数据量（文档 / 切片 / 向量 / Wiki 条数），供前端二次确认。
     *
     * @param kbId 知识库 ID
     * @return 统计信息
     */
    Map<String, Object> getExportManifest(Long kbId);
}
