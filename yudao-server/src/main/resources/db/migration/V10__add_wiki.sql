-- =====================================================================
-- V10__add_wiki.sql  (Flyway Versioned 迁移)
-- 新增 Wiki 模式两张表（对齐 WeKnora wiki_pages / wiki_page_revisions）：
--  kb_wiki_page        知识库蒸馏出的互联 markdown 页面
--  kb_wiki_revision    页面修订历史（只存被替换的旧版本快照）
-- 并发要点：
--  - UNIQUE(kb_id, slug, deleted)：slug 在 KB 内唯一，软删后可重建同名
--  - UNIQUE(page_id, revision)：revision 快照幂等，靠捕获 DuplicateKeyException 兜并发
--  - version 乐观锁：UPDATE ... WHERE id=? AND version=? 防并发覆盖
--  - FULLTEXT(title, content)：支撑 WikiSearch 全文检索
-- =====================================================================
CREATE TABLE IF NOT EXISTS `kb_wiki_page` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `kb_id` bigint NOT NULL COMMENT '所属知识库ID',
  `slug` varchar(255) NOT NULL COMMENT '页面唯一标识(URL友好，KB 内唯一)',
  `title` varchar(512) NOT NULL DEFAULT '' COMMENT '页面标题',
  `content` mediumtext NOT NULL COMMENT 'markdown 正文(内嵌 [[slug|显示名]] 链接)',
  `summary` text NULL COMMENT '摘要',
  `aliases` json NULL COMMENT '别名列表(消歧/WikiSearch ILIKE)',
  `page_type` varchar(32) NOT NULL DEFAULT 'entity' COMMENT '页面类型: summary/entity/concept/manual/root',
  `folder_id` bigint NOT NULL DEFAULT '0' COMMENT '所属目录ID(0=根)',
  `category_path` json NULL COMMENT '目录层级路径',
  `depth` int NOT NULL DEFAULT '0' COMMENT '目录深度',
  `source_refs` json NULL COMMENT '来源引用(文档粒度: "kbId|docTitle")',
  `chunk_refs` json NULL COMMENT '关联 chunk id 集合(chunk 粒度溯源)',
  `in_links` json NULL COMMENT '反向链接: 指向本页的 slug 集合',
  `out_links` json NULL COMMENT '外链: 本页指向的 slug 集合',
  `status` varchar(32) NOT NULL DEFAULT 'published' COMMENT '状态: draft/published/archived',
  `revision` int NOT NULL DEFAULT '0' COMMENT '当前版本号(真实内容变更时+1)',
  `version` int NOT NULL DEFAULT '1' COMMENT '乐观锁版本号',
  `last_edit_source` varchar(16) NOT NULL DEFAULT 'pipeline' COMMENT '最近编辑来源: pipeline/user/revert',
  `last_editor_id` bigint NOT NULL DEFAULT '0' COMMENT '最近编辑者ID',
  `last_editor_name` varchar(64) NOT NULL DEFAULT '' COMMENT '最近编辑者昵称',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '兄弟排序',
  `creator` varchar(64) NOT NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) NOT NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  -- slug 在 KB 内唯一；页面删除走【物理删除】(wiki 页彻底移除, Slug 可重建同名)。
  -- MySQL 无 PG partial unique index，故不含 deleted；软删置 deleted=1 也保留该行唯一约束。
  UNIQUE KEY `uk_wiki_kb_slug` (`kb_id`,`slug`),
  KEY `idx_wiki_kb` (`kb_id`),
  KEY `idx_wiki_folder` (`folder_id`),
  KEY `idx_wiki_status` (`status`),
  KEY `idx_wiki_tenant` (`tenant_id`),
  FULLTEXT KEY `ft_wiki_fulltext` (`title`,`content`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Wiki 页面表';

CREATE TABLE IF NOT EXISTS `kb_wiki_revision` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `page_id` bigint NOT NULL COMMENT '页面ID',
  `revision` int NOT NULL COMMENT '版本号(被替换的旧版本)',
  `title` varchar(512) NOT NULL DEFAULT '' COMMENT '该版本标题',
  `content` mediumtext NOT NULL COMMENT '该版本内容快照',
  `summary` text NULL COMMENT '该版本摘要',
  `aliases` json NULL COMMENT '该版本别名',
  `page_type` varchar(32) NOT NULL DEFAULT 'entity' COMMENT '页类型',
  `status` varchar(32) NOT NULL DEFAULT 'published' COMMENT '状态',
  `edit_source` varchar(16) NOT NULL DEFAULT 'pipeline' COMMENT '编辑来源: pipeline/user/revert',
  `operator_id` bigint NOT NULL DEFAULT '0' COMMENT '操作人ID',
  `operator_name` varchar(64) NOT NULL DEFAULT '' COMMENT '操作人昵称',
  `creator` varchar(64) NOT NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) NOT NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_wiki_rev_page` (`page_id`,`revision`),
  KEY `idx_wiki_rev_page` (`page_id`),
  KEY `idx_wiki_rev_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Wiki 页面修订历史表';