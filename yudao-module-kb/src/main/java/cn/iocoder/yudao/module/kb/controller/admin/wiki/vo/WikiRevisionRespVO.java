package cn.iocoder.yudao.module.kb.controller.admin.wiki.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Wiki 修订历史响应 VO（修订抽屉）
 */
@Schema(description = "Wiki 修订历史响应 VO")
@Data
public class WikiRevisionRespVO {

    @Schema(description = "修订ID")
    private Long id;

    @Schema(description = "页面ID")
    private Long pageId;

    @Schema(description = "版本号")
    private Integer revision;

    @Schema(description = "该版本标题")
    private String title;

    @Schema(description = "该版本内容")
    private String content;

    @Schema(description = "该版本摘要")
    private String summary;

    @Schema(description = "页类型")
    private String pageType;

    @Schema(description = "编辑来源: pipeline/user/revert")
    private String editSource;

    @Schema(description = "操作人昵称")
    private String operatorName;

    @Schema(description = "创建时间(该版本写入时间)")
    private LocalDateTime createTime;
}