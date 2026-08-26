-- =====================================================
-- R (repeatable): 系统菜单 system_menu 种子数据
--
-- 来源（合并自 sql/mysql，均为幂等，升级自动重跑）:
--   * kb_frontend_menu.sql —— 前端 C 端导航菜单（顶级目录「知识库菜单管理」）
--   * kb_screen.sql        —— 知识库大屏（顶级菜单，第 3 部分菜单数据）
--   * kb_tag.sql           —— 知识库 > 标签管理 菜单及按钮权限
--
-- 约定:
--   * system_menu 同时被 yudao 管理后台加载为动态路由，Vue Router 要求
--     path 以 / 开头，故 C 端导航 path 全部带 / 前缀。
--   * 每个节点按 path(+parent_id) 幂等，已存在则跳过；可安全重复执行。
-- =====================================================

-- =====================================================
-- 〇、C 端导航：迁移历史无 / 前缀的 path（幂等）
-- =====================================================
UPDATE `system_menu` SET `path` = CONCAT('/', `path`)
WHERE `deleted` = b'0'
  AND `path` IN (
    'kb-menu','home','user-center','basic-information','knowledge-base-approval',
    'add-agent','agent-management','add-api-key','knowledge-base','knowledge-hub',
    'my-public','my-follows','notes','personal-meetings','personal-notes','shares',
    'agent-hub','ai-writing','ai-check','ai-bid-document','ai-ppt','ai-pdf','one-paper'
  );

-- =====================================================
-- 一、C 端导航：顶级目录「知识库菜单管理」
-- =====================================================
INSERT INTO `system_menu` (`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT '知识库菜单管理','',1,26,0,'/kb-menu','',NULL,NULL,0,1,1,0,'1',NOW(),'1',NOW(),b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `path`='/kb-menu' AND `parent_id`=0 AND deleted=b'0');
SELECT id INTO @m_root FROM `system_menu` WHERE `path`='/kb-menu' AND `parent_id`=0 AND deleted=b'0' LIMIT 1;

-- 迁移：修复左侧导航图标（原失效 URL），统一为 iconfont 类名（幂等）
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

-- 首页
INSERT INTO `system_menu` (`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT '首页','',2,0,@m_root,'/home','icon-diqiu',NULL,NULL,0,1,1,0,'1',NOW(),'1',NOW(),b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `path`='/home' AND `parent_id`=@m_root AND deleted=b'0');

-- 个人中心（目录）
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

-- 知识库（目录）
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

-- 笔记（目录）
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

-- 智能体广场（目录）
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

-- 一张图
INSERT INTO `system_menu` (`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT '一张图','',2,5,@m_root,'/one-paper','icon-one-paper',NULL,NULL,0,1,1,0,'1',NOW(),'1',NOW(),b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `path`='/one-paper' AND `parent_id`=@m_root AND deleted=b'0');

-- =====================================================
-- 二、知识库大屏（顶级菜单，与「首页」「知识库总览」平级）
--    按钮权限复用已有 kb:library:* / kb:document:*，无需新增。
-- =====================================================
-- 迁移：若此前已作为「知识库管理」的子菜单插入，则移到顶级（幂等）
UPDATE `system_menu`
SET `parent_id` = 0, `sort` = 27
WHERE `name` = '知识库大屏' AND `component` = 'kb/screen/index' AND `deleted` = b'0';

-- 迁移：顶级菜单 path 必须以 / 开头（管理后台 vue-router 要求）
UPDATE `system_menu`
SET `path` = '/screen'
WHERE `name` = '知识库大屏' AND `component` = 'kb/screen/index' AND `path` = 'screen' AND `deleted` = b'0';

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT '知识库大屏', '', 2, 27, 0, '/screen', 'ep:data-analysis', 'kb/screen/index', 'KbScreen', 0, 1, 1, 0, NOW(), NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM `system_menu` WHERE `name` = '知识库大屏' AND `component` = 'kb/screen/index' AND `deleted` = b'0'
);

-- =====================================================
-- 三、知识库管理 > 标签管理（菜单 + 按钮权限）
-- =====================================================
SELECT id INTO @kb_parent_id FROM `system_menu` WHERE name = '知识库管理' AND deleted = b'0' AND type = 1 LIMIT 1;

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
VALUES ('标签管理', '', 2, 10, @kb_parent_id, 'tag', 'ep:price-tag', 'kb/tag/index', 'KbTag', 0, 1, 1, 0, NOW(), NOW(), b'0');

SET @tag_id = LAST_INSERT_ID();

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
VALUES
('标签查询', 'kb:tag:query', 3, 1, @tag_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'),
('标签创建', 'kb:tag:create', 3, 2, @tag_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'),
('标签更新', 'kb:tag:update', 3, 3, @tag_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'),
('标签删除', 'kb:tag:delete', 3, 4, @tag_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0');

-- =====================================================
-- 四、C 端导航：从管理后台隐藏（visible=0），C 端仍可用
--
-- C 端导航（首页/个人中心/知识库/笔记/智能体广场/一张图）只服务 C 端用户前端，
-- 其 component 为空，若被管理后台（yudao-admin）当作动态路由加载会打不开。
-- 因此把整棵「知识库菜单管理」子树 visible 置 0：
--   * 管理后台: dynamic route 生成 meta.hidden = !visible = true，侧边栏不再显示、不再生成可点击路由；
--   * C 端: FrontendMenuController.frontTree 使用 m -> true 过滤，不受 visible 影响，仍正常返回。
-- 放在文件末尾执行，确保覆盖本文件前面所有 INSERT 的重建可见性，幂等可重跑。
-- =====================================================
WITH RECURSIVE kb_menu_tree AS (
    SELECT `id` FROM `system_menu`
    WHERE `path` = '/kb-menu' AND `parent_id` = 0 AND `deleted` = b'0'
    UNION ALL
    SELECT sm.`id` FROM `system_menu` sm
    INNER JOIN kb_menu_tree kt ON sm.`parent_id` = kt.`id`
    WHERE sm.`deleted` = b'0'
)
UPDATE `system_menu` SET `visible` = 0
WHERE `id` IN (SELECT `id` FROM kb_menu_tree);