-- V17: 语音拨号弃用标记（SQLite 兼容版）
-- 基于 docs/sql/V23__deprecate_voice_alarm.sql 转换

-- 1. 插入系统配置开关（彻底关闭拨号能力）
INSERT OR IGNORE INTO sys_config (config_key, config_value, description)
VALUES ('voice.alarm.enabled', 'false', 'V17：语音拨号总开关（false=彻底关闭）');

-- 2. 更新现有配置为 false
UPDATE sys_config SET config_value = 'false', description = 'V17：语音拨号总开关（false=彻底关闭）'
WHERE config_key = 'voice.alarm.enabled';
