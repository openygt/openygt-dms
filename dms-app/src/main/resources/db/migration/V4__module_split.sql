-- V4__module_split.sql
-- 按模块前缀重命名所有表，字段不变，仅增加 tenant_id

-- ========== 主数据 (md_) ==========
ALTER TABLE hospital RENAME TO md_hospital;
ALTER TABLE decoct_scheme RENAME TO md_decoct_scheme;

-- ========== 设备物联 (eq_) ==========
ALTER TABLE device RENAME TO eq_device;
ALTER TABLE device_connection RENAME TO eq_device_connection;
ALTER TABLE device_alarm RENAME TO eq_device_alarm;
ALTER TABLE device_temperature_log RENAME TO eq_temperature_log;

-- ========== 生产执行 (prod_) ==========
ALTER TABLE prescription RENAME TO prod_prescription;
ALTER TABLE task RENAME TO prod_task;
ALTER TABLE task_status_history RENAME TO prod_task_status_history;
ALTER TABLE work_record RENAME TO prod_work_record;
ALTER TABLE step_log RENAME TO prod_step_log;
ALTER TABLE handover_detail RENAME TO prod_handover_detail;

-- ========== 多租户预留：所有表增加 tenant_id ==========
ALTER TABLE md_hospital ADD COLUMN tenant_id VARCHAR(32) DEFAULT 'default';
ALTER TABLE md_decoct_scheme ADD COLUMN tenant_id VARCHAR(32) DEFAULT 'default';
ALTER TABLE eq_device ADD COLUMN tenant_id VARCHAR(32) DEFAULT 'default';
ALTER TABLE eq_device_connection ADD COLUMN tenant_id VARCHAR(32) DEFAULT 'default';
ALTER TABLE eq_device_alarm ADD COLUMN tenant_id VARCHAR(32) DEFAULT 'default';
ALTER TABLE eq_temperature_log ADD COLUMN tenant_id VARCHAR(32) DEFAULT 'default';
ALTER TABLE prod_prescription ADD COLUMN tenant_id VARCHAR(32) DEFAULT 'default';
ALTER TABLE prod_task ADD COLUMN tenant_id VARCHAR(32) DEFAULT 'default';
ALTER TABLE prod_task_status_history ADD COLUMN tenant_id VARCHAR(32) DEFAULT 'default';
ALTER TABLE prod_work_record ADD COLUMN tenant_id VARCHAR(32) DEFAULT 'default';
ALTER TABLE prod_step_log ADD COLUMN tenant_id VARCHAR(32) DEFAULT 'default';
ALTER TABLE prod_handover_detail ADD COLUMN tenant_id VARCHAR(32) DEFAULT 'default';

-- ========== 质量追溯 (qt_) ==========
CREATE TABLE IF NOT EXISTS qt_inspection (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    tenant_id VARCHAR(32) DEFAULT 'default',
    task_id INTEGER NOT NULL,
    result VARCHAR(20) NOT NULL,
    operator_id VARCHAR(50),
    remark TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

-- ========== 打印中心 (prt_) ==========
CREATE TABLE IF NOT EXISTS prt_task (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    tenant_id VARCHAR(32) DEFAULT 'default',
    task_id INTEGER NOT NULL,
    device_code VARCHAR(50),
    operator_id VARCHAR(50),
    status VARCHAR(20) DEFAULT 'PENDING',
    copies INTEGER DEFAULT 1,
    retry_count INTEGER DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

CREATE TABLE IF NOT EXISTS prt_record (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    tenant_id VARCHAR(32) DEFAULT 'default',
    print_task_id INTEGER NOT NULL,
    result VARCHAR(20),
    error_message TEXT,
    printed_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

-- ========== 系统管理 (sys_) ==========
CREATE TABLE IF NOT EXISTS sys_user (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    tenant_id VARCHAR(32) DEFAULT 'default',
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(128),
    real_name VARCHAR(50),
    phone VARCHAR(20),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

CREATE TABLE IF NOT EXISTS sys_config (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    tenant_id VARCHAR(32) DEFAULT 'default',
    config_key VARCHAR(100) NOT NULL UNIQUE,
    config_value TEXT,
    description VARCHAR(200),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

CREATE TABLE IF NOT EXISTS sys_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    tenant_id VARCHAR(32) DEFAULT 'default',
    user_id VARCHAR(50),
    action VARCHAR(100),
    module VARCHAR(50),
    detail TEXT,
    ip_address VARCHAR(50),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
