-- =====================================================
-- V1 (合并基线): AI 智能体 + 知识库模块 全量累积表结构
--
-- 说明:
--   * 本文件为合并后的单一结构基线，由 V1-V11 分片迁移按「当前库真实结构」收敛而来。
--   * 只含 DDL(建表)，不含种子/参数数据 —— 种子数据统一由 R__*.sql 幂等管理。
--   * 以当前 docker MySQL 实测结构为准(与 DO 实体一致)，使用 CREATE TABLE IF NOT EXISTS 保证幂等。
--   * 约定: 后续结构变更按「一个功能/发布一个 V」新增，参数/种子数据一律走 R__。
-- =====================================================

-- =====================================================
-- 一、AI 智能体管理模块 (ai-agent)
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

-- =====================================================
-- 二、知识库模块 (kb)
-- =====================================================

-- 4. 层级配置表
CREATE TABLE IF NOT EXISTS `kb_level_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `level_code` varchar(50) NOT NULL COMMENT '层级编码',
  `level_name` varchar(100) NOT NULL COMMENT '层级名称',
  `visibility_rule` tinyint NOT NULL COMMENT '可见规则: 1=按所有者, 2=按归属部门, 3=全员, 5=指定部门列表',
  `owner_dim` tinyint DEFAULT '0' COMMENT '归属维度: 0=无, 1=用户, 2=部门',
  `dept_scope` varchar(1000) DEFAULT NULL COMMENT '分类可见部门范围: NULL=全员可见, JSON数组[101,102]=仅指定部门',
  `sort` int DEFAULT '0' COMMENT '排序',
  `status` tinyint DEFAULT '0' COMMENT '状态: 0=启用, 1=禁用',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint DEFAULT '0' COMMENT '逻辑删除: 0=未删除, 1=已删除',
  `tenant_id` bigint DEFAULT '0' COMMENT '租户ID: 0=默认租户',
  PRIMARY KEY (`id`),
  UNIQUE KEY `level_code` (`level_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库层级配置';

-- 5. 分类表(含自定义表头/列模板)
CREATE TABLE IF NOT EXISTS `kb_category` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(100) NOT NULL COMMENT '分类名称',
  `parent_id` bigint DEFAULT '0' COMMENT '父分类ID: 0=顶级分类',
  `kb_level_id` bigint DEFAULT NULL COMMENT '关联层级配置ID',
  `sort` int DEFAULT '0' COMMENT '排序',
  `status` tinyint DEFAULT '0' COMMENT '状态: 0=启用, 1=禁用',
  `column_config` varchar(2000) DEFAULT NULL COMMENT '列模板(JSON): 该分类下知识库列表的自定义表头，如 [{"source":"builtin","builtin":"name","label":"项目名称"},{"source":"custom","key":"project_member","label":"项目成员","type":"member"}]',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint DEFAULT '0' COMMENT '逻辑删除: 0=未删除, 1=已删除',
  `tenant_id` bigint DEFAULT '0' COMMENT '租户ID: 0=默认租户',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库分类';

-- 6. 知识库表
CREATE TABLE IF NOT EXISTS `kb_library` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(200) NOT NULL COMMENT '知识库名称',
  `category_id` bigint NOT NULL COMMENT '分类ID',
  `kb_level_id` bigint DEFAULT NULL COMMENT '关联层级配置ID',
  `owner_id` bigint DEFAULT NULL COMMENT '所有者ID: 用户或部门, 取决于层级配置的owner_dim',
  `description` varchar(500) DEFAULT '' COMMENT '描述',
  `cover_url` varchar(500) DEFAULT '' COMMENT '封面图片URL',
  `doc_count` int DEFAULT '0' COMMENT '文档数量',
  `status` tinyint DEFAULT '0' COMMENT '状态: 0=启用, 1=禁用',
  `is_public` tinyint DEFAULT '0' COMMENT '是否公开到广场: 0=否, 1=是',
  `is_project` tinyint DEFAULT '0' COMMENT '是否项目成果库: 0=否, 1=是',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint DEFAULT '0' COMMENT '逻辑删除: 0=未删除, 1=已删除',
  `tenant_id` bigint DEFAULT '0' COMMENT '租户ID: 0=默认租户',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库';

-- 7. 文档文件夹表
CREATE TABLE IF NOT EXISTS `kb_document_folder` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `kb_id` bigint NOT NULL COMMENT '所属知识库ID',
  `name` varchar(255) NOT NULL COMMENT '文件夹名称',
  `parent_id` bigint NOT NULL DEFAULT '0' COMMENT '父文件夹ID: 0=根目录',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_kb_id` (`kb_id`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档文件夹';

-- 8. 知识库文件表
CREATE TABLE IF NOT EXISTS `kb_document` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `kb_id` bigint NOT NULL COMMENT '所属知识库ID',
  `folder_id` bigint NOT NULL DEFAULT '0' COMMENT '所属文件夹ID: 0=根目录',
  `file_name` varchar(255) NOT NULL COMMENT '文件名称',
  `file_url` varchar(500) NOT NULL COMMENT '文件访问URL (芋道文件管理返回)',
  `file_type` varchar(50) DEFAULT '' COMMENT '文件类型: pdf/docx/xlsx/pptx/jpg/png等',
  `file_size` bigint DEFAULT '0' COMMENT '文件大小(字节)',
  `file_config_id` bigint DEFAULT NULL COMMENT '芋道文件配置ID (infra_file_config.id)',
  `file_path` varchar(500) DEFAULT '' COMMENT '文件存储路径 (芋道文件管理返回)',
  `description` varchar(500) DEFAULT '' COMMENT '文件描述',
  `tags` varchar(200) DEFAULT '' COMMENT '标签 (逗号分隔)',
  `download_count` int DEFAULT '0' COMMENT '下载次数',
  `view_count` int DEFAULT '0' COMMENT '查看次数',
  `status` tinyint DEFAULT '0' COMMENT '状态: 0=正常, 1=禁用',
  `vector_task_id` varchar(64) NOT NULL DEFAULT '' COMMENT '向量处理任务ID',
  `vector_status` tinyint NOT NULL DEFAULT '0' COMMENT '向量处理状态：0-未处理 1-处理中 2-已完成 3-失败 4-提交失败 5-超时',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint DEFAULT '0' COMMENT '逻辑删除: 0=未删除, 1=已删除',
  `tenant_id` bigint DEFAULT '0' COMMENT '租户ID: 0=默认租户',
  PRIMARY KEY (`id`),
  KEY `idx_kb` (`kb_id`),
  KEY `idx_file_type` (`file_type`),
  KEY `idx_folder_id` (`folder_id`),
  KEY `idx_vector_status` (`vector_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库文件';

-- 9. 用户部门关联表
CREATE TABLE IF NOT EXISTS `kb_user_dept` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `dept_id` bigint NOT NULL COMMENT '部门ID（院/公司/咨询评估中心）',
  `role` tinyint DEFAULT '0' COMMENT '角色: 0=成员, 1=管理员',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint DEFAULT '0' COMMENT '逻辑删除: 0=未删除, 1=已删除',
  `tenant_id` bigint DEFAULT '0' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_dept` (`user_id`,`dept_id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_dept` (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库用户部门关联';

-- 10. 共享部门关联表
CREATE TABLE IF NOT EXISTS `kb_share_dept` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `kb_id` bigint NOT NULL COMMENT '知识库ID',
  `dept_id` bigint NOT NULL COMMENT '共享目标部门ID',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint DEFAULT '0' COMMENT '逻辑删除: 0=未删除, 1=已删除',
  `tenant_id` bigint DEFAULT '0' COMMENT '租户ID: 0=默认租户',
  PRIMARY KEY (`id`),
  KEY `idx_kb` (`kb_id`),
  KEY `idx_dept` (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库共享部门关联';

-- 11. 项目成员表
CREATE TABLE IF NOT EXISTS `kb_project_member` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `kb_id` bigint NOT NULL COMMENT '知识库ID（项目）',
  `user_id` bigint NOT NULL COMMENT '项目成员用户ID',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint DEFAULT '0' COMMENT '逻辑删除: 0=未删除, 1=已删除',
  `tenant_id` bigint DEFAULT '0' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_kb_user` (`kb_id`,`user_id`),
  KEY `idx_kb` (`kb_id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库项目成员';

-- 12. 关注表
CREATE TABLE IF NOT EXISTS `kb_follow` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `kb_id` bigint NOT NULL COMMENT '知识库ID',
  `user_id` bigint NOT NULL COMMENT '关注用户ID',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint DEFAULT '0' COMMENT '逻辑删除: 0=未删除, 1=已删除',
  `tenant_id` bigint DEFAULT '0' COMMENT '租户ID: 0=默认租户',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_kb` (`user_id`,`kb_id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_kb` (`kb_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库关注';

-- 13. 向量处理任务表(已移除历史遗留 celery_task_id 列)
CREATE TABLE IF NOT EXISTS `kb_vector_task` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `task_id` varchar(64) NOT NULL COMMENT '任务ID（唯一标识，Java生成）',
  `doc_id` bigint NOT NULL COMMENT '文档ID',
  `kb_id` bigint NOT NULL COMMENT '知识库ID',
  `file_url` varchar(512) NOT NULL COMMENT '文件下载地址',
  `file_type` varchar(32) NOT NULL DEFAULT '' COMMENT '文件类型',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0-待提交 1-处理中 2-已完成 3-失败 4-提交失败 5-超时',
  `progress` int NOT NULL DEFAULT '0' COMMENT '进度（0-100）',
  `current_step` varchar(32) NOT NULL DEFAULT '' COMMENT '当前处理步骤',
  `chunk_count` int NOT NULL DEFAULT '0' COMMENT '分块数量',
  `error_msg` varchar(1024) NOT NULL DEFAULT '' COMMENT '错误信息',
  `params` varchar(2048) NOT NULL DEFAULT '' COMMENT '处理参数（JSON格式）',
  `creator` varchar(64) NOT NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) NOT NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_task_id` (`task_id`),
  KEY `idx_doc_id` (`doc_id`),
  KEY `idx_kb_id` (`kb_id`),
  KEY `idx_status` (`status`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='向量处理任务表';

-- 14. 切片方法定义表(已移除 avg_processing_speed/memory_footprint 冗余列)
CREATE TABLE IF NOT EXISTS `kb_chunk_method` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(100) NOT NULL COMMENT '方法名称',
  `method_type` varchar(20) NOT NULL COMMENT '方法类型: fixed_size=固定大小, semantic=语义分段, recursive=递归分割, sentence=按句子, paragraph=按段落, ...',
  `description` varchar(500) DEFAULT NULL COMMENT '方法描述',
  `code` varchar(50) NOT NULL COMMENT '方法代码(如 fixed_size)',
  `parameters_template` text COMMENT '参数模板(JSON Schema格式)',
  `default_parameters` text COMMENT '默认参数(JSON格式)',
  `handler_class` varchar(200) DEFAULT NULL COMMENT '处理器类全路径',
  `is_active` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否启用: 0=停用, 1=启用',
  `is_default_method` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否默认切片方法',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_method_type` (`method_type`),
  KEY `idx_is_active` (`is_active`),
  KEY `idx_is_default_method` (`is_default_method`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='切片方法定义表';

-- 15. 模型配置表(已精简: 去 provider/deploy/platform, 元数据并入 config)
CREATE TABLE IF NOT EXISTS `kb_model_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `uid` varchar(100) NOT NULL COMMENT '模型唯一标识',
  `model` varchar(200) DEFAULT NULL COMMENT '具体模型名(如 text-embedding-v4 / deepseek-chat / DeepSeek-OCR-2)',
  `name` varchar(100) NOT NULL COMMENT '模型名称',
  `url` varchar(500) NOT NULL COMMENT 'API地址',
  `appkey` varchar(500) NOT NULL COMMENT 'API密钥',
  `model_type` varchar(20) NOT NULL DEFAULT 'llm' COMMENT '用途分类: embedding=嵌入/向量模型, llm=大模型, ocr=OCR/多模态模型, rerank=重排模型',
  `thinking_enabled` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否启用思考能力',
  `vl_supported` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否支持多模态(VL): 0=否, 1=是',
  `is_active` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否激活: 0=停用, 1=激活',
  `description` varchar(500) DEFAULT NULL COMMENT '模型描述',
  `max_tokens` int NOT NULL DEFAULT '4096' COMMENT '最大Token数',
  `context_length` int NOT NULL DEFAULT '8192' COMMENT '上下文长度',
  `temperature` double NOT NULL DEFAULT '0.7' COMMENT '温度参数',
  `top_p` double NOT NULL DEFAULT '0.9' COMMENT 'Top-P参数',
  `config` varchar(2000) DEFAULT '{}' COMMENT '配置参数(JSON格式)',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序顺序(升序)',
  `is_pinned` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否置顶: 0=否, 1=是',
  `activated_at` datetime DEFAULT NULL COMMENT '激活时间',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_uid` (`uid`),
  KEY `idx_is_active` (`is_active`),
  KEY `idx_is_pinned` (`is_pinned`),
  KEY `idx_sort_order` (`sort_order`),
  KEY `idx_model_type` (`model_type`),
  KEY `idx_type_active` (`model_type`,`is_active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='大模型配置信息表';

-- 16. RAG 系统配置表(行为参数; 模型账户如 rerank 已迁往 kb_model_config)
CREATE TABLE IF NOT EXISTS `kb_rag_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `module` varchar(50) NOT NULL COMMENT '所属模块: retrieval=检索, rerank=重排序, chunking=切片, llm=大模型, cache=缓存, batch=批量处理, conversation=对话',
  `key` varchar(100) NOT NULL COMMENT '配置键名',
  `value` text NOT NULL COMMENT '配置值(字符串存储，根据value_type解析)',
  `value_type` varchar(10) NOT NULL DEFAULT 'str' COMMENT '值类型: int=整数, float=浮点数, bool=布尔值, str=字符串, json=JSON对象',
  `description` text COMMENT '配置说明',
  `is_active` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否启用: 0=停用, 1=启用',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_module_key` (`module`,`key`),
  KEY `idx_module` (`module`),
  KEY `idx_is_active` (`is_active`),
  KEY `idx_module_active` (`module`,`is_active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='RAG系统配置表';

-- 17. 新闻数据源配置表
CREATE TABLE IF NOT EXISTS `kb_news_source` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(100) NOT NULL COMMENT '数据源名称',
  `db_host` varchar(255) NOT NULL COMMENT '外部数据库主机',
  `db_port` int NOT NULL DEFAULT '3306' COMMENT '外部数据库端口',
  `db_name` varchar(100) NOT NULL COMMENT '外部数据库名称',
  `db_user` varchar(100) NOT NULL COMMENT '数据库用户名',
  `db_password` varchar(255) NOT NULL COMMENT '数据库密码',
  `table_name` varchar(100) NOT NULL COMMENT '外部表名',
  `id_field` varchar(100) NOT NULL DEFAULT 'id' COMMENT 'ID字段名',
  `title_field` varchar(100) NOT NULL DEFAULT 'doctitle' COMMENT '标题字段名',
  `content_field` varchar(100) NOT NULL DEFAULT 'doccontent' COMMENT '内容字段名',
  `channel_field` varchar(100) DEFAULT NULL COMMENT '频道字段名',
  `time_field` varchar(100) DEFAULT NULL COMMENT '时间字段名',
  `url_field` varchar(100) DEFAULT NULL COMMENT 'URL字段名',
  `crdept_field` varchar(100) DEFAULT NULL COMMENT '部门字段名',
  `cruser_field` varchar(100) DEFAULT NULL COMMENT '用户字段名',
  `sync_enabled` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否启用同步: 0=停用, 1=启用',
  `sync_interval` int DEFAULT '3600' COMMENT '同步间隔(秒)',
  `db_dept` bigint DEFAULT NULL COMMENT '所属部门ID',
  `last_sync_time` datetime DEFAULT NULL COMMENT '上次同步时间',
  `total_records` int NOT NULL DEFAULT '0' COMMENT '同步总记录数',
  `processed_records` int NOT NULL DEFAULT '0' COMMENT '已处理记录数',
  `error_count` int NOT NULL DEFAULT '0' COMMENT '错误数',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_sync_enabled` (`sync_enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='新闻数据源配置表';

-- 18. 新闻记录缓存表
CREATE TABLE IF NOT EXISTS `kb_news_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `source_id` bigint NOT NULL COMMENT '数据源ID',
  `external_id` varchar(100) NOT NULL COMMENT '外部记录ID',
  `external_title` varchar(500) DEFAULT NULL COMMENT '外部标题',
  `external_content` longtext COMMENT '外部内容',
  `external_channel` varchar(100) DEFAULT NULL COMMENT '频道',
  `external_time` varchar(50) DEFAULT NULL COMMENT '外部时间',
  `external_url` varchar(500) DEFAULT NULL COMMENT '外部URL',
  `external_crdept` varchar(200) DEFAULT NULL COMMENT '创建部门',
  `external_cruser` varchar(100) DEFAULT NULL COMMENT '创建用户',
  `status` varchar(20) NOT NULL DEFAULT 'pending' COMMENT '状态: pending=待处理, completed=已完成, failed=失败, skipped=已跳过',
  `processing_status` varchar(50) DEFAULT NULL COMMENT '处理阶段描述',
  `error_message` text COMMENT '错误信息',
  `retry_count` int NOT NULL DEFAULT '0' COMMENT '重试次数',
  `doc_id` bigint DEFAULT NULL COMMENT '关联文档ID',
  `kb_id` bigint DEFAULT NULL COMMENT '关联知识库ID',
  `file_url` varchar(512) DEFAULT NULL COMMENT '文件访问URL',
  `file_type` varchar(32) DEFAULT NULL COMMENT '文件类型',
  `last_processed_at` datetime DEFAULT NULL COMMENT '上次处理时间',
  `processed_at` datetime DEFAULT NULL COMMENT '处理完成时间',
  `external_updated_at` datetime DEFAULT NULL COMMENT '外部更新时间',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_source_external` (`source_id`,`external_id`),
  KEY `idx_source_id` (`source_id`),
  KEY `idx_status` (`status`),
  KEY `idx_external_channel` (`external_channel`),
  KEY `idx_external_time` (`external_time`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='新闻记录缓存表';

-- 19. 新闻同步日志表
CREATE TABLE IF NOT EXISTS `kb_news_sync_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `source_id` bigint NOT NULL COMMENT '数据源ID',
  `sync_type` varchar(20) NOT NULL COMMENT '同步类型: full=全量, incremental=增量, manual=手动',
  `status` varchar(20) NOT NULL DEFAULT 'started' COMMENT '状态: started=已开始, running=运行中, completed=已完成, failed=失败',
  `total_fetched` int NOT NULL DEFAULT '0' COMMENT '获取总数',
  `new_records` int NOT NULL DEFAULT '0' COMMENT '新增记录数',
  `updated_records` int NOT NULL DEFAULT '0' COMMENT '更新记录数',
  `skipped_records` int NOT NULL DEFAULT '0' COMMENT '跳过记录数',
  `failed_records` int NOT NULL DEFAULT '0' COMMENT '失败记录数',
  `started_at` datetime DEFAULT NULL COMMENT '开始时间',
  `completed_at` datetime DEFAULT NULL COMMENT '完成时间',
  `error_message` text COMMENT '错误信息',
  `details` json DEFAULT NULL COMMENT '详细信息(JSON)',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_source_id` (`source_id`),
  KEY `idx_status` (`status`),
  KEY `idx_sync_type` (`sync_type`),
  KEY `idx_started_at` (`started_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='新闻同步日志表';

-- 20. 知识库自定义字段值表
CREATE TABLE IF NOT EXISTS `kb_library_ext` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `kb_id` bigint NOT NULL COMMENT '知识库ID',
  `field_key` varchar(64) NOT NULL COMMENT '字段key（来自分类 column_config 的自定义字段定义）',
  `field_value` text COMMENT '字段值（文本；成员多选为 JSON 数组字符串；部门/日期/数字/下拉为字符串）',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_kb_field` (`kb_id`,`field_key`),
  KEY `idx_kb_id` (`kb_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库自定义字段值表';