ALTER TABLE `ai_agent`
    ADD COLUMN `approval_level` varchar(16) NOT NULL DEFAULT 'auto' COMMENT '工具审批级别: strict=所有工具需审批, auto=智能, off=免审批';