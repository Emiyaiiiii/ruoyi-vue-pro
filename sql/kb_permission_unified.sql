-- ============================================================
-- 知识库统一权限模型 DDL
-- 1. kb_library 添加 is_project 列（方案A：标记项目成果库）
-- 2. 新建 kb_user_dept（用户↔部门多对多，统一管理权限）
-- 3. 新建 kb_project_member（项目成果库内容访问控制）
-- ============================================================

-- 1. kb_library 添加项目标记
ALTER TABLE kb_library ADD COLUMN is_project TINYINT DEFAULT 0
  COMMENT '是否项目成果库: 0=否, 1=是' AFTER is_public;

-- 2. 用户-部门关联表（成员 + 管理员统一管理）
CREATE TABLE IF NOT EXISTS kb_user_dept (
    id            BIGINT       PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id       BIGINT       NOT NULL          COMMENT '用户ID',
    dept_id       BIGINT       NOT NULL          COMMENT '部门ID（院/公司/咨询评估中心）',
    role          TINYINT      DEFAULT 0         COMMENT '角色: 0=成员, 1=管理员',
    creator       VARCHAR(64)  DEFAULT ''        COMMENT '创建者',
    create_time   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater       VARCHAR(64)  DEFAULT ''        COMMENT '更新者',
    update_time   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted       TINYINT      DEFAULT 0         COMMENT '逻辑删除: 0=未删除, 1=已删除',
    tenant_id     BIGINT       DEFAULT 0         COMMENT '租户ID',
    INDEX idx_user (user_id),
    INDEX idx_dept (dept_id),
    UNIQUE KEY uk_user_dept (user_id, dept_id)
) COMMENT = '知识库用户部门关联';

-- 3. 项目成员表（项目成果库文档内容访问控制）
CREATE TABLE IF NOT EXISTS kb_project_member (
    id            BIGINT       PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    kb_id         BIGINT       NOT NULL          COMMENT '知识库ID（项目）',
    user_id       BIGINT       NOT NULL          COMMENT '项目成员用户ID',
    creator       VARCHAR(64)  DEFAULT ''        COMMENT '创建者',
    create_time   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater       VARCHAR(64)  DEFAULT ''        COMMENT '更新者',
    update_time   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted       TINYINT      DEFAULT 0         COMMENT '逻辑删除: 0=未删除, 1=已删除',
    tenant_id     BIGINT       DEFAULT 0         COMMENT '租户ID',
    INDEX idx_kb (kb_id),
    INDEX idx_user (user_id),
    UNIQUE KEY uk_kb_user (kb_id, user_id)
) COMMENT = '知识库项目成员';
