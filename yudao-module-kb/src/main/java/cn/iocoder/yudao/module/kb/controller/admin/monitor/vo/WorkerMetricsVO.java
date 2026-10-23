package cn.iocoder.yudao.module.kb.controller.admin.monitor.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - Worker 监控指标")
@Data
public class WorkerMetricsVO {

    @Schema(description = "Worker ID")
    private String workerId;

    @Schema(description = "主机/容器名")
    private String host;

    @Schema(description = "metrics 端口")
    private Integer port;

    @Schema(description = "状态：ONLINE / OFFLINE")
    private String status;

    @Schema(description = "指标来源：HTTP / REDIS")
    private String source;

    @Schema(description = "最近心跳时间")
    private LocalDateTime lastHeartbeat;

    @Schema(description = "并发上限")
    private Integer concurrency;

    @Schema(description = "当前执行中任务数")
    private Integer inflight;

    @Schema(description = "已完成任务数（本实例启动以来）")
    private Long completed;

    @Schema(description = "失败任务数")
    private Long failed;

    @Schema(description = "取消任务数")
    private Long cancelled;

    @Schema(description = "平均任务耗时（秒）")
    private Double avgTaskSeconds;

    @Schema(description = "embedding 成功请求数")
    private Long embeddingRequests;

    @Schema(description = "embedding 失败请求数")
    private Long embeddingErrors;

    @Schema(description = "抓取失败原因")
    private String error;
}
