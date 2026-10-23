-- =====================================================
-- V18: 幂等补齐 ai-agent 模块已有表的缺失字段
--
-- 背景:
--   * 生产服务器上 ai_agent / ai_mcp_meta / ai_skill_meta 等表是旧版本创建的，
--     部分字段（如 is_default / enable_kb_tool / visibility / tenant_id 等）缺失。
--   * CREATE TABLE IF NOT EXISTS 对已存在的表只会跳过，不会补列；
--     MySQL 的 ADD COLUMN 也不支持 IF NOT EXISTS（那是 MariaDB 语法）。
--   * 因此这里用存储过程：逐列查 information_schema.COLUMNS，缺失才 ALTER ADD。
--   * 本迁移幂等：重复执行、字段已存在时均安全跳过。
-- =====================================================

DROP PROCEDURE IF EXISTS `ckb_add_column`;
DELIMITER $$
CREATE PROCEDURE `ckb_add_column`(IN p_table VARCHAR(64), IN p_column VARCHAR(64), IN p_ddl TEXT)
BEGIN
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = p_table
        AND COLUMN_NAME = p_column) = 0 THEN
    SET @ddl = CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN ', p_ddl);
    PREPARE stmt FROM @ddl;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END$$
DELIMITER ;

-- ---------------- ai_agent ----------------
CALL ckb_add_column('ai_agent', 'tenant_id',        "`tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号'");
CALL ckb_add_column('ai_agent', 'user_id',          "`user_id` bigint NOT NULL COMMENT '所属用户ID'");
CALL ckb_add_column('ai_agent', 'name',             "`name` varchar(64) NOT NULL COMMENT '智能体名称'");
CALL ckb_add_column('ai_agent', 'description',      "`description` varchar(255) NOT NULL DEFAULT '' COMMENT '智能体描述'");
CALL ckb_add_column('ai_agent', 'avatar',           "`avatar` varchar(255) NOT NULL DEFAULT '' COMMENT '头像地址'");
CALL ckb_add_column('ai_agent', 'qwenpaw_agent_id', "`qwenpaw_agent_id` varchar(64) NOT NULL COMMENT 'QwenPaw 侧 agent ID（唯一）'");
CALL ckb_add_column('ai_agent', 'workspace_dir',    "`workspace_dir` varchar(512) NOT NULL DEFAULT '' COMMENT 'QwenPaw workspace 目录'");
CALL ckb_add_column('ai_agent', 'model_provider',   "`model_provider` varchar(32) NOT NULL DEFAULT '' COMMENT '模型供应商'");
CALL ckb_add_column('ai_agent', 'model_name',       "`model_name` varchar(128) NOT NULL DEFAULT '' COMMENT '模型名称'");
CALL ckb_add_column('ai_agent', 'system_prompt',    "`system_prompt` text COMMENT '系统提示词'");
CALL ckb_add_column('ai_agent', 'enable_kb_tool',   "`enable_kb_tool` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否启用知识库问答工具: 0=关闭, 1=开启'");
CALL ckb_add_column('ai_agent', 'status',           "`status` tinyint NOT NULL DEFAULT '1' COMMENT '状态: 0=停用, 1=启用'");
CALL ckb_add_column('ai_agent', 'sort_order',       "`sort_order` int NOT NULL DEFAULT '0' COMMENT '排序'");
CALL ckb_add_column('ai_agent', 'is_default',       "`is_default` tinyint(1) DEFAULT NULL COMMENT '是否为用户默认智能体: 1=是, NULL=否'");
CALL ckb_add_column('ai_agent', 'creator',          "`creator` varchar(64) DEFAULT '' COMMENT '创建者'");
CALL ckb_add_column('ai_agent', 'create_time',      "`create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'");
CALL ckb_add_column('ai_agent', 'updater',          "`updater` varchar(64) DEFAULT '' COMMENT '更新者'");
CALL ckb_add_column('ai_agent', 'update_time',      "`update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'");
CALL ckb_add_column('ai_agent', 'deleted',          "`deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除'");

-- ---------------- ai_mcp_meta ----------------
CALL ckb_add_column('ai_mcp_meta', 'tenant_id',       "`tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号'");
CALL ckb_add_column('ai_mcp_meta', 'name',            "`name` varchar(64) NOT NULL COMMENT 'MCP 名称'");
CALL ckb_add_column('ai_mcp_meta', 'code',            "`code` varchar(64) NOT NULL COMMENT 'MCP 编码（唯一标识）'");
CALL ckb_add_column('ai_mcp_meta', 'type',            "`type` tinyint NOT NULL DEFAULT '0' COMMENT '类型: 0=系统级, 1=用户级'");
CALL ckb_add_column('ai_mcp_meta', 'transport',       "`transport` varchar(32) NOT NULL DEFAULT 'stdio' COMMENT '传输协议: stdio/streamable_http/sse'");
CALL ckb_add_column('ai_mcp_meta', 'url',             "`url` varchar(512) NOT NULL DEFAULT '' COMMENT '远程地址（streamable_http/sse 时必填）'");
CALL ckb_add_column('ai_mcp_meta', 'command',         "`command` varchar(512) NOT NULL DEFAULT '' COMMENT '启动命令（stdio 时必填）'");
CALL ckb_add_column('ai_mcp_meta', 'args',            "`args` varchar(1024) NOT NULL DEFAULT '' COMMENT '启动参数（JSON 数组）'");
CALL ckb_add_column('ai_mcp_meta', 'env',             "`env` varchar(2048) NOT NULL DEFAULT '' COMMENT '环境变量（JSON 对象，值可加密）'");
CALL ckb_add_column('ai_mcp_meta', 'headers',         "`headers` varchar(2048) NOT NULL DEFAULT '' COMMENT '请求头（JSON 对象，用于远程鉴权）'");
CALL ckb_add_column('ai_mcp_meta', 'tools_whitelist', "`tools_whitelist` varchar(1024) NOT NULL DEFAULT '' COMMENT '工具白名单（JSON 数组，空表示全部）'");
CALL ckb_add_column('ai_mcp_meta', 'description',     "`description` varchar(255) NOT NULL DEFAULT '' COMMENT '描述'");
CALL ckb_add_column('ai_mcp_meta', 'icon',            "`icon` varchar(255) NOT NULL DEFAULT '' COMMENT '图标'");
CALL ckb_add_column('ai_mcp_meta', 'status',          "`status` tinyint NOT NULL DEFAULT '1' COMMENT '状态: 0=停用, 1=启用'");
CALL ckb_add_column('ai_mcp_meta', 'owner_user_id',   "`owner_user_id` bigint NOT NULL DEFAULT '0' COMMENT '归属用户ID（type=1 个人 MCP 时，仅创建者可见）'");
CALL ckb_add_column('ai_mcp_meta', 'sort_order',      "`sort_order` int NOT NULL DEFAULT '0' COMMENT '排序'");
CALL ckb_add_column('ai_mcp_meta', 'creator',         "`creator` varchar(64) DEFAULT '' COMMENT '创建者'");
CALL ckb_add_column('ai_mcp_meta', 'create_time',     "`create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'");
CALL ckb_add_column('ai_mcp_meta', 'updater',         "`updater` varchar(64) DEFAULT '' COMMENT '更新者'");
CALL ckb_add_column('ai_mcp_meta', 'update_time',     "`update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'");
CALL ckb_add_column('ai_mcp_meta', 'deleted',         "`deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除'");

-- ---------------- ai_skill_meta ----------------
CALL ckb_add_column('ai_skill_meta', 'tenant_id',     "`tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号'");
CALL ckb_add_column('ai_skill_meta', 'skill_name',    "`skill_name` varchar(128) NOT NULL COMMENT 'QwenPaw技能池中的技能名称（唯一标识）'");
CALL ckb_add_column('ai_skill_meta', 'display_name',  "`display_name` varchar(128) NOT NULL DEFAULT '' COMMENT '显示名称'");
CALL ckb_add_column('ai_skill_meta', 'description',   "`description` text COMMENT '描述'");
CALL ckb_add_column('ai_skill_meta', 'icon',          "`icon` varchar(256) DEFAULT '' COMMENT '图标（emoji或URL）'");
CALL ckb_add_column('ai_skill_meta', 'source',        "`source` varchar(32) NOT NULL DEFAULT 'customized' COMMENT '来源: builtin/customized（同步自QwenPaw）'");
CALL ckb_add_column('ai_skill_meta', 'version',       "`version` varchar(32) DEFAULT '' COMMENT '版本号（同步自QwenPaw）'");
CALL ckb_add_column('ai_skill_meta', 'visibility',    "`visibility` tinyint NOT NULL DEFAULT '1' COMMENT '可见性: 0=个人(仅创建者可见), 1=公开(所有用户可见)'");
CALL ckb_add_column('ai_skill_meta', 'owner_user_id', "`owner_user_id` bigint DEFAULT NULL COMMENT '创建者用户ID'");
CALL ckb_add_column('ai_skill_meta', 'tags',          "`tags` varchar(512) DEFAULT NULL COMMENT '标签（JSON数组字符串）'");
CALL ckb_add_column('ai_skill_meta', 'status',        "`status` tinyint NOT NULL DEFAULT '1' COMMENT '状态: 0=停用, 1=启用'");
CALL ckb_add_column('ai_skill_meta', 'creator',       "`creator` varchar(64) DEFAULT '' COMMENT '创建者'");
CALL ckb_add_column('ai_skill_meta', 'create_time',   "`create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'");
CALL ckb_add_column('ai_skill_meta', 'updater',       "`updater` varchar(64) DEFAULT '' COMMENT '更新者'");
CALL ckb_add_column('ai_skill_meta', 'update_time',   "`update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'");
CALL ckb_add_column('ai_skill_meta', 'deleted',       "`deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除'");

-- ---------------- ai_meta_follow ----------------
CALL ckb_add_column('ai_meta_follow', 'meta_type',   "`meta_type` tinyint NOT NULL COMMENT '被关注对象类型: 1=技能, 2=MCP, 3=智能体'");
CALL ckb_add_column('ai_meta_follow', 'meta_id',     "`meta_id` bigint NOT NULL COMMENT '被关注对象ID'");
CALL ckb_add_column('ai_meta_follow', 'user_id',     "`user_id` bigint NOT NULL COMMENT '关注用户ID'");
CALL ckb_add_column('ai_meta_follow', 'creator',     "`creator` varchar(64) DEFAULT '' COMMENT '创建者'");
CALL ckb_add_column('ai_meta_follow', 'create_time', "`create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'");
CALL ckb_add_column('ai_meta_follow', 'updater',     "`updater` varchar(64) DEFAULT '' COMMENT '更新者'");
CALL ckb_add_column('ai_meta_follow', 'update_time', "`update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'");
CALL ckb_add_column('ai_meta_follow', 'deleted',     "`deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除'");
CALL ckb_add_column('ai_meta_follow', 'tenant_id',   "`tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号'");

DROP PROCEDURE IF EXISTS `ckb_add_column`;
