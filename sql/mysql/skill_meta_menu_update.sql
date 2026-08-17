-- =============================================
-- 技能商店菜单权限补充（增量 SQL）
-- 为技能商店添加创建/更新/删除/同步权限按钮
-- =============================================

-- 查找技能商店菜单ID
SET @skill_menu_id = (SELECT id FROM system_menu WHERE name = '技能商店' AND parent_id != 0 AND deleted = 0 LIMIT 1);

-- 如果菜单不存在则不执行（安全保护）
SELECT IF(@skill_menu_id IS NULL, 'WARNING: 技能商店菜单未找到，请先执行 ai_agent.sql', CONCAT('Found skill menu id: ', @skill_menu_id));

-- 补充权限按钮（仅当菜单存在时）
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT '技能创建', 'ai-agent:skill-meta:create', 3, 2, @skill_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE @skill_menu_id IS NOT NULL;

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT '技能更新', 'ai-agent:skill-meta:update', 3, 3, @skill_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE @skill_menu_id IS NOT NULL;

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT '技能删除', 'ai-agent:skill-meta:delete', 3, 4, @skill_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE @skill_menu_id IS NOT NULL;
