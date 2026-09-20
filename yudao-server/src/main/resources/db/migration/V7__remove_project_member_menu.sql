-- =====================================================================
-- V7__remove_project_member_menu.sql  (Flyway Versioned 迁移)
-- 清理已废弃的「项目成员管理」菜单及其按钮权限。
-- 背景：项目成果库打开权限已改为实时读「分类指定成员字段」的 ext 值，
-- kb_project_member 权限表已废弃（见 V6），后台项目成员管理页/接口/菜单一并移除。
-- R__menu_seed 只在菜单不存在时插入，无法清除存量数据库中的旧菜单行，故需此迁移显式删除。
-- =====================================================================

-- 先清除角色对废弃菜单的关联，避免孤儿引用
DELETE `sm_role`
FROM `system_role_menu` `sm_role`
JOIN `system_menu` `sm`
  ON `sm_role`.`menu_id` = `sm`.`id`
WHERE `sm`.`component` = 'kb/projectmember/index'
   OR `sm`.`permission` LIKE 'kb:project-member:%';

-- 删除废弃菜单本身（页面菜单 + 查询/更新/删除按钮权限）
DELETE FROM `system_menu`
WHERE `component` = 'kb/projectmember/index'
   OR `permission` LIKE 'kb:project-member:%';