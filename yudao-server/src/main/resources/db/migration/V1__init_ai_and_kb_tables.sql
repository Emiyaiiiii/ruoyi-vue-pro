-- =====================================================
-- V1: AI 智能体模块 + 知识库模块 全量表结构
-- 说明: 结构以 docker MySQL (dvadmin3-mysql) 实际表为准导出，
--      与 DO 实体类一致；使用 CREATE TABLE IF NOT EXISTS 保证幂等。
-- =====================================================

-- =====================================================
-- 第一部分：AI 智能体管理模块 (ai-agent)
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
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_qwenpaw_agent_id` (`qwenpaw_agent_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='智能体实例表';

-- 2. 系统级 MCP 商店表
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

-- 3. 技能商店表（QwenPaw 技能池元数据影子表）
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
-- 第二部分：知识库模块 (kb)
-- =====================================================

-- 1. 知识库层级配置表
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

-- 2. 知识库分类表
CREATE TABLE IF NOT EXISTS `kb_category` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(100) NOT NULL COMMENT '分类名称',
  `parent_id` bigint DEFAULT '0' COMMENT '父分类ID: 0=顶级分类',
  `kb_level_id` bigint DEFAULT NULL COMMENT '关联层级配置ID',
  `sort` int DEFAULT '0' COMMENT '排序',
  `status` tinyint DEFAULT '0' COMMENT '状态: 0=启用, 1=禁用',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint DEFAULT '0' COMMENT '逻辑删除: 0=未删除, 1=已删除',
  `tenant_id` bigint DEFAULT '0' COMMENT '租户ID: 0=默认租户',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库分类';

-- 3. 知识库表
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

-- 4. 文档文件夹表
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

-- 5. 知识库文档表
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

-- 6. 知识库用户部门关联表
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

-- 7. 知识库共享部门关联表
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

-- 8. 知识库项目成员表
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

-- 9. 知识库关注表
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

-- 10. 向量处理任务表
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
  `celery_task_id` varchar(128) NOT NULL DEFAULT '' COMMENT 'Celery任务ID',
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

-- 11. 切片方法表
CREATE TABLE IF NOT EXISTS `kb_chunk_method` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(100) NOT NULL COMMENT '方法名称',
  `method_type` varchar(20) NOT NULL COMMENT '方法类型: fixed_size=固定大小, semantic=语义分段, hierarchical=层次分段, recursive=递归分割, sentence=按句子, paragraph=按段落, section=按章节, custom=自定义',
  `description` varchar(500) DEFAULT NULL COMMENT '方法描述',
  `code` varchar(50) NOT NULL COMMENT '方法代码(如 fixed_size)',
  `parameters_template` text COMMENT '参数模板(JSON Schema格式)',
  `default_parameters` text COMMENT '默认参数(JSON格式)',
  `handler_class` varchar(200) DEFAULT NULL COMMENT '处理器类全路径',
  `is_active` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否启用: 0=停用, 1=启用',
  `is_default_method` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否默认切片方法',
  `avg_processing_speed` double NOT NULL DEFAULT '1' COMMENT '平均处理速度(千字/秒)',
  `memory_footprint` int NOT NULL DEFAULT '100' COMMENT '内存占用(MB)',
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

-- 12. 大模型配置表
CREATE TABLE IF NOT EXISTS `kb_model_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `uid` varchar(100) NOT NULL COMMENT '模型唯一标识',
  `name` varchar(100) NOT NULL COMMENT '模型名称',
  `url` varchar(500) NOT NULL COMMENT 'API地址',
  `appkey` varchar(500) NOT NULL COMMENT 'API密钥',
  `deploy` varchar(50) NOT NULL DEFAULT 'doubao' COMMENT '部署类型: doubao=豆包, bailian=百炼, lite=LiteLLM, openai=OpenAI, api=通用API, xinf=Xinference, vllm=VLLM, zhipu=智谱AI, other=其他',
  `thinking_enabled` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否启用思考能力',
  `is_active` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否激活: 0=停用, 1=激活',
  `description` varchar(500) DEFAULT NULL COMMENT '模型描述',
  `max_tokens` int NOT NULL DEFAULT '4096' COMMENT '最大Token数',
  `context_length` int NOT NULL DEFAULT '8192' COMMENT '上下文长度',
  `temperature` double NOT NULL DEFAULT '0.7' COMMENT '温度参数',
  `top_p` double NOT NULL DEFAULT '0.9' COMMENT 'Top-P参数',
  `metadata` varchar(2000) DEFAULT '{}' COMMENT '元数据(JSON格式)',
  `config` varchar(2000) DEFAULT '{}' COMMENT '配置参数(JSON格式)',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序顺序(升序)',
  `is_pinned` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否置顶: 0=否, 1=是',
  `platform` varchar(20) NOT NULL DEFAULT 'both' COMMENT '支持平台: web=Web端, app=App端, both=两者都支持',
  `activated_at` datetime DEFAULT NULL COMMENT '激活时间',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_uid` (`uid`),
  KEY `idx_deploy` (`deploy`),
  KEY `idx_is_active` (`is_active`),
  KEY `idx_is_pinned` (`is_pinned`),
  KEY `idx_sort_order` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='大模型配置信息表';

-- 13. RAG 系统配置表
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

-- 14. 新闻数据源表
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

-- 15. 新闻记录表
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

-- 16. 新闻同步日志表
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

-- =====================================================
-- 第三部分：智能体管理 菜单与字典数据（幂等插入）
-- 说明: 所有 INSERT 均使用 NOT EXISTS 判断，重复执行安全
-- =====================================================

-- 1. 智能体管理 (一级菜单)
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT '智能体管理', '', 1, 8, 0, '/ai-agent', 'ep:chat-dot-round', '', 'AiAgent', 0, 1, 1, 1, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `name` = '智能体管理' AND `type` = 1 AND `deleted` = 0);

SELECT id INTO @ai_agent_parent_id FROM `system_menu` WHERE `name` = '智能体管理' AND `type` = 1 AND `deleted` = 0 LIMIT 1;

-- 2. 我的智能体 (菜单)
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT '我的智能体', '', 2, 1, @ai_agent_parent_id, 'agent', 'ep:avatar', 'ai/agent/index', 'AiAgentIndex', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `name` = '我的智能体' AND `type` = 2 AND `parent_id` = @ai_agent_parent_id AND `deleted` = 0);

SELECT id INTO @ai_agent_menu_id FROM `system_menu` WHERE `name` = '我的智能体' AND `type` = 2 AND `parent_id` = @ai_agent_parent_id AND `deleted` = 0 LIMIT 1;

-- 3. 我的智能体 按钮权限
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT '智能体查询', 'ai-agent:agent:query', 3, 1, @ai_agent_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'ai-agent:agent:query' AND `deleted` = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT '智能体创建', 'ai-agent:agent:create', 3, 2, @ai_agent_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'ai-agent:agent:create' AND `deleted` = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT '智能体更新', 'ai-agent:agent:update', 3, 3, @ai_agent_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'ai-agent:agent:update' AND `deleted` = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT '智能体删除', 'ai-agent:agent:delete', 3, 4, @ai_agent_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'ai-agent:agent:delete' AND `deleted` = 0);

-- 4. 技能商店 (菜单)
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT '技能商店', '', 2, 2, @ai_agent_parent_id, 'skill-store', 'ep:magic-stick', 'ai/skillmeta/index', 'AiSkillMetaIndex', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `name` = '技能商店' AND `type` = 2 AND `parent_id` = @ai_agent_parent_id AND `deleted` = 0);

SELECT id INTO @ai_skill_menu_id FROM `system_menu` WHERE `name` = '技能商店' AND `type` = 2 AND `parent_id` = @ai_agent_parent_id AND `deleted` = 0 LIMIT 1;

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT '技能查询', 'ai-agent:skill-meta:query', 3, 1, @ai_skill_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'ai-agent:skill-meta:query' AND `deleted` = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT '技能创建', 'ai-agent:skill-meta:create', 3, 2, @ai_skill_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'ai-agent:skill-meta:create' AND `deleted` = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT '技能更新', 'ai-agent:skill-meta:update', 3, 3, @ai_skill_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'ai-agent:skill-meta:update' AND `deleted` = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT '技能删除', 'ai-agent:skill-meta:delete', 3, 4, @ai_skill_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'ai-agent:skill-meta:delete' AND `deleted` = 0);

-- 5. MCP 商店 (菜单)
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT 'MCP商店', '', 2, 3, @ai_agent_parent_id, 'mcp-store', 'ep:hard-drive', 'ai/mcpmeta/index', 'AiMcpMetaIndex', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `name` = 'MCP商店' AND `type` = 2 AND `parent_id` = @ai_agent_parent_id AND `deleted` = 0);

SELECT id INTO @ai_mcp_menu_id FROM `system_menu` WHERE `name` = 'MCP商店' AND `type` = 2 AND `parent_id` = @ai_agent_parent_id AND `deleted` = 0 LIMIT 1;

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT 'MCP查询', 'ai-agent:mcp-meta:query', 3, 1, @ai_mcp_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'ai-agent:mcp-meta:query' AND `deleted` = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT 'MCP创建', 'ai-agent:mcp-meta:create', 3, 2, @ai_mcp_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'ai-agent:mcp-meta:create' AND `deleted` = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT 'MCP更新', 'ai-agent:mcp-meta:update', 3, 3, @ai_mcp_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'ai-agent:mcp-meta:update' AND `deleted` = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT 'MCP删除', 'ai-agent:mcp-meta:delete', 3, 4, @ai_mcp_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'ai-agent:mcp-meta:delete' AND `deleted` = 0);

-- 6. 问答会话 (菜单)
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT '问答会话', '', 2, 4, @ai_agent_parent_id, 'chat-session', 'ep:chat-line-round', 'ai/chatsession/index', 'AiChatSessionIndex', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `name` = '问答会话' AND `type` = 2 AND `parent_id` = @ai_agent_parent_id AND `deleted` = 0);

SELECT id INTO @ai_chat_menu_id FROM `system_menu` WHERE `name` = '问答会话' AND `type` = 2 AND `parent_id` = @ai_agent_parent_id AND `deleted` = 0 LIMIT 1;

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT '会话查询', 'ai-agent:chat-session:query', 3, 1, @ai_chat_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'ai-agent:chat-session:query' AND `deleted` = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT '会话创建', 'ai-agent:chat-session:create', 3, 2, @ai_chat_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'ai-agent:chat-session:create' AND `deleted` = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT '会话更新', 'ai-agent:chat-session:update', 3, 3, @ai_chat_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'ai-agent:chat-session:update' AND `deleted` = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT '会话删除', 'ai-agent:chat-session:delete', 3, 4, @ai_chat_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'ai-agent:chat-session:delete' AND `deleted` = 0);

-- 6. 模型管理 (菜单)
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT '模型管理', '', 2, 5, @ai_agent_parent_id, 'model-provider', 'ep:cpu', 'ai/model/provider/index', 'AiModelProvider', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `name` = '模型管理' AND `type` = 2 AND `parent_id` = @ai_agent_parent_id AND `deleted` = 0);

SELECT id INTO @ai_model_menu_id FROM `system_menu` WHERE `name` = '模型管理' AND `type` = 2 AND `parent_id` = @ai_agent_parent_id AND `deleted` = 0 LIMIT 1;

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT '模型查询', 'ai-agent:model:query', 3, 1, @ai_model_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'ai-agent:model:query' AND `deleted` = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT '模型创建', 'ai-agent:model:create', 3, 2, @ai_model_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'ai-agent:model:create' AND `deleted` = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT '模型更新', 'ai-agent:model:update', 3, 3, @ai_model_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'ai-agent:model:update' AND `deleted` = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT '模型删除', 'ai-agent:model:delete', 3, 4, @ai_model_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'ai-agent:model:delete' AND `deleted` = 0);

-- 7. Token 用量统计 (菜单)
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT 'Token用量', '', 2, 6, @ai_agent_parent_id, 'token-usage', 'ep:data-analysis', 'ai/tokenUsage/index', 'AiTokenUsageIndex', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `name` = 'Token用量' AND `type` = 2 AND `parent_id` = @ai_agent_parent_id AND `deleted` = 0);

SELECT id INTO @ai_token_usage_menu_id FROM `system_menu` WHERE `name` = 'Token用量' AND `type` = 2 AND `parent_id` = @ai_agent_parent_id AND `deleted` = 0 LIMIT 1;

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT 'Token用量查询', 'ai-agent:token-usage:query', 3, 1, @ai_token_usage_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'ai-agent:token-usage:query' AND `deleted` = 0);

-- =====================================================
-- 字典数据（幂等插入）
-- =====================================================

-- 智能体状态
INSERT INTO `system_dict_type` (`name`, `type`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '智能体状态', 'ai_agent_status', 0, '智能体管理-智能体状态', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_type` WHERE `type` = 'ai_agent_status' AND `deleted` = 0);

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 1, '启用', '1', 'ai_agent_status', 0, 'success', '', '智能体-启用', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` WHERE `dict_type` = 'ai_agent_status' AND `value` = '1' AND `deleted` = 0);

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 2, '停用', '0', 'ai_agent_status', 0, 'info', '', '智能体-停用', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` WHERE `dict_type` = 'ai_agent_status' AND `value` = '0' AND `deleted` = 0);

-- MCP 传输协议
INSERT INTO `system_dict_type` (`name`, `type`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 'MCP传输协议', 'ai_mcp_transport', 0, '智能体管理-MCP传输协议', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_type` WHERE `type` = 'ai_mcp_transport' AND `deleted` = 0);

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 1, '标准输入输出', 'stdio', 'ai_mcp_transport', 0, 'primary', '', 'MCP-stdio', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` WHERE `dict_type` = 'ai_mcp_transport' AND `value` = 'stdio' AND `deleted` = 0);

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 2, '流式HTTP', 'streamable_http', 'ai_mcp_transport', 0, 'success', '', 'MCP-streamable_http', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` WHERE `dict_type` = 'ai_mcp_transport' AND `value` = 'streamable_http' AND `deleted` = 0);

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 3, 'SSE', 'sse', 'ai_mcp_transport', 0, 'warning', '', 'MCP-sse', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` WHERE `dict_type` = 'ai_mcp_transport' AND `value` = 'sse' AND `deleted` = 0);

-- MCP 类型
INSERT INTO `system_dict_type` (`name`, `type`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 'MCP类型', 'ai_mcp_type', 0, '智能体管理-MCP类型', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_type` WHERE `type` = 'ai_mcp_type' AND `deleted` = 0);

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 1, '系统级', '0', 'ai_mcp_type', 0, 'primary', '', 'MCP-系统级', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` WHERE `dict_type` = 'ai_mcp_type' AND `value` = '0' AND `deleted` = 0);

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 2, '用户级', '1', 'ai_mcp_type', 0, 'warning', '', 'MCP-用户级', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` WHERE `dict_type` = 'ai_mcp_type' AND `value` = '1' AND `deleted` = 0);

-- 技能来源类型
INSERT INTO `system_dict_type` (`name`, `type`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '技能来源类型', 'ai_skill_source_type', 0, '智能体管理-技能来源类型', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_type` WHERE `type` = 'ai_skill_source_type' AND `deleted` = 0);

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 1, 'Git仓库', 'git', 'ai_skill_source_type', 0, 'primary', '', '技能-Git', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` WHERE `dict_type` = 'ai_skill_source_type' AND `value` = 'git' AND `deleted` = 0);

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 2, '上传压缩包', 'upload', 'ai_skill_source_type', 0, 'success', '', '技能-上传', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` WHERE `dict_type` = 'ai_skill_source_type' AND `value` = 'upload' AND `deleted` = 0);

-- 消息角色
INSERT INTO `system_dict_type` (`name`, `type`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '问答消息角色', 'ai_chat_role', 0, '智能体管理-问答消息角色', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_type` WHERE `type` = 'ai_chat_role' AND `deleted` = 0);

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 1, '用户', 'user', 'ai_chat_role', 0, 'primary', '', '消息-用户', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` WHERE `dict_type` = 'ai_chat_role' AND `value` = 'user' AND `deleted` = 0);

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 2, '助手', 'assistant', 'ai_chat_role', 0, 'success', '', '消息-助手', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` WHERE `dict_type` = 'ai_chat_role' AND `value` = 'assistant' AND `deleted` = 0);

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 3, '工具', 'tool', 'ai_chat_role', 0, 'warning', '', '消息-工具', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` WHERE `dict_type` = 'ai_chat_role' AND `value` = 'tool' AND `deleted` = 0);

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 4, '系统', 'system', 'ai_chat_role', 0, 'info', '', '消息-系统', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` WHERE `dict_type` = 'ai_chat_role' AND `value` = 'system' AND `deleted` = 0);

-- =====================================================
-- 第四部分：知识库管理 菜单与字典数据（幂等插入）
-- =====================================================

-- 1. 知识库管理 (一级菜单)
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT '知识库管理', '', 1, 25, 0, '/kb', 'carbon:data-set-encryption', '', NULL, 0, 1, 1, 1, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `name` = '知识库管理' AND `type` = 1 AND `deleted` = 0);

SELECT id INTO @kb_menu_id FROM `system_menu` WHERE `name` = '知识库管理' AND `type` = 1 AND `deleted` = 0 LIMIT 1;

-- 2. 知识库总览 (一级菜单，type=2 但 parent=0)
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT '知识库总览', '', 2, 27, 0, '/kb-overview', 'ep:files', '/kb/overview/index', '', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `name` = '知识库总览' AND `type` = 2 AND `parent_id` = 0 AND `deleted` = 0);

-- 3. 知识库二级菜单
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT '层级配置', '', 2, 0, @kb_menu_id, 'level-config', '', 'kb/levelconfig/index', 'LevelConfig', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `name` = '层级配置' AND `type` = 2 AND `parent_id` = @kb_menu_id AND `deleted` = 0);

SELECT id INTO @kb_level_menu_id FROM `system_menu` WHERE `name` = '层级配置' AND `type` = 2 AND `parent_id` = @kb_menu_id AND `deleted` = 0 LIMIT 1;

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT '分类管理', '', 2, 1, @kb_menu_id, 'category', '', 'kb/category/index', 'Category', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `name` = '分类管理' AND `type` = 2 AND `parent_id` = @kb_menu_id AND `deleted` = 0);

SELECT id INTO @kb_category_menu_id FROM `system_menu` WHERE `name` = '分类管理' AND `type` = 2 AND `parent_id` = @kb_menu_id AND `deleted` = 0 LIMIT 1;

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT '共享部门管理', '', 2, 2, @kb_menu_id, 'share-dept', '', 'kb/sharedept/index', 'ShareDept', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `name` = '共享部门管理' AND `type` = 2 AND `parent_id` = @kb_menu_id AND `deleted` = 0);

SELECT id INTO @kb_share_menu_id FROM `system_menu` WHERE `name` = '共享部门管理' AND `type` = 2 AND `parent_id` = @kb_menu_id AND `deleted` = 0 LIMIT 1;

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT '知识库管理', '', 2, 3, @kb_menu_id, 'library', '', 'kb/library/index', 'Library', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `name` = '知识库管理' AND `type` = 2 AND `parent_id` = @kb_menu_id AND `deleted` = 0);

SELECT id INTO @kb_library_menu_id FROM `system_menu` WHERE `name` = '知识库管理' AND `type` = 2 AND `parent_id` = @kb_menu_id AND `deleted` = 0 LIMIT 1;

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT '文档管理', '', 2, 5, @kb_menu_id, 'document', '', 'kb/document/index', 'Document', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `name` = '文档管理' AND `type` = 2 AND `parent_id` = @kb_menu_id AND `deleted` = 0);

SELECT id INTO @kb_document_menu_id FROM `system_menu` WHERE `name` = '文档管理' AND `type` = 2 AND `parent_id` = @kb_menu_id AND `deleted` = 0 LIMIT 1;

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT '切片方法', '', 2, 6, @kb_menu_id, 'chunk-method', '', 'kb/chunkmethod/index', 'KbChunkMethod', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `name` = '切片方法' AND `type` = 2 AND `parent_id` = @kb_menu_id AND `deleted` = 0);

SELECT id INTO @kb_chunk_menu_id FROM `system_menu` WHERE `name` = '切片方法' AND `type` = 2 AND `parent_id` = @kb_menu_id AND `deleted` = 0 LIMIT 1;

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT '部门成员管理', '', 2, 6, @kb_menu_id, 'user-dept', '', 'kb/userdept/index', 'UserDept', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `name` = '部门成员管理' AND `type` = 2 AND `parent_id` = @kb_menu_id AND `deleted` = 0);

SELECT id INTO @kb_userdept_menu_id FROM `system_menu` WHERE `name` = '部门成员管理' AND `type` = 2 AND `parent_id` = @kb_menu_id AND `deleted` = 0 LIMIT 1;

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT 'RAG配置', '', 2, 7, @kb_menu_id, 'rag-config', '', 'kb/ragconfig/index', 'KbRagConfig', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `name` = 'RAG配置' AND `type` = 2 AND `parent_id` = @kb_menu_id AND `deleted` = 0);

SELECT id INTO @kb_rag_menu_id FROM `system_menu` WHERE `name` = 'RAG配置' AND `type` = 2 AND `parent_id` = @kb_menu_id AND `deleted` = 0 LIMIT 1;

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT '项目成员管理', '', 2, 7, @kb_menu_id, 'project-member', '', 'kb/projectmember/index', 'ProjectMember', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `name` = '项目成员管理' AND `type` = 2 AND `parent_id` = @kb_menu_id AND `deleted` = 0);

SELECT id INTO @kb_project_menu_id FROM `system_menu` WHERE `name` = '项目成员管理' AND `type` = 2 AND `parent_id` = @kb_menu_id AND `deleted` = 0 LIMIT 1;

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT '模型配置', '', 2, 8, @kb_menu_id, 'model-config', '', 'kb/modelconfig/index', 'KbModelConfig', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `name` = '模型配置' AND `type` = 2 AND `parent_id` = @kb_menu_id AND `deleted` = 0);

SELECT id INTO @kb_model_menu_id FROM `system_menu` WHERE `name` = '模型配置' AND `type` = 2 AND `parent_id` = @kb_menu_id AND `deleted` = 0 LIMIT 1;

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT '新闻管理', '', 2, 8, @kb_menu_id, 'news', '', 'kb/news/index', 'KbNews', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `name` = '新闻管理' AND `type` = 2 AND `parent_id` = @kb_menu_id AND `deleted` = 0);

SELECT id INTO @kb_news_menu_id FROM `system_menu` WHERE `name` = '新闻管理' AND `type` = 2 AND `parent_id` = @kb_menu_id AND `deleted` = 0 LIMIT 1;

-- 4. 层级配置 按钮
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT '查询', 'kb:level-config:query', 3, 1, @kb_level_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'kb:level-config:query' AND `deleted` = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT '创建', 'kb:level-config:create', 3, 2, @kb_level_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'kb:level-config:create' AND `deleted` = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT '更新', 'kb:level-config:update', 3, 3, @kb_level_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'kb:level-config:update' AND `deleted` = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT '删除', 'kb:level-config:delete', 3, 4, @kb_level_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'kb:level-config:delete' AND `deleted` = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT '导出', 'kb:level-config:export', 3, 5, @kb_level_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'kb:level-config:export' AND `deleted` = 0);

-- 5. 分类管理 按钮
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT '查询', 'kb:category:query', 3, 1, @kb_category_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'kb:category:query' AND `deleted` = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT '创建', 'kb:category:create', 3, 2, @kb_category_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'kb:category:create' AND `deleted` = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT '更新', 'kb:category:update', 3, 3, @kb_category_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'kb:category:update' AND `deleted` = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT '删除', 'kb:category:delete', 3, 4, @kb_category_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'kb:category:delete' AND `deleted` = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT '导出', 'kb:category:export', 3, 5, @kb_category_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'kb:category:export' AND `deleted` = 0);

-- 6. 共享部门管理 按钮
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT '查询', 'kb:share-dept:query', 3, 1, @kb_share_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'kb:share-dept:query' AND `deleted` = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT '创建', 'kb:share-dept:create', 3, 2, @kb_share_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'kb:share-dept:create' AND `deleted` = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT '更新', 'kb:share-dept:update', 3, 3, @kb_share_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'kb:share-dept:update' AND `deleted` = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT '删除', 'kb:share-dept:delete', 3, 4, @kb_share_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'kb:share-dept:delete' AND `deleted` = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT '导出', 'kb:share-dept:export', 3, 5, @kb_share_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'kb:share-dept:export' AND `deleted` = 0);

-- 7. 知识库管理 按钮
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT '查询', 'kb:library:query', 3, 1, @kb_library_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'kb:library:query' AND `deleted` = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT '创建', 'kb:library:create', 3, 2, @kb_library_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'kb:library:create' AND `deleted` = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT '更新', 'kb:library:update', 3, 3, @kb_library_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'kb:library:update' AND `deleted` = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT '删除', 'kb:library:delete', 3, 4, @kb_library_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'kb:library:delete' AND `deleted` = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT '导出', 'kb:library:export', 3, 5, @kb_library_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'kb:library:export' AND `deleted` = 0);

-- 8. 文档管理 按钮
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT '查询', 'kb:document:query', 3, 1, @kb_document_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'kb:document:query' AND `deleted` = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT '创建', 'kb:document:create', 3, 2, @kb_document_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'kb:document:create' AND `deleted` = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT '更新', 'kb:document:update', 3, 3, @kb_document_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'kb:document:update' AND `deleted` = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT '删除', 'kb:document:delete', 3, 4, @kb_document_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'kb:document:delete' AND `deleted` = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT '导出', 'kb:document:export', 3, 5, @kb_document_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'kb:document:export' AND `deleted` = 0);

-- 9. 切片方法 按钮
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT '切片方法查询', 'kb:chunk-method:query', 3, 1, @kb_chunk_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'kb:chunk-method:query' AND `deleted` = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT '切片方法创建', 'kb:chunk-method:create', 3, 2, @kb_chunk_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'kb:chunk-method:create' AND `deleted` = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT '切片方法更新', 'kb:chunk-method:update', 3, 3, @kb_chunk_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'kb:chunk-method:update' AND `deleted` = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT '切片方法删除', 'kb:chunk-method:delete', 3, 4, @kb_chunk_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'kb:chunk-method:delete' AND `deleted` = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT '切片方法测试', 'kb:chunk-method:test', 3, 5, @kb_chunk_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'kb:chunk-method:test' AND `deleted` = 0);

-- 10. 部门成员管理 按钮
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT '查询', 'kb:user-dept:query', 3, 1, @kb_userdept_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'kb:user-dept:query' AND `deleted` = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT '更新', 'kb:user-dept:update', 3, 2, @kb_userdept_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'kb:user-dept:update' AND `deleted` = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT '删除', 'kb:user-dept:delete', 3, 3, @kb_userdept_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'kb:user-dept:delete' AND `deleted` = 0);

-- 11. RAG配置 按钮
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT 'RAG配置查询', 'kb:rag-config:query', 3, 1, @kb_rag_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'kb:rag-config:query' AND `deleted` = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT 'RAG配置创建', 'kb:rag-config:create', 3, 2, @kb_rag_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'kb:rag-config:create' AND `deleted` = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT 'RAG配置更新', 'kb:rag-config:update', 3, 3, @kb_rag_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'kb:rag-config:update' AND `deleted` = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT 'RAG配置删除', 'kb:rag-config:delete', 3, 4, @kb_rag_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'kb:rag-config:delete' AND `deleted` = 0);

-- 12. 项目成员管理 按钮
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT '查询', 'kb:project-member:query', 3, 1, @kb_project_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'kb:project-member:query' AND `deleted` = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT '更新', 'kb:project-member:update', 3, 2, @kb_project_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'kb:project-member:update' AND `deleted` = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT '删除', 'kb:project-member:delete', 3, 3, @kb_project_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'kb:project-member:delete' AND `deleted` = 0);

-- 13. 模型配置 按钮
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT '模型配置查询', 'kb:model-config:query', 3, 1, @kb_model_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'kb:model-config:query' AND `deleted` = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT '模型配置创建', 'kb:model-config:create', 3, 2, @kb_model_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'kb:model-config:create' AND `deleted` = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT '模型配置更新', 'kb:model-config:update', 3, 3, @kb_model_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'kb:model-config:update' AND `deleted` = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT '模型配置删除', 'kb:model-config:delete', 3, 4, @kb_model_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'kb:model-config:delete' AND `deleted` = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT '模型配置导出', 'kb:model-config:export', 3, 5, @kb_model_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'kb:model-config:export' AND `deleted` = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT '模型配置测试', 'kb:model-config:test', 3, 6, @kb_model_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'kb:model-config:test' AND `deleted` = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT '模型配置复制', 'kb:model-config:copy', 3, 7, @kb_model_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'kb:model-config:copy' AND `deleted` = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT '模型配置批量操作', 'kb:model-config:batch', 3, 8, @kb_model_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'kb:model-config:batch' AND `deleted` = 0);

-- 14. 新闻管理 按钮
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT '新闻数据源查询', 'kb:news-source:query', 3, 1, @kb_news_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'kb:news-source:query' AND `deleted` = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT '新闻数据源创建', 'kb:news-source:create', 3, 2, @kb_news_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'kb:news-source:create' AND `deleted` = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT '新闻数据源更新', 'kb:news-source:update', 3, 3, @kb_news_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'kb:news-source:update' AND `deleted` = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT '新闻数据源删除', 'kb:news-source:delete', 3, 4, @kb_news_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'kb:news-source:delete' AND `deleted` = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT '新闻记录查询', 'kb:news-record:query', 3, 5, @kb_news_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'kb:news-record:query' AND `deleted` = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT '新闻记录批量操作', 'kb:news-record:batch', 3, 6, @kb_news_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'kb:news-record:batch' AND `deleted` = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
SELECT '新闻同步日志查询', 'kb:news-sync-log:query', 3, 7, @kb_news_menu_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'kb:news-sync-log:query' AND `deleted` = 0);

-- =====================================================
-- 知识库 字典数据（幂等插入）
-- =====================================================

-- 切片方法类型
INSERT INTO `system_dict_type` (`name`, `type`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '切片方法类型', 'kb_chunk_method_type', 0, '知识库-切片方法类型', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_type` WHERE `type` = 'kb_chunk_method_type' AND `deleted` = 0);

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 1, '固定大小', 'fixed_size', 'kb_chunk_method_type', 0, '', '', '', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` WHERE `dict_type` = 'kb_chunk_method_type' AND `value` = 'fixed_size' AND `deleted` = 0);

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 2, '语义分段', 'semantic', 'kb_chunk_method_type', 0, 'success', '', '', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` WHERE `dict_type` = 'kb_chunk_method_type' AND `value` = 'semantic' AND `deleted` = 0);

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 3, '层次分段', 'hierarchical', 'kb_chunk_method_type', 0, 'warning', '', '', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` WHERE `dict_type` = 'kb_chunk_method_type' AND `value` = 'hierarchical' AND `deleted` = 0);

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 4, '递归分割', 'recursive', 'kb_chunk_method_type', 0, 'primary', '', '', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` WHERE `dict_type` = 'kb_chunk_method_type' AND `value` = 'recursive' AND `deleted` = 0);

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 5, '按句子', 'sentence', 'kb_chunk_method_type', 0, 'info', '', '', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` WHERE `dict_type` = 'kb_chunk_method_type' AND `value` = 'sentence' AND `deleted` = 0);

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 6, '按段落', 'paragraph', 'kb_chunk_method_type', 0, 'danger', '', '', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` WHERE `dict_type` = 'kb_chunk_method_type' AND `value` = 'paragraph' AND `deleted` = 0);

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 7, '按章节', 'section', 'kb_chunk_method_type', 0, '', '', '', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` WHERE `dict_type` = 'kb_chunk_method_type' AND `value` = 'section' AND `deleted` = 0);

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 8, '自定义', 'custom', 'kb_chunk_method_type', 0, '', '', '', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` WHERE `dict_type` = 'kb_chunk_method_type' AND `value` = 'custom' AND `deleted` = 0);

-- 模型部署类型
INSERT INTO `system_dict_type` (`name`, `type`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '模型部署类型', 'kb_model_deploy_type', 0, '知识库-模型部署类型', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_type` WHERE `type` = 'kb_model_deploy_type' AND `deleted` = 0);

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 1, '豆包', 'doubao', 'kb_model_deploy_type', 0, 'warning', '', '', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` WHERE `dict_type` = 'kb_model_deploy_type' AND `value` = 'doubao' AND `deleted` = 0);

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 2, '百炼', 'bailian', 'kb_model_deploy_type', 0, 'danger', '', '', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` WHERE `dict_type` = 'kb_model_deploy_type' AND `value` = 'bailian' AND `deleted` = 0);

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 3, 'LiteLLM', 'lite', 'kb_model_deploy_type', 0, 'success', '', '', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` WHERE `dict_type` = 'kb_model_deploy_type' AND `value` = 'lite' AND `deleted` = 0);

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 4, 'OpenAI', 'openai', 'kb_model_deploy_type', 0, 'success', '', '', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` WHERE `dict_type` = 'kb_model_deploy_type' AND `value` = 'openai' AND `deleted` = 0);

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 5, '通用API', 'api', 'kb_model_deploy_type', 0, 'info', '', '', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` WHERE `dict_type` = 'kb_model_deploy_type' AND `value` = 'api' AND `deleted` = 0);

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 6, 'Xinference', 'xinf', 'kb_model_deploy_type', 0, '', '', '', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` WHERE `dict_type` = 'kb_model_deploy_type' AND `value` = 'xinf' AND `deleted` = 0);

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 7, 'VLLM', 'vllm', 'kb_model_deploy_type', 0, '', '', '', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` WHERE `dict_type` = 'kb_model_deploy_type' AND `value` = 'vllm' AND `deleted` = 0);

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 8, '智谱AI', 'zhipu', 'kb_model_deploy_type', 0, 'primary', '', '', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` WHERE `dict_type` = 'kb_model_deploy_type' AND `value` = 'zhipu' AND `deleted` = 0);

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 9, '其他', 'other', 'kb_model_deploy_type', 0, '', '', '', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` WHERE `dict_type` = 'kb_model_deploy_type' AND `value` = 'other' AND `deleted` = 0);

-- 模型支持平台
INSERT INTO `system_dict_type` (`name`, `type`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '模型支持平台', 'kb_model_platform', 0, '知识库-模型支持平台', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_type` WHERE `type` = 'kb_model_platform' AND `deleted` = 0);

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 1, 'Web端', 'web', 'kb_model_platform', 0, 'primary', '', '', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` WHERE `dict_type` = 'kb_model_platform' AND `value` = 'web' AND `deleted` = 0);

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 2, 'App端', 'app', 'kb_model_platform', 0, 'success', '', '', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` WHERE `dict_type` = 'kb_model_platform' AND `value` = 'app' AND `deleted` = 0);

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 3, '两者都支持', 'both', 'kb_model_platform', 0, '', '', '', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` WHERE `dict_type` = 'kb_model_platform' AND `value` = 'both' AND `deleted` = 0);

-- 新闻记录状态
INSERT INTO `system_dict_type` (`name`, `type`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '新闻记录状态', 'kb_news_record_status', 0, '知识库-新闻记录状态', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_type` WHERE `type` = 'kb_news_record_status' AND `deleted` = 0);

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 1, '待处理', 'pending', 'kb_news_record_status', 0, 'info', '', '', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` WHERE `dict_type` = 'kb_news_record_status' AND `value` = 'pending' AND `deleted` = 0);

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 2, '已完成', 'completed', 'kb_news_record_status', 0, 'success', '', '', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` WHERE `dict_type` = 'kb_news_record_status' AND `value` = 'completed' AND `deleted` = 0);

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 3, '失败', 'failed', 'kb_news_record_status', 0, 'danger', '', '', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` WHERE `dict_type` = 'kb_news_record_status' AND `value` = 'failed' AND `deleted` = 0);

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 4, '已跳过', 'skipped', 'kb_news_record_status', 0, '', '', '', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` WHERE `dict_type` = 'kb_news_record_status' AND `value` = 'skipped' AND `deleted` = 0);

-- 新闻同步状态
INSERT INTO `system_dict_type` (`name`, `type`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '新闻同步状态', 'kb_news_sync_status', 0, '知识库-新闻同步状态', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_type` WHERE `type` = 'kb_news_sync_status' AND `deleted` = 0);

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 1, '已开始', 'started', 'kb_news_sync_status', 0, 'info', '', '', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` WHERE `dict_type` = 'kb_news_sync_status' AND `value` = 'started' AND `deleted` = 0);

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 2, '运行中', 'running', 'kb_news_sync_status', 0, 'warning', '', '', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` WHERE `dict_type` = 'kb_news_sync_status' AND `value` = 'running' AND `deleted` = 0);

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 3, '已完成', 'completed', 'kb_news_sync_status', 0, 'success', '', '', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` WHERE `dict_type` = 'kb_news_sync_status' AND `value` = 'completed' AND `deleted` = 0);

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 4, '失败', 'failed', 'kb_news_sync_status', 0, 'danger', '', '', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` WHERE `dict_type` = 'kb_news_sync_status' AND `value` = 'failed' AND `deleted` = 0);

-- 新闻同步类型
INSERT INTO `system_dict_type` (`name`, `type`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '新闻同步类型', 'kb_news_sync_type', 0, '知识库-新闻同步类型', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_type` WHERE `type` = 'kb_news_sync_type' AND `deleted` = 0);

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 1, '全量同步', 'full', 'kb_news_sync_type', 0, 'primary', '', '', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` WHERE `dict_type` = 'kb_news_sync_type' AND `value` = 'full' AND `deleted` = 0);

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 2, '增量同步', 'incremental', 'kb_news_sync_type', 0, 'success', '', '', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` WHERE `dict_type` = 'kb_news_sync_type' AND `value` = 'incremental' AND `deleted` = 0);

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 3, '手动同步', 'manual', 'kb_news_sync_type', 0, 'warning', '', '', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` WHERE `dict_type` = 'kb_news_sync_type' AND `value` = 'manual' AND `deleted` = 0);

-- RAG配置模块类型
INSERT INTO `system_dict_type` (`name`, `type`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 'RAG配置模块类型', 'kb_rag_module', 0, '知识库-RAG配置模块类型', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_type` WHERE `type` = 'kb_rag_module' AND `deleted` = 0);

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 1, '检索模块', 'retrieval', 'kb_rag_module', 0, 'primary', '', '', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` WHERE `dict_type` = 'kb_rag_module' AND `value` = 'retrieval' AND `deleted` = 0);

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 2, '重排序模块', 'rerank', 'kb_rag_module', 0, 'success', '', '', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` WHERE `dict_type` = 'kb_rag_module' AND `value` = 'rerank' AND `deleted` = 0);

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 3, '切片模块', 'chunking', 'kb_rag_module', 0, 'warning', '', '', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` WHERE `dict_type` = 'kb_rag_module' AND `value` = 'chunking' AND `deleted` = 0);

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 4, '大模型模块', 'llm', 'kb_rag_module', 0, 'danger', '', '', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` WHERE `dict_type` = 'kb_rag_module' AND `value` = 'llm' AND `deleted` = 0);

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 5, '缓存模块', 'cache', 'kb_rag_module', 0, 'info', '', '', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` WHERE `dict_type` = 'kb_rag_module' AND `value` = 'cache' AND `deleted` = 0);

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 6, '批量处理模块', 'batch', 'kb_rag_module', 0, '', '', '', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` WHERE `dict_type` = 'kb_rag_module' AND `value` = 'batch' AND `deleted` = 0);

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 7, '对话模块', 'conversation', 'kb_rag_module', 0, 'success', '', '', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` WHERE `dict_type` = 'kb_rag_module' AND `value` = 'conversation' AND `deleted` = 0);

-- RAG配置值类型
INSERT INTO `system_dict_type` (`name`, `type`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 'RAG配置值类型', 'kb_rag_value_type', 0, '知识库-RAG配置值类型', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_type` WHERE `type` = 'kb_rag_value_type' AND `deleted` = 0);

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 1, '整数', 'int', 'kb_rag_value_type', 0, 'primary', '', '', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` WHERE `dict_type` = 'kb_rag_value_type' AND `value` = 'int' AND `deleted` = 0);

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 2, '浮点数', 'float', 'kb_rag_value_type', 0, 'warning', '', '', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` WHERE `dict_type` = 'kb_rag_value_type' AND `value` = 'float' AND `deleted` = 0);

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 3, '布尔值', 'bool', 'kb_rag_value_type', 0, 'success', '', '', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` WHERE `dict_type` = 'kb_rag_value_type' AND `value` = 'bool' AND `deleted` = 0);

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 4, '字符串', 'str', 'kb_rag_value_type', 0, 'info', '', '', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` WHERE `dict_type` = 'kb_rag_value_type' AND `value` = 'str' AND `deleted` = 0);

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 5, 'JSON对象', 'json', 'kb_rag_value_type', 0, 'danger', '', '', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` WHERE `dict_type` = 'kb_rag_value_type' AND `value` = 'json' AND `deleted` = 0);