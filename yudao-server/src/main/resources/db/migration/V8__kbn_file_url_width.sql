-- 扩容 kb_document.file_url / file_path 列宽：
-- 预签名完整 URL 含 S3 签名参数与 URL 编码的中文文件名，varchar(500) 会超限导致入库失败
-- （Data truncation: Data too long for column 'file_url'）。扩至 2048 与业务链路对齐。
ALTER TABLE kb_document
    MODIFY COLUMN file_url varchar(2048) NOT NULL COMMENT '文件访问URL (芋道文件管理返回)',
    MODIFY COLUMN file_path varchar(2048) DEFAULT '' COMMENT '文件存储路径 (芋道文件管理返回)';

-- kb_vector_task 提交任务时也写入同一预签名完整 URL，一并扩容避免再次超限
ALTER TABLE kb_vector_task
    MODIFY COLUMN file_url varchar(2048) NOT NULL COMMENT '文件下载地址';