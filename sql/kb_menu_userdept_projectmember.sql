-- =============================================
-- 知识库部门成员管理 & 项目成员管理 菜单 SQL
-- 父菜单: 6735 (知识库目录)
-- 生成日期: 2026-08-02
-- =============================================

-- ----------------------------
-- 1. 知识库部门成员管理 (kb_user_dept)
-- ----------------------------
-- 菜单页面
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (6780, '部门成员管理', '', 2, 6, 6735, 'user-dept', 'ep:user', 'kb/userdept/index', 'UserDept', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0');

-- 查询权限
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (6781, '部门成员查询', 'kb:user-dept:query', 3, 1, 6780, '', '#', '', '', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0');

-- 更新权限（添加成员/管理员、设置角色）
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (6782, '部门成员更新', 'kb:user-dept:update', 3, 2, 6780, '', '#', '', '', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0');

-- 删除权限（移除关联）
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (6783, '部门成员删除', 'kb:user-dept:delete', 3, 3, 6780, '', '#', '', '', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0');

-- ----------------------------
-- 2. 知识库项目成员管理 (kb_project_member)
-- ----------------------------
-- 菜单页面
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (6784, '项目成员管理', '', 2, 7, 6735, 'project-member', 'ep:team', 'kb/projectmember/index', 'ProjectMember', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0');

-- 查询权限
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (6785, '项目成员查询', 'kb:project-member:query', 3, 1, 6784, '', '#', '', '', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0');

-- 更新权限（添加成员）
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (6786, '项目成员更新', 'kb:project-member:update', 3, 2, 6784, '', '#', '', '', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0');

-- 删除权限（移除成员）
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (6787, '项目成员删除', 'kb:project-member:delete', 3, 3, 6784, '', '#', '', '', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0');
