-- V29: 高频易落地优化项数据库变更
-- 包含: 时效预警升级、院区色彩隔离、审计日志补全、标签模板院区颜色

-- 1. 院区主数据 - 添加院区标识颜色
ALTER TABLE md_hospital ADD COLUMN color_code VARCHAR(16) NULL COMMENT '院区标识颜色(如#FF5722)' AFTER tenant_id;

-- 2. 标签模板 - 添加院区颜色字段
ALTER TABLE t_label_template ADD COLUMN hospital_color VARCHAR(16) NULL COMMENT '关联院区颜色' AFTER height_mm;

-- 3. 时效监控实例 - 添加预警相关字段
ALTER TABLE prod_time_monitor ADD COLUMN alert_level INT DEFAULT 0 COMMENT '告警级别:0=正常 1=预警 2=超时 3=告警 4=严重' AFTER remark;
ALTER TABLE prod_time_monitor ADD COLUMN warning_count INT DEFAULT 0 COMMENT '累计告警次数' AFTER alert_level;
ALTER TABLE prod_time_monitor ADD COLUMN last_warning_time DATETIME NULL COMMENT '最近一次告警时间' AFTER warning_count;
ALTER TABLE prod_time_monitor ADD COLUMN status INT DEFAULT 1 COMMENT '监控状态:1=计划 2=执行中 3=已超时 4=已完成' AFTER last_warning_time;

-- 4. PDA审计日志 - 补全用户ID
ALTER TABLE pda_audit_log ADD COLUMN user_id VARCHAR(64) NULL COMMENT '操作用户ID' AFTER operator_id;

-- 5. 预警日志表
CREATE TABLE IF NOT EXISTS prod_alert_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    monitor_id BIGINT NOT NULL COMMENT '关联时效监控ID',
    task_id BIGINT NOT NULL COMMENT '关联任务ID',
    alert_level INT NOT NULL COMMENT '告警级别',
    alert_type VARCHAR(32) NOT NULL COMMENT '告警类型:TIMEOUT/QUALITY/DEVICE',
    alert_content VARCHAR(512) NOT NULL COMMENT '告警内容',
    notify_target VARCHAR(256) NULL COMMENT '通知对象',
    is_resolved TINYINT DEFAULT 0 COMMENT '是否已处理:0=否 1=是',
    resolved_by VARCHAR(64) NULL COMMENT '处理人',
    resolved_at DATETIME NULL COMMENT '处理时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_task_id (task_id),
    INDEX idx_monitor_id (monitor_id),
    INDEX idx_alert_level (alert_level),
    INDEX idx_is_resolved (is_resolved)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='生产预警日志';
