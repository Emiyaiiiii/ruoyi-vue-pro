package cn.iocoder.yudao.module.kb.controller.admin.wiki.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * Wiki 图谱响应 VO（对齐 WeKnora WikiGraphData）
 *
 * <p>overview=全局 top-N 页；ego=以某页为中心展开邻居。前端据此渲染原生 SVG 图。</p>
 */
@Schema(description = "Wiki 图谱响应 VO")
@Data
public class WikiGraphRespVO {

    @Schema(description = "节点列表")
    private List<Node> nodes;

    @Schema(description = "边列表")
    private List<Edge> edges;

    @Schema(description = "图谱元信息（mode/total/returned/truncated，供前端截断提示）")
    private Meta meta;

    @Schema(description = "节点")
    @Data
    public static class Node {
        @Schema(description = "slug")
        private String slug;
        @Schema(description = "标题")
        private String title;
        @Schema(description = "页面类型")
        private String pageType;
        @Schema(description = "关联链接数(用于定节点半径)")
        private Integer linkCount;
    }

    @Schema(description = "边")
    @Data
    public static class Edge {
        @Schema(description = "来源 slug")
        private String source;
        @Schema(description = "目标 slug")
        private String target;
    }

    @Schema(description = "图谱元信息（对齐 WeKnora WikiGraphMeta）")
    @Data
    public static class Meta {
        @Schema(description = "模式：overview | ego")
        private String mode;
        @Schema(description = "过滤后总数")
        private Long total;
        @Schema(description = "实际返回数")
        private Long returned;
        @Schema(description = "是否被截断（returned < total）")
        private Boolean truncated;
        @Schema(description = "ego 中心 slug")
        private String center;
        @Schema(description = "ego BFS 深度")
        private Integer depth;
    }
}