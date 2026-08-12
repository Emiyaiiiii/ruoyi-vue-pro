package cn.iocoder.yudao.module.kb.dal.dataobject.news;

import lombok.*;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;

/**
 * 新闻同步日志 DO
 *
 * @author 吴皓
 */
@TableName("kb_news_sync_log")
@KeySequence("kb_news_sync_log_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsSyncLogDO extends BaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;

    /**
     * 数据源ID
     */
    private Long sourceId;

    /**
     * 同步类型: full=全量, incremental=增量, manual=手动
     */
    private String syncType;

    /**
     * 状态: started=已开始, running=运行中, completed=已完成, failed=失败
     */
    private String status;

    /**
     * 获取总数
     */
    private Integer totalFetched;

    /**
     * 新增记录数
     */
    private Integer newRecords;

    /**
     * 更新记录数
     */
    private Integer updatedRecords;

    /**
     * 跳过记录数
     */
    private Integer skippedRecords;

    /**
     * 失败记录数
     */
    private Integer failedRecords;

    /**
     * 开始时间
     */
    private LocalDateTime startedAt;

    /**
     * 完成时间
     */
    private LocalDateTime completedAt;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 详细信息(JSON)
     */
    private String details;

    // ========== 关联显示字段 ==========

    /**
     * 数据源名称（不持久化，查询时填充）
     */
    @TableField(exist = false)
    private String sourceName;

    /**
     * 获取同步类型显示名称
     */
    public String getSyncTypeDisplay() {
        if (this.syncType == null) return "";
        switch (this.syncType) {
            case "full": return "全量同步";
            case "incremental": return "增量同步";
            case "manual": return "手动同步";
            default: return this.syncType;
        }
    }

    /**
     * 获取状态显示名称
     */
    public String getStatusDisplay() {
        if (this.status == null) return "";
        switch (this.status) {
            case "started": return "已开始";
            case "running": return "运行中";
            case "completed": return "已完成";
            case "failed": return "失败";
            default: return this.status;
        }
    }

}
