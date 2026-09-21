package cn.iocoder.yudao.module.kb.controller.admin.chunk.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 切片编辑请求 VO（#7 chunk 编辑）
 */
@Schema(description = "切片编辑请求 VO")
@Data
public class ChunkUpdateReqVO {

    @Schema(description = "知识库ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "知识库ID不能为空")
    private Long kbId;

    @Schema(description = "文档ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "文档ID不能为空")
    private Long documentId;

    @Schema(description = "切片序号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "切片序号不能为空")
    private Integer chunkIndex;

    @Schema(description = "编辑后的切片内容", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "切片内容不能为空")
    private String content;

    @Schema(description = "期望版本号（乐观锁，0=从未编辑）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "期望版本号不能为空")
    private Integer expectedRevision;
}
