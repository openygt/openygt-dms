-- V3: 煎药室流程重构 —— 兼容全自动/半自动/人工，支持质检、交接、损耗记录

-- ========== 设备表扩展 ==========
ALTER TABLE device ADD COLUMN auto_level VARCHAR(16) DEFAULT 'manual';
ALTER TABLE device ADD COLUMN label_mode VARCHAR(16) DEFAULT NULL;
ALTER TABLE device ADD COLUMN alert_time TIMESTAMP DEFAULT NULL;
ALTER TABLE device ADD COLUMN resolved_by VARCHAR(64) DEFAULT NULL;
ALTER TABLE device ADD COLUMN resolved_at TIMESTAMP DEFAULT NULL;

-- 兼容旧数据：将现有 status 映射到新状态（IDLE→idle, BUSY→running）
UPDATE device SET status = 'idle' WHERE status = 'IDLE' OR status IS NULL;
UPDATE device SET status = 'running' WHERE status = 'BUSY';
UPDATE device SET status = 'fault' WHERE status = 'FAULT';

-- ========== 工单主表扩展 ==========
ALTER TABLE task ADD COLUMN current_step VARCHAR(32) DEFAULT NULL;
ALTER TABLE task ADD COLUMN pool_id VARCHAR(32) DEFAULT NULL;
ALTER TABLE task ADD COLUMN print_copies INT DEFAULT 1;
ALTER TABLE task ADD COLUMN is_exception INT DEFAULT 0;
ALTER TABLE task ADD COLUMN exception_reason VARCHAR(255) DEFAULT NULL;
ALTER TABLE task ADD COLUMN patient_agreement VARCHAR(255) DEFAULT NULL;
ALTER TABLE task ADD COLUMN standard_cost DECIMAL(10,2) DEFAULT NULL;
ALTER TABLE task ADD COLUMN actual_cost DECIMAL(10,2) DEFAULT NULL;
ALTER TABLE task ADD COLUMN handover_type VARCHAR(32) DEFAULT NULL;
ALTER TABLE task ADD COLUMN handover_user VARCHAR(64) DEFAULT NULL;
ALTER TABLE task ADD COLUMN handover_time TIMESTAMP DEFAULT NULL;

-- 兼容旧数据：将现有 status 映射到新语义
UPDATE task SET status = '待泡药' WHERE status = 'PENDING';
UPDATE task SET status = '泡药中' WHERE status = 'SOAKING';
UPDATE task SET status = '待煎药' WHERE status = 'SOAKED';
UPDATE task SET status = '煎药中' WHERE status = 'PROCESSING';
UPDATE task SET status = '出液中' WHERE status = 'POURING';
UPDATE task SET status = '待包装' WHERE status = 'DECOCTED' OR status = 'WRAPPING';
UPDATE task SET status = '已完成' WHERE status = 'COMPLETED';
UPDATE task SET status = '已取消' WHERE status = 'CANCELLED';
UPDATE task SET status = '已报废' WHERE status = 'CANCELLED' AND is_exception = 1;

-- ========== 工序记录表 ==========
CREATE TABLE IF NOT EXISTS step_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    parent_id BIGINT DEFAULT NULL,
    step_type VARCHAR(32) NOT NULL,
    device_id VARCHAR(32) DEFAULT NULL,
    operator_id VARCHAR(64) DEFAULT NULL,
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ended_at TIMESTAMP DEFAULT NULL,
    is_paused INT DEFAULT 0,
    pause_reason VARCHAR(255) DEFAULT NULL,
    pause_duration INT DEFAULT 0,
    delay_minutes INT DEFAULT 0,
    delay_reason VARCHAR(255) DEFAULT NULL,
    result VARCHAR(32) DEFAULT '正常',
    abort_reason VARCHAR(255) DEFAULT NULL,
    waste_amount DECIMAL(10,2) DEFAULT NULL,
    waste_unit VARCHAR(16) DEFAULT NULL,
    is_retry INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (task_id) REFERENCES task(id)
);

-- ========== 交接明细表 ==========
CREATE TABLE IF NOT EXISTS handover_detail (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    bag_count INT DEFAULT 0,
    handover_type VARCHAR(32) DEFAULT NULL,
    handover_user VARCHAR(64) DEFAULT NULL,
    handover_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    remark VARCHAR(255) DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (task_id) REFERENCES task(id)
);
