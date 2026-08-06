-- 为 kb_document 表添加向量处理相关字段
-- 执行时间: 2026-08-06

ALTER TABLE kb_document
    ADD COLUMN vector_task_id VARCHAR(64) DEFAULT NULL COMMENT '向量处理任务ID（关联 kb_vector_task.task_id）' AFTER status,
    ADD COLUMN vector_status TINYINT DEFAULT 0 COMMENT '向量处理状态：0-未处理 1-处理中 2-已完成 3-失败 4-提交失败 5-超时' AFTER vector_task_id;

-- 添加索引（可选，根据查询需求决定）
-- CREATE INDEX idx_vector_status ON kb_document(vector_status);
-- CREATE INDEX idx_vector_task_id ON kb_document(vector_task_id);
