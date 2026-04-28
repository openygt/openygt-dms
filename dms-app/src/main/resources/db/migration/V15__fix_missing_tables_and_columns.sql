-- ============================================
-- V15: 修复测试报告发现的数据库缺失问题
-- ============================================

-- 1. 补齐 sys_log 缺少的 updated_at 列
--    BaseAuditEntity 需要 updatedAt，但 V4 创建 sys_log 时未包含
--    SQLite ALTER TABLE 不支持 DEFAULT CURRENT_TIMESTAMP，故不指定默认值
ALTER TABLE sys_log ADD COLUMN updated_at DATETIME;

-- 2. 创建 inv_stock_log 表（消耗流水依赖）
--    从 V20 提取并适配 SQLite 语法
CREATE TABLE IF NOT EXISTS inv_stock_log (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id         BIGINT,
    medicine_id     BIGINT,
    medicine_code   VARCHAR(50),
    medicine_name   VARCHAR(100),
    batch_id        BIGINT,
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

CREATE INDEX idx_slog_task ON inv_stock_log(task_id);
CREATE INDEX idx_slog_medicine ON inv_stock_log(medicine_id);
CREATE INDEX idx_slog_batch ON inv_stock_log(batch_id);
CREATE INDEX idx_slog_created ON inv_stock_log(created_at);
CREATE INDEX idx_slog_ref ON inv_stock_log(ref_no);
