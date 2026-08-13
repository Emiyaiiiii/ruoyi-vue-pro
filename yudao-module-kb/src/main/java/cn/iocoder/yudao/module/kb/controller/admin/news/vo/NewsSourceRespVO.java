package cn.iocoder.yudao.module.kb.controller.admin.news.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.time.LocalDateTime;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;

@Schema(description = "管理后台 - 新闻数据源 Response VO")
@Data
@ExcelIgnoreUnannotated
public class NewsSourceRespVO {

    @Schema(description = "主键ID", example = "1")
    @ExcelProperty("主键ID")
    private Long id;

    @Schema(description = "数据源名称", example = "内部信息网")
    @ExcelProperty("数据源名称")
    private String name;

    @Schema(description = "数据库主机", example = "192.168.1.100")
    @ExcelProperty("数据库主机")
    private String dbHost;

    @Schema(description = "数据库端口", example = "3306")
    @ExcelProperty("数据库端口")
    private Integer dbPort;

    @Schema(description = "数据库名称", example = "news_db")
    @ExcelProperty("数据库名称")
    private String dbName;

    @Schema(description = "数据库用户名", example = "root")
    @ExcelProperty("数据库用户名")
    private String dbUser;

    @Schema(description = "表名", example = "news_table")
    @ExcelProperty("表名")
    private String tableName;

    // ========== 字段映射 ==========

    @Schema(description = "ID字段名", example = "id")
    private String idField;

    @Schema(description = "标题字段名", example = "doctitle")
    private String titleField;

    @Schema(description = "内容字段名", example = "doccontent")
    private String contentField;

    @Schema(description = "频道字段名", example = "docchannel")
    private String channelField;

    @Schema(description = "时间字段名", example = "doctime")
    private String timeField;

    @Schema(description = "URL字段名", example = "docurl")
    private String urlField;

    @Schema(description = "部门字段名", example = "crdept")
    private String crdeptField;

    @Schema(description = "用户字段名", example = "cruser")
    private String cruserField;

    // ========== 同步配置 ==========

    @Schema(description = "是否启用同步", example = "1")
    @ExcelProperty("启用同步")
    private Integer syncEnabled;

    @Schema(description = "同步间隔(秒)", example = "3600")
    @ExcelProperty("同步间隔")
    private Integer syncInterval;

    @Schema(description = "所属部门ID", example = "101")
    private Long dbDept;

    @Schema(description = "所属部门名称", example = "云河信息科技有限公司")
    private String deptName;

    @Schema(description = "上次同步时间")
    @ExcelProperty("上次同步")
    private LocalDateTime lastSyncTime;

    // ========== 统计字段 ==========

    @Schema(description = "同步总记录数", example = "500")
    private Integer totalRecords;

    @Schema(description = "已处理记录数", example = "480")
    private Integer processedRecords;

    @Schema(description = "错误数", example = "3")
    private Integer errorCount;

    // ========== 扩展统计(不持久化) ==========

    @Schema(description = "关联记录总数", example = "500")
    @ExcelProperty("记录总数")
    private Long recordsCount;

    @Schema(description = "待处理数", example = "20")
    private Long pendingCount;

    @Schema(description = "已完成数", example = "480")
    private Long completedCount;

    // ========== 时间戳 ==========

    @Schema(description = "创建时间")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @ExcelProperty("更新时间")
    private LocalDateTime updateTime;

}
