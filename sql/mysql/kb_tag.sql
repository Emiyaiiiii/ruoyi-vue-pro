-- ============================================================
-- 知识库管理 - 标签管理
-- 对应前端菜单: 知识库管理 > 标签管理
-- 可见性: owner_id 为空 = 全局标签(所有人可见)，非空 = 个人标签(仅本人可见)
-- ============================================================

SET NAMES utf8mb4;

DROP TABLE IF EXISTS `kb_tag`;
CREATE TABLE `kb_tag` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name` VARCHAR(100) NOT NULL COMMENT '标签名称',
    `color` VARCHAR(32) NOT NULL DEFAULT '#007bff' COMMENT '标签颜色',
    `type` VARCHAR(32) NOT NULL DEFAULT 'other' COMMENT '标签类型: knowledge_base=知识库, document=文档, chunk=文档切片, other=其他',
    `owner_id` BIGINT DEFAULT NULL COMMENT '归属用户ID，NULL=全局标签',
    `creator` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    KEY `idx_type` (`type`),
    KEY `idx_owner_id` (`owner_id`),
    KEY `idx_name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库标签表';

-- ============================================================
-- 菜单数据: 知识库管理 > 标签管理
-- ============================================================

-- 查询"知识库管理"父菜单ID并存入变量
SELECT id INTO @kb_parent_id FROM `system_menu` WHERE name = '知识库管理' AND deleted = b'0' AND type = 1 LIMIT 1;

-- 标签管理 (菜单)
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
VALUES ('标签管理', '', 2, 10, @kb_parent_id, 'tag', 'ep:price-tag', 'kb/tag/index', 'KbTag', 0, 1, 1, 0, NOW(), NOW(), b'0');

-- 获取刚插入的"标签管理"菜单ID
SET @tag_id = LAST_INSERT_ID();

-- 标签管理按钮权限 (挂在标签管理菜单下)
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
VALUES
('标签查询', 'kb:tag:query', 3, 1, @tag_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'),
('标签创建', 'kb:tag:create', 3, 2, @tag_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'),
('标签更新', 'kb:tag:update', 3, 3, @tag_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'),
('标签删除', 'kb:tag:delete', 3, 4, @tag_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0');
