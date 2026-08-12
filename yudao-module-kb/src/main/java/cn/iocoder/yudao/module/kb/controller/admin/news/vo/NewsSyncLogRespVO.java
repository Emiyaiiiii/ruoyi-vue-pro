package cn.iocoder.yudao.module.kb.controller.admin.news.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.time.LocalDateTime;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;

@Schema(description = "管理后台 - 新闻同步日志 Response VO")
@Data
@ExcelIgnoreUnannotated
public class NewsSyncLogRespVO {

    @Schema(description = "主键ID", example = "1")
    @ExcelProperty("主键ID")
    private Long id;

    @Schema(description = "数据源ID", example = "1")
    private Long sourceId;

    @Schema(description = "数据源名称", example = "内部信息网")
    @ExcelProperty("数据源")
    private String sourceName;

    @Schema(description = "同步类型", example = "incremental")
    private String syncType;

    @Schema(description = "同步类型显示名", example = "增量同步")
    @ExcelProperty("同步类型")
    private String syncTypeDisplay;

    @Schema(description = "状态", example = "completed")
    private String status;

    @Schema(description = "状态显示名", example = "已完成")
    @ExcelProperty("状态")
    private String statusDisplay;

    @Schema(description = "获取总数", example = "100")
    @ExcelProperty("获取总数")
    private Integer totalFetched;

    @Schema(description = "新增记录数", example = "50")
    @ExcelProperty("新增")
    private Integer newRecords;

    @Schema(description = "更新记录数", example = "30")
    @ExcelProperty("更新")
    private Integer updatedRecords;

    @Schema(description = "跳过记录数", example = "15")
    @ExcelProperty("跳过")
    private Integer skippedRecords;

    @Schema(description = "失败记录数", example = "5")
    @ExcelProperty("失败")
    private Integer failedRecords;

    @Schema(description = "开始时间")
    @ExcelProperty("开始时间")
    private LocalDateTime startedAt;

    @Schema(description = "完成时间")
    @ExcelProperty("完成时间")
    private LocalDateTime completedAt;

    @Schema(description = "错误信息")
    private String errorMessage;

    @Schema(description = "详细信息(JSON)")
    private String details;

    @Schema(description = "创建时间")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}
