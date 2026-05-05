-- 修复：dms_shelf.id 未设置自增时，INSERT 不包含 id 会触发
-- Field 'id' doesn't have a default value（与 V28 设计一致，应为 AUTO_INCREMENT）
ALTER TABLE dms_shelf MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID';
