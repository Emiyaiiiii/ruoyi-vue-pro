-- ============================================================
-- 前端 C 端导航菜单 映射到 system_menu（菜单管理）
-- ============================================================
-- 结构：新增顶级目录「知识库菜单管理」，其下挂载前端导航：
--   知识库菜单管理
--     ├─ 首页
--     ├─ 个人中心（含子菜单）
--     ├─ 知识库（子菜单：知识库广场 / 我创建的 / 我加入的；四类知识库分类由后端兼容层硬编码返回）
--     ├─ 笔记（含子菜单）
--     ├─ 智能体广场（含子菜单）
--     └─ 一张图
--
-- 数据来源（Python 端 + 前端 + Java 端）：
--   1. backend/dvadmin/system/fixtures/init_system_menu.json —— 左侧导航（SystemMenu）
--   2. 前端 xiaoyu-ai-front/src/router/routeRaw.ts           —— 路由（value 对应关系）
--   3. Java 端 kb 模块控制器                                  —— 知识库列表后端
--
-- 重要：system_menu 同时被 yudao 管理后台（yudao-ui-admin-vue3）加载为动态路由，
--   Vue Router 要求 path 必须以 / 开头，因此这里的 path 全部带 / 前缀；
--   Java 兼容层 FrontendMenuController 会去掉开头的 / 得到前端 C 端的 value（slug）。
--
-- 字段映射：
--   system_menu.name            <- 前端 label（菜单名）
--   system_menu.path            <- 前端 value（唯一键，前端据此拼接路由）；带 / 前缀
--   system_menu.parent_id       <- 前端 parent_value 层级关系
--   system_menu.type            <- 目录=1（有子级），菜单=2（叶子）
--   system_menu.icon            <- 前端 icon（URL 或空；URL 指向原 Python 环境，可自行替换）
--   system_menu.sort            <- 前端 sort
--   system_menu.visible         <- 前端 visible（0=隐藏，如「新增智能体」）
--   system_menu.permission      <- ''（纯导航，无权限标识）
--   system_menu.component       <- NULL（C 端导航，非 yudao 管理页）
--   system_menu.component_name  <- NULL
--
-- 说明：可重复执行（每个节点按 path+parent_id 幂等，已存在则跳过）。
-- ============================================================

SET NAMES utf8mb4;

-- ============================================================
-- 0. 迁移：把之前插入的无 / 前缀的 path 统一补上 /（幂等）
--    （否则 yudao 管理后台加载动态路由时会报 "Route paths should start with a /"）
-- ============================================================

UPDATE `system_menu` SET `path` = CONCAT('/', `path`)
WHERE `deleted` = b'0'
  AND `path` IN (
    'kb-menu','home','user-center','basic-information','knowledge-base-approval',
    'add-agent','agent-management','add-api-key','knowledge-base','knowledge-hub',
    'my-public','my-follows','notes','personal-meetings','personal-notes','shares',
    'agent-hub','ai-writing','ai-check','ai-bid-document','ai-ppt','ai-pdf','one-paper'
  );

-- ============================================================
-- 1. 顶级目录「知识库菜单管理」
-- ============================================================

INSERT INTO `system_menu` (`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT '知识库菜单管理','',1,0,0,'/kb-menu','',NULL,NULL,0,1,1,0,'1',NOW(),'1',NOW(),b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `path`='/kb-menu' AND `parent_id`=0 AND deleted=b'0');
SELECT id INTO @m_root FROM `system_menu` WHERE `path`='/kb-menu' AND `parent_id`=0 AND deleted=b'0' LIMIT 1;

-- ============================================================
-- 1.5 迁移：修复左侧导航图标（幂等）
--   原为失效的 http URL（旧 Python 头像服务器）或空字符串，导致前端左侧图标不可见。
--   统一改为前端 iconfont 类名（见 xiaoyu-ai-front/src/views/knowledge/components/iconConfig.ts）。
-- ============================================================

UPDATE `system_menu`
SET `icon` = CASE `path`
  WHEN '/home' THEN 'icon-diqiu'
  WHEN '/user-center' THEN 'icon-user-center'
  WHEN '/knowledge-base' THEN 'icon-knowledge-base'
  WHEN '/notes' THEN 'icon-notes'
  WHEN '/agent-hub' THEN 'icon-agent-hub'
  WHEN '/one-paper' THEN 'icon-one-paper'
  ELSE `icon`
END
WHERE `parent_id` = @m_root
  AND deleted = b'0'
  AND `path` IN ('/home','/user-center','/knowledge-base','/notes','/agent-hub','/one-paper');

-- ============================================================
-- 2. 首页（叶子菜单）
-- ============================================================

INSERT INTO `system_menu` (`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT '首页','',2,0,@m_root,'/home','icon-diqiu',NULL,NULL,0,1,1,0,'1',NOW(),'1',NOW(),b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `path`='/home' AND `parent_id`=@m_root AND deleted=b'0');

-- ============================================================
-- 3. 个人中心（目录 type=1）
-- ============================================================

INSERT INTO `system_menu` (`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT '个人中心','',1,1,@m_root,'/user-center','icon-user-center',NULL,NULL,0,1,1,0,'1',NOW(),'1',NOW(),b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `path`='/user-center' AND `parent_id`=@m_root AND deleted=b'0');
SELECT id INTO @m_user_center FROM `system_menu` WHERE `path`='/user-center' AND `parent_id`=@m_root AND deleted=b'0' LIMIT 1;

INSERT INTO `system_menu` (`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT '基本信息','',2,0,@m_user_center,'/basic-information','',NULL,NULL,0,1,1,0,'1',NOW(),'1',NOW(),b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `path`='/basic-information' AND `parent_id`=@m_user_center AND deleted=b'0');

INSERT INTO `system_menu` (`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT '知识库审批','',2,1,@m_user_center,'/knowledge-base-approval','',NULL,NULL,0,1,1,0,'1',NOW(),'1',NOW(),b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `path`='/knowledge-base-approval' AND `parent_id`=@m_user_center AND deleted=b'0');

INSERT INTO `system_menu` (`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT '新增智能体','',2,2,@m_user_center,'/add-agent','',NULL,NULL,0,0,1,0,'1',NOW(),'1',NOW(),b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `path`='/add-agent' AND `parent_id`=@m_user_center AND deleted=b'0');

INSERT INTO `system_menu` (`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT '智能体管理','',2,3,@m_user_center,'/agent-management','',NULL,NULL,0,1,1,0,'1',NOW(),'1',NOW(),b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `path`='/agent-management' AND `parent_id`=@m_user_center AND deleted=b'0');

INSERT INTO `system_menu` (`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT 'API Key管理','',2,4,@m_user_center,'/add-api-key','',NULL,NULL,0,1,1,0,'1',NOW(),'1',NOW(),b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `path`='/add-api-key' AND `parent_id`=@m_user_center AND deleted=b'0');

-- ============================================================
-- 4. 知识库（目录 type=1，子菜单：知识库广场 / 我创建的 / 我加入的）
--    四类知识库分类（个人/院级/公司/咨询评估）由后端 FrontendMenuController 硬编码返回
-- ============================================================

INSERT INTO `system_menu` (`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT '知识库','',1,2,@m_root,'/knowledge-base','icon-knowledge-base',NULL,NULL,0,1,1,0,'1',NOW(),'1',NOW(),b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `path`='/knowledge-base' AND `parent_id`=@m_root AND deleted=b'0');
SELECT id INTO @m_knowledge_base FROM `system_menu` WHERE `path`='/knowledge-base' AND `parent_id`=@m_root AND deleted=b'0' LIMIT 1;

INSERT INTO `system_menu` (`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT '知识库广场','',2,0,@m_knowledge_base,'/knowledge-hub','',NULL,NULL,0,1,1,0,'1',NOW(),'1',NOW(),b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `path`='/knowledge-hub' AND `parent_id`=@m_knowledge_base AND deleted=b'0');

INSERT INTO `system_menu` (`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT '我创建的','',2,1,@m_knowledge_base,'/my-public','',NULL,NULL,0,1,1,0,'1',NOW(),'1',NOW(),b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `path`='/my-public' AND `parent_id`=@m_knowledge_base AND deleted=b'0');

INSERT INTO `system_menu` (`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT '我加入的','',2,2,@m_knowledge_base,'/my-follows','',NULL,NULL,0,1,1,0,'1',NOW(),'1',NOW(),b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `path`='/my-follows' AND `parent_id`=@m_knowledge_base AND deleted=b'0');

-- ============================================================
-- 5. 笔记（目录 type=1）
-- ============================================================

INSERT INTO `system_menu` (`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT '笔记','',1,3,@m_root,'/notes','icon-notes',NULL,NULL,0,1,1,0,'1',NOW(),'1',NOW(),b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `path`='/notes' AND `parent_id`=@m_root AND deleted=b'0');
SELECT id INTO @m_notes FROM `system_menu` WHERE `path`='/notes' AND `parent_id`=@m_root AND deleted=b'0' LIMIT 1;

INSERT INTO `system_menu` (`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT '个人会议','',2,0,@m_notes,'/personal-meetings','',NULL,NULL,0,1,1,0,'1',NOW(),'1',NOW(),b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `path`='/personal-meetings' AND `parent_id`=@m_notes AND deleted=b'0');

INSERT INTO `system_menu` (`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT '个人笔记','',2,1,@m_notes,'/personal-notes','',NULL,NULL,0,1,1,0,'1',NOW(),'1',NOW(),b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `path`='/personal-notes' AND `parent_id`=@m_notes AND deleted=b'0');

INSERT INTO `system_menu` (`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT '他人分享','',2,1000,@m_notes,'/shares','',NULL,NULL,0,1,1,0,'1',NOW(),'1',NOW(),b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `path`='/shares' AND `parent_id`=@m_notes AND deleted=b'0');

-- ============================================================
-- 6. 智能体广场（目录 type=1）
-- ============================================================

INSERT INTO `system_menu` (`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT '智能体广场','',1,4,@m_root,'/agent-hub','icon-agent-hub',NULL,NULL,0,1,1,0,'1',NOW(),'1',NOW(),b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `path`='/agent-hub' AND `parent_id`=@m_root AND deleted=b'0');
SELECT id INTO @m_agent_hub FROM `system_menu` WHERE `path`='/agent-hub' AND `parent_id`=@m_root AND deleted=b'0' LIMIT 1;

INSERT INTO `system_menu` (`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT '智能写作','',2,0,@m_agent_hub,'/ai-writing','',NULL,NULL,0,1,1,0,'1',NOW(),'1',NOW(),b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `path`='/ai-writing' AND `parent_id`=@m_agent_hub AND deleted=b'0');

INSERT INTO `system_menu` (`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT '智能校审','',2,1,@m_agent_hub,'/ai-check','',NULL,NULL,0,1,1,0,'1',NOW(),'1',NOW(),b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `path`='/ai-check' AND `parent_id`=@m_agent_hub AND deleted=b'0');

INSERT INTO `system_menu` (`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT '智能标书','',2,2,@m_agent_hub,'/ai-bid-document','',NULL,NULL,0,1,1,0,'1',NOW(),'1',NOW(),b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `path`='/ai-bid-document' AND `parent_id`=@m_agent_hub AND deleted=b'0');

INSERT INTO `system_menu` (`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT 'ppt助手','',2,3,@m_agent_hub,'/ai-ppt','',NULL,NULL,0,1,1,0,'1',NOW(),'1',NOW(),b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `path`='/ai-ppt' AND `parent_id`=@m_agent_hub AND deleted=b'0');

INSERT INTO `system_menu` (`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT 'pdf助手','',2,4,@m_agent_hub,'/ai-pdf','',NULL,NULL,0,1,1,0,'1',NOW(),'1',NOW(),b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `path`='/ai-pdf' AND `parent_id`=@m_agent_hub AND deleted=b'0');

-- ============================================================
-- 7. 一张图（叶子菜单）
-- ============================================================

INSERT INTO `system_menu` (`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT '一张图','',2,5,@m_root,'/one-paper','icon-one-paper',NULL,NULL,0,1,1,0,'1',NOW(),'1',NOW(),b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `path`='/one-paper' AND `parent_id`=@m_root AND deleted=b'0');
