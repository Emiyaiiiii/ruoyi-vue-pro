-- ============================================================
-- 知识库管理 - 切片方法表
-- 对应前端菜单: 知识库管理 > 切片方法
-- ============================================================

-- 设置连接字符集为 utf8mb4 (避免与 gbk 连接的 collation 冲突)
SET NAMES utf8mb4;

DROP TABLE IF EXISTS `kb_chunk_method`;
CREATE TABLE `kb_chunk_method` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name` VARCHAR(100) NOT NULL COMMENT '方法名称',
    `method_type` VARCHAR(20) NOT NULL COMMENT '方法类型: fixed_size=固定大小, semantic=语义分段, hierarchical=层次分段, recursive=递归分割, sentence=按句子, paragraph=按段落, section=按章节, custom=自定义',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '方法描述',
    `code` VARCHAR(50) NOT NULL COMMENT '方法代码(如 fixed_size)',
    `parameters_template` TEXT DEFAULT NULL COMMENT '参数模板(JSON Schema格式)',
    `default_parameters` TEXT DEFAULT NULL COMMENT '默认参数(JSON格式)',
    `handler_class` VARCHAR(200) DEFAULT NULL COMMENT '处理器类全路径',
    `is_active` BIT(1) NOT NULL DEFAULT b'1' COMMENT '是否启用: 0=停用, 1=启用',
    `is_default_method` BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否默认切片方法',
    `avg_processing_speed` DOUBLE NOT NULL DEFAULT 1.0 COMMENT '平均处理速度(千字/秒)',
    `memory_footprint` INT NOT NULL DEFAULT 100 COMMENT '内存占用(MB)',
    `creator` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`),
    KEY `idx_method_type` (`method_type`),
    KEY `idx_is_active` (`is_active`),
    KEY `idx_is_default_method` (`is_default_method`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='切片方法定义表';


-- ============================================================
-- 菜单数据: 知识库管理 > 切片方法
-- ============================================================

-- 查询"知识库管理"父菜单ID并存入变量
SELECT id INTO @kb_parent_id FROM `system_menu` WHERE name = '知识库管理' AND deleted = b'0' AND type = 1 LIMIT 1;

-- 切片方法 (菜单)
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
VALUES ('切片方法', '', 2, 6, @kb_parent_id, 'chunk-method', 'ep:scissor', 'kb/chunkmethod/index', 'KbChunkMethod', 0, 1, 1, 0, NOW(), NOW(), b'0');

-- 获取刚插入的"切片方法"菜单ID
SET @chunk_method_id = LAST_INSERT_ID();

-- 切片方法按钮权限 (挂在切片方法菜单下)
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
VALUES
('切片方法查询', 'kb:chunk-method:query', 3, 1, @chunk_method_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'),
('切片方法创建', 'kb:chunk-method:create', 3, 2, @chunk_method_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'),
('切片方法更新', 'kb:chunk-method:update', 3, 3, @chunk_method_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'),
('切片方法删除', 'kb:chunk-method:delete', 3, 4, @chunk_method_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'),
('切片方法测试', 'kb:chunk-method:test', 3, 5, @chunk_method_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0');


-- ============================================================
-- 预置数据: 8 种切片方法
-- ============================================================

INSERT INTO `kb_chunk_method` (`name`, `method_type`, `description`, `code`, `parameters_template`, `default_parameters`, `handler_class`, `is_active`, `is_default_method`, `avg_processing_speed`, `memory_footprint`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
VALUES
(
    '固定大小分块器',
    'fixed_size',
    '适用于通用文档，按指定字符数进行均匀分割，支持重叠区域。',
    'fixed_size',
    '{"type":"object","required":["chunk_size"],"properties":{"chunk_size":{"type":"integer","title":"分块大小","description":"每个分块的字符数","default":1000,"minimum":100,"maximum":5000},"chunk_overlap":{"type":"integer","title":"重叠大小","description":"分块之间的重叠字符数","default":200,"minimum":0,"maximum":1000},"respect_boundaries":{"type":"boolean","title":"尊重边界","description":"是否在句子或段落边界处分割","default":true}}}',
    '{"chunk_size":1000,"chunk_overlap":200,"respect_boundaries":true}',
    'cn.iocoder.yudao.module.kb.service.chunk.ChunkMethodService',
    b'1', b'0', 5.0, 50, 'admin', NOW(), 'admin', NOW(), b'0', 0
),
(
    '语义分段分块器',
    'semantic',
    '利用文本语义相似度进行智能分段，保持语义连贯性。',
    'semantic',
    '{"type":"object","properties":{"similarity_threshold":{"type":"number","title":"相似度阈值","description":"语义相似度阈值，高于此值则合并","default":0.8,"minimum":0.1,"maximum":1.0},"min_segment_length":{"type":"integer","title":"最小段落长度","description":"最小段落字符数","default":200,"minimum":50},"max_segment_length":{"type":"integer","title":"最大段落长度","description":"最大段落字符数","default":1500,"minimum":500},"use_embeddings":{"type":"boolean","title":"使用向量","description":"是否使用文本向量计算相似度","default":true}}}',
    '{"similarity_threshold":0.8,"min_segment_length":200,"max_segment_length":1500,"use_embeddings":true}',
    'cn.iocoder.yudao.module.kb.service.chunk.ChunkMethodService',
    b'1', b'0', 1.5, 200, 'admin', NOW(), 'admin', NOW(), b'0', 0
),
(
    '层次分段分块器',
    'hierarchical',
    '为文档创建层次结构，适用于结构化的长文档。',
    'hierarchical',
    '{"type":"object","required":["levels"],"properties":{"levels":{"type":"integer","title":"层级数量","description":"层次结构的层级数","default":3,"minimum":1,"maximum":5},"parent_chunk_size":{"type":"integer","title":"父块大小","description":"父级分块字符数","default":2000,"minimum":1000},"child_chunk_size":{"type":"integer","title":"子块大小","description":"子级分块字符数","default":500,"minimum":100},"detect_headings":{"type":"boolean","title":"检测标题","description":"是否自动检测标题作为层级分割点","default":true},"heading_patterns":{"type":"array","title":"标题模式","description":"用于检测标题的正则表达式","default":["^# ","^## ","^### ","^第[一二三四五六七八九十]+章"]}}}',
    '{"levels":3,"parent_chunk_size":2000,"child_chunk_size":500,"detect_headings":true,"heading_patterns":["^# ","^## ","^### ","^第[一二三四五六七八九十]+章"]}',
    'cn.iocoder.yudao.module.kb.service.chunk.ChunkMethodService',
    b'1', b'0', 3.0, 150, 'admin', NOW(), 'admin', NOW(), b'0', 0
),
(
    '递归分割分块器',
    'recursive',
    '使用递归字符分割策略，依次尝试不同的分隔符。',
    'recursive',
    '{"type":"object","required":["separators","chunk_size"],"properties":{"separators":{"type":"array","title":"分隔符列表","description":"按优先级使用的分隔符列表","default":["\\n\\n","\\n","。","!","?","；",";"," ",""]},"chunk_size":{"type":"integer","title":"目标分块大小","description":"目标分块字符数","default":1000,"minimum":200},"chunk_overlap":{"type":"integer","title":"重叠大小","description":"分块之间的重叠字符数","default":200,"minimum":0},"respect_sentence_boundaries":{"type":"boolean","title":"尊重句子边界","description":"是否在句子边界处分割","default":true}}}',
    '{"separators":["\\n\\n","\\n","。","!","?","；",";"," ",""],"chunk_size":1000,"chunk_overlap":200,"respect_sentence_boundaries":true}',
    'cn.iocoder.yudao.module.kb.service.chunk.ChunkMethodService',
    b'1', b'0', 4.0, 80, 'admin', NOW(), 'admin', NOW(), b'0', 0
),
(
    '按句子分块器',
    'sentence',
    '按句子边界进行分割，保持句子完整性。',
    'sentence',
    '{"type":"object","required":["sentences_per_chunk","language"],"properties":{"sentences_per_chunk":{"type":"integer","title":"每块句子数","description":"每个分块包含的句子数","default":5,"minimum":1,"maximum":20},"language":{"type":"string","title":"语言","description":"文本语言","default":"zh","enum":["zh","en","ja","ko"]},"keep_punctuation":{"type":"boolean","title":"保留标点","description":"是否保留句子末尾标点","default":true},"respect_paragraphs":{"type":"boolean","title":"尊重段落","description":"是否不跨段落合并句子","default":true}}}',
    '{"sentences_per_chunk":5,"language":"zh","keep_punctuation":true,"respect_paragraphs":true}',
    'cn.iocoder.yudao.module.kb.service.chunk.ChunkMethodService',
    b'1', b'0', 4.5, 60, 'admin', NOW(), 'admin', NOW(), b'0', 0
),
(
    '按段落分块器',
    'paragraph',
    '按段落边界进行分割，保持段落完整性。',
    'paragraph',
    '{"type":"object","required":["paragraphs_per_chunk"],"properties":{"paragraphs_per_chunk":{"type":"integer","title":"每块段落数","description":"每个分块包含的段落数","default":3,"minimum":1,"maximum":10},"min_paragraph_length":{"type":"integer","title":"最小段落长度","description":"最小段落字符数","default":50,"minimum":10},"merge_short_paragraphs":{"type":"boolean","title":"合并短段落","description":"是否将过短的段落合并","default":true},"max_paragraph_length":{"type":"integer","title":"最大段落长度","description":"最大段落字符数，超过将分割","default":2000,"minimum":500}}}',
    '{"paragraphs_per_chunk":3,"min_paragraph_length":50,"merge_short_paragraphs":true,"max_paragraph_length":2000}',
    'cn.iocoder.yudao.module.kb.service.chunk.ChunkMethodService',
    b'1', b'1', 4.5, 60, 'admin', NOW(), 'admin', NOW(), b'0', 0
),
(
    '按章节分块器',
    'section',
    '按文档章节结构进行分割，保持章节完整性。',
    'section',
    '{"type":"object","required":["heading_patterns"],"properties":{"heading_patterns":{"type":"array","title":"标题模式","description":"用于检测标题的正则表达式","default":["^#\\\\s+.+","^##\\\\s+.+","^###\\\\s+.+","^第[一二三四五六七八九十]+章"]},"include_heading":{"type":"boolean","title":"包含标题","description":"是否将标题包含在分块中","default":true},"max_section_size":{"type":"integer","title":"最大章节大小","description":"最大章节字符数","default":5000,"minimum":1000},"split_large_sections":{"type":"boolean","title":"分割大章节","description":"是否对过大的章节进行二次分割","default":true},"respect_hierarchy":{"type":"boolean","title":"尊重层级","description":"是否根据标题级别创建层次结构","default":true}}}',
    '{"heading_patterns":["^#\\s+.+","^##\\s+.+","^###\\s+.+","^第[一二三四五六七八九十]+章"],"include_heading":true,"max_section_size":5000,"split_large_sections":true,"respect_hierarchy":true}',
    'cn.iocoder.yudao.module.kb.service.chunk.ChunkMethodService',
    b'1', b'0', 4.0, 70, 'admin', NOW(), 'admin', NOW(), b'0', 0
),
(
    '自定义分块器',
    'custom',
    '用户自定义的分块器实现，支持完全自定义的切片逻辑。',
    'custom',
    '{"type":"object","properties":{"custom_parameters":{"type":"object","title":"自定义参数","description":"自定义切片方法所需的参数","default":{}}}}',
    '{"custom_parameters":{}}',
    'cn.iocoder.yudao.module.kb.service.chunk.ChunkMethodService',
    b'1', b'0', 2.0, 100, 'admin', NOW(), 'admin', NOW(), b'0', 0
);


-- ============================================================
-- 字典数据: 切片方法类型
-- ============================================================
INSERT INTO `system_dict_type` (`name`, `type`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES ('切片方法类型', 'kb_chunk_method_type', 0, '知识库-切片方法类型', 'admin', NOW(), 'admin', NOW(), b'0');

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
(1, '固定大小', 'fixed_size', 'kb_chunk_method_type', 0, '', '', '固定大小切片', 'admin', NOW(), 'admin', NOW(), b'0'),
(2, '语义分段', 'semantic', 'kb_chunk_method_type', 0, 'success', '', '语义分段切片', 'admin', NOW(), 'admin', NOW(), b'0'),
(3, '层次分段', 'hierarchical', 'kb_chunk_method_type', 0, 'warning', '', '层次分段切片', 'admin', NOW(), 'admin', NOW(), b'0'),
(4, '递归分割', 'recursive', 'kb_chunk_method_type', 0, 'primary', '', '递归分割切片', 'admin', NOW(), 'admin', NOW(), b'0'),
(5, '按句子', 'sentence', 'kb_chunk_method_type', 0, 'info', '', '按句子切片', 'admin', NOW(), 'admin', NOW(), b'0'),
(6, '按段落', 'paragraph', 'kb_chunk_method_type', 0, 'danger', '', '按段落切片', 'admin', NOW(), 'admin', NOW(), b'0'),
(7, '按章节', 'section', 'kb_chunk_method_type', 0, '', '', '按章节切片', 'admin', NOW(), 'admin', NOW(), b'0'),
(8, '自定义', 'custom', 'kb_chunk_method_type', 0, '', '', '自定义切片方法', 'admin', NOW(), 'admin', NOW(), b'0');
