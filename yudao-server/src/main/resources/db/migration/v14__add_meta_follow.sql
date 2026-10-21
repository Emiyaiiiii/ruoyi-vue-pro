-- ============================================================
-- V4: 新增 ai_meta_follow —— 智囊元数据（技能 / MCP / 智能体）关注表
--
-- 背景：C 端「水利智囊」需要收藏（关注）能力，旧后台为独立 follow 接口，
--       yudao 侧原先仅知识库有 kb_follow，故此处补一张统一的元数据关注表。
--
-- 说明：
--   * 一次性结构变更（不可重复执行）。
--   * meta_type: 1=技能(ai_skill_meta), 2=MCP(ai_mcp_meta), 3=智能体(ai_agent)
--   * meta_id 为对应元数据表主键；不设唯一键（与 kb_follow 一致），
--     由服务层做「已关注即跳过 / 不存在即删」的幂等处理。
-- ============================================================

CREATE TABLE `ai_meta_follow` (
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
