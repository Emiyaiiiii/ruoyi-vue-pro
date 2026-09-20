-- =====================================================================
-- V4__add_kb_library_process_config.sql  (Flyway Versioned 迁移)
-- kb_library 增加 KB 级处理配置列：切片/检索/重排/模型 差异化配置。
-- 优先级：上传任务参数 > KB 级 process_config > 租户级(kb_rag_config/kb_chunk_method/kb_model_config) > 内置默认。
-- 键名与系统配置保持一致：chunking 对齐 kb_chunk_method.default_parameters，
--   retrieval/rerank 对齐 kb_rag_config 同模块键名，model 对齐 kb_model_config.uid。
-- =====================================================================
ALTER TABLE `kb_library`
    ADD COLUMN `process_config` text NULL COMMENT 'KB级处理配置(JSON): {chunking:{strategy,chunk_size,chunk_overlap,min_chunk_size,max_chunk_size,image_strategy}, retrieval:{top_k,search_type,use_parallel,score_threshold,vector_weight,keyword_weight,parallel_workers,parallel_timeout}, rerank:{enabled,timeout,top_k}, model:{embedding_model_uid,rerank_model_uid}}；键名与系统配置一致，KB 配置优先，缺省回退租户级配置' AFTER `image_strategy`;
