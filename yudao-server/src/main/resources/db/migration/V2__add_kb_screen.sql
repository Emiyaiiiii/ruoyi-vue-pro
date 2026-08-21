-- ============================================================
-- 知识库大屏 + 自定义表头（顶级菜单，与「首页」「知识库总览」平级）
-- 功能：
--   1. 左侧知识库分类树 + 右侧知识库列表（自定义表头）
--   2. 点击知识库进入文件列表（文件夹 + 文档 CRUD）
--   3. 自定义表头：在「分类管理」中为每个分类配置列模板
--      （内置列可改名 + 自定义字段：文本/日期/数字/成员/部门/下拉）
--   4. 自定义字段值存到 kb_library_ext 键值表
-- ============================================================

-- 设置连接字符集为 utf8mb4
SET NAMES utf8mb4;

-- ============================================================
-- 1. 为 kb_category 表添加「表头配置」字段（列模板 JSON）
-- ============================================================
-- 若字段已存在，执行会报错，可忽略该错误
ALTER TABLE `kb_category`
    ADD COLUMN `column_config` VARCHAR(2000) DEFAULT NULL COMMENT '列模板(JSON): 该分类下知识库列表的自定义表头，如 [{"source":"builtin","builtin":"name","label":"项目名称"},{"source":"custom","key":"project_member","label":"项目成员","type":"member"}]' AFTER `status`;

-- ============================================================
-- 2. 知识库自定义字段值表 kb_library_ext
-- ============================================================
DROP TABLE IF EXISTS `kb_library_ext`;
CREATE TABLE `kb_library_ext` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `kb_id` BIGINT NOT NULL COMMENT '知识库ID',
    `field_key` VARCHAR(64) NOT NULL COMMENT '字段key（来自分类 column_config 的自定义字段定义）',
    `field_value` TEXT DEFAULT NULL COMMENT '字段值（文本；成员多选为 JSON 数组字符串；部门/日期/数字/下拉为字符串）',
    `creator` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk_kb_field` (`kb_id`, `field_key`),
    KEY `idx_kb_id` (`kb_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库自定义字段值表';

-- ============================================================
-- 3. 菜单数据: 知识库大屏（顶级菜单，与「首页」「知识库总览」平级）
-- ============================================================

-- 3.1 迁移：若此前已作为「知识库管理」的子菜单插入，则移到顶级（parent_id=0）
UPDATE `system_menu`
SET `parent_id` = 0, `sort` = 27
WHERE `name` = '知识库大屏' AND `component` = 'kb/screen/index' AND `deleted` = b'0';

-- 3.2 若不存在则插入为顶级菜单
-- 说明：
--   * parent_id=0 表示顶级菜单，与「首页」「知识库总览」同级；如需调整顺序，改 sort 或在「菜单管理」中拖动即可
--   * 按钮权限复用已有的 kb:library:* / kb:document:* 等，无需新增按钮权限
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT '知识库大屏', '', 2, 27, 0, '/screen', 'ep:data-analysis', 'kb/screen/index', 'KbScreen', 0, 1, 1, 0, NOW(), NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM `system_menu` WHERE `name` = '知识库大屏' AND `component` = 'kb/screen/index' AND `deleted` = b'0'
);