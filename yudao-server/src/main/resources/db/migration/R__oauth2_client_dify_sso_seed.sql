-- =====================================================================
-- R__oauth2_client_dify_sso_seed.sql  (Flyway Repeatable 迁移)
-- 预置 Dify 单点登录所需的 OAuth2 客户端（应用管理）。
-- 与 dify-sso 容器内配置对齐：
--   client_id = dify-sso, secret = 20261qaz@WSX
--   redirect_uri = http://localhost:8000/console/api/enterprise/sso/oidc/callback
--   scope = openid profile email, grant_type = authorization_code
-- 幂等策略：system_oauth2_client 仅 client_id 上有普通索引（无唯一键），
--   故用 NOT EXISTS 守卫，避免重复执行插入多行。
-- =====================================================================

INSERT INTO `system_oauth2_client`
(`client_id`, `secret`, `name`, `logo`, `description`, `status`,
 `access_token_validity_seconds`, `refresh_token_validity_seconds`,
 `redirect_uris`, `authorized_grant_types`, `scopes`, `auto_approve_scopes`,
 `authorities`, `resource_ids`, `additional_information`,
 `creator`, `create_time`, `updater`, `update_time`, `deleted`)
-- 仅当目标 client_id 尚不存在时插入
SELECT 'dify-sso',
       '20261qaz@WSX',
       'Dify 单点登录',
       '',
       '通过 OIDC 使用芋道账号登录 Dify',
       0,
       1800,
       2592000,
       '["http://localhost:8000/console/api/enterprise/sso/oidc/callback"]',
       '["authorization_code","refresh_token"]',
       '["openid","profile","email"]',
       '["openid","profile","email"]',
       '[]',
       '[]',
       '{}',
       '1',
       '2026-09-01 00:00:00',
       '1',
       '2026-09-01 00:00:00',
       b'0'
WHERE NOT EXISTS (
    SELECT 1 FROM `system_oauth2_client` WHERE `client_id` = 'dify-sso'
);