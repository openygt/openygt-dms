-- V42: 为煎药方案补充状态字段
ALTER TABLE md_decoct_scheme ADD COLUMN status INT DEFAULT 1 COMMENT '状态: 1启用 0禁用';

-- 历史数据默认置为启用
UPDATE md_decoct_scheme SET status = 1 WHERE status IS NULL;
