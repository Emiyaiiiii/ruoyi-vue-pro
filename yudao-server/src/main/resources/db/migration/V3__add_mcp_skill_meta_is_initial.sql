-- ============================================================
-- V3: ai_mcp_meta / ai_skill_meta 增加 is_initial（默认挂载到新建智能体）
--   结构变更：一次性的 V 迁移（不可重复执行）
--   对应种子数据见 R__mcp_meta_seed.sql（知识库检索 MCP 默认挂载）
-- ============================================================

ALTER TABLE `ai_mcp_meta`
    ADD COLUMN `is_initial` tinyint NOT NULL DEFAULT 0
    COMMENT '是否默认挂载到新建智能体: 0=否, 1=是' AFTER `sort_order`;

ALTER TABLE `ai_skill_meta`
    ADD COLUMN `is_initial` tinyint NOT NULL DEFAULT 0
    COMMENT '是否默认挂载到新建智能体: 0=否, 1=是' AFTER `status`;