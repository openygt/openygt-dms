-- ============================================================
-- 异常工单处理模块 — 数据库迁移 V9
-- 表: prod_exception_log, exc_template, sys_exc_threshold, exc_escalation_log
-- ============================================================

-- 1. 异常模板表
CREATE TABLE IF NOT EXISTS exc_template (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    exc_type VARCHAR(50) NOT NULL,
    exc_level VARCHAR(20),
    root_cause_level1 VARCHAR(50),
    root_cause_level2 VARCHAR(50),
    root_cause_template TEXT,
    corrective_template TEXT,
    preventive_template TEXT,
    suggested_action VARCHAR(20),
    is_active INTEGER DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 初始化模板数据
INSERT INTO exc_template (exc_type, exc_level, root_cause_level1, root_cause_level2, root_cause_template, corrective_template, preventive_template, suggested_action) VALUES
('温度超温', 'HIGH', '设备异常', '温控失灵', '温控器故障/传感器漂移/加热管老化', '立即关机→质检药液→可用继续/不可用报废', '每日开班前校验温控器→月度更换老化探头', 'scrap'),
('设备故障', 'HIGH', '设备异常', '机械故障', '电机损坏/传送带断裂/控制系统死机', '停机→报修→启用备用设备→任务转移', '每周设备巡检→关键设备备件库存', 'scrap'),
('标签错误', 'MEDIUM', '人为失误', '操作失误', '标签贴错/条码扫错/信息录入错误', '破袋重包→重新贴标→双人复核', '升级扫码校验→增加复核环节', 'rework'),
('泡药超时', 'LOW', '生产管理', '排产问题', '排产过多/设备不足/前序延误', '评估药材状态→可用继续煎/不可用报废', '优化排产算法→增加泡药罐数量', 'rework'),
('煎糊报废', 'HIGH', '人为失误', '操作失误', '加水量不足/火力过大/时间设置错误', '整锅报废→记录损耗→重新煎制', '加水量双人确认→温度曲线实时监控', 'scrap'),
('串方', 'HIGH', '人为失误', '严重失误', '处方混淆/药材拿错/标签贴错', '整锅报废→通知医生→重新开方煎制', '扫码核对处方→药材上架分区管理', 'scrap'),
('条码不符', 'MEDIUM', '人为失误', '操作失误', '条码扫描错误/系统录入错误', '重新扫描核对→修正系统记录', '升级扫码校验→增加复核环节', 'rework'),
('温度不足', 'MEDIUM', '设备异常', '温控失灵', '温控器故障/加热管老化/电压不稳', '检修温控系统→评估药液质量', '每日开班前校验温控器', 'scrap'),
('设备离线', 'HIGH', '设备异常', '通信故障', '网络中断/控制器死机/电源故障', '重启设备→检查网络→切换备用设备', '设备心跳监控→双网冗余', 'scrap');

-- 2. 预警阈值配置表
CREATE TABLE IF NOT EXISTS sys_exc_threshold (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    exc_type VARCHAR(50) NOT NULL,
    param_name VARCHAR(50) NOT NULL,
    param_value INTEGER NOT NULL,
    unit VARCHAR(20),
    description TEXT,
    is_active INTEGER DEFAULT 1,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO sys_exc_threshold (exc_type, param_name, param_value, unit, description) VALUES
('temp_high', 'max_temp', 120, '℃', '煎药最高温度'),
('temp_low', 'min_temp', 50, '℃', '煎药最低温度'),
('soak_timeout', 'max_soak_minutes', 120, '分钟', '最长浸泡时间'),
('soak_timeout', 'min_soak_minutes', 30, '分钟', '最短浸泡时间'),
('device_offline', 'max_offline_seconds', 300, '秒', '设备心跳超时'),
('exception_sla', 'high_level_minutes', 30, '分钟', 'HIGH级异常处理时限'),
('exception_sla', 'medium_level_minutes', 120, '分钟', 'MEDIUM级异常处理时限'),
('exception_sla', 'low_level_minutes', 240, '分钟', 'LOW级异常处理时限'),
('rework_time_lock', 'max_idle_hours', 4, '小时', '煎好后允许返工的最大闲置时间');

-- 3. 异常升级记录表
CREATE TABLE IF NOT EXISTS exc_escalation_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    exception_id INTEGER NOT NULL,
    from_level INTEGER,
    to_level INTEGER,
    from_handler VARCHAR(64),
    to_handler VARCHAR(64),
    escalation_reason VARCHAR(128),
    escalated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_esc_exc_id ON exc_escalation_log(exception_id);

-- 4. 生产异常统一记录表（主表）
CREATE TABLE IF NOT EXISTS prod_exception_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    exception_no VARCHAR(32) NOT NULL,
    task_id INTEGER NOT NULL,
    original_task_id INTEGER,
    new_task_id INTEGER,
    prescription_no VARCHAR(64),
    patient_name VARCHAR(32),
    exception_type VARCHAR(50) NOT NULL,
    exception_level VARCHAR(20) NOT NULL,
    source_module VARCHAR(20),
    current_step VARCHAR(32),
    description TEXT,
    discover_channel VARCHAR(20),
    discoverer_id VARCHAR(64),
    is_auto_handled INTEGER DEFAULT 0,
    handle_status VARCHAR(20) DEFAULT 'PENDING',
    handler_id VARCHAR(64),
    handler_role VARCHAR(32),
    assigned_at DATETIME,
    resolution VARCHAR(50),
    resolution_result VARCHAR(50),
    resolved_at DATETIME,
    rework_target_step VARCHAR(32),
    rework_validate_result TEXT,
    loss_weight_gram INTEGER DEFAULT 0,
    loss_amount_yuan REAL DEFAULT 0.00,
    waste_liquid_ml INTEGER DEFAULT 0,
    device_no VARCHAR(32),
    operator_id VARCHAR(64),
    shift_type VARCHAR(16),
    consumable_batch VARCHAR(64),
    root_cause_level1 VARCHAR(50),
    root_cause_level2 VARCHAR(50),
    root_cause TEXT,
    corrective_action TEXT,
    preventive_action TEXT,
    evidence_type VARCHAR(20),
    evidence_barcode VARCHAR(128),
    evidence_urls TEXT,
    handler_sign VARCHAR(128),
    paper_record_no VARCHAR(64),
    evidence_hash VARCHAR(64),
    sla_deadline DATETIME,
    is_timeout INTEGER DEFAULT 0,
    escalation_level INTEGER DEFAULT 0,
    escalated_at DATETIME,
    is_concession INTEGER DEFAULT 0,
    doctor_sign VARCHAR(128),
    doctor_sign2 VARCHAR(128),
    patient_consent TEXT,
    derived_workorder_id INTEGER,
    derived_workorder_type VARCHAR(20),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(64),
    last_modified_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    last_modified_by VARCHAR(64),
    is_deleted INTEGER DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_exc_task_id ON prod_exception_log(task_id);
CREATE INDEX IF NOT EXISTS idx_exc_prescription ON prod_exception_log(prescription_no);
CREATE INDEX IF NOT EXISTS idx_exc_level_status ON prod_exception_log(exception_level, handle_status);
CREATE INDEX IF NOT EXISTS idx_exc_created ON prod_exception_log(created_at);
CREATE INDEX IF NOT EXISTS idx_exc_handler ON prod_exception_log(handler_id, handle_status);
CREATE INDEX IF NOT EXISTS idx_exc_device ON prod_exception_log(device_no, created_at);
CREATE INDEX IF NOT EXISTS idx_exc_operator ON prod_exception_log(operator_id, created_at);
CREATE INDEX IF NOT EXISTS idx_exc_sla ON prod_exception_log(sla_deadline, is_timeout);
