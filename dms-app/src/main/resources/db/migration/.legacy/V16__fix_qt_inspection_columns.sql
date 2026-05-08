-- ============================================
-- V16: 补齐 qt_inspection 缺失的字段
-- ============================================

-- Inspection 实体类包含 is_exception 和 exception_reason，但 V4 建表时未创建
ALTER TABLE qt_inspection ADD COLUMN is_exception BIGINT DEFAULT 0;
ALTER TABLE qt_inspection ADD COLUMN exception_reason VARCHAR(500);
