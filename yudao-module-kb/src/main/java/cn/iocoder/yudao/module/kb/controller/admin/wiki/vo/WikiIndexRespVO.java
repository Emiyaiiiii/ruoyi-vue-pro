package cn.iocoder.yudao.module.kb.controller.admin.wiki.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * Wiki index 页响应 VO（对齐 WeKnora WikiIndexResponse）
 *
 * <p>intro 为 KB 级总览（python 蒸馏生成、落库为 page_type=index 页面 content）；
 * groups 按 page_type 分组聚合全部页面目录条目，前端动态渲染。</p>
 */
@Schema(description = "Wiki index 页响应 VO")
@Data
public class WikiIndexRespVO {

    @Schema(description = "引言(markdown 标题+简介)")
    private String intro;

    @Schema(description = "index 页版本号")
    private Integer version;

    @Schema(description = "按 page_type 分组的目录")
    private List<Group> groups;

    @Schema(description = "目录分组")
    @Data
    public static class Group {

        @Schema(description = "页面类型: summary/entity/concept/manual")
        private String type;

        @Schema(description = "该类型页面总数")
        private Long total;

        @Schema(description = "目录条目（slug/title/summary）")
        private List<Entry> items;
    }

    @Schema(description = "目录条目")
    @Data
    public static class Entry {

        @Schema(description = "页面 slug")
        private String slug;

        @Schema(description = "页面标题")
        private String title;

        @Schema(description = "一行摘要（index listing）")
        private String summary;

        @Schema(description = "目录路径(json)")
        private String categoryPath;
    }
}
