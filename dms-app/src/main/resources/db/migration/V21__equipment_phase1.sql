-- V21: 设备管理模块第一阶段 - 数据模型扩展
-- 编制日期: 2026-04-30

-- ========== 1. eq_device 表扩展字段 ==========
ALTER TABLE eq_device
    ADD COLUMN detail_status VARCHAR(50) DEFAULT 'IDLE' COMMENT '精细状态: 24种设备状态',
    ADD COLUMN manufacturer VARCHAR(100) COMMENT '厂商',
    ADD COLUMN model_num VARCHAR(100) COMMENT '型号',
    ADD COLUMN serial_number VARCHAR(100) COMMENT '序列号',
    ADD COLUMN communication_id VARCHAR(50) COMMENT '通信ID',
    ADD COLUMN barcode_data VARCHAR(200) COMMENT '条码数据',
    ADD COLUMN install_date DATE COMMENT '安装日期',
    ADD COLUMN warranty_expire DATE COMMENT '保修到期日',
    ADD COLUMN config_id BIGINT COMMENT 'MQTT配置ID(外键)',
    ADD COLUMN current_prescription_code VARCHAR(100) COMMENT '当前处方编号',
    ADD COLUMN current_operator_id BIGINT COMMENT '当前操作人ID',
    ADD COLUMN current_operator_name VARCHAR(50) COMMENT '当前操作人姓名',
    ADD COLUMN estimated_finish_time DATETIME COMMENT '预计完成时间',
    ADD COLUMN remaining_time INT DEFAULT 0 COMMENT '剩余时间(秒)',
    ADD COLUMN progress_percent INT DEFAULT 0 COMMENT '当前工序进度百分比',
    ADD COLUMN water_level INT DEFAULT 0 COMMENT '水位百分比(0-100)',
    ADD COLUMN pressure DECIMAL(5,2) DEFAULT 0.00 COMMENT '压力值(MPa)';

CREATE INDEX idx_eq_device_detail_status ON eq_device(detail_status);
CREATE INDEX idx_eq_device_manufacturer ON eq_device(manufacturer);
CREATE INDEX idx_eq_device_model ON eq_device(model_num);
CREATE INDEX idx_eq_device_comm_id ON eq_device(communication_id);
CREATE INDEX idx_eq_device_config ON eq_device(config_id);

-- ========== 2. eq_device_mqtt_config MQTT配置独立表 ==========
CREATE TABLE IF NOT EXISTS eq_device_mqtt_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    device_code VARCHAR(50) NOT NULL UNIQUE COMMENT '设备编码',
    broker_url VARCHAR(200) COMMENT 'Broker地址',
    port INT DEFAULT 1883 COMMENT '端口',
    username VARCHAR(100) COMMENT '用户名',
    password_encrypted VARCHAR(500) COMMENT '加密密码',
    publish_topic VARCHAR(200) COMMENT '设备发布Topic',
    subscribe_topic VARCHAR(200) COMMENT '设备订阅Topic',
    token_secret VARCHAR(256) COMMENT 'Token密钥',
    tenant_id VARCHAR(32) DEFAULT 'default' COMMENT '租户ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT DEFAULT 0 COMMENT '逻辑删除',
    UNIQUE KEY uk_device_code (device_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备MQTT配置表';

CREATE INDEX idx_eq_mqtt_config_tenant ON eq_device_mqtt_config(tenant_id);

-- ========== 3. eq_device_operator 设备-操作人绑定表 ==========
CREATE TABLE IF NOT EXISTS eq_device_operator (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    device_code VARCHAR(50) NOT NULL COMMENT '设备编码',
    operator_id BIGINT NOT NULL COMMENT '操作人ID',
    operator_name VARCHAR(50) COMMENT '操作人姓名',
    shift_start_time DATETIME COMMENT '班次开始时间',
    shift_end_time DATETIME COMMENT '班次结束时间',
    is_current TINYINT(1) DEFAULT 1 COMMENT '是否当前班次:1=是,0=否',
    tenant_id VARCHAR(32) DEFAULT 'default' COMMENT '租户ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT DEFAULT 0 COMMENT '逻辑删除',
    UNIQUE KEY uk_device_shift (device_code, shift_start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备操作人绑定表';

CREATE INDEX idx_eq_device_operator_code ON eq_device_operator(device_code);
CREATE INDEX idx_eq_device_operator_current ON eq_device_operator(device_code, is_current);

-- ========== 4. eq_device_status 设备实时状态快照表 ==========
CREATE TABLE IF NOT EXISTS eq_device_status (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    device_code VARCHAR(50) NOT NULL COMMENT '设备编码',
    device_type INT COMMENT '设备类型',
    status VARCHAR(20) COMMENT '兼容旧状态',
    detail_status VARCHAR(50) COMMENT '精细状态',
    current_temp DECIMAL(5,2) DEFAULT 0.00 COMMENT '当前温度',
    target_temp DECIMAL(5,2) DEFAULT 0.00 COMMENT '目标温度',
    water_level INT DEFAULT 0 COMMENT '水位',
    pressure DECIMAL(5,2) DEFAULT 0.00 COMMENT '压力',
    prescription_code VARCHAR(100) COMMENT '当前处方编号',
    scheme_name VARCHAR(100) COMMENT '当前方案名称',
    operator_id BIGINT COMMENT '操作人ID',
    operator_name VARCHAR(50) COMMENT '操作人姓名',
    progress_percent INT DEFAULT 0 COMMENT '进度百分比',
    remaining_time INT DEFAULT 0 COMMENT '剩余秒数',
    fault_code VARCHAR(50) COMMENT '故障码',
    fault_message VARCHAR(500) COMMENT '故障描述',
    snapshot_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '快照时间',
    tenant_id VARCHAR(32) DEFAULT 'default' COMMENT '租户ID'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备实时状态快照表';

CREATE INDEX idx_eq_device_status_code ON eq_device_status(device_code);
CREATE INDEX idx_eq_device_status_time ON eq_device_status(snapshot_time);

-- ========== 5. device_command 设备指令表 ==========
CREATE TABLE IF NOT EXISTS device_command (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    device_code VARCHAR(50) NOT NULL COMMENT '目标设备编码',
    command_type VARCHAR(50) NOT NULL COMMENT '指令类型:START_SOAK/START_DECOCT/PAUSE/RESUME/EMERGENCY_STOP/START_PACKAGE/SET_TEMP/ADD_LATE_REMIND/CONFIRM_ADD_LATE',
    command_payload TEXT COMMENT '指令参数JSON',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态:PENDING/SENT/ACKED/FAILED/TIMEOUT',
    response_payload TEXT COMMENT '设备响应JSON',
    retry_count INT DEFAULT 0 COMMENT '重试次数',
    send_time DATETIME COMMENT '发送时间',
    ack_time DATETIME COMMENT '确认时间',
    fail_reason VARCHAR(500) COMMENT '失败原因',
    tenant_id VARCHAR(32) DEFAULT 'default' COMMENT '租户ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT DEFAULT 0 COMMENT '逻辑删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备指令表';

CREATE INDEX idx_device_command_code ON device_command(device_code);
CREATE INDEX idx_device_command_status ON device_command(status);
CREATE INDEX idx_device_command_type ON device_command(command_type);

-- ========== 6. eq_temperature_log 补充字段 ==========
ALTER TABLE eq_temperature_log ADD COLUMN IF NOT EXISTS recorded_at DATETIME COMMENT '设备上报时间';
CREATE INDEX idx_eq_temp_log_recorded ON eq_temperature_log(device_id, recorded_at);

-- ========== 7. md_decoct_scheme 扩展字段 ==========
ALTER TABLE md_decoct_scheme
    ADD COLUMN IF NOT EXISTS first_decoct_time INT DEFAULT 30 COMMENT '一煎时间(分钟)',
    ADD COLUMN IF NOT EXISTS second_decoct_time INT DEFAULT 20 COMMENT '二煎时间(分钟)',
    ADD COLUMN IF NOT EXISTS soak_time INT DEFAULT 30 COMMENT '浸泡时间(分钟)',
    ADD COLUMN IF NOT EXISTS drain_time INT DEFAULT 10 COMMENT '出液时间(分钟)',
    ADD COLUMN IF NOT EXISTS package_time INT DEFAULT 15 COMMENT '包装时间(分钟)',
    ADD COLUMN IF NOT EXISTS late_add_remind_time INT DEFAULT 5 COMMENT '后下提醒提前时间(分钟)',
    ADD COLUMN IF NOT EXISTS temp_rise_rate DECIMAL(3,1) DEFAULT 3.0 COMMENT '升温速率(°C/min)',
    ADD COLUMN IF NOT EXISTS is_default TINYINT(1) DEFAULT 0 COMMENT '是否默认方案';
