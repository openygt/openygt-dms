-- ============================================
-- V7: 补全 eq_device 当前运行方案字段
-- ============================================

ALTER TABLE eq_device ADD COLUMN current_scheme_id BIGINT DEFAULT NULL;
