-- =====================================================================
-- R__rag_config_seed.sql  (Flyway Repeatable 迁移)
-- 可重复迁移：内容变更时 migrate 自动重跑。接管原 V7 的「RAG 检索/重排」种子，
-- 并反映 V8 之后的状态（rerank 仅保留行为参数 enabled/timeout/top_k，
-- 账户字段 endpoint/api_key/default_model 已迁至 kb_model_config）。
-- 幂等策略：与 V7 一致，先按 (module, key) 删除本租户后重新插入，保证结果确定。
-- =====================================================================

DELETE FROM `kb_rag_config`
WHERE `tenant_id` = 1
  AND ((`module` = 'retrieval' AND `key` IN ('top_k', 'search_type', 'use_parallel', 'score_threshold', 'vector_weight', 'keyword_weight', 'parallel_workers', 'parallel_timeout'))
    OR (`module` = 'rerank' AND `key` IN ('enabled', 'timeout', 'top_k')));

INSERT INTO `kb_rag_config`
(`module`, `key`, `value`, `value_type`, `description`, `is_active`, `sort_order`, `tenant_id`)
VALUES
-- 检索模块
('retrieval', 'top_k', '30', 'int', '统一检索返回结果数量', b'1', 10, 1),
('retrieval', 'search_type', 'unified', 'str', '检索方式: unified/hybrid/vector/keyword', b'1', 20, 1),
('retrieval', 'use_parallel', 'true', 'bool', '是否并行多路召回', b'1', 30, 1),
('retrieval', 'score_threshold', '0.5', 'float', '向量检索相似度阈值', b'1', 40, 1),
('retrieval', 'vector_weight', '0.7', 'float', '向量召回权重', b'1', 50, 1),
('retrieval', 'keyword_weight', '0.3', 'float', '关键词召回权重', b'1', 60, 1),
('retrieval', 'parallel_workers', '5', 'int', '并行召回线程数', b'1', 70, 1),
('retrieval', 'parallel_timeout', '10.0', 'float', '并行召回超时(秒)', b'1', 80, 1),
-- 重排模块（行为参数；账户字段在 kb_model_config）
('rerank', 'enabled', 'true', 'bool', '是否启用结果重排', b'1', 10, 1),
('rerank', 'timeout', '30', 'int', '重排超时(秒)', b'1', 50, 1),
('rerank', 'top_k', '30', 'int', '重排候选数量', b'1', 60, 1);