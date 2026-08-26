package cn.iocoder.yudao.module.kb.controller.admin.vectortask.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@Schema(description = "管理后台 - 向量处理任务提交 Request VO")
@Data
public class VectorTaskSubmitReqVO {

    @Schema(description = "文档ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "123")
    @NotNull(message = "文档ID不能为空")
    private Long docId;

    @Schema(description = "知识库ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "456")
    @NotNull(message = "知识库ID不能为空")
    private Long kbId;

    @Schema(description = "文件下载地址", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "文件下载地址不能为空")
    private String fileUrl;

    @Schema(description = "文件类型", example = "pdf")
    private String fileType;

    @Schema(description = "分块大小", example = "512")
    private Integer chunkSize;

    @Schema(description = "分块重叠", example = "50")
    private Integer chunkOverlap;

    @Schema(description = "Embedding模型", example = "text-embedding-3-small")
    private String embeddingModel;

    @Schema(description = "大模型配置(JSON字符串，覆盖python-vector本地配置)", example = "{\"base_url\":\"http://xxx/v1\",\"api_key\":\"sk-xxx\",\"model\":\"qwen-max\"}")
    private String llmConfig;

    @Schema(description = "OCR/多模态模型配置(JSON字符串)", example = "{\"ocr_api_url\":\"http://xxx/v1\",\"ocr_model\":\"deepseek-ai/DeepSeek-OCR-2\",\"multimodal_api_url\":\"http://xxx/v1\",\"multimodal_model\":\"qwen-vl\"}")
    private String ocrConfig;

    @Schema(description = "分块策略", example = "fixed_size")
    private String strategy;

    @Schema(description = "最小分块大小", example = "100")
    private Integer minChunkSize;

    @Schema(description = "最大分块大小", example = "2000")
    private Integer maxChunkSize;

    @Schema(description = "解析选项(JSON字符串)", example = "{\"ocr\":true}")
    private String parseOptions;

    @Schema(description = "文档标题", example = "产品手册.pdf")
    private String documentTitle;

    @Schema(description = "索引分组(用于按知识库/租户路由集合)", example = "kb_456")
    private String indexGroup;
}
