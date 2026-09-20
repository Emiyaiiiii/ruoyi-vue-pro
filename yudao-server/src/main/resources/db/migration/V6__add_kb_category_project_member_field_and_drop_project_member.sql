-- 项目成果库打开权限改造：不再使用 kb_project_member 权限表，改为实时读分类指定的「打开依据成员字段」的 ext 值。
-- 1) 分类表新增「打开依据成员字段」key（为空回退内置 project_member）
ALTER TABLE `kb_category`
    ADD COLUMN `project_member_field_key` varchar(128) DEFAULT NULL COMMENT '项目成果库打开依据成员字段key（columnConfig 中 type=member 字段的 key，为空回退内置 project_member）' AFTER `is_project`;

-- 2) 废弃权限表：打开权限已由 ext 成员字段实时推导，删除 kb_project_member（含引用已由代码改造移除）
DROP TABLE IF EXISTS `kb_project_member`;