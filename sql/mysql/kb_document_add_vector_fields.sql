-- 为 kb_document 表新增向量处理状态字段
ALTER TABLE `kb_document`
    ADD COLUMN `vector_task_id` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '向量处理任务ID' AFTER `status`,
    ADD COLUMN `vector_status` TINYINT NOT NULL DEFAULT 0 COMMENT '向量处理状态：0-未处理 1-处理中 2-已完成 3-失败 4-提交失败 5-超时' AFTER `vector_task_id`,
    ADD INDEX `idx_vector_status` (`vector_status`);
