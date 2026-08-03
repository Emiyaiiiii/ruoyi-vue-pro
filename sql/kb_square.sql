-- ============================================================
-- 知识库广场功能 DDL
-- 1. kb_library 添加 is_public 列
-- 2. 新建 kb_follow 关注表
-- 3. 更新 kb_level_config：SHARED → SQUARE
-- ============================================================

-- 在 kb_library 表加公开标记
ALTER TABLE kb_library ADD COLUMN is_public TINYINT DEFAULT 0
  COMMENT '是否公开到广场: 0=否, 1=是' AFTER status;

-- 新建关注表
CREATE TABLE IF NOT EXISTS kb_follow (
    id            BIGINT       PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    kb_id         BIGINT       NOT NULL          COMMENT '知识库ID',
    user_id       BIGINT       NOT NULL          COMMENT '关注用户ID',
    creator       VARCHAR(64)  DEFAULT ''        COMMENT '创建者',
    create_time   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater       VARCHAR(64)  DEFAULT ''        COMMENT '更新者',
    update_time   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted       TINYINT      DEFAULT 0         COMMENT '逻辑删除: 0=未删除, 1=已删除',
    tenant_id     BIGINT       DEFAULT 0         COMMENT '租户ID: 0=默认租户',
    INDEX idx_user (user_id),
    INDEX idx_kb (kb_id),
    UNIQUE KEY uk_user_kb (user_id, kb_id)
) COMMENT = '知识库关注';

-- 将 SHARED 层级改为 SQUARE（知识库广场）
UPDATE kb_level_config SET
    level_code = 'SQUARE',
    level_name = '知识库广场',
    visibility_rule = 6,
    owner_dim = 0,
    sort = 4
WHERE level_code = 'SHARED';
