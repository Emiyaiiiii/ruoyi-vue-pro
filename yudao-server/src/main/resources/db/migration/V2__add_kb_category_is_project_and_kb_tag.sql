-- =====================================================
-- V2: 知识库/智能体 增量变更（原 V2/V3/V4 整合）
--
-- 来源（并入原 V2/V3/V4 三个一次性增量）:
--   * sql/mysql/kb_category_is_project.sql —— kb_category 增加 is_project
--   * V3__add_library_image_strategy.sql    —— kb_library 增加 image_strategy
--   * sql/mysql/kb_tag.sql                  —— 新建 kb_tag 表
--   * V4__add_agent_approval_level.sql      —— ai_agent 增加 approval_level
--
-- 说明:
--   * V1 基线里的 kb_library 已经包含 is_project / image_strategy 两列，
--     早期版本此处用裸 ALTER ADD COLUMN，会在全新库上以
--     Duplicate column name 'image_strategy' 失败并留下 success=0 的
--     flyway_schema_history 记录，导致后续每次启动都 FlywayValidateException。
--   * 因此本迁移改为**幂等**写法：先查 information_schema.COLUMNS，
--     不存在才 ALTER ADD；建表用 CREATE TABLE IF NOT EXISTS。
--     （MySQL 不支持 ADD COLUMN IF NOT EXISTS，那是 MariaDB 语法）
--     与 V18__patch_ai_agent_columns.sql 使用同一套 ckb_add_column 存储过程。
--   * 对应菜单种子见 R__menu_seed.sql（行业数据走 R__，升级自动重跑）。
-- =====================================================

DROP PROCEDURE IF EXISTS `ckb_add_column`;
DELIMITER $$
CREATE PROCEDURE `ckb_add_column`(IN p_table VARCHAR(64), IN p_column VARCHAR(64), IN p_ddl TEXT)
BEGIN
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = p_table
        AND COLUMN_NAME = p_column) = 0 THEN
    SET @ddl = CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN ', p_ddl);
    PREPARE stmt FROM @ddl;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END$$
DELIMITER ;

-- =====================================================
-- 1. kb_category 增加 is_project
--    院级/公司下的「项目成果库」分类可配置为项目库分类，
--    该分类下创建的知识库自动纳入项目成员管理。
-- =====================================================
CALL ckb_add_column('kb_category', 'is_project',
    "`is_project` TINYINT NOT NULL DEFAULT 0 COMMENT '是否项目成果库分类: 0=否, 1=是。该分类下创建的知识库自动纳入项目成员管理' AFTER `column_config`");

-- =====================================================
-- 2. 知识库标签表 kb_tag
--    可见性: owner_id 为空 = 全局标签(所有人可见)，非空 = 个人标签(仅本人可见)
-- =====================================================
CREATE TABLE IF NOT EXISTS `kb_tag` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name` varchar(100) NOT NULL COMMENT '标签名称',
    `color` varchar(32) NOT NULL DEFAULT '#007bff' COMMENT '标签颜色',
    `type` varchar(32) NOT NULL DEFAULT 'other' COMMENT '标签类型: knowledge_base=知识库, document=文档, chunk=文档切片, other=其他',
    `owner_id` bigint DEFAULT NULL COMMENT '归属用户ID，NULL=全局标签',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    KEY `idx_type` (`type`),
    KEY `idx_owner_id` (`owner_id`),
    KEY `idx_name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库标签表';

-- =====================================================
-- 3. kb_library 增加 image_strategy（图片处理方案）
--    原 V3__add_library_image_strategy.sql。
--    库级优先，未设置时空字符串回退全局缺省/存量推断。
--    取值: ''=按激活模型推断; none=纯文本; ocr=OCR文字; vl_summary=VL总结; vision=视觉召回
--    注意: V1 基线已含该列，此处仅对旧库补齐，幂等跳过。
-- =====================================================
CALL ckb_add_column('kb_library', 'image_strategy',
    "`image_strategy` varchar(20) NOT NULL DEFAULT '' COMMENT '图片处理方案' AFTER `is_project`");

-- =====================================================
-- 4. ai_agent 增加 approval_level（工具审批级别）
--    原 V4__add_agent_approval_level.sql。
--    控制智能体调用工具的审批机制。
--    取值: strict=所有工具需审批, auto=智能, off=免审批
-- =====================================================
CALL ckb_add_column('ai_agent', 'approval_level',
    "`approval_level` varchar(16) NOT NULL DEFAULT 'auto' COMMENT '工具审批级别: strict=所有工具需审批, auto=智能, off=免审批'");

DROP PROCEDURE IF EXISTS `ckb_add_column`;
