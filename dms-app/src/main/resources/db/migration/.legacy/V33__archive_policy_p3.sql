-- ============================================================
-- Flyway V33: 数据保留策略与归档表结构
-- ============================================================

-- 1. 处方任务归档表
CREATE TABLE prod_task_archive LIKE prod_task;
ALTER TABLE prod_task_archive DROP COLUMN deleted;
ALTER TABLE prod_task_archive ADD COLUMN archived_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '归档时间';
ALTER TABLE prod_task_archive ADD INDEX idx_archived (archived_at);
ALTER TABLE prod_task_archive ADD INDEX idx_complete_time (complete_time);

-- 2. 处方归档表
CREATE TABLE prod_prescription_archive LIKE prod_prescription;
ALTER TABLE prod_prescription_archive DROP COLUMN deleted;
ALTER TABLE prod_prescription_archive ADD COLUMN archived_at DATETIME DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE prod_prescription_archive ADD INDEX idx_archived (archived_at);

-- 3. 温度曲线归档表
CREATE TABLE eq_temperature_log_archive LIKE eq_temperature_log;
ALTER TABLE eq_temperature_log_archive ADD COLUMN archived_at DATETIME DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE eq_temperature_log_archive ADD INDEX idx_archived (archived_at);
ALTER TABLE eq_temperature_log_archive ADD INDEX idx_recorded (device_id, recorded_at);

-- 4. 系统操作日志归档表
CREATE TABLE sys_log_archive LIKE sys_log;
-- sys_log 在 V4 中创建时没有 deleted 列，因此 LIKE 复制的表也没有，此处条件删除
ALTER TABLE sys_log_archive DROP COLUMN IF EXISTS deleted;
ALTER TABLE sys_log_archive ADD COLUMN archived_at DATETIME DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE sys_log_archive ADD INDEX idx_archived (archived_at);

-- 5. 归档配置表
CREATE TABLE sys_archive_config (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    table_name      VARCHAR(64) NOT NULL COMMENT '表名',
    retention_days  INT NOT NULL COMMENT '保留天数',
    archive_strategy VARCHAR(20) NOT NULL COMMENT 'MIGRATE/COMPRESS/DELETE',
    last_archive_time DATETIME COMMENT '上次归档时间',
    is_active       TINYINT DEFAULT 1,
    tenant_id       VARCHAR(32) DEFAULT 'default',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_table (table_name)
) COMMENT='归档策略配置';

INSERT INTO sys_archive_config (table_name, retention_days, archive_strategy) VALUES
('prod_task', 1095, 'MIGRATE'),
('prod_prescription', 1095, 'MIGRATE'),
('eq_temperature_log', 365, 'MIGRATE'),
('sys_log', 730, 'MIGRATE'),
('eq_device_alarm', 365, 'DELETE'),
('t_interface_log', 180, 'DELETE'),
('dms_alert_log', 365, 'DELETE'),
('pda_operation_log', 365, 'DELETE');
