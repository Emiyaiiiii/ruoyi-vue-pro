-- V13: Wiki 修订薄表承载 "页面在 ES、历史在 MySQL"
-- 页面正文已由 python 直落 ES(kb_wiki_all) 按 (kb_id, slug) 键；kb_wiki_revision 仅作修订历史纯载体，
-- 由 (kb_id, slug, revision) 唯一幂等定位，不再依赖 kb_wiki_page.id。

ALTER TABLE `kb_wiki_revision`
    ADD COLUMN `kb_id`  BIGINT NULL COMMENT '知识库ID（页面在 ES，供修订定位）' AFTER `page_id`,
    ADD COLUMN `slug`   VARCHAR(255) NULL COMMENT '页面 slug（页面在 ES，供修订定位）' AFTER `kb_id`;

-- 由 (kb_id, slug, revision) 唯一幂等（替换原 page_id+revision 语义）
CREATE UNIQUE INDEX `uk_kb_slug_revision` ON `kb_wiki_revision` (`kb_id`, `slug`, `revision`);