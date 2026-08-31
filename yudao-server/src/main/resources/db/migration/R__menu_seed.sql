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
-- 三、管理端「智能体」子树（/ai-agent）：顶级目录 + 6 子菜单 + 按钮权限
--
-- 背景：这些管理后台菜单在 docker init 时由 sql/mysql 脚本导入，未进入 Flyway 迁移，
-- 仅靠 init 脚本重建会丢。故在此以幂等方式补齐，与 DB 现状保持一致。
-- 全部使用 NOT EXISTS 守卫（子菜单按 name+parent、按钮按 permission），R__ 可安全重跑。
-- 管理后台需展示，visible=1。
-- =====================================================
INSERT INTO `system_menu` (`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`create_time`,`update_time`,`deleted`)
SELECT '智能体管理','',1,21,0,'/ai-agent','ep:chat-dot-round','','AiAgent',0,1,1,1,NOW(),NOW(),b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `path`='/ai-agent' AND `parent_id`=0 AND `deleted`=b'0');
SELECT id INTO @m_ai_agent FROM `system_menu` WHERE `path`='/ai-agent' AND `parent_id`=0 AND `deleted`=b'0' LIMIT 1;

-- /ai-agent 下子菜单（parent_id 由 @m_ai_agent 解析）
INSERT INTO `system_menu` (`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`create_time`,`update_time`,`deleted`)
SELECT `v`.`name`,'',2,`v`.`sort`,@m_ai_agent,`v`.`path`,`v`.`icon`,`v`.`component`,`v`.`component_name`,0,1,1,0,NOW(),NOW(),b'0'
FROM (
    SELECT '我的智能体' `name`,1 `sort`,'agent' `path`,'ep:avatar' `icon`,'ai/agent/index' `component`,'AiAgentIndex' `component_name`
    UNION ALL SELECT '技能商店',2,'skill-store','ep:magic-stick','ai/skillmeta/index','AiSkillMetaIndex'
    UNION ALL SELECT 'MCP商店',3,'mcp-store','ep:burger','ai/mcpmeta/index','AiMcpMetaIndex'
    UNION ALL SELECT '问答会话',4,'chat-session','ep:chat-line-round','ai/chatsession/index','AiChatSessionIndex'
    UNION ALL SELECT '模型管理',5,'model-provider','ep:cpu','ai/model/provider/index','AiModelProvider'
    UNION ALL SELECT 'Token用量',6,'token-usage','ep:data-analysis','ai/tokenUsage/index','AiTokenUsageIndex'
) `v`
WHERE NOT EXISTS (
    SELECT 1 FROM `system_menu` `sm` WHERE `sm`.`name`=`v`.`name` AND `sm`.`parent_id`=@m_ai_agent AND `sm`.`deleted`=b'0'
);

-- /ai-agent 下按钮权限（parent_id 按子菜单 path 解析）
INSERT INTO `system_menu` (`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`status`,`visible`,`keep_alive`,`always_show`,`create_time`,`update_time`,`deleted`)
SELECT `v`.`name`,`v`.`permission`,3,`v`.`sort`,`m`.`id`,'','','',0,1,1,0,NOW(),NOW(),b'0'
FROM (
    SELECT 'agent' `child_path`,'智能体查询' `name`,'ai-agent:agent:query' `permission`,1 `sort`
    UNION ALL SELECT 'agent','智能体创建','ai-agent:agent:create',2
    UNION ALL SELECT 'agent','智能体更新','ai-agent:agent:update',3
    UNION ALL SELECT 'agent','智能体删除','ai-agent:agent:delete',4
    UNION ALL SELECT 'skill-store','技能查询','ai-agent:skill-meta:query',1
    UNION ALL SELECT 'skill-store','技能创建','ai-agent:skill-meta:create',2
    UNION ALL SELECT 'skill-store','技能更新','ai-agent:skill-meta:update',3
    UNION ALL SELECT 'skill-store','技能删除','ai-agent:skill-meta:delete',4
    UNION ALL SELECT 'mcp-store','MCP查询','ai-agent:mcp-meta:query',1
    UNION ALL SELECT 'mcp-store','MCP创建','ai-agent:mcp-meta:create',2
    UNION ALL SELECT 'mcp-store','MCP更新','ai-agent:mcp-meta:update',3
    UNION ALL SELECT 'mcp-store','MCP删除','ai-agent:mcp-meta:delete',4
    UNION ALL SELECT 'chat-session','会话查询','ai-agent:chat-session:query',1
    UNION ALL SELECT 'chat-session','会话创建','ai-agent:chat-session:create',2
    UNION ALL SELECT 'chat-session','会话更新','ai-agent:chat-session:update',3
    UNION ALL SELECT 'chat-session','会话删除','ai-agent:chat-session:delete',4
    UNION ALL SELECT 'model-provider','模型查询','ai-agent:model:query',1
    UNION ALL SELECT 'model-provider','模型创建','ai-agent:model:create',2
    UNION ALL SELECT 'model-provider','模型更新','ai-agent:model:update',3
    UNION ALL SELECT 'model-provider','模型删除','ai-agent:model:delete',4
    UNION ALL SELECT 'token-usage','Token用量查询','ai-agent:token-usage:query',1
) `v`
JOIN `system_menu` `m` ON `m`.`path`=`v`.`child_path` AND `m`.`parent_id`=@m_ai_agent AND `m`.`deleted`=b'0'
WHERE NOT EXISTS (
    SELECT 1 FROM `system_menu` `sm` WHERE `sm`.`permission`=`v`.`permission` AND `sm`.`deleted`=b'0'
);

-- =====================================================
-- 四、管理端「知识库」子树（/kb）：顶级目录 + 12 子菜单（含标签管理）+ 按钮权限
--    实现方式与「三、智能体子树」一致，全部 NOT EXISTS 守卫幂等。
-- =====================================================
INSERT INTO `system_menu` (`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`create_time`,`update_time`,`deleted`)
SELECT '知识库管理','',1,25,0,'/kb','carbon:data-set-encryption','','',0,1,1,1,NOW(),NOW(),b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `path`='/kb' AND `parent_id`=0 AND `deleted`=b'0');
SELECT id INTO @m_kb FROM `system_menu` WHERE `path`='/kb' AND `parent_id`=0 AND `deleted`=b'0' LIMIT 1;

-- /kb 下子菜单（parent_id 由 @m_kb 解析，含原先单独维护且非幂等的「标签管理」）
INSERT INTO `system_menu` (`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`create_time`,`update_time`,`deleted`)
SELECT `v`.`name`,'',2,`v`.`sort`,@m_kb,`v`.`path`,`v`.`icon`,`v`.`component`,`v`.`component_name`,0,1,1,0,NOW(),NOW(),b'0'
FROM (
    SELECT '知识库总览' `name`,-1 `sort`,'overview' `path`,'' `icon`,'kb/overview/index' `component`,'KbOverview' `component_name`
    UNION ALL SELECT '层级配置',0,'level-config','','kb/levelconfig/index','LevelConfig'
    UNION ALL SELECT '分类管理',1,'category','','kb/category/index','Category'
    UNION ALL SELECT '共享部门管理',2,'share-dept','','kb/sharedept/index','ShareDept'
    UNION ALL SELECT '知识库管理',3,'library','','kb/library/index','Library'
    UNION ALL SELECT '文档管理',5,'document','','kb/document/index','Document'
    UNION ALL SELECT '切片方法',6,'chunk-method','','kb/chunkmethod/index','KbChunkMethod'
    UNION ALL SELECT '部门成员管理',6,'user-dept','','kb/userdept/index','UserDept'
    UNION ALL SELECT 'RAG配置',7,'rag-config','','kb/ragconfig/index','KbRagConfig'
    UNION ALL SELECT '项目成员管理',7,'project-member','','kb/projectmember/index','ProjectMember'
    UNION ALL SELECT '模型配置',8,'model-config','','kb/modelconfig/index','KbModelConfig'
    UNION ALL SELECT '新闻管理',8,'news','','kb/news/index','KbNews'
    UNION ALL SELECT '标签管理',10,'tag','ep:price-tag','kb/tag/index','KbTag'
) `v`
WHERE NOT EXISTS (
    SELECT 1 FROM `system_menu` `sm` WHERE `sm`.`name`=`v`.`name` AND `sm`.`parent_id`=@m_kb AND `sm`.`deleted`=b'0'
);

-- /kb 下按钮权限（parent_id 按子菜单 path 解析）
INSERT INTO `system_menu` (`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`status`,`visible`,`keep_alive`,`always_show`,`create_time`,`update_time`,`deleted`)
SELECT `v`.`name`,`v`.`permission`,3,`v`.`sort`,`m`.`id`,'','','',0,1,1,0,NOW(),NOW(),b'0'
FROM (
    SELECT 'level-config' `child_path`,'查询' `name`,'kb:level-config:query' `permission`,1 `sort`
    UNION ALL SELECT 'level-config','创建','kb:level-config:create',2
    UNION ALL SELECT 'level-config','更新','kb:level-config:update',3
    UNION ALL SELECT 'level-config','删除','kb:level-config:delete',4
    UNION ALL SELECT 'level-config','导出','kb:level-config:export',5
    UNION ALL SELECT 'category','查询','kb:category:query',1
    UNION ALL SELECT 'category','创建','kb:category:create',2
    UNION ALL SELECT 'category','更新','kb:category:update',3
    UNION ALL SELECT 'category','删除','kb:category:delete',4
    UNION ALL SELECT 'category','导出','kb:category:export',5
    UNION ALL SELECT 'share-dept','查询','kb:share-dept:query',1
    UNION ALL SELECT 'share-dept','创建','kb:share-dept:create',2
    UNION ALL SELECT 'share-dept','更新','kb:share-dept:update',3
    UNION ALL SELECT 'share-dept','删除','kb:share-dept:delete',4
    UNION ALL SELECT 'share-dept','导出','kb:share-dept:export',5
    UNION ALL SELECT 'library','查询','kb:library:query',1
    UNION ALL SELECT 'library','创建','kb:library:create',2
    UNION ALL SELECT 'library','更新','kb:library:update',3
    UNION ALL SELECT 'library','删除','kb:library:delete',4
    UNION ALL SELECT 'library','导出','kb:library:export',5
    UNION ALL SELECT 'document','查询','kb:document:query',1
    UNION ALL SELECT 'document','创建','kb:document:create',2
    UNION ALL SELECT 'document','更新','kb:document:update',3
    UNION ALL SELECT 'document','删除','kb:document:delete',4
    UNION ALL SELECT 'document','导出','kb:document:export',5
    UNION ALL SELECT 'chunk-method','切片方法查询','kb:chunk-method:query',1
    UNION ALL SELECT 'chunk-method','切片方法创建','kb:chunk-method:create',2
    UNION ALL SELECT 'chunk-method','切片方法更新','kb:chunk-method:update',3
    UNION ALL SELECT 'chunk-method','切片方法删除','kb:chunk-method:delete',4
    UNION ALL SELECT 'chunk-method','切片方法测试','kb:chunk-method:test',5
    UNION ALL SELECT 'user-dept','查询','kb:user-dept:query',1
    UNION ALL SELECT 'user-dept','更新','kb:user-dept:update',2
    UNION ALL SELECT 'user-dept','删除','kb:user-dept:delete',3
    UNION ALL SELECT 'rag-config','RAG配置查询','kb:rag-config:query',1
    UNION ALL SELECT 'rag-config','RAG配置创建','kb:rag-config:create',2
    UNION ALL SELECT 'rag-config','RAG配置更新','kb:rag-config:update',3
    UNION ALL SELECT 'rag-config','RAG配置删除','kb:rag-config:delete',4
    UNION ALL SELECT 'project-member','查询','kb:project-member:query',1
    UNION ALL SELECT 'project-member','更新','kb:project-member:update',2
    UNION ALL SELECT 'project-member','删除','kb:project-member:delete',3
    UNION ALL SELECT 'model-config','模型配置查询','kb:model-config:query',1
    UNION ALL SELECT 'model-config','模型配置创建','kb:model-config:create',2
    UNION ALL SELECT 'model-config','模型配置更新','kb:model-config:update',3
    UNION ALL SELECT 'model-config','模型配置删除','kb:model-config:delete',4
    UNION ALL SELECT 'model-config','模型配置导出','kb:model-config:export',5
    UNION ALL SELECT 'model-config','模型配置测试','kb:model-config:test',6
    UNION ALL SELECT 'model-config','模型配置复制','kb:model-config:copy',7
    UNION ALL SELECT 'model-config','模型配置批量操作','kb:model-config:batch',8
    UNION ALL SELECT 'news','新闻数据源查询','kb:news-source:query',1
    UNION ALL SELECT 'news','新闻数据源创建','kb:news-source:create',2
    UNION ALL SELECT 'news','新闻数据源更新','kb:news-source:update',3
    UNION ALL SELECT 'news','新闻数据源删除','kb:news-source:delete',4
    UNION ALL SELECT 'news','新闻记录查询','kb:news-record:query',5
    UNION ALL SELECT 'news','新闻记录批量操作','kb:news-record:batch',6
    UNION ALL SELECT 'news','新闻同步日志查询','kb:news-sync-log:query',7
    UNION ALL SELECT 'tag','标签查询','kb:tag:query',1
    UNION ALL SELECT 'tag','标签创建','kb:tag:create',2
    UNION ALL SELECT 'tag','标签更新','kb:tag:update',3
    UNION ALL SELECT 'tag','标签删除','kb:tag:delete',4
) `v`
JOIN `system_menu` `m` ON `m`.`path`=`v`.`child_path` AND `m`.`parent_id`=@m_kb AND `m`.`deleted`=b'0'
WHERE NOT EXISTS (
    SELECT 1 FROM `system_menu` `sm` WHERE `sm`.`permission`=`v`.`permission` AND `sm`.`deleted`=b'0'
);

-- =====================================================
-- 五、C 端导航：从管理后台隐藏（visible=0），C 端仍可用
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