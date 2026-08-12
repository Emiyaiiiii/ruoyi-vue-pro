package cn.iocoder.yudao.module.kb.dal.dataobject.news;

import lombok.*;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;

/**
 * 新闻数据源 DO
 *
 * @author 吴皓
 */
@TableName("kb_news_source")
@KeySequence("kb_news_source_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsSourceDO extends BaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;

    /**
     * 数据源名称
     */
    private String name;

    /**
     * 外部数据库主机
     */
    private String dbHost;

    /**
     * 外部数据库端口
     */
    private Integer dbPort;

    /**
     * 外部数据库名称
     */
    private String dbName;

    /**
     * 数据库用户名
     */
    private String dbUser;

    /**
     * 数据库密码
     */
    private String dbPassword;

    /**
     * 外部表名
     */
    private String tableName;

    // ========== 字段映射 ==========

    /**
     * ID字段名
     */
    private String idField;

    /**
     * 标题字段名
     */
    private String titleField;

    /**
     * 内容字段名
     */
    private String contentField;

    /**
     * 频道字段名
     */
    private String channelField;

    /**
     * 时间字段名
     */
    private String timeField;

    /**
     * URL字段名
     */
    private String urlField;

    /**
     * 部门字段名
     */
    private String crdeptField;

    /**
     * 用户字段名
     */
    private String cruserField;

    // ========== 同步配置 ==========

    /**
     * 是否启用同步: 0=停用, 1=启用
     */
    private Integer syncEnabled;

    /**
     * 同步间隔(秒)
     */
    private Integer syncInterval;

    /**
     * 上次同步时间
     */
    private LocalDateTime lastSyncTime;

    // ========== 统计字段 ==========

    /**
     * 同步总记录数
     */
    private Integer totalRecords;

    /**
     * 已处理记录数
     */
    private Integer processedRecords;

    /**
     * 错误数
     */
    private Integer errorCount;

}
