-- ============================================================
-- Flyway V30: 业务流程改进 P0
-- 药材安全管控、设备清洗、异常工单、任务挂起
-- ============================================================

-- 1. 毒性药材清单表
CREATE TABLE base_toxic_medicine (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    medicine_id         BIGINT NOT NULL COMMENT '药材ID',
    medicine_name       VARCHAR(100) NOT NULL COMMENT '药材名称',
    toxicity_level      TINYINT NOT NULL COMMENT '毒性等级: 1小毒 2有毒 3大毒',
    max_dosage          DECIMAL(10,3) COMMENT '单次最大用量(g)',
    max_daily_dosage    DECIMAL(10,3) COMMENT '每日最大用量(g)',
    wash_level          TINYINT DEFAULT 1 COMMENT '清洗级别: 1常规 2强化',
    is_active           TINYINT DEFAULT 1,
    remark              VARCHAR(200),
    tenant_id           VARCHAR(32) DEFAULT 'default',
    deleted             INT DEFAULT 0,
    created_at          DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_medicine (medicine_id),
    INDEX idx_toxic_level (toxicity_level),
    INDEX idx_active (is_active)
) COMMENT='毒性药材清单';

-- 2. 留样记录表
CREATE TABLE qt_retain_sample (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    task_id             BIGINT NOT NULL COMMENT '任务ID',
    prescription_id     BIGINT NOT NULL COMMENT '处方ID',
    sample_no           VARCHAR(32) NOT NULL COMMENT '留样编号',
    sample_type         TINYINT NOT NULL COMMENT '1=质检样 2=24h留样 3=72h留样',
    retain_duration     INT COMMENT '留样时长(小时)',
    retain_time         DATETIME NOT NULL COMMENT '留样时间',
    expire_time         DATETIME COMMENT '销毁时间',
    status              TINYINT DEFAULT 1 COMMENT '1=留样中 2=已复检 3=可销毁 4=已销毁',
    destroy_time        DATETIME COMMENT '销毁时间',
    destroy_by          BIGINT COMMENT '销毁人ID',
    operator_id         BIGINT NOT NULL COMMENT '留样操作人',
    remark              VARCHAR(200),
    tenant_id           VARCHAR(32) DEFAULT 'default',
    deleted             INT DEFAULT 0,
    created_at          DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_task (task_id),
    INDEX idx_prescription (prescription_id),
    INDEX idx_expire (expire_time),
    INDEX idx_status (status),
    INDEX idx_sample_no (sample_no)
) COMMENT='留样记录表';

-- 3. 设备清洗记录表
CREATE TABLE eq_wash_record (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    device_id           BIGINT NOT NULL COMMENT '设备ID',
    device_code         VARCHAR(32) NOT NULL COMMENT '设备编码',
    task_id             BIGINT COMMENT '关联任务',
    prescription_id     BIGINT COMMENT '关联处方',
    wash_type           TINYINT NOT NULL COMMENT '1=常规 2=强化',
    standard_duration   INT NOT NULL COMMENT '标准时长(分钟)',
    start_time          DATETIME NOT NULL,
    end_time            DATETIME,
    duration_min        INT COMMENT '实际时长(分钟)',
    result              TINYINT COMMENT '1=合格 0=不合格',
    operator_id         BIGINT NOT NULL,
    operator_name       VARCHAR(50),
    remark              VARCHAR(500),
    tenant_id           VARCHAR(32) DEFAULT 'default',
    deleted             INT DEFAULT 0,
    created_at          DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_device (device_id),
    INDEX idx_task (task_id),
    INDEX idx_start_time (start_time)
) COMMENT='设备清洗记录';

-- 4. 清洗标准配置表
CREATE TABLE eq_wash_standard (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    device_type         TINYINT NOT NULL COMMENT '1=煎药机 2=包装机',
    wash_type           TINYINT NOT NULL COMMENT '1=常规 2=强化',
    standard_duration   INT NOT NULL COMMENT '标准时长(分钟)',
    wash_steps          VARCHAR(500) COMMENT '步骤说明',
    alert_threshold     INT COMMENT '超时告警阈值(分钟)',
    is_active           TINYINT DEFAULT 1,
    tenant_id           VARCHAR(32) DEFAULT 'default',
    deleted             INT DEFAULT 0,
    created_at          DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_type (device_type, wash_type),
    INDEX idx_active (is_active)
) COMMENT='清洗标准配置';

INSERT INTO eq_wash_standard (device_type, wash_type, standard_duration, wash_steps, alert_threshold) VALUES
(1, 1, 5, '["1.排空药渣","2.清水冲洗3遍","3.检查加热管","4.空载运行1分钟"]', 10),
(1, 2, 15, '["1.排空药渣","2.碱水浸泡5分钟","3.毛刷清洗加热管","4.清水冲洗5遍","5.消毒水喷洒","6.空载运行2分钟"]', 20),
(2, 1, 3, '["1.清理包装膜残渣","2.擦拭封装口","3.空包测试1次"]', 8),
(2, 2, 8, '["1.清理包装膜残渣","2.拆卸封装口深度清洁","3.消毒擦拭","4.空包测试3次"]', 15);

-- 5. 异常工单表
CREATE TABLE prod_exception_order (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    exception_no        VARCHAR(32) NOT NULL COMMENT '异常单号',
    task_id             BIGINT COMMENT '关联任务',
    device_id           BIGINT COMMENT '关联设备',
    exception_type      TINYINT NOT NULL COMMENT '1=设备故障 2=停电 3=药材缺货 4=操作异常 5=打印失败 6=其他',
    exception_level     TINYINT NOT NULL COMMENT '1=一般 2=严重 3=紧急',
    description         VARCHAR(1000) NOT NULL,
    current_status      TINYINT DEFAULT 0 COMMENT '0=待处理 1=处理中 2=已解决 3=已升级',
    handler_id          BIGINT COMMENT '处理人',
    handle_result       VARCHAR(500),
    handle_time         DATETIME,
    escalated           TINYINT DEFAULT 0,
    escalate_time       DATETIME,
    exception_log_id    BIGINT COMMENT '关联技术档案',
    tenant_id           VARCHAR(32) DEFAULT 'default',
    deleted             INT DEFAULT 0,
    created_at          DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_no (exception_no),
    INDEX idx_status (current_status),
    INDEX idx_task (task_id),
    INDEX idx_device (device_id),
    INDEX idx_handler (handler_id, current_status),
    INDEX idx_level (exception_level),
    INDEX idx_created (created_at)
) COMMENT='异常工单表';

-- 6. 处方药材表扩展
ALTER TABLE prod_prescription_medicine
    ADD COLUMN batch_no VARCHAR(64) COMMENT '药材批号' AFTER unit,
    ADD COLUMN is_toxic TINYINT DEFAULT 0 COMMENT '是否毒性药材' AFTER batch_no,
    ADD COLUMN toxicity_level TINYINT COMMENT '毒性等级' AFTER is_toxic,
    ADD INDEX idx_batch_no (batch_no),
    ADD INDEX idx_toxic (is_toxic, toxicity_level);

-- 7. 任务表扩展
ALTER TABLE prod_task
    ADD COLUMN suspended_from VARCHAR(32) COMMENT '挂起前状态' AFTER status,
    ADD COLUMN suspend_reason VARCHAR(200) COMMENT '挂起原因' AFTER suspended_from,
    ADD COLUMN suspend_time DATETIME COMMENT '挂起时间' AFTER suspend_reason,
    ADD COLUMN expected_resume_time DATETIME COMMENT '预计恢复时间' AFTER suspend_time,
    ADD COLUMN scheme_snapshot JSON COMMENT '工艺参数快照' AFTER scheme_id,
    ADD INDEX idx_suspended (suspended_from),
    ADD INDEX idx_suspend_time (suspend_time);

-- 8. 医院表扩展
ALTER TABLE md_hospital
    ADD COLUMN his_protocol VARCHAR(32) DEFAULT 'JSON' COMMENT '协议' AFTER status,
    ADD COLUMN adapter_class VARCHAR(200) COMMENT '适配器实现类' AFTER his_protocol;

-- 9. 接口日志表扩展
ALTER TABLE sys_interface_log
    ADD COLUMN hospital_id BIGINT COMMENT '来源医院ID' AFTER interface_id,
    ADD INDEX idx_hospital (hospital_id);

-- 10. 温度日志表扩展
ALTER TABLE eq_temperature_log
    ADD COLUMN is_abnormal TINYINT DEFAULT 0 COMMENT '是否异常' AFTER alarm_reason,
    ADD COLUMN expected_temp DECIMAL(5,2) COMMENT '标准温度' AFTER is_abnormal,
    ADD COLUMN deviation DECIMAL(5,2) COMMENT '偏差值' AFTER expected_temp,
    ADD INDEX idx_abnormal (is_abnormal, recorded_at);
