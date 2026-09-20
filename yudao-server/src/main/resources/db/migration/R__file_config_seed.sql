-- =====================================================================
-- R__file_config_seed.sql  (Flyway Repeatable 迁移)
-- 文件服务主配置（MinIO，私有文档桶）。
-- 保证 infra_file_config 的 master 指向真实 MinIO 文档桶；上传落 ckb-documents 私有桶，
-- 浏览按对外签名 URL 访问（详见 S3FileClientConfig.domain 语义）。
-- 幂等策略：
--   1) 修正「示例占位 master」：全新环境 ruoyi-vue-pro.sql 基线自带一条百度示例 master
--      （endpoint=www.baidu.com / domain=www.xxx.com / bucket=1），作为 master 会导致所有上传/
--      预览失败。此处将其改写为真实 MinIO 配置；仅当 master 是示例时才改写，
--      用户自建/已修正的真实 master 不受影响。domain 留 null，由部署方在配置界面填对外地址。
--   2) 若无任何 master，则插入固定 id=100 的 MinIO 主配置兜底。
-- 注意：
--   - config 保持 enablePublicAccess=false（私有签名）。
--   - config 为整段 JSON 字符串字面量，**不要**用 `||` 拼串：
--     本库 sql_mode 未启用 PIPES_AS_CONCAT，`||` 会被当作 OR，
--     导致整串被求值为 1，写入时抛 MySQL 1292（Truncated incorrect value）。
-- =====================================================================

UPDATE `infra_file_config`
SET `name` = 'MinIO（主配置）',
    `config` = '{"@class":"cn.iocoder.yudao.module.infra.framework.file.core.client.s3.S3FileClientConfig","endpoint":"http://minio:9000","domain":null,"bucket":"ckb-documents","accessKey":"admin","accessSecret":"20261qaz@WSX","enablePathStyleAccess":true,"enablePublicAccess":false}',
    `updater` = '1', `update_time` = NOW()
WHERE `deleted` = b'0' AND `master` = b'1'
  AND (`config` LIKE '%www.baidu.com%' OR `config` LIKE '%www.xxx.com%');

INSERT INTO `infra_file_config`
(`id`, `name`, `storage`, `remark`, `master`, `config`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT
  100,
  'MinIO（主配置）',
  20,
  '私有文档桶：上传走容器内网 minio:9000；浏览/下载经签名 URL 访问',
  b'1',
  '{"@class":"cn.iocoder.yudao.module.infra.framework.file.core.client.s3.S3FileClientConfig","endpoint":"http://minio:9000","domain":null,"bucket":"ckb-documents","accessKey":"admin","accessSecret":"20261qaz@WSX","enablePathStyleAccess":true,"enablePublicAccess":false}',
  '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (
    SELECT 1 FROM `infra_file_config`
    WHERE `deleted` = b'0' AND `master` = b'1'
);