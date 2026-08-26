-- =====================================================================
-- R__model_config_seed.sql  (Flyway Repeatable 迁移)
-- 可重复迁移：内容变更时 migrate 自动重跑。接管原 V7/V8 的「默认模型」种子，
-- 使默认模型(embedding/llm/ocr/rerank)这类易微调整的种子数据纳入 R* 模式。
-- 幂等策略：INSERT IGNORE，按唯一键 uid 仅补缺省、不覆盖用户在界面上的改动。
-- 注意：须用当前 schema 列（V9 已删除 provider/deploy/platform/metadata）。
-- =====================================================================

INSERT IGNORE INTO `kb_model_config`
(`uid`, `model`, `name`, `url`, `appkey`, `model_type`,
 `thinking_enabled`, `vl_supported`, `is_active`, `description`,
 `max_tokens`, `context_length`, `temperature`, `top_p`,
 `config`, `sort_order`, `is_pinned`, `tenant_id`)
VALUES
-- embedding：默认嵌入模型（火山方舟，OpenAI Embedding 兼容）
('embedding-ark', 'ep-20250701150431-bxtzk', '默认嵌入模型',
 'https://ark.cn-beijing.volces.com/api/v3', 'ff47bc89-2fa3-4cd2-ae4f-49f11cb38cf0', 'embedding',
 b'0', b'0', b'1', NULL, 4096, 8192, 0.7, 0.9,
 '{"adapter":"embedding","dimension":2560,"max_length":512,"batch_size":32,"timeout":30,"max_retries":3,"retry_delay":1}',
 1, b'0', 1),
-- llm：默认大模型（火山方舟，支持视觉/多模态）
('llm-ark', 'ep-20260225095958-b65x5', '默认大模型',
 'https://ark.cn-beijing.volces.com/api/v3', 'ff47bc89-2fa3-4cd2-ae4f-49f11cb38cf0', 'llm',
 b'0', b'1', b'1', NULL, 102400, 131072, 0.3, 0.9,
 '{"temperature":0.3,"max_tokens":102400}',
 1, b'1', 1),
-- ocr：默认 OCR 模型（硅基流动，逐图 OCR）
('ocr-siliconflow', 'deepseek-ai/DeepSeek-OCR', '默认OCR模型',
 'https://api.siliconflow.cn/v1/chat/completions', 'sk-cxlynupqhgunokbtzzrocadxcqqsjyswyxbqrwxxbswwyjhy', 'ocr',
 b'0', b'0', b'1', NULL, 4096, 8192, 0.0, 0.9,
 '{"ocr_kind":"deepseek_ocr","timeout":120,"max_tokens":4096}',
 1, b'0', 1),
-- rerank：默认重排模型（硅基流动，OpenAI rerank 兼容 /v1/rerank）
('rerank-siliconflow', 'BAAI/bge-reranker-v2-m3', '默认重排模型',
 'https://api.siliconflow.cn/v1/rerank', 'sk-cxlynupqhgunokbtzzrocadxcqqsjyswyxbqrwxxbswwyjhy', 'rerank',
 b'0', b'0', b'1', NULL, 4096, 8192, 0.0, 0.9,
 '{"adapter":"rerank","timeout":30,"top_k":30,"max_retries":0,"retry_delay":1.0,"batch_size":50}',
 1, b'0', 1);