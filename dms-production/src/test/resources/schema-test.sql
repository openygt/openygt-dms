-- ============================================================
-- dms-production 单元测试 H2 建表脚本
-- H2 以 MySQL 兼容模式运行 (MODE=MySQL)
-- ============================================================

-- 处方表
CREATE TABLE IF NOT EXISTS prod_prescription (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id VARCHAR(32) DEFAULT 'default',
    hospital_id BIGINT,
    scheme_id BIGINT,
    prescription_no VARCHAR(50) NOT NULL UNIQUE,
    patient_name VARCHAR(50),
    patient_phone VARCHAR(20),
    total_dose INT,
    status VARCHAR(20) DEFAULT 'PENDING',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
);

-- 生产任务表（状态机核心）
CREATE TABLE IF NOT EXISTS prod_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id VARCHAR(32) DEFAULT 'default',
    prescription_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    decoct_device_id BIGINT,
    package_device_id BIGINT,
    current_temp DECIMAL(5,2),
    current_stage_duration INT DEFAULT 0,
    operator_id VARCHAR(50),
    print_status VARCHAR(20) DEFAULT 'PENDING',
    is_exception INT DEFAULT 0,
    exception_reason VARCHAR(200),
    handover_type VARCHAR(50),
    handover_user VARCHAR(50),
    handover_time DATETIME,
    stage_start_time DATETIME,
    complete_time DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
);

-- 状态变更历史表（不含逻辑删除）
CREATE TABLE IF NOT EXISTS prod_task_status_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id VARCHAR(32) DEFAULT 'default',
    task_id BIGINT NOT NULL,
    from_status VARCHAR(20),
    to_status VARCHAR(20),
    operator_id VARCHAR(50),
    operate_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    remark TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 工序记录表
CREATE TABLE IF NOT EXISTS prod_step_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id VARCHAR(32) DEFAULT 'default',
    task_id BIGINT NOT NULL,
    step_type VARCHAR(20),
    device_id BIGINT,
    operator_id VARCHAR(50),
    started_at DATETIME,
    ended_at DATETIME,
    result VARCHAR(20) DEFAULT '正常',
    abort_reason VARCHAR(200),
    is_paused INT DEFAULT 0,
    pause_reason VARCHAR(200),
    pause_duration INT DEFAULT 0,
    parent_id BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
);

-- 工时记录表
CREATE TABLE IF NOT EXISTS prod_work_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id VARCHAR(32) DEFAULT 'default',
    task_id BIGINT NOT NULL,
    operator_id VARCHAR(50),
    operator_name VARCHAR(50),
    action VARCHAR(20),
    work_time INT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
);

-- 交接明细表
CREATE TABLE IF NOT EXISTS prod_handover_detail (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id VARCHAR(32) DEFAULT 'default',
    task_id BIGINT NOT NULL,
    bag_count INT,
    handover_type VARCHAR(50),
    handover_user VARCHAR(50),
    handover_time DATETIME,
    remark TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
);
