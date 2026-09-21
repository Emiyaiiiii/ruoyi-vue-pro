-- =====================================================================
-- V9__add_kb_chunk_revision.sql  (Flyway Versioned 迁移)
-- 新增切片修订历史表（#7 chunk 编辑 + 修订历史）：
--   每次变更（编辑/回滚）写入一个「变更后」内容快照，revision 从 1 递增，
--   当前版本 = MAX(revision)（无记录表示从未编辑，版本号 0）。
--   回滚 = 取目标版本内容应用为当前，并新增一条 revert 快照（版本号继续递增）。
--   并发冲突检测依赖 UNIQUE(chunk_id, revision) + 事务内 MAX 校验（乐观锁）。
-- =====================================================================
CREATE TABLE IF NOT EXISTS `kb_chunk_revision` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `chunk_id` varchar(64) NOT NULL COMMENT '切片标识(复合: {document_id}_{chunk_index}，与 ES/Milvus doc id 一致)',
  `document_id` bigint NOT NULL COMMENT '所属文档ID',
  `kb_id` bigint NOT NULL COMMENT '所属知识库ID',
  `revision` int NOT NULL COMMENT '版本号(从1递增; 0=从未编辑)',
  `content` mediumtext NOT NULL COMMENT '该版本内容快照(变更后)',
  `content_hash` varchar(64) NOT NULL DEFAULT '' COMMENT '内容 sha256',
  `editor_id` bigint NOT NULL DEFAULT '0' COMMENT '编辑者用户ID',
  `editor_name` varchar(64) NOT NULL DEFAULT '' COMMENT '编辑者昵称',
  `change_type` varchar(16) NOT NULL DEFAULT 'edit' COMMENT '变更类型: edit=编辑, revert=回滚',
  `creator` varchar(64) NOT NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) NOT NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_chunk_revision` (`chunk_id`,`revision`),
  KEY `idx_document_id` (`document_id`),
  KEY `idx_kb_id` (`kb_id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='切片修订历史表';
