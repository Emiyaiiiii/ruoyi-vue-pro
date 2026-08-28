-- 知识库新增图片处理方案字段（库级优先，未设置时空字符串回退全局缺省/存量推断）
-- 取值：''=按激活模型推断；none=纯文本；ocr=OCR文字；vl_summary=VL总结；vision=视觉召回
ALTER TABLE `kb_library` ADD COLUMN `image_strategy` varchar(20) NOT NULL DEFAULT '' COMMENT '图片处理方案' AFTER `is_project`;