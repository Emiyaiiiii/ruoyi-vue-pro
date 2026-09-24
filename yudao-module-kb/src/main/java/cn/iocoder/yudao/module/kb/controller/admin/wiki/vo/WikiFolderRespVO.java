package cn.iocoder.yudao.module.kb.controller.admin.wiki.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Wiki 目录树响应 VO（对齐 WeKnora WikiFolderNode）
 */
@Schema(description = "Wiki 目录树响应 VO")
@Data
public class WikiFolderRespVO {

    @Schema(description = "目录ID")
    private Long id;

    @Schema(description = "知识库ID")
    private Long kbId;

    @Schema(description = "父目录ID(0=根)")
    private Long parentId;

    @Schema(description = "目录名称")
    private String name;

    @Schema(description = "物化路径(/ 连接的名称链，如 AI/RAG)")
    private String path;

    @Schema(description = "目录深度")
    private Integer depth;

    @Schema(description = "兄弟排序")
    private Integer sortOrder;

    @Schema(description = "直接挂载的页面数")
    private Long pageCount;
}
