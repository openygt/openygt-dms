-- V15: 为 sys_log 补齐 updated_at 字段（与 BaseAuditEntity 对齐）
ALTER TABLE sys_log ADD COLUMN updated_at DATETIME DEFAULT CURRENT_TIMESTAMP;
