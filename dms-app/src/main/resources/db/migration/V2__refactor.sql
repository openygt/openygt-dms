-- V2 refactor: complete decoction room model
-- No dispensing, no logistics

-- Drop old V1 tables to recreate with expanded schema
DROP TABLE IF EXISTS task;
DROP TABLE IF EXISTS device;
DROP TABLE IF EXISTS prescription;

CREATE TABLE IF NOT EXISTS hospital (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) UNIQUE,
    deleted INTEGER DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS decoct_scheme (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name VARCHAR(100) NOT NULL,
    scheme_type INTEGER DEFAULT 0,
    decoct_times INTEGER DEFAULT 0,
    pressure INTEGER DEFAULT 1,
    upper_water DECIMAL(6,1) DEFAULT 0,
    heating_time INTEGER DEFAULT 30,
    pre_heating_time INTEGER DEFAULT NULL,
    post_heating_time INTEGER DEFAULT NULL,
    description VARCHAR(500),
    deleted INTEGER DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

INSERT OR IGNORE INTO decoct_scheme (id, name, scheme_type, decoct_times, pressure, upper_water, heating_time, description) VALUES
(1, '常压汤药', 0, 0, 1, 0, 30, '常规常压煎药'),
(2, '常压补药', 0, 0, 1, 0, 60, '常压补药，文火60分钟'),
(3, '微压(密闭)汤药', 0, 0, 0, 0, 30, '微压密闭煎药'),
(4, '先煎汤药', 1, 0, 1, 0, 30, '先煎类汤药'),
(5, '后下汤药', 2, 0, 1, 0, 30, '后下类汤药'),
(6, '先煎后下汤药', 3, 0, 1, 0, 30, '先煎后下类汤药');

CREATE TABLE IF NOT EXISTS prescription (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    hospital_id INTEGER,
    prescription_number VARCHAR(100),
    patient_name VARCHAR(100) NOT NULL,
    patient_type INTEGER DEFAULT 0,
    outpatient_no VARCHAR(50),
    inpatient_no VARCHAR(50),
    bed_no VARCHAR(50),
    disease VARCHAR(200),
    doctor_name VARCHAR(50),
    department VARCHAR(50),
    disease_area VARCHAR(50),
    medicine_list VARCHAR(500) NOT NULL,
    repetition INTEGER DEFAULT 1,
    bags_per_repetition INTEGER DEFAULT 1,
    bag_capacity INTEGER DEFAULT 200,
    decocting_type INTEGER DEFAULT 0,
    usage_method VARCHAR(100),
    scheme_id INTEGER,
    remark VARCHAR(500),
    receive_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (scheme_id) REFERENCES decoct_scheme(id)
);

CREATE TABLE IF NOT EXISTS device (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    device_code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(100),
    device_type INTEGER DEFAULT 1,
    ip_address VARCHAR(20),
    port INTEGER,
    protocol_type VARCHAR(20) DEFAULT 'MQTT',
    location_x INTEGER DEFAULT 0,
    location_y INTEGER DEFAULT 0,
    decoct_mode INTEGER DEFAULT NULL,
    pressure_mode INTEGER DEFAULT 2,
    slow_fire_time INTEGER DEFAULT 30,
    package_num INTEGER DEFAULT NULL,
    package_capacity INTEGER DEFAULT NULL,
    alarm_min_temp DECIMAL(5,2) DEFAULT 0,
    alarm_max_temp DECIMAL(5,2) DEFAULT 120,
    fault_code VARCHAR(50) DEFAULT NULL,
    version VARCHAR(20),
    status VARCHAR(20) DEFAULT 'IDLE',
    current_temp DECIMAL(5,2) DEFAULT 0.00,
    enabled INTEGER DEFAULT 1,
    deleted INTEGER DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS device_connection (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    decoct_device_id INTEGER NOT NULL,
    package_device_id INTEGER NOT NULL,
    is_primary INTEGER DEFAULT 1,
    deleted INTEGER DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (decoct_device_id) REFERENCES device(id),
    FOREIGN KEY (package_device_id) REFERENCES device(id),
    UNIQUE(decoct_device_id, package_device_id)
);

CREATE TABLE IF NOT EXISTS task (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    prescription_id INTEGER NOT NULL,
    decoct_device_id INTEGER,
    package_device_id INTEGER,
    scheme_id INTEGER,
    operator_id VARCHAR(50),
    status VARCHAR(20) DEFAULT 'PENDING',
    current_temp DECIMAL(5,2) DEFAULT 0.00,
    target_temp DECIMAL(5,2) DEFAULT 100.00,
    soak_duration INTEGER DEFAULT 30,
    soak_start_time DATETIME,
    soak_end_time DATETIME,
    decoct_start_time DATETIME,
    decoct_end_time DATETIME,
    pour_start_time DATETIME,
    pour_end_time DATETIME,
    wrap_start_time DATETIME,
    wrap_end_time DATETIME,
    complete_time DATETIME,
    current_stage_duration INTEGER DEFAULT 0,
    print_device_id INTEGER,
    print_status VARCHAR(20) DEFAULT 'PENDING',
    print_time DATETIME,
    deleted INTEGER DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (prescription_id) REFERENCES prescription(id),
    FOREIGN KEY (decoct_device_id) REFERENCES device(id),
    FOREIGN KEY (package_device_id) REFERENCES device(id),
    FOREIGN KEY (scheme_id) REFERENCES decoct_scheme(id)
);

CREATE TABLE IF NOT EXISTS task_status_history (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    task_id INTEGER NOT NULL,
    from_status VARCHAR(20),
    to_status VARCHAR(20) NOT NULL,
    operator_id VARCHAR(50),
    operate_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    remark VARCHAR(500),
    FOREIGN KEY (task_id) REFERENCES task(id)
);

CREATE TABLE IF NOT EXISTS device_alarm (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    device_id INTEGER NOT NULL,
    alarm_type VARCHAR(50) NOT NULL,
    alarm_level INTEGER DEFAULT 1,
    message VARCHAR(500),
    status INTEGER DEFAULT 0,
    resolved_time DATETIME,
    is_resolved INTEGER DEFAULT 0,
    resolved_at DATETIME,
    deleted INTEGER DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (device_id) REFERENCES device(id)
);

CREATE TABLE IF NOT EXISTS work_record (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    operator_id VARCHAR(50) NOT NULL,
    operator_name VARCHAR(50),
    task_id INTEGER NOT NULL,
    action VARCHAR(20) NOT NULL,
    work_time INTEGER DEFAULT 0,
    deleted INTEGER DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (task_id) REFERENCES task(id)
);

-- 设备温度曲线记录表
CREATE TABLE IF NOT EXISTS device_temperature_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    device_id INTEGER,
    device_code VARCHAR(100),
    temperature DECIMAL(5,2),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (device_id) REFERENCES device(id)
);
