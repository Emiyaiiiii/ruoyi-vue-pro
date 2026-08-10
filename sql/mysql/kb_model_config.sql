-- ============================================================
-- 知识库管理 - 模型配置表
-- 对应前端菜单: 知识库管理 > 模型配置
-- ============================================================

-- 设置连接字符集为 utf8mb4 (避免与 gbk 连接的 collation 冲突)
SET NAMES utf8mb4;

DROP TABLE IF EXISTS `kb_model_config`;
CREATE TABLE `kb_model_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `uid` VARCHAR(100) NOT NULL COMMENT '模型唯一标识',
    `name` VARCHAR(100) NOT NULL COMMENT '模型名称',
    `url` VARCHAR(500) NOT NULL COMMENT 'API地址',
    `appkey` VARCHAR(500) NOT NULL COMMENT 'API密钥',
    `deploy` VARCHAR(50) NOT NULL DEFAULT 'doubao' COMMENT '部署类型: doubao=豆包, bailian=百炼, lite=LiteLLM, openai=OpenAI, api=通用API, xinf=Xinference, vllm=VLLM, zhipu=智谱AI, other=其他',
    `thinking_enabled` BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否启用思考能力',
    `is_active` BIT(1) NOT NULL DEFAULT b'1' COMMENT '是否激活: 0=停用, 1=激活',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '模型描述',
    `max_tokens` INT NOT NULL DEFAULT 4096 COMMENT '最大Token数',
    `context_length` INT NOT NULL DEFAULT 8192 COMMENT '上下文长度',
    `temperature` DOUBLE NOT NULL DEFAULT 0.7 COMMENT '温度参数',
    `top_p` DOUBLE NOT NULL DEFAULT 0.9 COMMENT 'Top-P参数',
    `metadata` VARCHAR(2000) DEFAULT '{}' COMMENT '元数据(JSON格式)',
    `config` VARCHAR(2000) DEFAULT '{}' COMMENT '配置参数(JSON格式)',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序顺序(升序)',
    `is_pinned` BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否置顶: 0=否, 1=是',
    `platform` VARCHAR(20) NOT NULL DEFAULT 'both' COMMENT '支持平台: web=Web端, app=App端, both=两者都支持',
    `activated_at` DATETIME DEFAULT NULL COMMENT '激活时间',
    `creator` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_uid` (`uid`),
    KEY `idx_deploy` (`deploy`),
    KEY `idx_is_active` (`is_active`),
    KEY `idx_is_pinned` (`is_pinned`),
    KEY `idx_sort_order` (`sort_order`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='大模型配置信息表';


-- ============================================================
-- 菜单数据: 知识库管理 > 模型配置
-- parent_id 需要根据实际"知识库管理"菜单的ID填写
-- 如果知识库管理菜单ID未知，请先查询 system_menu 表获取
-- ============================================================

-- 假设"知识库管理"目录菜单的 name='知识库管理'或类似，请根据实际情况调整 parent_id
-- 以下SQL中的 parent_id 需要替换为实际的知识库管理菜单ID

-- 查询"知识库管理"父菜单ID并存入变量
SELECT id INTO @kb_parent_id FROM `system_menu` WHERE name = '知识库管理' AND deleted = b'0' AND type = 1 LIMIT 1;

-- 模型配置 (菜单)
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
VALUES ('模型配置', '', 2, 5, @kb_parent_id, 'model-config', 'ep:connection', 'kb/modelconfig/index', 'KbModelConfig', 0, 1, 1, 0, NOW(), NOW(), b'0');

-- 获取刚插入的"模型配置"菜单ID
SET @model_config_id = LAST_INSERT_ID();

-- 模型配置按钮权限 (挂在模型配置菜单下)
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`, `deleted`)
VALUES
('模型配置查询', 'kb:model-config:query', 3, 1, @model_config_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'),
('模型配置创建', 'kb:model-config:create', 3, 2, @model_config_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'),
('模型配置更新', 'kb:model-config:update', 3, 3, @model_config_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'),
('模型配置删除', 'kb:model-config:delete', 3, 4, @model_config_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'),
('模型配置导出', 'kb:model-config:export', 3, 5, @model_config_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'),
('模型配置测试', 'kb:model-config:test', 3, 6, @model_config_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'),
('模型配置复制', 'kb:model-config:copy', 3, 7, @model_config_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0'),
('模型配置批量操作', 'kb:model-config:batch', 3, 8, @model_config_id, '', '', '', 0, 1, 1, 0, NOW(), NOW(), b'0');


-- ============================================================
-- 字典数据: 模型部署类型
-- ============================================================
INSERT INTO `system_dict_type` (`name`, `type`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES ('模型部署类型', 'kb_model_deploy_type', 0, '知识库-大模型部署类型', 'admin', NOW(), 'admin', NOW(), b'0');

-- 获取刚插入的字典类型对应的 type 值来插入字典数据
INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
(1, '豆包', 'doubao', 'kb_model_deploy_type', 0, 'warning', '', '豆包模型', 'admin', NOW(), 'admin', NOW(), b'0'),
(2, '百炼', 'bailian', 'kb_model_deploy_type', 0, 'danger', '', '百炼模型', 'admin', NOW(), 'admin', NOW(), b'0'),
(3, 'LiteLLM', 'lite', 'kb_model_deploy_type', 0, 'success', '', 'LiteLLM', 'admin', NOW(), 'admin', NOW(), b'0'),
(4, 'OpenAI', 'openai', 'kb_model_deploy_type', 0, 'success', '', 'OpenAI格式', 'admin', NOW(), 'admin', NOW(), b'0'),
(5, '通用API', 'api', 'kb_model_deploy_type', 0, 'info', '', '通用API', 'admin', NOW(), 'admin', NOW(), b'0'),
(6, 'Xinference', 'xinf', 'kb_model_deploy_type', 0, '', '', 'Xinference', 'admin', NOW(), 'admin', NOW(), b'0'),
(7, 'VLLM', 'vllm', 'kb_model_deploy_type', 0, '', '', 'VLLM', 'admin', NOW(), 'admin', NOW(), b'0'),
(8, '智谱AI', 'zhipu', 'kb_model_deploy_type', 0, 'primary', '', '智谱AI', 'admin', NOW(), 'admin', NOW(), b'0'),
(9, '其他', 'other', 'kb_model_deploy_type', 0, '', '', '其他部署类型', 'admin', NOW(), 'admin', NOW(), b'0');

-- 字典数据: 模型支持平台
INSERT INTO `system_dict_type` (`name`, `type`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES ('模型支持平台', 'kb_model_platform', 0, '知识库-模型支持平台', 'admin', NOW(), 'admin', NOW(), b'0');

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
(1, 'Web端', 'web', 'kb_model_platform', 0, 'primary', '', '仅Web端可用', 'admin', NOW(), 'admin', NOW(), b'0'),
(2, 'App端', 'app', 'kb_model_platform', 0, 'success', '', '仅App端可用', 'admin', NOW(), 'admin', NOW(), b'0'),
(3, '两者都支持', 'both', 'kb_model_platform', 0, '', '', 'Web和App都支持', 'admin', NOW(), 'admin', NOW(), b'0');
