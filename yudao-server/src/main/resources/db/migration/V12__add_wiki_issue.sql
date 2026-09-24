-- =====================================================================
-- V12__add_wiki_issue.sql  (Flyway Versioned 迁移)
-- 新增 Wiki 待修复问题表（对齐 WeKnora wiki_page_issues）：
--  kb_wiki_issue  AI 巡检/用户标记的页面问题（事实冲突、混合实体、过期信息等）
--  status: pending(待处理) / ignored(误报/忽略) / resolved(已处理)
-- =====================================================================
CREATE TABLE IF NOT EXISTS `kb_wiki_issue` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `kb_id` bigint NOT NULL COMMENT '所属知识库ID',
  `slug` varchar(255) NOT NULL COMMENT '关联页面 slug',
  `issue_type` varchar(50) NOT NULL COMMENT '问题类型: contradictory_facts/mixed_entities/out_of_date/...',
  `description` text NULL COMMENT '问题描述',
  `status` varchar(20) NOT NULL DEFAULT 'pending' COMMENT '状态: pending/ignored/resolved',
  `reported_by` varchar(100) NOT NULL DEFAULT 'wiki-researcher-agent' COMMENT '上报来源(AI 巡检/用户)',
  `creator` varchar(64) NOT NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) NOT NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_wiki_issue_kb` (`kb_id`),
  KEY `idx_wiki_issue_kb_status` (`kb_id`,`status`),
  KEY `idx_wiki_issue_slug` (`kb_id`,`slug`),
  KEY `idx_wiki_issue_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Wiki 待修复问题表';