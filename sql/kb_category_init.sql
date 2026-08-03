-- ============================================================
-- 知识库分类初始化 SQL
-- 按照五大知识库类别 + 子分类初始化 kb_category 表
-- 生成日期: 2026-08-02
-- ============================================================

-- 先清空已有数据（如有）
UPDATE kb_category SET deleted = b'1' WHERE deleted = b'0';

-- ============================================================
-- 一、顶级分类（5个，对应层级配置）
-- ============================================================

-- 1. 个人知识库 (level_id=1, visibility_rule=1 按所有者, owner_dim=1 用户)
INSERT INTO kb_category (id, name, parent_id, kb_level_id, sort, status, creator, create_time, updater, update_time, deleted, tenant_id)
VALUES (1001, '个人知识库', 0, 1, 1, 0, 'admin', NOW(), 'admin', NOW(), b'0', 0);

-- 2. 院级知识库 (level_id=2, visibility_rule=2 按归属部门, owner_dim=2 部门)
INSERT INTO kb_category (id, name, parent_id, kb_level_id, sort, status, creator, create_time, updater, update_time, deleted, tenant_id)
VALUES (1002, '院级知识库', 0, 2, 2, 0, 'admin', NOW(), 'admin', NOW(), b'0', 0);

-- 3. 公司知识库 (level_id=3, visibility_rule=3 全员, owner_dim=0 无)
INSERT INTO kb_category (id, name, parent_id, kb_level_id, sort, status, creator, create_time, updater, update_time, deleted, tenant_id)
VALUES (1003, '公司知识库', 0, 3, 3, 0, 'admin', NOW(), 'admin', NOW(), b'0', 0);

-- 4. 咨询评估库 (level_id=5, visibility_rule=2 按归属部门, owner_dim=2 部门)
INSERT INTO kb_category (id, name, parent_id, kb_level_id, sort, status, creator, create_time, updater, update_time, deleted, tenant_id)
VALUES (1004, '咨询评估库', 0, 5, 4, 0, 'admin', NOW(), 'admin', NOW(), b'0', 0);

-- 5. 知识库广场 (level_id=4, visibility_rule=6 广场, owner_dim=0 无)
INSERT INTO kb_category (id, name, parent_id, kb_level_id, sort, status, creator, create_time, updater, update_time, deleted, tenant_id)
VALUES (1005, '知识库广场', 0, 4, 5, 0, 'admin', NOW(), 'admin', NOW(), b'0', 0);

-- ============================================================
-- 二、个人知识库子分类（parent_id=1001, level_id=1）
-- ============================================================
INSERT INTO kb_category (id, name, parent_id, kb_level_id, sort, status, creator, create_time, updater, update_time, deleted, tenant_id) VALUES
(1010, '项目', 1001, 1, 1, 0, 'admin', NOW(), 'admin', NOW(), b'0', 0),
(1011, '会议', 1001, 1, 2, 0, 'admin', NOW(), 'admin', NOW(), b'0', 0),
(1012, '默认', 1001, 1, 3, 0, 'admin', NOW(), 'admin', NOW(), b'0', 0);

-- ============================================================
-- 三、院级知识库子分类（parent_id=1002, level_id=2）
-- ============================================================
INSERT INTO kb_category (id, name, parent_id, kb_level_id, sort, status, creator, create_time, updater, update_time, deleted, tenant_id) VALUES
(1020, '项目成果库', 1002, 2, 1, 0, 'admin', NOW(), 'admin', NOW(), b'0', 0),
(1021, '会议信息库', 1002, 2, 2, 0, 'admin', NOW(), 'admin', NOW(), b'0', 0),
(1022, '四体系专题库', 1002, 2, 3, 0, 'admin', NOW(), 'admin', NOW(), b'0', 0),
(1023, '人力资源专题库', 1002, 2, 4, 0, 'admin', NOW(), 'admin', NOW(), b'0', 0),
(1024, '综合管理专题库', 1002, 2, 5, 0, 'admin', NOW(), 'admin', NOW(), b'0', 0),
(1025, '安全生产专题库', 1002, 2, 6, 0, 'admin', NOW(), 'admin', NOW(), b'0', 0),
(1026, '党务专题库', 1002, 2, 7, 0, 'admin', NOW(), 'admin', NOW(), b'0', 0);

-- ============================================================
-- 四、公司知识库子分类（parent_id=1003, level_id=3）
-- ============================================================
INSERT INTO kb_category (id, name, parent_id, kb_level_id, sort, status, creator, create_time, updater, update_time, deleted, tenant_id) VALUES
(1030, '公司项目库', 1003, 3, 1, 0, 'admin', NOW(), 'admin', NOW(), b'0', 0),
(1031, '项目成果库', 1003, 3, 2, 0, 'admin', NOW(), 'admin', NOW(), b'0', 0),
(1032, '标准规范库', 1003, 3, 3, 0, 'admin', NOW(), 'admin', NOW(), b'0', 0),
(1033, '公司文档库', 1003, 3, 4, 0, 'admin', NOW(), 'admin', NOW(), b'0', 0),
(1034, '综合专题库', 1003, 3, 5, 0, 'admin', NOW(), 'admin', NOW(), b'0', 0),
(1035, '四体系专题库', 1003, 3, 6, 0, 'admin', NOW(), 'admin', NOW(), b'0', 0),
(1036, '会议信息库', 1003, 3, 7, 0, 'admin', NOW(), 'admin', NOW(), b'0', 0),
(1037, '安全生产库', 1003, 3, 8, 0, 'admin', NOW(), 'admin', NOW(), b'0', 0),
(1038, '资金政策库', 1003, 3, 9, 0, 'admin', NOW(), 'admin', NOW(), b'0', 0),
(1039, '招标信息库', 1003, 3, 10, 0, 'admin', NOW(), 'admin', NOW(), b'0', 0),
(1040, '模型软件库', 1003, 3, 11, 0, 'admin', NOW(), 'admin', NOW(), b'0', 0),
(1041, '科技动态库', 1003, 3, 12, 0, 'admin', NOW(), 'admin', NOW(), b'0', 0),
(1042, '科技成果库', 1003, 3, 13, 0, 'admin', NOW(), 'admin', NOW(), b'0', 0),
(1043, '水利通识库', 1003, 3, 14, 0, 'admin', NOW(), 'admin', NOW(), b'0', 0);

-- ============================================================
-- 五、咨询评估库子分类（parent_id=1004, level_id=5）
-- ============================================================
INSERT INTO kb_category (id, name, parent_id, kb_level_id, sort, status, creator, create_time, updater, update_time, deleted, tenant_id) VALUES
(1050, '会议信息库', 1004, 5, 1, 0, 'admin', NOW(), 'admin', NOW(), b'0', 0),
(1051, '标准规范库', 1004, 5, 2, 0, 'admin', NOW(), 'admin', NOW(), b'0', 0),
(1052, '评估专家库', 1004, 5, 3, 0, 'admin', NOW(), 'admin', NOW(), b'0', 0),
(1053, '管理办法库', 1004, 5, 4, 0, 'admin', NOW(), 'admin', NOW(), b'0', 0),
(1054, '评估项目库', 1004, 5, 5, 0, 'admin', NOW(), 'admin', NOW(), b'0', 0),
(1055, '资金政策库', 1004, 5, 6, 0, 'admin', NOW(), 'admin', NOW(), b'0', 0),
(1056, '发文', 1004, 5, 7, 0, 'admin', NOW(), 'admin', NOW(), b'0', 0),
(1057, '收文', 1004, 5, 8, 0, 'admin', NOW(), 'admin', NOW(), b'0', 0);

-- ============================================================
-- 六、验证查询
-- ============================================================
-- SELECT id, name, parent_id, kb_level_id, sort FROM kb_category WHERE deleted=0 ORDER BY parent_id, sort;
