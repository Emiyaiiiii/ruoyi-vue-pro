package cn.iocoder.yudao.module.kb.controller.admin.chunk.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 切片回滚请求 VO（#7 修订历史回滚）
 */
@Schema(description = "切片回滚请求 VO")
@Data
public class ChunkRevertReqVO {

    @Schema(description = "知识库ID（可选，后端按文档归属兜底）")
    private Long kbId;

    @Schema(description = "文档ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "文档ID不能为空")
    private Long documentId;

    @Schema(description = "切片序号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "切片序号不能为空")
    private Integer chunkIndex;

    @Schema(description = "目标版本号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "目标版本号不能为空")
    private Integer targetRevision;

    @Schema(description = "期望版本号（乐观锁）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "期望版本号不能为空")
    private Integer expectedRevision;
}
