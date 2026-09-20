-- =====================================================================
-- V5__add_kb_vector_task_stages.sql  (Flyway Versioned 迁移)
-- kb_vector_task 增加 stages 阶段时间线列：记录文档处理各阶段
-- （download→parse→chunk→embed→store）的状态/开始结束时间/耗时/指标，
-- 供前端渲染处理时间轴（对齐 #13 pipeline 阶段时间线观测）。
-- =====================================================================
ALTER TABLE `kb_vector_task`
    ADD COLUMN `stages` text NULL COMMENT '处理阶段时间线(JSON数组): [{stage,status,begin_ts,end_ts,duration_s,metrics}]；前端时间轴渲染用' AFTER `current_step`;
