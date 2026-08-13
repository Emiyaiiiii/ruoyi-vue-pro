-- ============================================================
-- 知识库管理 - 新闻管理模块
-- 对应前端菜单: 知识库管理 > 新闻管理
-- 包含: 新闻数据源 / 新闻记录 / 同步日志
-- ============================================================

-- 设置连接字符集为 utf8mb4 (避免与 gbk 连接的 collation 冲突)
SET NAMES utf8mb4;

-- ============================================================
-- 1. 新闻数据源配置表
-- ============================================================
DROP TABLE IF EXISTS `kb_news_source`;
CREATE TABLE `kb_news_source` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name` VARCHAR(100) NOT NULL COMMENT '数据源名称',
    `db_host` VARCHAR(255) NOT NULL COMMENT '外部数据库主机',
    `db_port` INT NOT NULL DEFAULT 3306 COMMENT '外部数据库端口',
    `db_name` VARCHAR(100) NOT NULL COMMENT '外部数据库名称',
    `db_user` VARCHAR(100) NOT NULL COMMENT '数据库用户名',
    `db_password` VARCHAR(255) NOT NULL COMMENT '数据库密码',
    `table_name` VARCHAR(100) NOT NULL COMMENT '外部表名',
    -- 字段映射
    `id_field` VARCHAR(100) NOT NULL DEFAULT 'id' COMMENT 'ID字段名',
    `title_field` VARCHAR(100) NOT NULL DEFAULT 'doctitle' COMMENT '标题字段名',
    `content_field` VARCHAR(100) NOT NULL DEFAULT 'doccontent' COMMENT '内容字段名',
    `channel_field` VARCHAR(100) DEFAULT NULL COMMENT '频道字段名',
    `time_field` VARCHAR(100) DEFAULT NULL COMMENT '时间字段名',
    `url_field` VARCHAR(100) DEFAULT NULL COMMENT 'URL字段名',
    `crdept_field` VARCHAR(100) DEFAULT NULL COMMENT '部门字段名',
    `cruser_field` VARCHAR(100) DEFAULT NULL COMMENT '用户字段名',
    -- 同步配置
    `sync_enabled` BIT(1) NOT NULL DEFAULT b'1' COMMENT '是否启用同步: 0=停用, 1=启用',
    `sync_interval` INT DEFAULT 3600 COMMENT '同步间隔(秒)',
    `db_dept` BIGINT DEFAULT NULL COMMENT '所属部门ID',
    `last_sync_time` DATETIME DEFAULT NULL COMMENT '上次同步时间',
    -- 统计字段
    `total_records` INT NOT NULL DEFAULT 0 COMMENT '同步总记录数',
    `processed_records` INT NOT NULL DEFAULT 0 COMMENT '已处理记录数',
    `error_count` INT NOT NULL DEFAULT 0 COMMENT '错误数',
    -- 基础字段
    `creator` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_sync_enabled` (`sync_enabled`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='新闻数据源配置表';


-- ============================================================
-- 2. 新闻记录缓存表
-- ============================================================
DROP TABLE IF EXISTS `kb_news_record`;
CREATE TABLE `kb_news_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `source_id` BIGINT NOT NULL COMMENT '数据源ID',
    `external_id` VARCHAR(100) NOT NULL COMMENT '外部记录ID',
    `external_title` VARCHAR(500) DEFAULT NULL COMMENT '外部标题',
    `external_content` LONGTEXT DEFAULT NULL COMMENT '外部内容',
    `external_channel` VARCHAR(100) DEFAULT NULL COMMENT '频道',
    `external_time` VARCHAR(50) DEFAULT NULL COMMENT '外部时间',
    `external_url` VARCHAR(500) DEFAULT NULL COMMENT '外部URL',
    `external_crdept` VARCHAR(200) DEFAULT NULL COMMENT '创建部门',
    `external_cruser` VARCHAR(100) DEFAULT NULL COMMENT '创建用户',
    -- 处理状态（简化版）
    `status` VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '状态: pending=待处理, completed=已完成, failed=失败, skipped=已跳过',
    `processing_status` VARCHAR(50) DEFAULT NULL COMMENT '处理阶段描述',
    -- 错误追踪
    `error_message` TEXT DEFAULT NULL COMMENT '错误信息',
    `retry_count` INT NOT NULL DEFAULT 0 COMMENT '重试次数',
    -- 关联文档信息（同步时回写）
    `doc_id` BIGINT DEFAULT NULL COMMENT '关联文档ID',
    `kb_id` BIGINT DEFAULT NULL COMMENT '关联知识库ID',
    `file_url` VARCHAR(500) DEFAULT NULL COMMENT '文件访问URL',
    `file_type` VARCHAR(20) DEFAULT NULL COMMENT '文件类型',
    -- 时间戳
    `last_processed_at` DATETIME DEFAULT NULL COMMENT '上次处理时间',
    `processed_at` DATETIME DEFAULT NULL COMMENT '处理完成时间',
    `external_updated_at` DATETIME DEFAULT NULL COMMENT '外部更新时间',
    -- 基础字段
    `creator` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk_source_external` (`source_id`, `external_id`),
    KEY `idx_source_id` (`source_id`),
    KEY `idx_status` (`status`),
    KEY `idx_external_channel` (`external_channel`),
    KEY `idx_external_time` (`external_time`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='新闻记录缓存表';


-- ============================================================
-- 3. 新闻同步日志表
-- ============================================================
DROP TABLE IF EXISTS `kb_news_sync_log`;
CREATE TABLE `kb_news_sync_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `source_id` BIGINT NOT NULL COMMENT '数据源ID',
    `sync_type` VARCHAR(20) NOT NULL COMMENT '同步类型: full=全量, incremental=增量, manual=手动',
    `status` VARCHAR(20) NOT NULL DEFAULT 'started' COMMENT '状态: started=已开始, running=运行中, completed=已完成, failed=失败',
    `total_fetched` INT NOT NULL DEFAULT 0 COMMENT '获取总数',
    `new_records` INT NOT NULL DEFAULT 0 COMMENT '新增记录数',
    `updated_records` INT NOT NULL DEFAULT 0 COMMENT '更新记录数',
    `skipped_records` INT NOT NULL DEFAULT 0 COMMENT '跳过记录数',
    `failed_records` INT NOT NULL DEFAULT 0 COMMENT '失败记录数',
    `started_at` DATETIME DEFAULT NULL COMMENT '开始时间',
    `completed_at` DATETIME DEFAULT NULL COMMENT '完成时间',
    `error_message` TEXT DEFAULT NULL COMMENT '错误信息',
    `details` JSON DEFAULT NULL COMMENT '详细信息(JSON)',
    -- 基础字段
    `creator` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_source_id` (`source_id`),
    KEY `idx_status` (`status`),
    KEY `idx_sync_type` (`sync_type`),
    KEY `idx_started_at` (`started_at`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='新闻同步日志表';


-- ============================================================
-- 4. 菜单数据: 知识库管理 > 新闻管理
-- ============================================================

-- 查询"知识库管理"父菜单ID并存入变量
SELECT id INTO @kb_parent_id FROM `system_menu` WHERE name = '知识库管理' AND deleted = b'0' AND type = 1 LIMIT 1;

-- 新闻管理 (菜单)
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
VALUES ('新闻管理', '', 2, 8, @kb_parent_id, 'news', 'ep:news', 'kb/news/index', 'KbNews', 0, 1, 1, 0, NOW(), NOW(), b'0');

-- 获取刚插入的"新闻管理"菜单ID
SET @news_menu_id = LAST_INSERT_ID();

-- 新闻管理按钮权限 (挂在新闻管理菜单下)
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
VALUES
('新闻数据源查询', 'kb:news-source:query', 3, 1, @news_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'),
('新闻数据源创建', 'kb:news-source:create', 3, 2, @news_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'),
('新闻数据源更新', 'kb:news-source:update', 3, 3, @news_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'),
('新闻数据源删除', 'kb:news-source:delete', 3, 4, @news_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'),
('新闻记录查询', 'kb:news-record:query', 3, 5, @news_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'),
('新闻记录批量操作', 'kb:news-record:batch', 3, 6, @news_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'),
('新闻同步日志查询', 'kb:news-sync-log:query', 3, 7, @news_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0');


-- ============================================================
-- 5. 字典数据 — 新闻记录状态
-- ============================================================

-- 查询"知识库字典"父ID
SELECT id INTO @kb_dict_parent_id FROM `system_dict_type` WHERE `type` = 'kb_chunk_method' AND deleted = b'0' LIMIT 1;
-- 如果上面没查到，尝试用 system_dict_data 里的通用字典定位
SELECT id INTO @kb_dict_parent_id FROM `system_dict_type` WHERE `type` LIKE 'kb\_%' AND deleted = b'0' LIMIT 1;

-- 新闻记录状态 (news_record_status)
INSERT IGNORE INTO `system_dict_type` (`name`, `type`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES ('新闻记录状态', 'kb_news_record_status', 0, '新闻管理 - 新闻记录处理状态', '1', NOW(), '1', NOW(), b'0');

SET @dict_type_id = LAST_INSERT_ID();

INSERT IGNORE INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
(1, '待处理', 'pending', 'kb_news_record_status', 0, 'info', '', '待同步或待处理的新闻记录', '1', NOW(), '1', NOW(), b'0'),
(2, '已完成', 'completed', 'kb_news_record_status', 0, 'success', '', '已成功处理的新闻记录', '1', NOW(), '1', NOW(), b'0'),
(3, '失败', 'failed', 'kb_news_record_status', 0, 'danger', '', '处理失败的新闻记录', '1', NOW(), '1', NOW(), b'0'),
(4, '已跳过', 'skipped', 'kb_news_record_status', 0, '', '', '内容为空或无有效信息的记录', '1', NOW(), '1', NOW(), b'0');

-- 新闻同步类型 (news_sync_type)
INSERT IGNORE INTO `system_dict_type` (`name`, `type`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES ('新闻同步类型', 'kb_news_sync_type', 0, '新闻管理 - 同步类型', '1', NOW(), '1', NOW(), b'0');

SET @dict_type_id2 = LAST_INSERT_ID();

INSERT IGNORE INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
(1, '全量同步', 'full', 'kb_news_sync_type', 0, 'primary', '', '从外部数据库全量同步全部数据', '1', NOW(), '1', NOW(), b'0'),
(2, '增量同步', 'incremental', 'kb_news_sync_type', 0, 'success', '', '仅同步上次同步时间之后的新增/变更数据', '1', NOW(), '1', NOW(), b'0'),
(3, '手动同步', 'manual', 'kb_news_sync_type', 0, 'warning', '', '管理员手动触发的同步操作', '1', NOW(), '1', NOW(), b'0');

-- 新闻同步状态 (news_sync_status)
INSERT IGNORE INTO `system_dict_type` (`name`, `type`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES ('新闻同步状态', 'kb_news_sync_status', 0, '新闻管理 - 同步任务状态', '1', NOW(), '1', NOW(), b'0');

SET @dict_type_id3 = LAST_INSERT_ID();

INSERT IGNORE INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
(1, '已开始', 'started', 'kb_news_sync_status', 0, 'info', '', '同步任务已创建', '1', NOW(), '1', NOW(), b'0'),
(2, '运行中', 'running', 'kb_news_sync_status', 0, 'warning', '', '同步任务正在执行中', '1', NOW(), '1', NOW(), b'0'),
(3, '已完成', 'completed', 'kb_news_sync_status', 0, 'success', '', '同步任务已成功完成', '1', NOW(), '1', NOW(), b'0'),
(4, '失败', 'failed', 'kb_news_sync_status', 0, 'danger', '', '同步任务执行失败', '1', NOW(), '1', NOW(), b'0');


-- ============================================================
-- 6. 定时任务注册 — 新闻数据同步 Job
-- ============================================================
-- 说明: handler_name = Spring Bean 名称 (newsSyncJob)
--       cron_expression: 0 0 * * * ? = 每小时整点执行
--       retry_count: 3 次重试
--       retry_interval: 60000 毫秒 (60秒)
--       status: 1 = NORMAL (正常启用; 0=初始化, 1=开启, 2=暂停)
-- 如果 infra_job 表不存在，跳过此步骤
INSERT IGNORE INTO `infra_job` (`name`, `status`, `handler_name`, `handler_param`, `cron_expression`, `retry_count`, `retry_interval`, `monitor_timeout`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES ('新闻数据同步 Job', 1, 'newsSyncJob', NULL, '0 0 * * * ?', 3, 60000, 0, '1', NOW(), '1', NOW(), b'0');

-- ⚠️ 重要：SQL 直接插入 infra_job 只是在数据库里加了记录，
--    还需要同步到 Quartz 调度器才能生效。执行以下任一操作：
--
--  方式 A（推荐）：重启后端服务后，在管理后台点击
--     「基础设施 → 定时任务 → 同步」按钮
--
--  方式 B：调用 API 同步
--     POST /infra/job/sync
--
--  方式 C：在管理后台「基础设施 → 定时任务」页面删除该记录，
--     然后通过「新建」按钮重新创建（自动注册到 Quartz）
--
-- 同步完成后，「新闻数据同步 Job」会在每小时整点自动执行，
-- 也可以在管理后台点击「执行一次」手动触发。


-- ============================================================
-- 7. 增量升级：为已存在的 kb_news_source 表添加 db_dept 字段
-- ============================================================
-- 如果表已存在，执行此语句添加所属部门字段（先执行，若字段已存在则报错可忽略）
ALTER TABLE `kb_news_source` ADD COLUMN `db_dept` BIGINT DEFAULT NULL COMMENT '所属部门ID' AFTER `sync_interval`;

-- 为 kb_news_record 添加关联文档字段
ALTER TABLE `kb_news_record` ADD COLUMN `doc_id` BIGINT DEFAULT NULL COMMENT '关联文档ID' AFTER `retry_count`;
ALTER TABLE `kb_news_record` ADD COLUMN `kb_id` BIGINT DEFAULT NULL COMMENT '关联知识库ID' AFTER `doc_id`;
ALTER TABLE `kb_news_record` ADD COLUMN `file_url` VARCHAR(500) DEFAULT NULL COMMENT '文件访问URL' AFTER `kb_id`;
ALTER TABLE `kb_news_record` ADD COLUMN `file_type` VARCHAR(20) DEFAULT NULL COMMENT '文件类型' AFTER `file_url`;
