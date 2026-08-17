-- ============================================================
-- 智能体管理模块（ai-agent）建表脚本
-- 对应前端菜单: 智能体管理
-- 说明: 该模块把 QwenPaw 作为共享「智能体仓库」，芋道侧负责
--       智能体生命周期、系统级 MCP/Skills 商店、用户级绑定与问答会话。
-- ============================================================

SET NAMES utf8mb4;

-- ============================================================
-- 1. 智能体实例表 ai_agent
--    每行映射到一个 QwenPaw agent（qwenpaw_agent_id 一对一）
-- ============================================================
DROP TABLE IF EXISTS `ai_agent`;
CREATE TABLE `ai_agent` (
    `id`                 BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id`          BIGINT       NOT NULL DEFAULT 0 COMMENT '租户编号',
    `user_id`            BIGINT       NOT NULL COMMENT '所属用户ID',
    `name`               VARCHAR(64)  NOT NULL COMMENT '智能体名称',
    `description`        VARCHAR(255) NOT NULL DEFAULT '' COMMENT '智能体描述',
    `avatar`             VARCHAR(255) NOT NULL DEFAULT '' COMMENT '头像地址',
    `qwenpaw_agent_id`   VARCHAR(64)  NOT NULL COMMENT 'QwenPaw 侧 agent ID（唯一）',
    `workspace_dir`      VARCHAR(512) NOT NULL DEFAULT '' COMMENT 'QwenPaw workspace 目录',
    `model_provider`     VARCHAR(32)  NOT NULL DEFAULT '' COMMENT '模型供应商',
    `model_name`         VARCHAR(128) NOT NULL DEFAULT '' COMMENT '模型名称',
    `system_prompt`      TEXT         COMMENT '系统提示词',
    `enable_kb_tool`     BIT(1)       NOT NULL DEFAULT b'1' COMMENT '是否启用知识库问答工具: 0=关闭, 1=开启',
    `status`             TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 0=停用, 1=启用',
    `sort_order`         INT          NOT NULL DEFAULT 0 COMMENT '排序',
    `creator`            VARCHAR(64)  DEFAULT '' COMMENT '创建者',
    `create_time`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater`            VARCHAR(64)  DEFAULT '' COMMENT '更新者',
    `update_time`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`            BIT(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_qwenpaw_agent_id` (`qwenpaw_agent_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_tenant_id` (`tenant_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='智能体实例表';

-- ============================================================
-- 2. 系统级 MCP 商店表 ai_mcp_meta
--    type: 0=系统级(全体可见), 1=用户级(管理员下发)
--    transport: stdio / streamable_http / sse
-- ============================================================
DROP TABLE IF EXISTS `ai_mcp_meta`;
CREATE TABLE `ai_mcp_meta` (
    `id`               BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id`        BIGINT        NOT NULL DEFAULT 0 COMMENT '租户编号',
    `name`             VARCHAR(64)   NOT NULL COMMENT 'MCP 名称',
    `code`             VARCHAR(64)   NOT NULL COMMENT 'MCP 编码（唯一标识）',
    `type`             TINYINT       NOT NULL DEFAULT 0 COMMENT '类型: 0=系统级, 1=用户级',
    `transport`        VARCHAR(32)   NOT NULL DEFAULT 'stdio' COMMENT '传输协议: stdio/streamable_http/sse',
    `url`              VARCHAR(512)  NOT NULL DEFAULT '' COMMENT '远程地址（streamable_http/sse 时必填）',
    `command`          VARCHAR(512)  NOT NULL DEFAULT '' COMMENT '启动命令（stdio 时必填）',
    `args`             VARCHAR(1024) NOT NULL DEFAULT '' COMMENT '启动参数（JSON 数组）',
    `env`              VARCHAR(2048) NOT NULL DEFAULT '' COMMENT '环境变量（JSON 对象，值可加密）',
    `headers`          VARCHAR(2048) NOT NULL DEFAULT '' COMMENT '请求头（JSON 对象，用于远程鉴权）',
    `tools_whitelist`  VARCHAR(1024) NOT NULL DEFAULT '' COMMENT '工具白名单（JSON 数组，空表示全部）',
    `description`      VARCHAR(255)  NOT NULL DEFAULT '' COMMENT '描述',
    `icon`             VARCHAR(255)  NOT NULL DEFAULT '' COMMENT '图标',
    `status`           TINYINT       NOT NULL DEFAULT 1 COMMENT '状态: 0=停用, 1=启用',
    `sort_order`       INT           NOT NULL DEFAULT 0 COMMENT '排序',
    `creator`          VARCHAR(64)   DEFAULT '' COMMENT '创建者',
    `create_time`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater`          VARCHAR(64)   DEFAULT '' COMMENT '更新者',
    `update_time`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`          BIT(1)        NOT NULL DEFAULT b'0' COMMENT '是否删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_code` (`tenant_id`, `code`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MCP 商店表';

-- ============================================================
-- 3. 系统级 Skills 商店表 ai_skill_meta
--    source_type: git / upload
-- ============================================================
DROP TABLE IF EXISTS `ai_skill_meta`;
CREATE TABLE `ai_skill_meta` (
    `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id`      BIGINT        NOT NULL DEFAULT 0 COMMENT '租户编号',
    `name`           VARCHAR(64)   NOT NULL COMMENT '技能名称',
    `code`           VARCHAR(64)   NOT NULL COMMENT '技能编码（唯一标识）',
    `version`        VARCHAR(32)   NOT NULL DEFAULT '1.0.0' COMMENT '版本号',
    `source_type`    VARCHAR(16)   NOT NULL DEFAULT 'upload' COMMENT '来源类型: git=Git仓库, upload=上传压缩包',
    `source_url`     VARCHAR(512)  NOT NULL DEFAULT '' COMMENT '来源地址（git 仓库地址或上传文件 URL）',
    `description`    VARCHAR(255)  NOT NULL DEFAULT '' COMMENT '描述',
    `icon`           VARCHAR(255)  NOT NULL DEFAULT '' COMMENT '图标',
    `status`         TINYINT       NOT NULL DEFAULT 1 COMMENT '状态: 0=停用, 1=启用',
    `sort_order`     INT           NOT NULL DEFAULT 0 COMMENT '排序',
    `creator`        VARCHAR(64)   DEFAULT '' COMMENT '创建者',
    `create_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater`        VARCHAR(64)   DEFAULT '' COMMENT '更新者',
    `update_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`        BIT(1)        NOT NULL DEFAULT b'0' COMMENT '是否删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_code` (`tenant_id`, `code`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Skills 商店表';

-- ============================================================
-- 4. 智能体-MCP 绑定表 ai_agent_mcp
-- ============================================================
DROP TABLE IF EXISTS `ai_agent_mcp`;
CREATE TABLE `ai_agent_mcp` (
    `id`                BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id`         BIGINT        NOT NULL DEFAULT 0 COMMENT '租户编号',
    `agent_id`          BIGINT        NOT NULL COMMENT '智能体ID',
    `mcp_meta_id`       BIGINT        NOT NULL COMMENT 'MCP 商店项ID',
    `client_key`        VARCHAR(64)   NOT NULL DEFAULT '' COMMENT 'QwenPaw MCP client key',
    `config_override`   VARCHAR(2048) NOT NULL DEFAULT '' COMMENT '用户级配置覆盖（JSON，如 url/headers/command）',
    `tools_whitelist`   VARCHAR(1024) NOT NULL DEFAULT '' COMMENT '工具白名单（JSON 数组）',
    `enabled`           TINYINT       NOT NULL DEFAULT 1 COMMENT '是否启用: 0=停用, 1=启用',
    `sort_order`        INT           NOT NULL DEFAULT 0 COMMENT '排序',
    `creator`           VARCHAR(64)   DEFAULT '' COMMENT '创建者',
    `create_time`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater`           VARCHAR(64)   DEFAULT '' COMMENT '更新者',
    `update_time`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`           BIT(1)        NOT NULL DEFAULT b'0' COMMENT '是否删除',
    PRIMARY KEY (`id`),
    KEY `idx_agent_id` (`agent_id`),
    KEY `idx_mcp_meta_id` (`mcp_meta_id`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='智能体-MCP 绑定表';

-- ============================================================
-- 5. 智能体-Skill 绑定表 ai_agent_skill
-- ============================================================
DROP TABLE IF EXISTS `ai_agent_skill`;
CREATE TABLE `ai_agent_skill` (
    `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id`      BIGINT        NOT NULL DEFAULT 0 COMMENT '租户编号',
    `agent_id`       BIGINT        NOT NULL COMMENT '智能体ID',
    `skill_meta_id`  BIGINT        NOT NULL COMMENT '技能商店项ID',
    `version`        VARCHAR(32)   NOT NULL DEFAULT '1.0.0' COMMENT '安装版本',
    `enabled`        TINYINT       NOT NULL DEFAULT 1 COMMENT '是否启用: 0=停用, 1=启用',
    `install_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '安装时间',
    `sort_order`     INT           NOT NULL DEFAULT 0 COMMENT '排序',
    `creator`        VARCHAR(64)   DEFAULT '' COMMENT '创建者',
    `create_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater`        VARCHAR(64)   DEFAULT '' COMMENT '更新者',
    `update_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`        BIT(1)        NOT NULL DEFAULT b'0' COMMENT '是否删除',
    PRIMARY KEY (`id`),
    KEY `idx_agent_id` (`agent_id`),
    KEY `idx_skill_meta_id` (`skill_meta_id`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='智能体-Skill 绑定表';

-- ============================================================
-- 6. 问答会话表 ai_chat_session
-- ============================================================
DROP TABLE IF EXISTS `ai_chat_session`;
CREATE TABLE `ai_chat_session` (
    `id`            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id`     BIGINT        NOT NULL DEFAULT 0 COMMENT '租户编号',
    `agent_id`      BIGINT        NOT NULL COMMENT '智能体ID',
    `user_id`       BIGINT        NOT NULL COMMENT '用户ID',
    `session_key`   VARCHAR(64)   NOT NULL DEFAULT '' COMMENT 'QwenPaw session id',
    `title`         VARCHAR(255)  NOT NULL DEFAULT '新对话' COMMENT '会话标题',
    `status`        TINYINT       NOT NULL DEFAULT 1 COMMENT '状态: 0=关闭, 1=进行中',
    `creator`       VARCHAR(64)   DEFAULT '' COMMENT '创建者',
    `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater`       VARCHAR(64)   DEFAULT '' COMMENT '更新者',
    `update_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`       BIT(1)        NOT NULL DEFAULT b'0' COMMENT '是否删除',
    PRIMARY KEY (`id`),
    KEY `idx_agent_id` (`agent_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='问答会话表';

-- ============================================================
-- 7. 问答消息表 ai_chat_message
-- ============================================================
DROP TABLE IF EXISTS `ai_chat_message`;
CREATE TABLE `ai_chat_message` (
    `id`            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id`     BIGINT        NOT NULL DEFAULT 0 COMMENT '租户编号',
    `session_id`    BIGINT        NOT NULL COMMENT '会话ID',
    `agent_id`      BIGINT        NOT NULL COMMENT '智能体ID',
    `user_id`       BIGINT        NOT NULL COMMENT '用户ID',
    `role`          VARCHAR(16)   NOT NULL COMMENT '角色: user/assistant/tool/system',
    `content`           LONGTEXT      COMMENT '消息内容',
    `reasoning_content` LONGTEXT      COMMENT '思考/推理内容',
    `tool_calls`        VARCHAR(2048) NOT NULL DEFAULT '' COMMENT '工具调用记录（JSON）',
    `tokens`            INT           NOT NULL DEFAULT 0 COMMENT 'Token 用量',
    `creator`       VARCHAR(64)   DEFAULT '' COMMENT '创建者',
    `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater`       VARCHAR(64)   DEFAULT '' COMMENT '更新者',
    `update_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`       BIT(1)        NOT NULL DEFAULT b'0' COMMENT '是否删除',
    PRIMARY KEY (`id`),
    KEY `idx_session_id` (`session_id`),
    KEY `idx_agent_id` (`agent_id`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='问答消息表';


-- ============================================================
-- 菜单数据: 智能体管理（一级菜单）
-- ============================================================

-- 智能体管理 (一级菜单)
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
VALUES ('智能体管理', '', 1, 8, 0, '/ai-agent', 'ep:chat-dot-round', '', 'AiAgent', 0, 1, 1, 1, NOW(), NOW(), b'0');

-- 获取"智能体管理"菜单ID
SET @ai_agent_parent_id = LAST_INSERT_ID();

-- 我的智能体 (菜单)
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
VALUES ('我的智能体', '', 2, 1, @ai_agent_parent_id, 'agent', 'ep:avatar', 'ai/agent/index', 'AiAgentIndex', 0, 1, 1, 0, NOW(), NOW(), b'0');
SET @ai_agent_menu_id = LAST_INSERT_ID();

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
VALUES
('智能体查询', 'ai-agent:agent:query', 3, 1, @ai_agent_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'),
('智能体创建', 'ai-agent:agent:create', 3, 2, @ai_agent_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'),
('智能体更新', 'ai-agent:agent:update', 3, 3, @ai_agent_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'),
('智能体删除', 'ai-agent:agent:delete', 3, 4, @ai_agent_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0');

-- Skills 商店 (菜单) —— 即 QwenPaw 技能池浏览页，无本地 CRUD
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
VALUES ('技能商店', '', 2, 2, @ai_agent_parent_id, 'skill-store', 'ep:magic-stick', 'ai/skillmeta/index', 'AiSkillMetaIndex', 0, 1, 1, 0, NOW(), NOW(), b'0');
SET @ai_skill_menu_id = LAST_INSERT_ID();

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
VALUES
('技能查询', 'ai-agent:skill-meta:query', 3, 1, @ai_skill_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0');

-- 问答会话 (菜单)
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
VALUES ('问答会话', '', 2, 4, @ai_agent_parent_id, 'chat-session', 'ep:chat-line-round', 'ai/chatsession/index', 'AiChatSessionIndex', 0, 1, 1, 0, NOW(), NOW(), b'0');
SET @ai_chat_menu_id = LAST_INSERT_ID();

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
VALUES
('会话查询', 'ai-agent:chat-session:query', 3, 1, @ai_chat_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'),
('会话创建', 'ai-agent:chat-session:create', 3, 2, @ai_chat_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'),
('会话更新', 'ai-agent:chat-session:update', 3, 3, @ai_chat_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'),
('会话删除', 'ai-agent:chat-session:delete', 3, 4, @ai_chat_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0');

-- 模型管理 (菜单)
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
VALUES ('模型管理', '', 2, 5, @ai_agent_parent_id, 'model-provider', 'ep:cpu', 'ai/model/provider/index', 'AiModelProvider', 0, 1, 1, 0, NOW(), NOW(), b'0');
SET @ai_model_menu_id = LAST_INSERT_ID();

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
VALUES
('模型查询', 'ai-agent:model:query', 3, 1, @ai_model_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'),
('模型创建', 'ai-agent:model:create', 3, 2, @ai_model_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'),
('模型更新', 'ai-agent:model:update', 3, 3, @ai_model_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'),
('模型删除', 'ai-agent:model:delete', 3, 4, @ai_model_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0');

-- Token 用量统计 (菜单)
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
VALUES ('Token用量', '', 2, 6, @ai_agent_parent_id, 'token-usage', 'ep:data-analysis', 'ai/tokenUsage/index', 'AiTokenUsageIndex', 0, 1, 1, 0, NOW(), NOW(), b'0');
SET @ai_token_usage_menu_id = LAST_INSERT_ID();

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
VALUES
('Token用量查询', 'ai-agent:token-usage:query', 3, 1, @ai_token_usage_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0');


-- ============================================================
-- 字典数据
-- ============================================================

-- 智能体状态
INSERT INTO `system_dict_type` (`name`, `type`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES ('智能体状态', 'ai_agent_status', 0, '智能体管理-智能体状态', 'admin', NOW(), 'admin', NOW(), b'0');

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
(1, '启用', '1', 'ai_agent_status', 0, 'success', '', '智能体-启用', 'admin', NOW(), 'admin', NOW(), b'0'),
(2, '停用', '0', 'ai_agent_status', 0, 'info', '', '智能体-停用', 'admin', NOW(), 'admin', NOW(), b'0');

-- MCP 传输协议
INSERT INTO `system_dict_type` (`name`, `type`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES ('MCP传输协议', 'ai_mcp_transport', 0, '智能体管理-MCP传输协议', 'admin', NOW(), 'admin', NOW(), b'0');

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
(1, '标准输入输出', 'stdio', 'ai_mcp_transport', 0, 'primary', '', 'MCP-stdio', 'admin', NOW(), 'admin', NOW(), b'0'),
(2, '流式HTTP', 'streamable_http', 'ai_mcp_transport', 0, 'success', '', 'MCP-streamable_http', 'admin', NOW(), 'admin', NOW(), b'0'),
(3, 'SSE', 'sse', 'ai_mcp_transport', 0, 'warning', '', 'MCP-sse', 'admin', NOW(), 'admin', NOW(), b'0');

-- MCP 类型
INSERT INTO `system_dict_type` (`name`, `type`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES ('MCP类型', 'ai_mcp_type', 0, '智能体管理-MCP类型', 'admin', NOW(), 'admin', NOW(), b'0');

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
(1, '系统级', '0', 'ai_mcp_type', 0, 'primary', '', 'MCP-系统级', 'admin', NOW(), 'admin', NOW(), b'0'),
(2, '用户级', '1', 'ai_mcp_type', 0, 'warning', '', 'MCP-用户级', 'admin', NOW(), 'admin', NOW(), b'0');

-- 技能来源类型
INSERT INTO `system_dict_type` (`name`, `type`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES ('技能来源类型', 'ai_skill_source_type', 0, '智能体管理-技能来源类型', 'admin', NOW(), 'admin', NOW(), b'0');

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
(1, 'Git仓库', 'git', 'ai_skill_source_type', 0, 'primary', '', '技能-Git', 'admin', NOW(), 'admin', NOW(), b'0'),
(2, '上传压缩包', 'upload', 'ai_skill_source_type', 0, 'success', '', '技能-上传', 'admin', NOW(), 'admin', NOW(), b'0');

-- 消息角色
INSERT INTO `system_dict_type` (`name`, `type`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES ('问答消息角色', 'ai_chat_role', 0, '智能体管理-问答消息角色', 'admin', NOW(), 'admin', NOW(), b'0');

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
(1, '用户', 'user', 'ai_chat_role', 0, 'primary', '', '消息-用户', 'admin', NOW(), 'admin', NOW(), b'0'),
(2, '助手', 'assistant', 'ai_chat_role', 0, 'success', '', '消息-助手', 'admin', NOW(), 'admin', NOW(), b'0'),
(3, '工具', 'tool', 'ai_chat_role', 0, 'warning', '', '消息-工具', 'admin', NOW(), 'admin', NOW(), b'0'),
(4, '系统', 'system', 'ai_chat_role', 0, 'info', '', '消息-系统', 'admin', NOW(), 'admin', NOW(), b'0');
