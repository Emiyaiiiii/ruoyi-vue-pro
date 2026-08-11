-- ============================================================
-- 知识库管理 - RAG系统配置表
-- 对应前端菜单: 知识库管理 > RAG配置
-- ============================================================

-- 设置连接字符集为 utf8mb4 (避免与 gbk 连接的 collation 冲突)
SET NAMES utf8mb4;

DROP TABLE IF EXISTS `kb_rag_config`;
CREATE TABLE `kb_rag_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `module` VARCHAR(50) NOT NULL COMMENT '所属模块: retrieval=检索, rerank=重排序, chunking=切片, llm=大模型, cache=缓存, batch=批量处理, conversation=对话',
    `key` VARCHAR(100) NOT NULL COMMENT '配置键名',
    `value` TEXT NOT NULL COMMENT '配置值(字符串存储，根据value_type解析)',
    `value_type` VARCHAR(10) NOT NULL DEFAULT 'str' COMMENT '值类型: int=整数, float=浮点数, bool=布尔值, str=字符串, json=JSON对象',
    `description` TEXT DEFAULT NULL COMMENT '配置说明',
    `is_active` BIT(1) NOT NULL DEFAULT b'1' COMMENT '是否启用: 0=停用, 1=启用',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
    `creator` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_module_key` (`module`, `key`),
    KEY `idx_module` (`module`),
    KEY `idx_is_active` (`is_active`),
    KEY `idx_module_active` (`module`, `is_active`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='RAG系统配置表';


-- ============================================================
-- 菜单数据: 知识库管理 > RAG配置
-- ============================================================

-- 查询"知识库管理"父菜单ID并存入变量
SELECT id INTO @kb_parent_id FROM `system_menu` WHERE name = '知识库管理' AND deleted = b'0' AND type = 1 LIMIT 1;

-- RAG配置 (菜单)
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
VALUES ('RAG配置', '', 2, 7, @kb_parent_id, 'rag-config', 'ep:setting', 'kb/ragconfig/index', 'KbRagConfig', 0, 1, 1, 0, NOW(), NOW(), b'0');

-- 获取刚插入的"RAG配置"菜单ID
SET @rag_config_id = LAST_INSERT_ID();

-- RAG配置按钮权限 (挂在RAG配置菜单下)
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
VALUES
('RAG配置查询', 'kb:rag-config:query', 3, 1, @rag_config_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'),
('RAG配置创建', 'kb:rag-config:create', 3, 2, @rag_config_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'),
('RAG配置更新', 'kb:rag-config:update', 3, 3, @rag_config_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'),
('RAG配置删除', 'kb:rag-config:delete', 3, 4, @rag_config_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0');


-- ============================================================
-- 预置数据: 26 条RAG系统配置
-- ============================================================

INSERT INTO `kb_rag_config` (`module`, `key`, `value`, `value_type`, `description`, `is_active`, `sort_order`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
VALUES
-- 检索模块 (13条)
('retrieval', 'top_k', '30', 'int', '最终返回给 LLM 的文档片段数量', b'1', 1, 'admin', NOW(), 'admin', NOW(), b'0', 0),
('retrieval', 'parallel_top_k', '100', 'int', '并行检索粗筛候选数（知识库场景）', b'1', 2, 'admin', NOW(), 'admin', NOW(), b'0', 0),
('retrieval', 'rerank_top_k', '30', 'int', '送入重排序模型的数量（知识库场景）', b'1', 3, 'admin', NOW(), 'admin', NOW(), b'0', 0),
('retrieval', 'final_top_k', '30', 'int', '重排序后最终保留数量（知识库场景）', b'1', 4, 'admin', NOW(), 'admin', NOW(), b'0', 0),
('retrieval', 'news_parallel_top_k', '100', 'int', '并行检索粗筛候选数（新闻/联网场景）', b'1', 5, 'admin', NOW(), 'admin', NOW(), b'0', 0),
('retrieval', 'news_rerank_top_k', '40', 'int', '送入重排序模型的数量（新闻/联网场景）', b'1', 6, 'admin', NOW(), 'admin', NOW(), b'0', 0),
('retrieval', 'news_final_top_k', '40', 'int', '重排序后最终保留数量（新闻/联网场景）', b'1', 7, 'admin', NOW(), 'admin', NOW(), b'0', 0),
('retrieval', 'use_query_rewrite', 'true', 'bool', '是否启用查询改写', b'1', 10, 'admin', NOW(), 'admin', NOW(), b'0', 0),
('retrieval', 'use_deep_semantic', 'true', 'bool', '是否启用深度语义理解', b'1', 11, 'admin', NOW(), 'admin', NOW(), b'0', 0),
('retrieval', 'use_intent_analysis', 'true', 'bool', '是否启用意图分析', b'1', 12, 'admin', NOW(), 'admin', NOW(), b'0', 0),
('retrieval', 'use_parallel', 'true', 'bool', '是否启用并行检索', b'1', 13, 'admin', NOW(), 'admin', NOW(), b'0', 0),
('retrieval', 'use_rerank', 'true', 'bool', '是否启用重排序', b'1', 14, 'admin', NOW(), 'admin', NOW(), b'0', 0),
('retrieval', 'router_top_k', '10', 'int', '触发知识库路由分类的知识库数量阈值', b'1', 15, 'admin', NOW(), 'admin', NOW(), b'0', 0),

-- 重排序模块 (2条)
('rerank', 'model_name', 'bge-rerank', 'str', '重排序模型名称', b'1', 1, 'admin', NOW(), 'admin', NOW(), b'0', 0),
('rerank', 'batch_size', '32', 'int', '重排序批处理大小', b'1', 2, 'admin', NOW(), 'admin', NOW(), b'0', 0),

-- 切片模块 (2条)
('chunking', 'default_chunk_size', '2000', 'int', '默认切片大小（字符数）', b'1', 1, 'admin', NOW(), 'admin', NOW(), b'0', 0),
('chunking', 'default_chunk_overlap', '200', 'int', '默认切片重叠大小', b'1', 2, 'admin', NOW(), 'admin', NOW(), b'0', 0),

-- 大模型模块 (3条)
('llm', 'default_temperature', '0.7', 'float', '默认温度参数', b'1', 1, 'admin', NOW(), 'admin', NOW(), b'0', 0),
('llm', 'default_top_p', '0.9', 'float', '默认 Top-P 采样参数', b'1', 2, 'admin', NOW(), 'admin', NOW(), b'0', 0),
('llm', 'max_tokens_limit', '4096', 'int', '最大生成 token 数限制', b'1', 3, 'admin', NOW(), 'admin', NOW(), b'0', 0),

-- 缓存模块 (2条)
('cache', 'search_cache_ttl_seconds', '3600', 'int', '搜索结果缓存有效期（秒）', b'1', 1, 'admin', NOW(), 'admin', NOW(), b'0', 0),
('cache', 'max_cache_entries_per_user', '100', 'int', '每用户最大缓存条目数', b'1', 2, 'admin', NOW(), 'admin', NOW(), b'0', 0),

-- 批量处理模块 (2条)
('batch', 'max_files_per_upload', '50', 'int', '单次批量上传最大文件数', b'1', 1, 'admin', NOW(), 'admin', NOW(), b'0', 0),
('batch', 'max_total_size_mb', '1024', 'int', '单次批量上传总大小限制（MB）', b'1', 2, 'admin', NOW(), 'admin', NOW(), b'0', 0),

-- 对话模块 (2条)
('conversation', 'max_history_turns', '5', 'int', '对话历史最大轮数', b'1', 1, 'admin', NOW(), 'admin', NOW(), b'0', 0),
('conversation', 'stream_chunk_delay_ms', '50', 'int', '流式响应每块延迟（毫秒）', b'1', 2, 'admin', NOW(), 'admin', NOW(), b'0', 0);


-- ============================================================
-- 字典数据: RAG配置模块类型
-- ============================================================
INSERT INTO `system_dict_type` (`name`, `type`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES ('RAG配置模块类型', 'kb_rag_module', 0, '知识库-RAG配置模块类型', 'admin', NOW(), 'admin', NOW(), b'0');

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
(1, '检索模块', 'retrieval', 'kb_rag_module', 0, 'primary', '', 'RAG-检索模块', 'admin', NOW(), 'admin', NOW(), b'0'),
(2, '重排序模块', 'rerank', 'kb_rag_module', 0, 'success', '', 'RAG-重排序模块', 'admin', NOW(), 'admin', NOW(), b'0'),
(3, '切片模块', 'chunking', 'kb_rag_module', 0, 'warning', '', 'RAG-切片模块', 'admin', NOW(), 'admin', NOW(), b'0'),
(4, '大模型模块', 'llm', 'kb_rag_module', 0, 'danger', '', 'RAG-大模型模块', 'admin', NOW(), 'admin', NOW(), b'0'),
(5, '缓存模块', 'cache', 'kb_rag_module', 0, 'info', '', 'RAG-缓存模块', 'admin', NOW(), 'admin', NOW(), b'0'),
(6, '批量处理模块', 'batch', 'kb_rag_module', 0, '', '', 'RAG-批量处理模块', 'admin', NOW(), 'admin', NOW(), b'0'),
(7, '对话模块', 'conversation', 'kb_rag_module', 0, 'success', '', 'RAG-对话模块', 'admin', NOW(), 'admin', NOW(), b'0');

-- 字典数据: RAG配置值类型
INSERT INTO `system_dict_type` (`name`, `type`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES ('RAG配置值类型', 'kb_rag_value_type', 0, '知识库-RAG配置值类型', 'admin', NOW(), 'admin', NOW(), b'0');

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
(1, '整数', 'int', 'kb_rag_value_type', 0, 'primary', '', 'RAG-整数值', 'admin', NOW(), 'admin', NOW(), b'0'),
(2, '浮点数', 'float', 'kb_rag_value_type', 0, 'warning', '', 'RAG-浮点数值', 'admin', NOW(), 'admin', NOW(), b'0'),
(3, '布尔值', 'bool', 'kb_rag_value_type', 0, 'success', '', 'RAG-布尔值', 'admin', NOW(), 'admin', NOW(), b'0'),
(4, '字符串', 'str', 'kb_rag_value_type', 0, 'info', '', 'RAG-字符串值', 'admin', NOW(), 'admin', NOW(), b'0'),
(5, 'JSON对象', 'json', 'kb_rag_value_type', 0, 'danger', '', 'RAG-JSON对象值', 'admin', NOW(), 'admin', NOW(), b'0');
