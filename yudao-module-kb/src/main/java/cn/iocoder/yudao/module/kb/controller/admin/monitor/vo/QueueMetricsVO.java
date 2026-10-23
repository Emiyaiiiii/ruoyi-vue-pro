package cn.iocoder.yudao.module.kb.controller.admin.monitor.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 队列指标")
@Data
public class QueueMetricsVO {

    @Schema(description = "队列名")
    private String name;

    @Schema(description = "积压消息数（-1 表示查询失败）")
    private Integer depth;

    @Schema(description = "消费者数（-1 表示查询失败）")
    private Integer consumers;
}
