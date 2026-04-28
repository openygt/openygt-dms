-- V2 refactor: complete decoction room model
-- No dispensing, no logistics

-- Drop old V1 tables to recreate with expanded schema
DROP TABLE IF EXISTS task;
DROP TABLE IF EXISTS device;
DROP TABLE IF EXISTS prescription;

CREATE TABLE IF NOT EXISTS hospital (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) UNIQUE,
    deleted BIGINT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS decoct_scheme (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    scheme_type BIGINT DEFAULT 0,
    decoct_times BIGINT DEFAULT 0,
    pressure BIGINT DEFAULT 1,
    upper_water DECIMAL(6,1) DEFAULT 0,
    heating_time BIGINT DEFAULT 30,
    pre_heating_time BIGINT DEFAULT NULL,
    post_heating_time BIGINT DEFAULT NULL,
    description VARCHAR(500),
    deleted BIGINT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

INSERT IGNORE INTO decoct_scheme (id, name, scheme_type, decoct_times, pressure, upper_water, heating_time, description) VALUES
(1, '常压汤药', 0, 0, 1, 0, 30, '常规常压煎药'),
(2, '常压补药', 0, 0, 1, 0, 60, '常压补药，文火60分钟'),
(3, '微压(密闭)汤药', 0, 0, 0, 0, 30, '微压密闭煎药'),
(4, '先煎汤药', 1, 0, 1, 0, 30, '先煎类汤药'),
(5, '后下汤药', 2, 0, 1, 0, 30, '后下类汤药'),
(6, '先煎后下汤药', 3, 0, 1, 0, 30, '先煎后下类汤药');

CREATE TABLE IF NOT EXISTS prescription (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    hospital_id BIGINT,
    prescription_number VARCHAR(100),
    patient_name VARCHAR(100) NOT NULL,
    patient_type BIGINT DEFAULT 0,
    outpatient_no VARCHAR(50),
    inpatient_no VARCHAR(50),
    bed_no VARCHAR(50),
    disease VARCHAR(200),
    doctor_name VARCHAR(50),
    department VARCHAR(50),
    disease_area VARCHAR(50),
    medicine_list VARCHAR(500) NOT NULL,
    repetition BIGINT DEFAULT 1,
    bags_per_repetition BIGINT DEFAULT 1,
    bag_capacity BIGINT DEFAULT 200,
    decocting_type BIGINT DEFAULT 0,
    usage_method VARCHAR(100),
    scheme_id BIGINT,
    remark VARCHAR(500),
    receive_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted BIGINT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (scheme_id) REFERENCES decoct_scheme(id)
);

CREATE TABLE IF NOT EXISTS device (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(100),
    device_type BIGINT DEFAULT 1,
    ip_address VARCHAR(20),
    port BIGINT,
    protocol_type VARCHAR(20) DEFAULT 'MQTT',
    location_x BIGINT DEFAULT 0,
    location_y BIGINT DEFAULT 0,
    decoct_mode BIGINT DEFAULT NULL,
    pressure_mode BIGINT DEFAULT 2,
    slow_fire_time BIGINT DEFAULT 30,
    package_num BIGINT DEFAULT NULL,
    package_capacity BIGINT DEFAULT NULL,
    alarm_min_temp DECIMAL(5,2) DEFAULT 0,
    alarm_max_temp DECIMAL(5,2) DEFAULT 120,
    fault_code VARCHAR(50) DEFAULT NULL,
    version VARCHAR(20),
    status VARCHAR(20) DEFAULT 'IDLE',
    current_temp DECIMAL(5,2) DEFAULT 0.00,
    enabled BIGINT DEFAULT 1,
    deleted BIGINT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS device_connection (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    decoct_device_id BIGINT NOT NULL,
    package_device_id BIGINT NOT NULL,
    is_primary BIGINT DEFAULT 1,
    deleted BIGINT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (decoct_device_id) REFERENCES device(id),
    FOREIGN KEY (package_device_id) REFERENCES device(id),
    UNIQUE(decoct_device_id, package_device_id)
);

CREATE TABLE IF NOT EXISTS task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    prescription_id BIGINT NOT NULL,
    decoct_device_id BIGINT,
    package_device_id BIGINT,
    scheme_id BIGINT,
    operator_id VARCHAR(50),
    status VARCHAR(20) DEFAULT 'PENDING',
    current_temp DECIMAL(5,2) DEFAULT 0.00,
    target_temp DECIMAL(5,2) DEFAULT 100.00,
    soak_duration BIGINT DEFAULT 30,
    soak_start_time DATETIME,
    soak_end_time DATETIME,
    decoct_start_time DATETIME,
    decoct_end_time DATETIME,
    pour_start_time DATETIME,
    pour_end_time DATETIME,
    wrap_start_time DATETIME,
    wrap_end_time DATETIME,
    complete_time DATETIME,
    current_stage_duration BIGINT DEFAULT 0,
    print_device_id BIGINT,
    print_status VARCHAR(20) DEFAULT 'PENDING',
    print_time DATETIME,
    deleted BIGINT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (prescription_id) REFERENCES prescription(id),
    FOREIGN KEY (decoct_device_id) REFERENCES device(id),
    FOREIGN KEY (package_device_id) REFERENCES device(id),
    FOREIGN KEY (scheme_id) REFERENCES decoct_scheme(id)
);

CREATE TABLE IF NOT EXISTS task_status_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    from_status VARCHAR(20),
    to_status VARCHAR(20) NOT NULL,
    operator_id VARCHAR(50),
    operate_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    remark VARCHAR(500),
    FOREIGN KEY (task_id) REFERENCES task(id)
);

CREATE TABLE IF NOT EXISTS device_alarm (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_id BIGINT NOT NULL,
    alarm_type VARCHAR(50) NOT NULL,
    alarm_level BIGINT DEFAULT 1,
    message VARCHAR(500),
    status BIGINT DEFAULT 0,
    resolved_time DATETIME,
    is_resolved BIGINT DEFAULT 0,
    resolved_at DATETIME,
    deleted BIGINT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (device_id) REFERENCES device(id)
);

CREATE TABLE IF NOT EXISTS work_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    operator_id VARCHAR(50) NOT NULL,
    operator_name VARCHAR(50),
    task_id BIGINT NOT NULL,
    action VARCHAR(20) NOT NULL,
    work_time BIGINT DEFAULT 0,
    deleted BIGINT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (task_id) REFERENCES task(id)
);

-- 设备温度曲线记录表
CREATE TABLE IF NOT EXISTS device_temperature_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_id BIGINT,
    device_code VARCHAR(100),
    temperature DECIMAL(5,2),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (device_id) REFERENCES device(id)
);
