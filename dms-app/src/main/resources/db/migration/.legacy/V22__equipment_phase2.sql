-- V22: 设备管理模块第二阶段 - 生产执行与追溯数据模型
-- 编制日期: 2026-04-30
-- 覆盖: 煎药过程追溯、时间校验、Dashboard、工作量统计、设备利用率、加水量公式、处方默认设置、药材分组、告警配置

-- ========== 1. decoction_trace 煎药过程追溯主表 ==========
CREATE TABLE IF NOT EXISTS decoction_trace (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    prescription_no VARCHAR(50) NOT NULL COMMENT '处方号（唯一业务键）',
    patient_name VARCHAR(50) COMMENT '患者姓名',
    patient_phone VARCHAR(20) COMMENT '患者电话（脱敏存储）',
    task_id BIGINT COMMENT '关联任务ID',

    -- 设备信息
    decoct_device_code VARCHAR(50) COMMENT '煎药机编码',
    decoct_device_name VARCHAR(100) COMMENT '煎药机名称',
    packer_device_code VARCHAR(50) COMMENT '包装机编码',
    labeler_device_code VARCHAR(50) COMMENT '标签打印机编码',

    -- 方案信息快照
    scheme_id BIGINT COMMENT '煎药方案ID',
    scheme_name VARCHAR(100) COMMENT '方案名称',
    soak_time INT COMMENT '实际浸泡时间(分钟)',
    pre_decoct_time INT COMMENT '实际先煎时间(分钟)',
    first_decoct_time INT COMMENT '实际一煎时间(分钟)',
    add_late_time INT COMMENT '实际后下时间(分钟)',
    second_decoct_time INT COMMENT '实际二煎时间(分钟)',
    package_time INT COMMENT '实际包装时间(分钟)',
    package_volume DECIMAL(10,2) COMMENT '实际包装药液量(ml)',
    sample_count INT COMMENT '实际留样数量',

    -- 时间戳链
    receive_time DATETIME COMMENT '接方时间',
    audit_time DATETIME COMMENT '审方时间',
    audit_pass_time DATETIME COMMENT '审方通过时间',
    dispense_time DATETIME COMMENT '调剂时间',
    review_time DATETIME COMMENT '复核时间',
    soak_start_time DATETIME COMMENT '浸泡开始',
    soak_end_time DATETIME COMMENT '浸泡结束',
    pre_decoct_start DATETIME COMMENT '先煎开始',
    pre_decoct_end DATETIME COMMENT '先煎结束',
    first_decoct_start DATETIME COMMENT '一煎开始',
    first_decoct_end DATETIME COMMENT '一煎结束',
    add_late_time_actual DATETIME COMMENT '后下实际时间',
    second_decoct_start DATETIME COMMENT '二煎开始',
    second_decoct_end DATETIME COMMENT '二煎结束',
    package_start_time DATETIME COMMENT '包装开始',
    package_end_time DATETIME COMMENT '包装结束',
    deliver_time DATETIME COMMENT '发货时间',
    complete_time DATETIME COMMENT '全流程完成时间',

    -- 操作人链
    receive_operator VARCHAR(50) COMMENT '接方员',
    audit_operator VARCHAR(50) COMMENT '审方员',
    dispense_operator VARCHAR(50) COMMENT '调剂员',
    review_operator VARCHAR(50) COMMENT '复核员',
    soak_operator VARCHAR(50) COMMENT '泡药员',
    decoct_operator VARCHAR(50) COMMENT '煎药员',
    package_operator VARCHAR(50) COMMENT '包装员',
    deliver_operator VARCHAR(50) COMMENT '发货员',

    -- 温度与质量数据
    temp_curve_data TEXT COMMENT '温度曲线关键点位JSON',
    max_temp DECIMAL(5,2) COMMENT '最高温度',
    avg_temp DECIMAL(5,2) COMMENT '平均温度',
    water_quality_check VARCHAR(200) COMMENT '水质检测结果',

    -- 状态与异常
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '追溯状态:PENDING/PROCESSING/COMPLETED/EXCEPTION',
    exception_reason VARCHAR(500) COMMENT '异常原因',
    exception_handle_result VARCHAR(500) COMMENT '异常处理结果',

    -- 打印与交付
    label_print_count INT DEFAULT 0 COMMENT '标签打印次数',
    delivery_no VARCHAR(50) COMMENT '快递单号',
    delivery_company VARCHAR(50) COMMENT '快递公司',

    tenant_id VARCHAR(32) DEFAULT 'default' COMMENT '租户ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT DEFAULT 0 COMMENT '逻辑删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='煎药过程追溯主表';


-- ========== 2. decoction_trace_event 追溯事件明细表 ==========
CREATE TABLE IF NOT EXISTS decoction_trace_event (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    trace_id BIGINT NOT NULL COMMENT '关联追溯主表',
    prescription_no VARCHAR(50) COMMENT '处方号（冗余）',
    event_code VARCHAR(50) COMMENT '事件编码',
    event_name VARCHAR(100) COMMENT '事件名称',
    event_type VARCHAR(20) COMMENT '事件类型:SYSTEM/MANUAL/DEVICE/AUTO',
    operator_id BIGINT COMMENT '操作人ID',
    operator_name VARCHAR(50) COMMENT '操作人姓名',
    device_code VARCHAR(50) COMMENT '关联设备编码',
    device_type VARCHAR(20) COMMENT '设备类型',
    event_time DATETIME COMMENT '事件发生时间',
    before_value VARCHAR(500) COMMENT '变更前值',
    after_value VARCHAR(500) COMMENT '变更后值',
    remark VARCHAR(500) COMMENT '备注',
    attachment_url VARCHAR(500) COMMENT '附件URL',
    tenant_id VARCHAR(32) DEFAULT 'default' COMMENT '租户ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted INT DEFAULT 0 COMMENT '逻辑删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='追溯事件明细表';


-- ========== 3. time_check_rule 时间校验规则表 ==========
CREATE TABLE IF NOT EXISTS time_check_rule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    rule_code VARCHAR(50) NOT NULL UNIQUE COMMENT '规则编码',
    rule_name VARCHAR(100) COMMENT '规则名称',
    from_step VARCHAR(50) COMMENT '起始步骤编码',
    to_step VARCHAR(50) COMMENT '目标步骤编码',
    min_duration INT COMMENT '最小间隔（秒），-1表示不限制',
    max_duration INT COMMENT '最大间隔（秒），-1表示不限制',
    check_type VARCHAR(20) DEFAULT 'BLOCK' COMMENT '校验类型:BLOCK/WARN/PASS',
    warning_message VARCHAR(200) COMMENT '警告提示文案',
    block_message VARCHAR(200) COMMENT '阻断提示文案',
    enabled TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    sort_order INT DEFAULT 0 COMMENT '排序',
    tenant_id VARCHAR(32) DEFAULT 'default' COMMENT '租户ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT DEFAULT 0 COMMENT '逻辑删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='时间校验规则表';


-- 插入默认时间校验规则
INSERT INTO time_check_rule (rule_code, rule_name, from_step, to_step, min_duration, max_duration, check_type, warning_message, block_message, sort_order) VALUES
('SOAK_MIN', '浸泡最小时间', 'SOAK_START', 'FIRST_DECOCT_START', 1800, -1, 'BLOCK', NULL, '浸泡时间不足，药材未充分浸泡，影响药效。还需等待 %s 分钟。', 1),
('SOAK_MAX', '浸泡最大时间', 'SOAK_START', 'FIRST_DECOCT_START', -1, 7200, 'WARN', '浸泡时间过长（%s 分钟），可能影响药材有效成分。建议尽快启动煎煮。', NULL, 2),
('FIRST_DECOCT_MIN', '一煎最小时间', 'FIRST_DECOCT_START', 'FIRST_DECOCT_END', 1800, -1, 'BLOCK', NULL, '一煎时间不足，药效可能未充分释放。还需等待 %s 分钟。', 3),
('SECOND_DECOCT_MIN', '二煎最小时间', 'SECOND_DECOCT_START', 'SECOND_DECOCT_END', 1200, -1, 'BLOCK', NULL, '二煎时间不足。还需等待 %s 分钟。', 4),
('PACKAGE_MAX_IDLE', '包装后最大 idle', 'PACKAGE_COMPLETE', 'DELIVER', -1, 3600, 'WARN', '包装后已闲置 %s 分钟，建议尽快发货。', NULL, 5),
('ADD_LATE_WINDOW', '后下确认窗口', 'ADD_LATE_REMIND', 'ADD_LATE_CONFIRM', -1, 300, 'WARN', '后下提醒已发出 %s 分钟，请尽快确认加药。', NULL, 6)
ON DUPLICATE KEY UPDATE rule_name=VALUES(rule_name);

-- ========== 4. water_formula 加水量公式表 ==========
CREATE TABLE IF NOT EXISTS water_formula (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    formula_code VARCHAR(50) NOT NULL UNIQUE COMMENT '公式编码',
    formula_name VARCHAR(100) COMMENT '公式名称',
    expression VARCHAR(500) COMMENT '公式表达式',
    expression_desc VARCHAR(500) COMMENT '公式说明',
    variables VARCHAR(500) COMMENT '变量说明JSON',
    is_default TINYINT(1) DEFAULT 0 COMMENT '是否默认',
    enabled TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    sort_order INT DEFAULT 0 COMMENT '排序',
    tenant_id VARCHAR(32) DEFAULT 'default' COMMENT '租户ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT DEFAULT 0 COMMENT '逻辑删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='加水量公式表';

-- 插入默认公式
INSERT INTO water_formula (formula_code, formula_name, expression, expression_desc, variables, is_default, enabled, sort_order) VALUES
('FORMULA_001', '标准公式', 'prescription.volume * 2.5 + 50', '标准加水量计算公式', '{"prescription.volume":"处方体积(ml)","2.5":"系数","50":"基础水量(ml)"}', 1, 1, 1),
('FORMULA_002', '浓缩配方', 'prescription.volume * 2.0 + 30', '浓缩剂型加水量', '{"prescription.volume":"处方体积(ml)","2.0":"系数","30":"基础水量(ml)"}', 0, 1, 2)
ON DUPLICATE KEY UPDATE formula_name=VALUES(formula_name);

-- ========== 5. prescription_default 处方默认设置表 ==========
CREATE TABLE IF NOT EXISTS prescription_default (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    setting_key VARCHAR(100) NOT NULL UNIQUE COMMENT '设置键',
    setting_name VARCHAR(100) COMMENT '设置名称',
    setting_value VARCHAR(100) COMMENT '设置值',
    setting_type VARCHAR(20) DEFAULT 'INT' COMMENT '设置类型:INT/STRING/BOOLEAN/DECIMAL',
    description VARCHAR(500) COMMENT '说明',
    tenant_id VARCHAR(32) DEFAULT 'default' COMMENT '租户ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT DEFAULT 0 COMMENT '逻辑删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='处方默认设置表';

-- 插入默认配置
INSERT INTO prescription_default (setting_key, setting_name, setting_value, setting_type, description) VALUES
('default.soak.time', '默认浸泡时间', '30', 'INT', '浸泡时间(分钟)'),
('default.first.decoct.time', '默认一煎时间', '30', 'INT', '一煎时间(分钟)'),
('default.second.decoct.time', '默认二煎时间', '20', 'INT', '二煎时间(分钟)'),
('default.addLate.time', '默认后下时间', '10', 'INT', '后下时间(分钟)'),
('default.preDecoct.time', '默认先煎时间', '15', 'INT', '先煎时间(分钟)'),
('default.sample.count', '默认留样数量', '1', 'INT', '留样数量(袋)'),
('default.package.volume', '默认包装容量', '200', 'INT', '包装容量(ml/袋)'),
('default.water.formula', '默认加水量公式', 'FORMULA_001', 'STRING', '默认加水量公式编码')
ON DUPLICATE KEY UPDATE setting_name=VALUES(setting_name);

-- ========== 6. medicine_group 药材分组表 ==========
CREATE TABLE IF NOT EXISTS medicine_group (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    prescription_id BIGINT COMMENT '处方ID',
    prescription_no VARCHAR(50) COMMENT '处方号',
    medicine_name VARCHAR(100) COMMENT '药材名称',
    group_type VARCHAR(20) COMMENT '分组类型:PRE_DECOCT/MAIN/ADD_LATE',
    dosage DECIMAL(10,2) COMMENT '剂量',
    unit VARCHAR(20) COMMENT '单位',
    sort_order INT DEFAULT 0 COMMENT '组内排序',
    remark VARCHAR(200) COMMENT '备注',
    tenant_id VARCHAR(32) DEFAULT 'default' COMMENT '租户ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT DEFAULT 0 COMMENT '逻辑删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='药材分组表';


-- ========== 7. workload_stat 工作量统计表 ==========
CREATE TABLE IF NOT EXISTS workload_stat (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    stat_date DATE NOT NULL COMMENT '统计日期',
    operator_id BIGINT COMMENT '操作人ID',
    operator_name VARCHAR(50) COMMENT '操作人姓名',
    work_type VARCHAR(20) COMMENT '工作类型:DISPENSE/REVIEW/SOAK/DECOCT/PACKAGE/DELIVER',
    task_count INT DEFAULT 0 COMMENT '任务数',
    prescription_count INT DEFAULT 0 COMMENT '处方数',
    package_count INT DEFAULT 0 COMMENT '包装数',
    duration_minutes INT DEFAULT 0 COMMENT '工作时长(分钟)',
    efficiency DECIMAL(5,2) COMMENT '效率(处方数/小时)',
    tenant_id VARCHAR(32) DEFAULT 'default' COMMENT '租户ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT DEFAULT 0 COMMENT '逻辑删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作量统计表';


-- ========== 8. device_utilization 设备利用率表 ==========
CREATE TABLE IF NOT EXISTS device_utilization (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    device_code VARCHAR(50) NOT NULL COMMENT '设备编码',
    stat_date DATE NOT NULL COMMENT '统计日期',
    total_minutes INT DEFAULT 1440 COMMENT '总分钟数',
    run_minutes INT DEFAULT 0 COMMENT '运行分钟数',
    idle_minutes INT DEFAULT 0 COMMENT '空闲分钟数',
    fault_minutes INT DEFAULT 0 COMMENT '故障分钟数',
    offline_minutes INT DEFAULT 0 COMMENT '离线分钟数',
    maintenance_minutes INT DEFAULT 0 COMMENT '维护分钟数',
    utilization_rate DECIMAL(5,2) COMMENT '利用率%',
    availability_rate DECIMAL(5,2) COMMENT '可用率%',
    fault_count INT DEFAULT 0 COMMENT '故障次数',
    task_count INT DEFAULT 0 COMMENT '完成任务数',
    tenant_id VARCHAR(32) DEFAULT 'default' COMMENT '租户ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT DEFAULT 0 COMMENT '逻辑删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备利用率表';


-- ========== 9. alarm_config 告警配置表 ==========
CREATE TABLE IF NOT EXISTS alarm_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    alarm_type VARCHAR(50) NOT NULL COMMENT '告警类型:TEMP_HIGH/TEMP_LOW/TIMEOUT/DEVICE_FAULT',
    alarm_level VARCHAR(20) COMMENT '告警级别:INFO/WARNING/CRITICAL',
    threshold_type VARCHAR(20) COMMENT '阈值类型:FIXED/PERCENTAGE/DEVIATION',
    threshold_value DECIMAL(10,2) COMMENT '阈值',
    duration_seconds INT COMMENT '持续时间(秒)',
    notify_type VARCHAR(50) COMMENT '通知方式:WEB/PUSH/SMS/VOICE/ALL',
    notify_target VARCHAR(50) COMMENT '通知对象:OPERATOR/BADMIN/ALL',
    sound_file VARCHAR(200) COMMENT '提示音文件',
    enabled TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    tenant_id VARCHAR(32) DEFAULT 'default' COMMENT '租户ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT DEFAULT 0 COMMENT '逻辑删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警配置表';

-- 插入默认告警配置
INSERT INTO alarm_config (alarm_type, alarm_level, threshold_type, threshold_value, duration_seconds, notify_type, notify_target, sound_file, enabled) VALUES
('TEMP_HIGH', 'CRITICAL', 'FIXED', 110.00, 10, 'ALL', 'ALL', '/assets/alarm-critical.mp3', 1),
('TEMP_LOW', 'WARNING', 'FIXED', 80.00, 60, 'WEB', 'OPERATOR', '/assets/alarm-warning.mp3', 1),
('TIMEOUT', 'WARNING', 'FIXED', 3600.00, 0, 'WEB,PUSH', 'OPERATOR', '/assets/alarm-warning.mp3', 1),
('DEVICE_FAULT', 'CRITICAL', 'FIXED', 1.00, 0, 'ALL', 'ALL', '/assets/alarm-critical.mp3', 1)
ON DUPLICATE KEY UPDATE alarm_level=VALUES(alarm_level);

