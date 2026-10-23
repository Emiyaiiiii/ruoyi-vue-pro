package cn.iocoder.yudao.module.kb.controller.admin.monitor.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 处理监控概览")
@Data
public class MonitorOverviewVO {

    @Schema(description = "注册的 Worker 总数")
    private Integer workerCount;

    @Schema(description = "在线 Worker 数")
    private Integer onlineWorkerCount;

    @Schema(description = "Worker 明细")
    private List<WorkerMetricsVO> workers;

    @Schema(description = "队列指标")
    private QueueMetricsVO queue;

    @Schema(description = "任务统计")
    private TaskStatsVO tasks;

    @Schema(description = "告警列表")
    private List<MonitorAlertVO> alerts;

    @Schema(description = "汇总时间")
    private LocalDateTime collectedAt;
}
