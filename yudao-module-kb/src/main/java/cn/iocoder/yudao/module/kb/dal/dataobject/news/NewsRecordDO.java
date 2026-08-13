package cn.iocoder.yudao.module.kb.dal.dataobject.news;

import lombok.*;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;

/**
 * 新闻记录 DO
 *
 * @author 吴皓
 */
@TableName("kb_news_record")
@KeySequence("kb_news_record_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsRecordDO extends BaseDO {

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
     * 外部记录ID
     */
    private String externalId;

    /**
     * 外部标题
     */
    private String externalTitle;

    /**
     * 外部内容
     */
    private String externalContent;

    /**
     * 频道
     */
    private String externalChannel;

    /**
     * 外部时间
     */
    private String externalTime;

    /**
     * 外部URL
     */
    private String externalUrl;

    /**
     * 创建部门
     */
    private String externalCrdept;

    /**
     * 创建用户
     */
    private String externalCruser;

    // ========== 处理状态 ==========

    /**
     * 状态: pending=待处理, completed=已完成, failed=失败, skipped=已跳过
     */
    private String status;

    /**
     * 处理阶段描述
     */
    private String processingStatus;

    // ========== 错误追踪 ==========

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 重试次数
     */
    private Integer retryCount;

    // ========== 关联文档信息（同步时回写） ==========

    /**
     * 关联文档ID
     */
    private Long docId;

    /**
     * 关联知识库ID
     */
    private Long kbId;

    /**
     * 文件访问URL
     */
    private String fileUrl;

    /**
     * 文件类型
     */
    private String fileType;

    // ========== 时间戳 ==========

    /**
     * 上次处理时间
     */
    private LocalDateTime lastProcessedAt;

    /**
     * 处理完成时间
     */
    private LocalDateTime processedAt;

    /**
     * 外部更新时间
     */
    private LocalDateTime externalUpdatedAt;


    /**
     * 数据源名称（非持久化，查询时填充）
     */
    @TableField(exist = false)
    private String sourceName;

    // ========== 状态显示辅助方法 ==========

    /**
     * 获取状态显示名称
     */
    public String getStatusDisplay() {
        if (this.status == null) return "";
        switch (this.status) {
            case "pending": return "待处理";
            case "completed": return "已完成";
            case "failed": return "失败";
            case "skipped": return "已跳过";
            default: return this.status;
        }
    }

}
