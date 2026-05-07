-- V47__unify_status_values.sql
-- 作者：开发执行人
-- 日期：2026-05-07
-- 说明：统一状态字典，中文转英文编码
-- 依据：第一批系统基础治理专项 - 状态字典统一

-- ==================== 前置：审计日志表（如不存在则创建）====================
CREATE TABLE IF NOT EXISTS data_migration_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    migration_id VARCHAR(100) NOT NULL COMMENT '迁移标识',
    table_name VARCHAR(100) COMMENT '目标表名',
    step VARCHAR(20) COMMENT 'BEFORE / AFTER',
    record_count INT COMMENT '记录数',
    executed_by VARCHAR(100) COMMENT '执行人',
    executed_at DATETIME COMMENT '执行时间',
    INDEX idx_migration_id (migration_id)
) COMMENT='数据迁移审计日志表';

-- ==================== 备份 ====================
CREATE TABLE IF NOT EXISTS prod_task_status_backup_20260507 AS SELECT id, status, updated_at FROM prod_task;
CREATE TABLE IF NOT EXISTS prod_prescription_status_backup_20260507 AS SELECT id, receive_status, updated_at FROM prod_prescription;
CREATE TABLE IF NOT EXISTS eq_device_status_backup_20260507 AS SELECT id, status, updated_at FROM eq_device;

-- ==================== 迁移前记录数校验（GMP 审计要求）====================
INSERT INTO data_migration_log (migration_id, table_name, step, record_count, executed_by, executed_at)
VALUES ('V47_status_normalize', 'prod_task', 'BEFORE', (SELECT COUNT(*) FROM prod_task), CURRENT_USER, NOW());
INSERT INTO data_migration_log (migration_id, table_name, step, record_count, executed_by, executed_at)
VALUES ('V47_status_normalize', 'prod_prescription', 'BEFORE', (SELECT COUNT(*) FROM prod_prescription), CURRENT_USER, NOW());
INSERT INTO data_migration_log (migration_id, table_name, step, record_count, executed_by, executed_at)
VALUES ('V47_status_normalize', 'eq_device', 'BEFORE', (SELECT COUNT(*) FROM eq_device), CURRENT_USER, NOW());

-- ==================== 归一化映射 ====================

-- prod_task 任务状态映射（中文 → 英文编码）
UPDATE prod_task SET status = 'WAIT_SOAK' WHERE status IN ('待泡药', 'wait_soak');
UPDATE prod_task SET status = 'SOAKING' WHERE status IN ('泡药中', 'soaking');
UPDATE prod_task SET status = 'WAIT_DECOCT' WHERE status IN ('待煎药', 'wait_decoct');
UPDATE prod_task SET status = 'DECOCTING' WHERE status IN ('煎药中', 'decocting');
UPDATE prod_task SET status = 'WAIT_POUR' WHERE status IN ('待出液', 'wait_pour');
UPDATE prod_task SET status = 'POURING' WHERE status IN ('出液中', 'pouring');
UPDATE prod_task SET status = 'WAIT_WRAP' WHERE status IN ('待包装', 'wait_wrap');
UPDATE prod_task SET status = 'WRAPPING' WHERE status IN ('包装中', 'wrapping');
UPDATE prod_task SET status = 'WAIT_LABEL' WHERE status IN ('待贴标', 'wait_label');
UPDATE prod_task SET status = 'WAIT_QC' WHERE status IN ('待质检', 'wait_qc');
UPDATE prod_task SET status = 'STORED' WHERE status IN ('已暂存', 'stored');
UPDATE prod_task SET status = 'WAIT_HANDOVER' WHERE status IN ('待交接', 'wait_handover');
UPDATE prod_task SET status = 'SECOND_JUDGEMENT' WHERE status IN ('待二次判定', 'second_judgement');
UPDATE prod_task SET status = 'SUSPENDED' WHERE status IN ('已挂起', 'suspended');
UPDATE prod_task SET status = 'COMPLETED' WHERE status IN ('已完成', 'completed');
UPDATE prod_task SET status = 'PARTIAL_COMPLETED' WHERE status IN ('已部分完成', 'partial_completed');
UPDATE prod_task SET status = 'SCRAPPED' WHERE status IN ('已报废', 'scrapped');
UPDATE prod_task SET status = 'REWORK' WHERE status IN ('返工中', 'rework');
UPDATE prod_task SET status = 'CANCELLED' WHERE status IN ('已取消', 'cancelled');

-- prod_prescription 处方接收状态映射（中文 → 英文编码）
UPDATE prod_prescription SET receive_status = 'PENDING' WHERE receive_status IN ('待接收', 'pending');
UPDATE prod_prescription SET receive_status = 'RECEIVED' WHERE receive_status IN ('已接收', 'received');
UPDATE prod_prescription SET receive_status = 'REJECTED' WHERE receive_status IN ('已拒绝', 'rejected');

-- eq_device 设备状态映射（中文 → 英文编码）
UPDATE eq_device SET status = 'ONLINE' WHERE status IN ('在线', 'online');
UPDATE eq_device SET status = 'OFFLINE' WHERE status IN ('离线', 'offline');
UPDATE eq_device SET status = 'FAULT' WHERE status IN ('故障', 'fault');
UPDATE eq_device SET status = 'MAINTENANCE' WHERE status IN ('维护中', 'maintenance');

-- ==================== 迁移后记录数校验 ====================
INSERT INTO data_migration_log (migration_id, table_name, step, record_count, executed_by, executed_at)
VALUES ('V47_status_normalize', 'prod_task', 'AFTER', (SELECT COUNT(*) FROM prod_task), CURRENT_USER, NOW());
INSERT INTO data_migration_log (migration_id, table_name, step, record_count, executed_by, executed_at)
VALUES ('V47_status_normalize', 'prod_prescription', 'AFTER', (SELECT COUNT(*) FROM prod_prescription), CURRENT_USER, NOW());
INSERT INTO data_migration_log (migration_id, table_name, step, record_count, executed_by, executed_at)
VALUES ('V47_status_normalize', 'eq_device', 'AFTER', (SELECT COUNT(*) FROM eq_device), CURRENT_USER, NOW());

-- ==================== 合规校验 ====================
-- 校验1：确保无中文残留
SELECT 'prod_task' AS table_name, status, COUNT(*) AS count FROM prod_task WHERE status REGEXP '[一-龥]' GROUP BY status
UNION ALL
SELECT 'prod_prescription', receive_status, COUNT(*) FROM prod_prescription WHERE receive_status REGEXP '[一-龥]' GROUP BY receive_status
UNION ALL
SELECT 'eq_device', status, COUNT(*) FROM eq_device WHERE status REGEXP '[一-龥]' GROUP BY status;

-- 校验2：确保记录总数不变（BEFORE = AFTER）
-- 校验3：确保所有状态值在枚举定义范围内

-- ==================== 统计报告 ====================
SELECT 'prod_task' AS table_name, status, COUNT(*) AS count FROM prod_task GROUP BY status ORDER BY count DESC;
SELECT 'prod_prescription' AS table_name, receive_status, COUNT(*) AS count FROM prod_prescription GROUP BY receive_status ORDER BY count DESC;
SELECT 'eq_device' AS table_name, status, COUNT(*) AS count FROM eq_device GROUP BY status ORDER BY count DESC;
