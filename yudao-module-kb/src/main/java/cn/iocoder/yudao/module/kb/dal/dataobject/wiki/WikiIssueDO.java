package cn.iocoder.yudao.module.kb.dal.dataobject.wiki;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * Wiki 待修复问题 DO（对齐 WeKnora wiki_page_issues）
 *
 * <p>AI 巡检/用户标记的页面问题（事实冲突、混合实体、过期信息等）。
 * status: pending(待处理) / ignored(误报/忽略) / resolved(已处理)。</p>
 */
@TableName("kb_wiki_issue")
@KeySequence("kb_wiki_issue_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
public class WikiIssueDO extends BaseDO {

    @TableId
    private Long id;

    /** 所属知识库ID */
    private Long kbId;

    /** 关联页面 slug */
    private String slug;

    /** 问题类型: contradictory_facts/mixed_entities/out_of_date/... */
    private String issueType;

    /** 问题描述 */
    private String description;

    /** 状态: pending/ignored/resolved */
    private String status;

    /** 上报来源(AI 巡检/用户) */
    private String reportedBy;
}