package cn.iocoder.yudao.module.kb.controller.admin.wiki.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Wiki 待修复问题响应 VO（对齐 WeKnora WikiPageIssue）
 */
@Schema(description = "Wiki 待修复问题响应 VO")
@Data
public class WikiIssueRespVO {

    @Schema(description = "问题ID")
    private Long id;

    @Schema(description = "知识库ID")
    private Long kbId;

    @Schema(description = "关联页面 slug")
    private String slug;

    @Schema(description = "问题类型: contradictory_facts/mixed_entities/out_of_date/...")
    private String issueType;

    @Schema(description = "问题描述")
    private String description;

    @Schema(description = "怀疑导致污染的来源文档 ID 列表（对齐 WeKnora suspected_knowledge_ids）")
    private List<String> suspectedKnowledgeIds;

    @Schema(description = "状态: pending/ignored/resolved")
    private String status;

    @Schema(description = "上报来源(AI 巡检/用户)")
    private String reportedBy;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}