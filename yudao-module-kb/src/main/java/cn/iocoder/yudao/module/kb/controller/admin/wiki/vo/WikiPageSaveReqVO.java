package cn.iocoder.yudao.module.kb.controller.admin.wiki.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Wiki 页面保存请求 VO（人工编辑/更新）
 *
 * <p>乐观锁：expectedRevision 必须等于当前 version，冲突返回 WIKI_PAGE_CONFLICT。</p>
 */
@Schema(description = "Wiki 页面保存请求 VO")
@Data
public class WikiPageSaveReqVO {

    @Schema(description = "知识库ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "知识库ID不能为空")
    private Long kbId;

    @Schema(description = "页面 slug（URL 友好，KB 内唯一）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "页面 slug 不能为空")
    private String slug;

    @Schema(description = "页面标题", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "页面标题不能为空")
    private String title;

    @Schema(description = "markdown 正文", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "页面内容不能为空")
    private String content;

    @Schema(description = "摘要")
    private String summary;

    @Schema(description = "别名列表(json)")
    private String aliases;

    @Schema(description = "页面类型: manual/entity/concept/root")
    private String pageType;

    @Schema(description = "所属目录ID(0=根)")
    private Long folderId;

    @Schema(description = "状态: draft/published/archived")
    private String status;

    @Schema(description = "期望版本号（乐观锁）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "期望版本号不能为空")
    private Integer expectedRevision;
}