-- ============================================================
-- 知识库菜单优化 SQL
-- 1. 清理重复菜单条目
-- 2. 优化菜单名称（去除冗余前缀，简化命名）
-- 3. 设置合理的排序
-- 生成日期: 2026-08-02
-- ============================================================

-- ----------------------------
-- 1. 软删除重复的菜单条目
-- ----------------------------
-- 6737 是 6755 的重复（知识库分类管理），6755 有子菜单，保留 6755
UPDATE system_menu SET deleted = b'1' WHERE id = 6737;

-- 6749 是 6743 的重复（知识库层级配置管理），6743 有子菜单，保留 6743
UPDATE system_menu SET deleted = b'1' WHERE id = 6749;

-- 6736 是空目录条目（无 path、无 component），删除
UPDATE system_menu SET deleted = b'1' WHERE id = 6736;

-- ----------------------------
-- 2. 优化一级菜单名称
-- ----------------------------
-- 父菜单保持不变
-- UPDATE system_menu SET name = '知识库' WHERE id = 6735;

-- 知识库管理
UPDATE system_menu SET name = '知识库管理', sort = 1 WHERE id = 6761;

-- 分类管理（原"知识库分类管理"）
UPDATE system_menu SET name = '分类管理', sort = 2 WHERE id = 6755;

-- 层级配置（原"知识库层级配置管理"）
UPDATE system_menu SET name = '层级配置', sort = 3 WHERE id = 6743;

-- 共享部门管理（原"知识库共享部门关联管理"）
UPDATE system_menu SET name = '共享部门管理', sort = 4 WHERE id = 6767;

-- 文档管理（原"知识库文件管理"）
UPDATE system_menu SET name = '文档管理', sort = 5 WHERE id = 6773;

-- 部门成员管理（保持不变，排序已正确）
UPDATE system_menu SET sort = 6 WHERE id = 6780;

-- 项目成员管理（保持不变，排序已正确）
UPDATE system_menu SET sort = 7 WHERE id = 6784;

-- ----------------------------
-- 3. 简化子菜单（权限按钮）名称
-- ----------------------------

-- 知识库管理 子菜单
UPDATE system_menu SET name = '查询' WHERE id = 6762;
UPDATE system_menu SET name = '创建' WHERE id = 6763;
UPDATE system_menu SET name = '更新' WHERE id = 6764;
UPDATE system_menu SET name = '删除' WHERE id = 6765;
UPDATE system_menu SET name = '导出' WHERE id = 6766;

-- 分类管理 子菜单
UPDATE system_menu SET name = '查询' WHERE id = 6756;
UPDATE system_menu SET name = '创建' WHERE id = 6757;
UPDATE system_menu SET name = '更新' WHERE id = 6758;
UPDATE system_menu SET name = '删除' WHERE id = 6759;
UPDATE system_menu SET name = '导出' WHERE id = 6760;

-- 层级配置 子菜单
UPDATE system_menu SET name = '查询' WHERE id = 6744;
UPDATE system_menu SET name = '创建' WHERE id = 6745;
UPDATE system_menu SET name = '更新' WHERE id = 6746;
UPDATE system_menu SET name = '删除' WHERE id = 6747;
UPDATE system_menu SET name = '导出' WHERE id = 6748;

-- 共享部门管理 子菜单
UPDATE system_menu SET name = '查询' WHERE id = 6768;
UPDATE system_menu SET name = '创建' WHERE id = 6769;
UPDATE system_menu SET name = '更新' WHERE id = 6770;
UPDATE system_menu SET name = '删除' WHERE id = 6771;
UPDATE system_menu SET name = '导出' WHERE id = 6772;

-- 文档管理 子菜单
UPDATE system_menu SET name = '查询' WHERE id = 6774;
UPDATE system_menu SET name = '创建' WHERE id = 6775;
UPDATE system_menu SET name = '更新' WHERE id = 6776;
UPDATE system_menu SET name = '删除' WHERE id = 6777;
UPDATE system_menu SET name = '导出' WHERE id = 6778;

-- 部门成员管理 子菜单
UPDATE system_menu SET name = '查询' WHERE id = 6781;
UPDATE system_menu SET name = '更新' WHERE id = 6782;
UPDATE system_menu SET name = '删除' WHERE id = 6783;

-- 项目成员管理 子菜单
UPDATE system_menu SET name = '查询' WHERE id = 6785;
UPDATE system_menu SET name = '更新' WHERE id = 6786;
UPDATE system_menu SET name = '删除' WHERE id = 6787;
