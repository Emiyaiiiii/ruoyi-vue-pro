package cn.iocoder.yudao.module.kb.controller.admin.wiki.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Wiki 页面响应 VO（列表/详情）
 */
@Schema(description = "Wiki 页面响应 VO")
@Data
public class WikiPageRespVO {

    @Schema(description = "页面ID")
    private Long id;

    @Schema(description = "知识库ID")
    private Long kbId;

    @Schema(description = "页面 slug（KB 内唯一）")
    private String slug;

    @Schema(description = "页面标题")
    private String title;

    @Schema(description = "markdown 正文")
    private String content;

    @Schema(description = "摘要")
    private String summary;

    @Schema(description = "别名列表(json)")
    private String aliases;

    @Schema(description = "页面类型: summary/entity/concept/manual/root")
    private String pageType;

    @Schema(description = "所属目录ID(0=根)")
    private Long folderId;

    @Schema(description = "目录层级路径(json)")
    private String categoryPath;

    @Schema(description = "目录深度")
    private Integer depth;

    @Schema(description = "来源引用(json)")
    private List<String> sourceRefs;

    @Schema(description = "关联 chunk 集合(json)")
    private List<String> chunkRefs;

    @Schema(description = "反向链接(slug 列表)")
    private List<String> inLinks;

    @Schema(description = "外链(slug 列表)")
    private List<String> outLinks;

    @Schema(description = "状态: draft/published/archived")
    private String status;

    @Schema(description = "当前版本号")
    private Integer revision;

    @Schema(description = "乐观锁版本号")
    private Integer version;

    @Schema(description = "最近编辑来源")
    private String lastEditSource;

    @Schema(description = "最近编辑者昵称")
    private String lastEditorName;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}