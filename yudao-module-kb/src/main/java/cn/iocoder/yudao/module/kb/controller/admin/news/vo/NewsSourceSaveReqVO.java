package cn.iocoder.yudao.module.kb.controller.admin.news.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import javax.validation.constraints.*;

@Schema(description = "管理后台 - 新闻数据源新增/修改 Request VO")
@Data
public class NewsSourceSaveReqVO {

    @Schema(description = "主键ID", example = "1")
    private Long id;

    @Schema(description = "数据源名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "内部信息网")
    @NotEmpty(message = "数据源名称不能为空")
    @Size(max = 100, message = "数据源名称长度不能超过100个字符")
    private String name;

    @Schema(description = "数据库主机", requiredMode = Schema.RequiredMode.REQUIRED, example = "192.168.1.100")
    @NotEmpty(message = "数据库主机不能为空")
    private String dbHost;

    @Schema(description = "数据库端口", requiredMode = Schema.RequiredMode.REQUIRED, example = "3306")
    @NotNull(message = "数据库端口不能为空")
    private Integer dbPort;

    @Schema(description = "数据库名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "news_db")
    @NotEmpty(message = "数据库名称不能为空")
    private String dbName;

    @Schema(description = "数据库用户名", requiredMode = Schema.RequiredMode.REQUIRED, example = "root")
    @NotEmpty(message = "数据库用户名不能为空")
    private String dbUser;

    @Schema(description = "数据库密码", requiredMode = Schema.RequiredMode.REQUIRED, example = "password")
    @NotEmpty(message = "数据库密码不能为空")
    private String dbPassword;

    @Schema(description = "表名", requiredMode = Schema.RequiredMode.REQUIRED, example = "news_table")
    @NotEmpty(message = "表名不能为空")
    private String tableName;

    // ========== 字段映射 ==========

    @Schema(description = "ID字段名", example = "id")
    @NotEmpty(message = "ID字段名不能为空")
    private String idField;

    @Schema(description = "标题字段名", example = "doctitle")
    @NotEmpty(message = "标题字段名不能为空")
    private String titleField;

    @Schema(description = "内容字段名", example = "doccontent")
    @NotEmpty(message = "内容字段名不能为空")
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
    private Integer syncEnabled;

    @Schema(description = "同步间隔(秒)", example = "3600")
    private Integer syncInterval;

    @Schema(description = "所属部门ID", example = "101")
    private Long dbDept;

}
