-- ============================================================
-- V28: OpenYGT-DMS V1.1 新增模块数据库迁移
-- 模块：智能分配、时效预警、患者端、步骤可视化、药材分组、
--       货架管理、PDA语音、任务回退、条码打印、急诊处方
-- ============================================================

-- 1. 任务智能分配表
CREATE TABLE IF NOT EXISTS dms_task_assignment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL COMMENT '煎药任务ID',
    prescription_id BIGINT COMMENT '处方ID',
    device_id BIGINT COMMENT '分配设备ID',
    employee_id BIGINT COMMENT '分配员工ID',
    assign_type TINYINT DEFAULT 1 COMMENT '分配类型：1自动 2手动 3应急',
    assign_reason VARCHAR(500) COMMENT '分配原因/策略说明',
    scheduled_start_time DATETIME COMMENT '计划开始时间',
    scheduled_end_time DATETIME COMMENT '计划结束时间',
    actual_start_time DATETIME COMMENT '实际开始时间',
    actual_end_time DATETIME COMMENT '实际完成时间',
    status TINYINT DEFAULT 1 COMMENT '状态：1待执行 2执行中 3已完成 4已取消',
    stage_breakdown_json TEXT COMMENT '阶段拆分JSON',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_task_id (task_id),
    INDEX idx_prescription_id (prescription_id),
    INDEX idx_employee_id (employee_id),
    INDEX idx_device_id (device_id),
    INDEX idx_status (status)
) COMMENT='任务智能分配表';

-- 2. 员工技能标签表
CREATE TABLE IF NOT EXISTS dms_employee_skill (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    employee_id BIGINT NOT NULL COMMENT '员工ID',
    skill_code VARCHAR(50) COMMENT '技能编码：XIE_BIAO-解表药, ZI_BU-滋补药, WAI_YONG-外用药',
    skill_name VARCHAR(100),
    proficiency_level TINYINT DEFAULT 1 COMMENT '熟练度：1初级 2中级 3高级',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_employee_id (employee_id)
) COMMENT='员工技能标签表';

-- 3. 时效规则表
CREATE TABLE IF NOT EXISTS dms_time_rule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    rule_code VARCHAR(50) NOT NULL UNIQUE COMMENT '规则编码',
    rule_name VARCHAR(100) COMMENT '规则名称',
    prescription_type VARCHAR(50) DEFAULT 'NORMAL' COMMENT '适用处方类型：NORMAL-普通, EMERGENCY-急诊, TONIC-滋补',
    stage VARCHAR(50) COMMENT '阶段：SOAK-泡药, FIRST_DECOCTION-头煎, SECOND_DECOCTION-二煎, PACKING-包装',
    standard_duration INT COMMENT '标准时长（分钟）',
    warning_threshold INT COMMENT '预警阈值（提前X分钟）',
    alert_threshold INT COMMENT '告警阈值（超时X分钟）',
    critical_threshold INT COMMENT '严重阈值（超时X分钟）',
    is_default TINYINT DEFAULT 0 COMMENT '是否默认规则',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_prescription_type (prescription_type),
    INDEX idx_stage (stage)
) COMMENT='时效规则表';

-- 4. 时效监控实例表
CREATE TABLE IF NOT EXISTS dms_time_monitor (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    assignment_id BIGINT COMMENT '分配记录ID',
    prescription_id BIGINT COMMENT '处方ID',
    stage VARCHAR(50) COMMENT '当前阶段',
    planned_start DATETIME COMMENT '计划开始',
    planned_end DATETIME COMMENT '计划结束',
    actual_start DATETIME COMMENT '实际开始',
    actual_end DATETIME COMMENT '实际结束',
    remaining_seconds INT COMMENT '剩余秒数',
    status TINYINT DEFAULT 1 COMMENT '状态：1正常 2预警中 3已超时 4已处理',
    warning_count INT DEFAULT 0 COMMENT '预警次数',
    last_warning_time DATETIME COMMENT '上次预警时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_task_id (task_id),
    INDEX idx_status (status),
    INDEX idx_planned_end (planned_end)
) COMMENT='时效监控实例表';

-- 5. 预警记录表（时效专用）
CREATE TABLE IF NOT EXISTS dms_alert_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    monitor_id BIGINT NOT NULL,
    task_id BIGINT COMMENT '任务ID',
    alert_level TINYINT COMMENT '等级：1预警 2告警 3严重',
    alert_type VARCHAR(50) COMMENT '类型：TIMEOUT-超时, APPROACHING-即将超时',
    alert_content VARCHAR(500),
    notify_channels VARCHAR(200) COMMENT '通知渠道：BOARD,PDA,WECHAT,SMS',
    notify_targets VARCHAR(500) COMMENT '通知对象ID列表',
    is_resolved TINYINT DEFAULT 0,
    resolved_by BIGINT COMMENT '处理人',
    resolved_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_monitor_id (monitor_id),
    INDEX idx_task_id (task_id),
    INDEX idx_alert_level (alert_level),
    INDEX idx_is_resolved (is_resolved)
) COMMENT='预警记录表';

-- 6. 患者查询令牌表
CREATE TABLE IF NOT EXISTS dms_patient_token (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    token VARCHAR(64) NOT NULL UNIQUE COMMENT '查询令牌',
    prescription_id BIGINT NOT NULL,
    patient_phone VARCHAR(20),
    expire_at DATETIME COMMENT '过期时间',
    access_count INT DEFAULT 0 COMMENT '查询次数',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_token (token),
    INDEX idx_prescription_id (prescription_id)
) COMMENT='患者查询令牌表';

-- 7. 患者通知记录表
CREATE TABLE IF NOT EXISTS dms_patient_notify (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    prescription_id BIGINT NOT NULL,
    notify_type VARCHAR(50) COMMENT '类型：START-开始, COMPLETE-完成, DELIVERY-配送, REMIND-服药提醒',
    channel VARCHAR(50) COMMENT '渠道：SMS-短信, WECHAT-微信, APP-APP推送',
    content TEXT,
    is_sent TINYINT DEFAULT 0,
    sent_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_prescription_id (prescription_id)
) COMMENT='患者通知记录表';

-- 8. 药材分组规则表
CREATE TABLE IF NOT EXISTS dms_herb_group_rule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    group_code VARCHAR(50) NOT NULL UNIQUE COMMENT '分组编码',
    group_name VARCHAR(100) COMMENT '分组名称',
    group_color VARCHAR(20) DEFAULT '#4488FF' COMMENT '显示颜色',
    group_icon VARCHAR(50) COMMENT '图标',
    process_type VARCHAR(50) COMMENT '处理方式：PRE_DECOCT-先煎, NORMAL-群煎, POST_DECOCT-后下, WRAP-包煎, MELT-烊化, DIRECT-冲服',
    standard_duration INT COMMENT '标准处理时长（分钟）',
    special_instruction TEXT COMMENT '特殊说明',
    sort_order INT DEFAULT 0 COMMENT '排序',
    is_active TINYINT DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_process_type (process_type)
) COMMENT='药材分组规则表';

-- 9. 处方药材分组实例表
CREATE TABLE IF NOT EXISTS dms_prescription_herb_group (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    prescription_id BIGINT NOT NULL,
    group_code VARCHAR(50) NOT NULL,
    group_seq INT DEFAULT 0 COMMENT '组序号',
    herbs_json JSON COMMENT '本组药材列表',
    process_status TINYINT DEFAULT 0 COMMENT '处理状态：0待处理 1处理中 2已完成',
    process_time DATETIME COMMENT '实际处理时间',
    operator_id BIGINT COMMENT '操作人',
    device_id BIGINT COMMENT '使用的设备',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_prescription_id (prescription_id),
    INDEX idx_group_code (group_code)
) COMMENT='处方药材分组实例表';

-- 10. 货架定义表
CREATE TABLE IF NOT EXISTS dms_shelf (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    shelf_code VARCHAR(50) NOT NULL UNIQUE COMMENT '货架编码：A-01-03',
    shelf_name VARCHAR(100),
    area_code VARCHAR(50) COMMENT '区域编码',
    area_name VARCHAR(100) COMMENT '区域名称',
    row_num INT COMMENT '排号',
    layer_num INT COMMENT '层号',
    capacity INT COMMENT '容量（袋数）',
    current_count INT DEFAULT 0 COMMENT '当前存放数',
    shelf_type VARCHAR(50) DEFAULT 'NORMAL' COMMENT '类型：NORMAL-常温, COLD-冷藏, EXPRESS-快递专区',
    status TINYINT DEFAULT 1 COMMENT '状态：1启用 2停用 3维护中',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_area_code (area_code),
    INDEX idx_shelf_type (shelf_type),
    INDEX idx_status (status)
) COMMENT='货架定义表';

-- 11. 上架记录表
CREATE TABLE IF NOT EXISTS dms_shelf_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    prescription_id BIGINT NOT NULL,
    package_barcode VARCHAR(100) COMMENT '药袋条码',
    shelf_id BIGINT NOT NULL,
    shelf_code VARCHAR(50),
    put_on_time DATETIME COMMENT '上架时间',
    put_on_by BIGINT COMMENT '上架人',
    take_off_time DATETIME COMMENT '下架时间',
    take_off_by BIGINT COMMENT '下架人',
    take_off_type VARCHAR(50) COMMENT '下架类型：SELF_PICK-自取, DELIVERY-配送, EXPIRED-过期处理',
    status TINYINT DEFAULT 1 COMMENT '状态：1在架 2已取 3过期',
    expire_warning_time DATETIME COMMENT '过期预警时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_prescription_id (prescription_id),
    INDEX idx_shelf_id (shelf_id),
    INDEX idx_package_barcode (package_barcode),
    INDEX idx_status (status)
) COMMENT='上架记录表';

-- 12. 任务回退记录表
CREATE TABLE IF NOT EXISTS dms_task_rollback (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    original_task_id BIGINT NOT NULL COMMENT '原任务ID',
    new_task_id BIGINT COMMENT '回退后生成的新任务ID',
    rollback_from VARCHAR(50) COMMENT '从哪个阶段回退',
    rollback_to VARCHAR(50) COMMENT '回退到哪个阶段',
    rollback_reason VARCHAR(500) NOT NULL COMMENT '回退原因',
    rollback_type TINYINT DEFAULT 1 COMMENT '回退类型：1普通 2跨阶段 3已发货召回',
    operator_id BIGINT NOT NULL COMMENT '操作人',
    reviewer_id BIGINT COMMENT '复核人',
    approver_id BIGINT COMMENT '审批人',
    approval_status TINYINT DEFAULT 0 COMMENT '审批状态：0待审批 1已通过 2已拒绝',
    approval_comment VARCHAR(500),
    inventory_reverted TINYINT DEFAULT 0 COMMENT '库存是否已退回',
    device_reset TINYINT DEFAULT 0 COMMENT '设备是否已重置',
    patient_notified TINYINT DEFAULT 0 COMMENT '患者是否已通知',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    approved_at DATETIME,
    INDEX idx_original_task_id (original_task_id),
    INDEX idx_approval_status (approval_status),
    INDEX idx_rollback_type (rollback_type)
) COMMENT='任务回退记录表';

-- 13. 回退原因字典表
CREATE TABLE IF NOT EXISTS dms_rollback_reason (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    reason_code VARCHAR(50) NOT NULL UNIQUE,
    reason_name VARCHAR(200) NOT NULL,
    reason_category VARCHAR(50) COMMENT '类别',
    need_approval TINYINT DEFAULT 0 COMMENT '是否需要审批',
    approval_level TINYINT COMMENT '审批级别：1班组长 2主任 3医务科',
    is_active TINYINT DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT='回退原因字典表';

-- 14. 员工条码表
CREATE TABLE IF NOT EXISTS dms_employee_barcode (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    employee_id BIGINT NOT NULL,
    barcode VARCHAR(100) NOT NULL UNIQUE COMMENT '条码内容',
    barcode_type VARCHAR(20) DEFAULT 'QR' COMMENT '类型：CODE128-一维码, QR-二维码',
    card_type VARCHAR(50) DEFAULT 'BADGE' COMMENT '卡片类型：BADGE-工牌, WRIST-腕带, STICKER-贴纸',
    print_count INT DEFAULT 1 COMMENT '打印次数',
    last_print_time DATETIME,
    valid_from DATETIME COMMENT '有效期起',
    valid_to DATETIME COMMENT '有效期止',
    is_active TINYINT DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_employee_id (employee_id)
) COMMENT='员工条码表';

-- 15. 急诊处方扩展表
CREATE TABLE IF NOT EXISTS dms_emergency_prescription (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    prescription_id BIGINT NOT NULL UNIQUE,
    emergency_level TINYINT DEFAULT 1 COMMENT '急诊级别：1普通急诊 2危重急诊 3抢救',
    request_time DATETIME COMMENT '急诊请求时间',
    promised_finish_time DATETIME COMMENT '承诺完成时间',
    actual_finish_time DATETIME COMMENT '实际完成时间',
    is_on_time TINYINT COMMENT '是否按时完成',
    delay_reason VARCHAR(500),
    delivery_type VARCHAR(50) COMMENT '送达方式：SELF_PICK-自取, DIRECT_DELIVERY-直送急诊室, EXPRESS-快递',
    delivery_location VARCHAR(200) COMMENT '送达位置',
    nurse_name VARCHAR(100) COMMENT '接收护士',
    nurse_sign_time DATETIME COMMENT '护士签收时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_prescription_id (prescription_id),
    INDEX idx_emergency_level (emergency_level)
) COMMENT='急诊处方扩展表';

-- 16. PDA语音设置表
CREATE TABLE IF NOT EXISTS dms_voice_setting (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    device_id VARCHAR(100) COMMENT '设备ID（PDA设备标识）',
    user_id BIGINT COMMENT '用户ID',
    speech_rate TINYINT DEFAULT 50 COMMENT '语速 0-100',
    volume TINYINT DEFAULT 80 COMMENT '音量 0-100',
    voice_type VARCHAR(20) DEFAULT 'female' COMMENT '语音类型：male-男声, female-女声',
    enable_voice TINYINT DEFAULT 1 COMMENT '是否启用语音',
    quiet_start VARCHAR(10) DEFAULT '12:00' COMMENT '免打扰开始时间',
    quiet_end VARCHAR(10) DEFAULT '13:30' COMMENT '免打扰结束时间',
    repeat_count TINYINT DEFAULT 1 COMMENT '重复播报次数',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_device_id (device_id),
    INDEX idx_user_id (user_id)
) COMMENT='PDA语音设置表';

-- 插入默认数据
-- 时效规则默认数据
INSERT INTO dms_time_rule (rule_code, rule_name, prescription_type, stage, standard_duration, warning_threshold, alert_threshold, critical_threshold, is_default) VALUES
('NORMAL_SOAK', '标准泡药', 'NORMAL', 'SOAK', 30, 5, 10, 20, 1),
('NORMAL_FIRST', '标准头煎', 'NORMAL', 'FIRST_DECOCTION', 45, 5, 10, 20, 1),
('NORMAL_SECOND', '标准二煎', 'NORMAL', 'SECOND_DECOCTION', 30, 5, 10, 20, 1),
('NORMAL_PACK', '标准包装', 'NORMAL', 'PACKING', 10, 2, 5, 10, 1),
('EMERGENCY_SOAK', '急诊泡药', 'EMERGENCY', 'SOAK', 15, 3, 5, 10, 1),
('EMERGENCY_FIRST', '急诊头煎', 'EMERGENCY', 'FIRST_DECOCTION', 20, 3, 5, 10, 1),
('EMERGENCY_SECOND', '急诊二煎', 'EMERGENCY', 'SECOND_DECOCTION', 15, 3, 5, 10, 1),
('EMERGENCY_PACK', '急诊包装', 'EMERGENCY', 'PACKING', 5, 1, 3, 5, 1),
('TONIC_SOAK', '滋补泡药', 'TONIC', 'SOAK', 45, 5, 10, 20, 1),
('TONIC_FIRST', '滋补头煎', 'TONIC', 'FIRST_DECOCTION', 60, 5, 10, 20, 1)
ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP;

-- 回退原因字典默认数据
INSERT INTO dms_rollback_reason (reason_code, reason_name, reason_category, need_approval, approval_level, is_active) VALUES
('HERB_ERROR', '药材投错', 'HERB_ERROR', 1, 1, 1),
('DOSE_ERROR', '剂量错误', 'DOSE_ERROR', 1, 1, 1),
('DEVICE_FAULT', '设备故障', 'DEVICE_FAULT', 1, 1, 1),
('PRESCRIPTION_CHANGE', '处方变更', 'PRESCRIPTION_CHANGE', 1, 2, 1),
('QUALITY_ISSUE', '质量问题', 'QUALITY_ISSUE', 1, 2, 1),
('EMERGENCY_RECALL', '急诊召回', 'EMERGENCY_RECALL', 1, 3, 1)
ON DUPLICATE KEY UPDATE reason_name = VALUES(reason_name);

-- 药材分组规则默认数据
INSERT INTO dms_herb_group_rule (group_code, group_name, group_color, group_icon, process_type, standard_duration, special_instruction, sort_order, is_active) VALUES
('PRE_DECOCT', '先煎组', '#FF4444', 'fire', 'PRE_DECOCT', 30, '矿物类、毒性药需提前煎煮30分钟', 1, 1),
('NORMAL', '群煎组', '#4488FF', 'experiment', 'NORMAL', 0, '普通饮片正常煎煮', 2, 1),
('POST_DECOCT', '后下组', '#44BB44', 'plus', 'POST_DECOCT', 5, '芳香类药材出锅前5-10分钟下入', 3, 1),
('WRAP', '包煎组', '#FFAA00', 'box', 'WRAP', 0, '粉末类药材需纱布包煎', 4, 1),
('MELT', '烊化组', '#AA44FF', 'drop', 'MELT', 0, '胶类药材用药液冲服，不入锅', 5, 1),
('DIRECT', '冲服组', '#888888', 'dot', 'DIRECT', 0, '贵重粉直接冲服', 6, 1)
ON DUPLICATE KEY UPDATE group_name = VALUES(group_name);
