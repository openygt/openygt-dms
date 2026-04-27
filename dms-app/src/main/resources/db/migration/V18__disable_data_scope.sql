-- V18: 数据权限运行时下线（SQLite 兼容版）
-- 基于 docs/sql/V24__disable_data_scope_runtime.sql 转换

-- 1. 插入运行时开关
INSERT OR IGNORE INTO sys_config (config_key, config_value, description)
VALUES ('rbac.data-scope.enabled', 'false', 'V18：数据权限运行时开关（false=下线，仅菜单/按钮权限生效）');

-- 2. 更新现有配置为 false
UPDATE sys_config SET config_value = 'false', description = 'V18：数据权限运行时开关（false=下线，仅菜单/按钮权限生效）'
WHERE config_key = 'rbac.data-scope.enabled';
