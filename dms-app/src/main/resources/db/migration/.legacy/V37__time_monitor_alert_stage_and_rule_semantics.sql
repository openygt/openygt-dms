-- V37: 时效监控规则语义收口与预警阶段补全

ALTER TABLE dms_alert_log
    ADD COLUMN stage VARCHAR(50) NULL COMMENT '触发预警的生产阶段' AFTER task_id;

ALTER TABLE dms_time_rule
    MODIFY COLUMN prescription_type VARCHAR(50) DEFAULT 'NORMAL' COMMENT '适用处方类型：NORMAL-普通, EMERGENCY-普通急诊, CRITICAL_EMERGENCY-危重急诊',
    MODIFY COLUMN stage VARCHAR(50) COMMENT '阶段：SOAK-泡药, FIRST_DECOCTION-一煎, SECOND_DECOCTION-二煎, DECOCT-煎药, WRAP-包装';
