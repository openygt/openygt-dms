-- ============================================================
-- V16: 库存流水表创建 + 索引（SQLite 兼容版）
-- 基于 docs/sql/V22__consume_log_optional.sql 转换
-- ============================================================

CREATE TABLE IF NOT EXISTS inv_stock_log (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    task_id         INTEGER,
    medicine_id     INTEGER,
    medicine_code   VARCHAR(50),
    medicine_name   VARCHAR(100),
    batch_id        INTEGER,
    change_type     VARCHAR(20) NOT NULL DEFAULT 'CONSUME',
    change_quantity DECIMAL(12,3) NOT NULL,
    before_quantity DECIMAL(12,3) DEFAULT NULL,
    after_quantity  DECIMAL(12,3) DEFAULT NULL,
    ref_no          VARCHAR(64),
    operator_id     VARCHAR(50),
    remark          VARCHAR(200),
    tenant_id       VARCHAR(32) DEFAULT 'default',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_slog_task_id ON inv_stock_log(task_id);
CREATE INDEX IF NOT EXISTS idx_slog_ref_no ON inv_stock_log(ref_no);
CREATE INDEX IF NOT EXISTS idx_slog_created_at ON inv_stock_log(created_at);
