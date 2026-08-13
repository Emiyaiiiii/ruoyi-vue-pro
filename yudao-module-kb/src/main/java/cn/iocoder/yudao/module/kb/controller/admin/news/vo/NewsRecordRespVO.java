package cn.iocoder.yudao.module.kb.controller.admin.news.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.time.LocalDateTime;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;

@Schema(description = "管理后台 - 新闻记录 Response VO")
@Data
@ExcelIgnoreUnannotated
public class NewsRecordRespVO {

    @Schema(description = "主键ID", example = "1")
    @ExcelProperty("主键ID")
    private Long id;

    @Schema(description = "数据源ID", example = "1")
    @ExcelProperty("数据源ID")
    private Long sourceId;

    @Schema(description = "数据源名称", example = "内部信息网")
    @ExcelProperty("数据源")
    private String sourceName;

    @Schema(description = "外部记录ID", example = "12345")
    @ExcelProperty("外部ID")
    private String externalId;

    @Schema(description = "外部标题", example = "公司年度总结大会")
    @ExcelProperty("标题")
    private String externalTitle;

    @Schema(description = "外部内容", example = "大会于2025年1月15日召开...")
    private String externalContent;

    @Schema(description = "频道", example = "公司要闻")
    @ExcelProperty("频道")
    private String externalChannel;

    @Schema(description = "外部时间", example = "2025-01-15")
    @ExcelProperty("发布时间")
    private String externalTime;

    @Schema(description = "外部URL", example = "http://news.example.com/article/12345")
    @ExcelProperty("URL")
    private String externalUrl;

    @Schema(description = "创建部门", example = "技术部")
    @ExcelProperty("部门")
    private String externalCrdept;

    @Schema(description = "创建用户", example = "张三")
    @ExcelProperty("用户")
    private String externalCruser;

    // ========== 处理状态 ==========

    @Schema(description = "状态", example = "pending")
    private String status;

    @Schema(description = "状态显示名", example = "待处理")
    @ExcelProperty("状态")
    private String statusDisplay;

    @Schema(description = "处理阶段描述", example = "ready")
    private String processingStatus;

    // ========== 错误追踪 ==========

    @Schema(description = "错误信息", example = "连接超时")
    private String errorMessage;

    @Schema(description = "重试次数", example = "2")
    @ExcelProperty("重试")
    private Integer retryCount;

    // ========== 关联文档信息（同步时回写） ==========

    @Schema(description = "关联文档ID", example = "1001")
    private Long docId;

    @Schema(description = "关联知识库ID", example = "10")
    private Long kbId;

    @Schema(description = "文件访问URL", example = "http://file.example.com/doc/xxx.txt")
    private String fileUrl;

    @Schema(description = "文件类型", example = "txt")
    private String fileType;

    // ========== 时间戳 ==========

    @Schema(description = "上次处理时间")
    private LocalDateTime lastProcessedAt;

    @Schema(description = "处理完成时间")
    private LocalDateTime processedAt;

    @Schema(description = "外部更新时间")
    private LocalDateTime externalUpdatedAt;

    @Schema(description = "创建时间")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @ExcelProperty("更新时间")
    private LocalDateTime updateTime;

}
