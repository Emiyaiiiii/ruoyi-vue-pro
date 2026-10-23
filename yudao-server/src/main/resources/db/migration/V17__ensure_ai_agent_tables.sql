-- =====================================================
-- V17: 幂等补齐 ai-agent 模块所需表结构
--
-- 背景:
--   * ai-agent 模块实际使用 4 张表: ai_agent / ai_mcp_meta / ai_skill_meta / ai_meta_follow。
--   * 其中前 3 张由 V1__init_kb_schema.sql 创建；
--     ai_meta_follow 原本写在 v14__add_meta_follow.sql（文件名首字母小写 v），
--     不符合 Flyway 命名规范，启动时被跳过（日志: 1 SQL migrations were detected but not run），
--     导致全新数据库（如生产服务器）缺少 ai_meta_follow，查询时报
--     "Table '...ai_meta_follow' doesn't exist"，并被全局异常处理器统一翻译成
--     "[AI 大模型 yudao-module-ai - 表结构未导入]"（实际是 ai-agent 模块的表）。
--   * 本迁移使用 CREATE TABLE IF NOT EXISTS，对已有库/全新库均幂等安全。
-- =====================================================

-- 1. 智能体实例表
CREATE TABLE IF NOT EXISTS `ai_agent` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `user_id` bigint NOT NULL COMMENT '所属用户ID',
  `name` varchar(64) NOT NULL COMMENT '智能体名称',
  `description` varchar(255) NOT NULL DEFAULT '' COMMENT '智能体描述',
  `avatar` varchar(255) NOT NULL DEFAULT '' COMMENT '头像地址',
  `qwenpaw_agent_id` varchar(64) NOT NULL COMMENT 'QwenPaw 侧 agent ID（唯一）',
  `workspace_dir` varchar(512) NOT NULL DEFAULT '' COMMENT 'QwenPaw workspace 目录',
  `model_provider` varchar(32) NOT NULL DEFAULT '' COMMENT '模型供应商',
  `model_name` varchar(128) NOT NULL DEFAULT '' COMMENT '模型名称',
  `system_prompt` text COMMENT '系统提示词',
  `enable_kb_tool` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否启用知识库问答工具: 0=关闭, 1=开启',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态: 0=停用, 1=启用',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序',
  `is_default` tinyint(1) DEFAULT NULL COMMENT '是否为用户默认智能体: 1=是, NULL=否',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_qwenpaw_agent_id` (`qwenpaw_agent_id`),
  UNIQUE KEY `uk_user_default` (`user_id`,`is_default`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='智能体实例表';

-- 2. 系统级/用户级 MCP 商店表
CREATE TABLE IF NOT EXISTS `ai_mcp_meta` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `name` varchar(64) NOT NULL COMMENT 'MCP 名称',
  `code` varchar(64) NOT NULL COMMENT 'MCP 编码（唯一标识）',
  `type` tinyint NOT NULL DEFAULT '0' COMMENT '类型: 0=系统级, 1=用户级',
  `transport` varchar(32) NOT NULL DEFAULT 'stdio' COMMENT '传输协议: stdio/streamable_http/sse',
  `url` varchar(512) NOT NULL DEFAULT '' COMMENT '远程地址（streamable_http/sse 时必填）',
  `command` varchar(512) NOT NULL DEFAULT '' COMMENT '启动命令（stdio 时必填）',
  `args` varchar(1024) NOT NULL DEFAULT '' COMMENT '启动参数（JSON 数组）',
  `env` varchar(2048) NOT NULL DEFAULT '' COMMENT '环境变量（JSON 对象，值可加密）',
  `headers` varchar(2048) NOT NULL DEFAULT '' COMMENT '请求头（JSON 对象，用于远程鉴权）',
  `tools_whitelist` varchar(1024) NOT NULL DEFAULT '' COMMENT '工具白名单（JSON 数组，空表示全部）',
  `description` varchar(255) NOT NULL DEFAULT '' COMMENT '描述',
  `icon` varchar(255) NOT NULL DEFAULT '' COMMENT '图标',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态: 0=停用, 1=启用',
  `owner_user_id` bigint NOT NULL DEFAULT '0' COMMENT '归属用户ID（type=1 个人 MCP 时，仅创建者可见）',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_code` (`tenant_id`,`code`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MCP 商店表';

-- 3. 技能商店表(QwenPaw 技能池元数据)
CREATE TABLE IF NOT EXISTS `ai_skill_meta` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `skill_name` varchar(128) NOT NULL COMMENT 'QwenPaw技能池中的技能名称（唯一标识）',
  `display_name` varchar(128) NOT NULL DEFAULT '' COMMENT '显示名称',
  `description` text COMMENT '描述',
  `icon` varchar(256) DEFAULT '' COMMENT '图标（emoji或URL）',
  `source` varchar(32) NOT NULL DEFAULT 'customized' COMMENT '来源: builtin/customized（同步自QwenPaw）',
  `version` varchar(32) DEFAULT '' COMMENT '版本号（同步自QwenPaw）',
  `visibility` tinyint NOT NULL DEFAULT '1' COMMENT '可见性: 0=个人(仅创建者可见), 1=公开(所有用户可见)',
  `owner_user_id` bigint DEFAULT NULL COMMENT '创建者用户ID',
  `tags` varchar(512) DEFAULT NULL COMMENT '标签（JSON数组字符串）',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态: 0=停用, 1=启用',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_skill_name` (`skill_name`,`tenant_id`,`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='技能商店（QwenPaw技能池元数据）';

-- 4. 智囊元数据关注表（原 v14__add_meta_follow.sql，因小写 v 被 Flyway 跳过）
CREATE TABLE IF NOT EXISTS `ai_meta_follow` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `meta_type` tinyint NOT NULL COMMENT '被关注对象类型: 1=技能, 2=MCP, 3=智能体',
  `meta_id` bigint NOT NULL COMMENT '被关注对象ID（ai_skill_meta.id / ai_mcp_meta.id / ai_agent.id）',
  `user_id` bigint NOT NULL COMMENT '关注用户ID',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_user_type` (`user_id`, `meta_type`),
  KEY `idx_meta` (`meta_type`, `meta_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='智囊元数据关注表';
