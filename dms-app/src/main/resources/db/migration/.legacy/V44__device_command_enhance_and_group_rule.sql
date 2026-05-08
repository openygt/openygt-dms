-- ============================================
-- V44: 设备指令增强 + 设备分组联动规则表
-- 第二批 DDL
-- ============================================

-- --------------------------------------------------
-- 1. device_command 增加指令等级、风险等级、确认标志
-- --------------------------------------------------
ALTER TABLE device_command ADD COLUMN command_level VARCHAR(20) DEFAULT 'NORMAL' COMMENT '指令等级: NORMAL/IMPORTANT/CRITICAL';
ALTER TABLE device_command ADD COLUMN risk_level VARCHAR(20) DEFAULT 'LOW' COMMENT '风险等级: LOW/MEDIUM/HIGH';
ALTER TABLE device_command ADD COLUMN require_confirm TINYINT(1) DEFAULT 0 COMMENT '是否需要确认: 0否/1是';

-- 历史数据默认置为低等级、无需确认
UPDATE device_command SET command_level = 'NORMAL' WHERE command_level IS NULL;
UPDATE device_command SET risk_level = 'LOW' WHERE risk_level IS NULL;
UPDATE device_command SET require_confirm = 0 WHERE require_confirm IS NULL;

-- --------------------------------------------------
-- 2. 设备分组联动规则表
-- --------------------------------------------------
CREATE TABLE IF NOT EXISTS eq_device_group_rule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    group_id BIGINT NOT NULL COMMENT '关联设备分组ID',
    rule_name VARCHAR(100) NOT NULL COMMENT '规则名称',
    trigger_condition VARCHAR(255) NOT NULL COMMENT '触发条件JSON',
    action_type VARCHAR(50) NOT NULL COMMENT '动作类型: START/STOP/ALARM/NOTIFY/EMERGENCY_STOP',
    target_devices TEXT COMMENT '目标设备编码列表JSON',
    enabled TINYINT(1) DEFAULT 1 COMMENT '是否启用: 0禁用/1启用',
    tenant_id VARCHAR(32) DEFAULT 'default' COMMENT '租户ID',
    deleted INT DEFAULT 0 COMMENT '逻辑删除',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备分组联动规则表';

CREATE INDEX idx_eq_device_group_rule_group_id ON eq_device_group_rule(group_id);
CREATE INDEX idx_eq_device_group_rule_enabled ON eq_device_group_rule(enabled);
