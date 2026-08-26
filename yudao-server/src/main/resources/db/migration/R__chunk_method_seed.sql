-- =====================================================================
-- R__chunk_method_seed.sql  (Flyway Repeatable 迁移)
-- 可重复迁移：内容一旦变更，下次 migrate 会自动重跑；以 code 唯一键幂等 upsert。
-- 用途：承载「可变的业务种子数据」——本文件收拢 kb_chunk_method 的 5 种
--       切片方法 + 各自的示例默认参数，以及字典清理。
-- 约定：结构 DDL 走 V*__xxx.sql（不可变）；此类易微调的种子数据改这里即可，
--       不要再因"改一点参数"就新增 V 版本。
-- =====================================================================

-- 1) upsert 5 种切片方法（code 为唯一键；含 V10 初始化 + V11 特性化示例参数 的合并成果）
INSERT INTO `kb_chunk_method`
  (`id`,`name`,`method_type`,`description`,`code`,`parameters_template`,`default_parameters`,`handler_class`,`is_active`,`is_default_method`,`creator`,`create_time`,`updater`,`update_time`,`deleted`,`tenant_id`)
VALUES
  (1,'固定大小','fixed_size','按字符数均匀切分，通用性强','fixed_size','{}',
   '{"strategy":"fixed_size","chunk_size":1000,"chunk_overlap":200,"min_chunk_size":100,"max_chunk_size":2000}',
   '',1,1,'admin',NOW(),'admin',NOW(),0,1),
  (2,'按句子','sentence','向最近的句读边界对齐，避免截断完整句子','sentence','{}',
   '{"strategy":"sentence","chunk_size":800,"chunk_overlap":100,"min_chunk_size":50,"max_chunk_size":2000}',
   '',1,0,'admin',NOW(),'admin',NOW(),0,1),
  (3,'按段落','paragraph','向段落边界对齐，适合结构性较强的文档','paragraph','{}',
   '{"strategy":"paragraph","chunk_size":1500,"chunk_overlap":150,"min_chunk_size":200,"max_chunk_size":4000}',
   '',1,0,'admin',NOW(),'admin',NOW(),0,1),
  (4,'递归分割','recursive','按分隔符层级递归切分，契合 Markdown/代码等层级结构','recursive','{}',
   '{"strategy":"recursive","chunk_size":1200,"chunk_overlap":150,"min_chunk_size":100,"max_chunk_size":3000}',
   '',1,0,'admin',NOW(),'admin',NOW(),0,1),
  (5,'语义分段','semantic','按语义相似度合并段落，需嵌入模型；未配置时退化为固定大小','semantic','{}',
   '{"strategy":"semantic","chunk_size":1000,"chunk_overlap":0,"min_chunk_size":100,"max_chunk_size":2000}',
   '',1,0,'admin',NOW(),'admin',NOW(),0,1)
ON DUPLICATE KEY UPDATE
  `name`=VALUES(`name`),
  `method_type`=VALUES(`method_type`),
  `description`=VALUES(`description`),
  `parameters_template`=VALUES(`parameters_template`),
  `default_parameters`=VALUES(`default_parameters`),
  `handler_class`=VALUES(`handler_class`),
  `is_active`=VALUES(`is_active`),
  `is_default_method`=VALUES(`is_default_method`),
  `deleted`=VALUES(`deleted`);

-- 2) 字典一致性：剔除 python-vector 端不存在的类型（幂等，删不到也无所谓）
DELETE FROM `system_dict_data`
WHERE `dict_type` = 'kb_chunk_method_type' AND `deleted` = 0
  AND `value` IN ('hierarchical', 'section', 'custom');