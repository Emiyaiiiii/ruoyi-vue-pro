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
}
