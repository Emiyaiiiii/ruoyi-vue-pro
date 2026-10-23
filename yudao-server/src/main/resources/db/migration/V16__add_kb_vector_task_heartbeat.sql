-- =====================================================================
-- V16__add_kb_vector_task_heartbeat.sql  (Flyway Versioned 迁移)
-- 假超时治理：区分“排队时长”和“执行时长”
--   queued_at     入队时间（PENDING 排队计时）
--   started_at    开始执行时间（PROCESSING 计时起点）
--   heartbeat_at  最近心跳时间（判定僵尸/执行超时）
--   attempt       已尝试次数（为自动重试预留）
--   max_attempts  最大尝试次数
-- =====================================================================
ALTER TABLE `kb_vector_task`
    ADD COLUMN `queued_at` datetime NULL COMMENT '入队时间' AFTER `status`,
    ADD COLUMN `started_at` datetime NULL COMMENT '开始执行时间' AFTER `queued_at`,
    ADD COLUMN `heartbeat_at` datetime NULL COMMENT '最近心跳时间' AFTER `started_at`,
    ADD COLUMN `attempt` int NOT NULL DEFAULT 0 COMMENT '已尝试次数' AFTER `heartbeat_at`,
    ADD COLUMN `max_attempts` int NOT NULL DEFAULT 3 COMMENT '最大尝试次数' AFTER `attempt`,
    ADD INDEX `idx_status_heartbeat` (`status`, `heartbeat_at`),
    ADD INDEX `idx_status_queued` (`status`, `queued_at`);

-- 历史数据回填：避免升级后立即被“心跳为空”误判
UPDATE `kb_vector_task`
SET `heartbeat_at` = `update_time`
WHERE `status` = 1 AND `heartbeat_at` IS NULL;

UPDATE `kb_vector_task`
SET `queued_at` = `create_time`
WHERE `status` = 0 AND `queued_at` IS NULL;
