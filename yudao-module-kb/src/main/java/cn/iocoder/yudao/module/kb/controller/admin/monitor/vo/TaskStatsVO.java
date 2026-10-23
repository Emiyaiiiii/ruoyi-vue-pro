package cn.iocoder.yudao.module.kb.controller.admin.monitor.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 任务统计")
@Data
public class TaskStatsVO {

    @Schema(description = "任务总数")
    private Long total;

    @Schema(description = "待处理/排队中")
    private Long pending;

    @Schema(description = "处理中")
    private Long processing;

    @Schema(description = "已完成")
    private Long completed;

    @Schema(description = "失败")
    private Long failed;

    @Schema(description = "提交失败")
    private Long submitFailed;

    @Schema(description = "超时")
    private Long timeout;

    @Schema(description = "已取消")
    private Long cancelled;

    @Schema(description = "近 24h 已完成任务平均耗时（秒）")
    private Double avgDurationSeconds;

    @Schema(description = "近 24h 超时率（百分比）")
    private Double timeoutRate24h;

    @Schema(description = "最早排队任务已等待分钟数")
    private Long oldestPendingMinutes;
}
