package cn.iocoder.yudao.module.kb.controller.admin.modelconfig.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.List;

@Schema(description = "管理后台 - 大模型配置统计 Response VO")
@Data
public class ModelConfigStatisticsRespVO {

    @Schema(description = "总配置数")
    private Long totalConfigs;

    @Schema(description = "激活配置数")
    private Long activeConfigs;

    @Schema(description = "总使用量")
    private Long totalUsage;

    @Schema(description = "配置详情列表")
    private List<ModelConfigStatItem> statistics;

    @Data
    @Schema(description = "配置统计项")
    public static class ModelConfigStatItem {
        @Schema(description = "配置ID")
        private Long configId;
        @Schema(description = "模型名称")
        private String name;
        @Schema(description = "用途分类: embedding/llm/ocr/rerank")
        private String modelType;
        @Schema(description = "是否激活")
        private Integer isActive;
        @Schema(description = "使用次数")
        private Long usageCount;
        @Schema(description = "总对话数")
        private Long totalSessions;
        @Schema(description = "最后使用时间")
        private String lastUsed;
        @Schema(description = "创建时间")
        private String createTime;
        @Schema(description = "描述")
        private String description;
    }

}
