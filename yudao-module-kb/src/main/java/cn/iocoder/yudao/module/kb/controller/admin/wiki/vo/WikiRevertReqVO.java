package cn.iocoder.yudao.module.kb.controller.admin.wiki.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Wiki 页面回滚请求 VO
 */
@Schema(description = "Wiki 页面回滚请求 VO")
@Data
public class WikiRevertReqVO {

    @Schema(description = "知识库ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "知识库ID不能为空")
    private Long kbId;

    @Schema(description = "页面 slug", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "页面 slug 不能为空")
    private String slug;

    @Schema(description = "目标版本号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "目标版本号不能为空")
    private Integer targetRevision;

    @Schema(description = "期望版本号（乐观锁）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "期望版本号不能为空")
    private Integer expectedRevision;
}