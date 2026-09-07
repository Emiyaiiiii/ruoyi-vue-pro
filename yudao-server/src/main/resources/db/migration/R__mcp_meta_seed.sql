-- ============================================================
-- R__mcp_meta_seed.sql  (Flyway Repeatable 迁移，可安全重跑)
--   方案决策：知识库检索 MCP 不再进 MCP 商店，改由代码内联配置驱动
--   （AiAgentServiceImpl.registerKbRetrievalMcp 直接使用 kb.retrieval.base-url）。
--   因此这里只负责清理早期版本种子化落库的 knowledge-base-retrieval 条目。
--   幂等：仅删除特定 code，重复执行无副作用。
--
--   !!! 注意：此文件不可删除 !!!
--   历史/生产库 flyway_schema_history 已记录 "mcp meta seed" 迁移，
--   删除文件会导致启动时 Flyway 校验失败
--   （Detected applied migration not resolved locally: mcp meta seed）。
-- ============================================================

DELETE FROM `ai_mcp_meta`
WHERE `code` = 'knowledge-base-retrieval'
  AND `deleted` = b'0';
