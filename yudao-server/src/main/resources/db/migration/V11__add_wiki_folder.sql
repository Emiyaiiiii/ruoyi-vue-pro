-- =====================================================================
-- V11__add_wiki_folder.sql  (Flyway Versioned 迁移)
-- 新增 Wiki 目录树表（对齐 WeKnora wiki_folders）：
--  kb_wiki_folder   Wiki 页面的一级目录节点
-- 树结构为邻接表（parent_id，0=根）；path 为物化的 "/" 连接名称链，
-- 用于廉价展示/排序；页面经 kb_wiki_page.folder_id 挂到树上。
-- 目录可先于页面存在（用户可先建骨架再归档页面）。
-- 删除/移动目录时同步更新子树与该目录下页面的 category_path/depth。
-- =====================================================================
CREATE TABLE IF NOT EXISTS `kb_wiki_folder` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `kb_id` bigint NOT NULL COMMENT '所属知识库ID',
  `parent_id` bigint NOT NULL DEFAULT '0' COMMENT '父目录ID(0=根)',
  `name` varchar(255) NOT NULL COMMENT '目录名称',
  `path` varchar(1024) NOT NULL DEFAULT '' COMMENT '物化路径(/ 连接的名称链，如 AI/RAG)',
  `depth` int NOT NULL DEFAULT '0' COMMENT '目录深度(根目录的直接子为 1)',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '兄弟排序',
  `creator` varchar(64) NOT NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) NOT NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  -- 同一父目录下名称唯一（对齐 WeKnora idx_wiki_folders_parent_name）
  UNIQUE KEY `uk_wiki_folder_parent_name` (`kb_id`,`parent_id`,`name`),
  KEY `idx_wiki_folder_kb_parent` (`kb_id`,`parent_id`),
  KEY `idx_wiki_folder_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Wiki 目录树表';
