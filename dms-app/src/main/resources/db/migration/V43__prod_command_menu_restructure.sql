-- ============================================
-- V43: 生产指挥菜单重构 —— 第一批成果
-- 对齐最终版方案：菜单、路由、权限码统一
-- ============================================

-- --------------------------------------------------
-- 1. 生产指挥一级目录
-- --------------------------------------------------
INSERT IGNORE INTO sys_menu (name, code, path, component, icon, sort_order, menu_type, parent_id, permission, status)
VALUES ('生产指挥', 'prod_command', '/command', NULL, 'DataLine', 1, 0, 0, NULL, 'ACTIVE');

-- --------------------------------------------------
-- 2. 生产指挥子菜单（13项，按最终版方案排序）
-- --------------------------------------------------

-- 2.1 生产看板
INSERT IGNORE INTO sys_menu (name, code, path, component, icon, sort_order, menu_type, parent_id, permission, status)
SELECT '生产看板', 'cmd_dashboard', '/dashboard', 'DashboardView', 'DataLine', 1, 1, id, 'ops:dashboard:view', 'ACTIVE'
FROM sys_menu WHERE code = 'prod_command';

-- 2.2 生产记录（原排产调度改名）
INSERT IGNORE INTO sys_menu (name, code, path, component, icon, sort_order, menu_type, parent_id, permission, status)
SELECT '生产记录', 'cmd_task_record', '/task-assignment', 'prod/TaskAssignmentView', 'Document', 2, 1, id, 'prod:record:view', 'ACTIVE'
FROM sys_menu WHERE code = 'prod_command';

-- 2.3 时效监控
INSERT IGNORE INTO sys_menu (name, code, path, component, icon, sort_order, menu_type, parent_id, permission, status)
SELECT '时效监控', 'cmd_time_monitor', '/time-monitor', 'monitor/TimeMonitorView', 'Timer', 3, 1, id, 'prod:monitor:view', 'ACTIVE'
FROM sys_menu WHERE code = 'prod_command';

-- 2.4 设备监控
INSERT IGNORE INTO sys_menu (name, code, path, component, icon, sort_order, menu_type, parent_id, permission, status)
SELECT '设备监控', 'cmd_device_monitor', '/device-monitor', 'eq/DeviceMonitorView', 'Monitor', 4, 1, id, 'eq:device:monitor', 'ACTIVE'
FROM sys_menu WHERE code = 'prod_command';

-- 2.5 告警管理
INSERT IGNORE INTO sys_menu (name, code, path, component, icon, sort_order, menu_type, parent_id, permission, status)
SELECT '告警管理', 'cmd_alarm', '/alarms', 'monitor/AlarmLogView', 'Bell', 5, 1, id, 'eq:alarm:view', 'ACTIVE'
FROM sys_menu WHERE code = 'prod_command';

-- 2.6 远程急停（原设备操控改名）
INSERT IGNORE INTO sys_menu (name, code, path, component, icon, sort_order, menu_type, parent_id, permission, status)
SELECT '远程急停', 'cmd_emergency', '/device-command', 'eq/DeviceCommandView', 'SwitchButton', 6, 1, id, 'eq:device:emergency', 'ACTIVE'
FROM sys_menu WHERE code = 'prod_command';

-- 2.7 产能统计
INSERT IGNORE INTO sys_menu (name, code, path, component, icon, sort_order, menu_type, parent_id, permission, status)
SELECT '产能统计', 'cmd_capacity', '/capacity', 'ops/CapacityReportView', 'TrendCharts', 7, 1, id, 'ops:capacity:view', 'ACTIVE'
FROM sys_menu WHERE code = 'prod_command';

-- 2.8 设备效能
INSERT IGNORE INTO sys_menu (name, code, path, component, icon, sort_order, menu_type, parent_id, permission, status)
SELECT '设备效能', 'cmd_efficiency', '/device-utilization', 'eq/DeviceUtilizationView', 'Odometer', 8, 1, id, 'eq:device:efficiency', 'ACTIVE'
FROM sys_menu WHERE code = 'prod_command';

-- 2.9 设备管理（正式挂出）
INSERT IGNORE INTO sys_menu (name, code, path, component, icon, sort_order, menu_type, parent_id, permission, status)
SELECT '设备管理', 'cmd_device_list', '/devices', 'eq/DeviceListView', 'SetUp', 9, 1, id, 'eq:device:list', 'ACTIVE'
FROM sys_menu WHERE code = 'prod_command';

-- 2.10 设备联网（新增）
INSERT IGNORE INTO sys_menu (name, code, path, component, icon, sort_order, menu_type, parent_id, permission, status)
SELECT '设备联网', 'cmd_network', '/device-network', 'eq/DeviceNetworkView', 'Link', 10, 1, id, 'eq:network:view', 'ACTIVE'
FROM sys_menu WHERE code = 'prod_command';

-- 2.11 设备分组（新增）
INSERT IGNORE INTO sys_menu (name, code, path, component, icon, sort_order, menu_type, parent_id, permission, status)
SELECT '设备分组', 'cmd_group', '/device-group-manage', 'eq/DeviceGroupManageView', 'Grid', 11, 1, id, 'eq:group:view', 'ACTIVE'
FROM sys_menu WHERE code = 'prod_command';

-- 2.12 设备维保（从工艺配置归位）
INSERT IGNORE INTO sys_menu (name, code, path, component, icon, sort_order, menu_type, parent_id, permission, status)
SELECT '设备维保', 'cmd_maintenance', '/device-maintenance', 'eq/DeviceMaintenanceView', 'Tools', 12, 1, id, 'eq:maint:view', 'ACTIVE'
FROM sys_menu WHERE code = 'prod_command';

-- 2.13 清洗记录（从工艺配置归位）
INSERT IGNORE INTO sys_menu (name, code, path, component, icon, sort_order, menu_type, parent_id, permission, status)
SELECT '清洗记录', 'cmd_wash', '/wash-record', 'wash-record/WashRecordView', 'Brush', 13, 1, id, 'eq:wash:view', 'ACTIVE'
FROM sys_menu WHERE code = 'prod_command';

-- --------------------------------------------------
-- 3. 停用不再作为正式二级菜单的入口
-- --------------------------------------------------

-- 3.1 停用数字孪生（能力并入设备监控）
-- 若之前已存在数字孪生菜单记录，标记为 INACTIVE
UPDATE sys_menu SET status = 'INACTIVE', updated_at = NOW()
WHERE code = 'digital_twin' OR name = '数字孪生';

-- 3.2 更新旧权限码 → 新权限码（若存在旧记录）
UPDATE sys_menu SET permission = 'prod:record:view', name = '生产记录', code = 'cmd_task_record', updated_at = NOW()
WHERE permission = 'prod:assignment:view' AND status = 'ACTIVE';

UPDATE sys_menu SET permission = 'eq:device:emergency', name = '远程急停', code = 'cmd_emergency', updated_at = NOW()
WHERE permission = 'eq:device:control' AND status = 'ACTIVE';

UPDATE sys_menu SET permission = 'eq:alarm:view', name = '告警管理', updated_at = NOW()
WHERE permission = 'eq:alarm:list' AND status = 'ACTIVE';

-- --------------------------------------------------
-- 4. 角色菜单绑定（新菜单）
-- --------------------------------------------------

-- 4.1 ADMIN 绑定生产指挥全部菜单
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r, sys_menu m
WHERE r.role_code = 'ROLE_ADMIN' AND m.code LIKE 'cmd_%';

-- 4.2 DIRECTOR 绑定生产指挥全部菜单
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r, sys_menu m
WHERE r.role_code = 'ROLE_DIRECTOR' AND m.code LIKE 'cmd_%';

-- 4.3 LEADER 绑定生产指挥查看类菜单（不含远程急停、设备联网、设备分组编辑）
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r, sys_menu m
WHERE r.role_code = 'ROLE_LEADER'
  AND m.code IN (
    'cmd_dashboard', 'cmd_task_record', 'cmd_time_monitor',
    'cmd_device_monitor', 'cmd_alarm', 'cmd_capacity',
    'cmd_efficiency', 'cmd_device_list'
  );

-- 4.4 WORKER 绑定基础查看菜单
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r, sys_menu m
WHERE r.role_code = 'ROLE_WORKER'
  AND m.code IN (
    'cmd_dashboard', 'cmd_task_record', 'cmd_time_monitor',
    'cmd_device_monitor', 'cmd_capacity'
  );

-- 4.5 INSPECTOR 绑定查看类 + 质量相关
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r, sys_menu m
WHERE r.role_code = 'ROLE_INSPECTOR'
  AND m.code IN (
    'cmd_dashboard', 'cmd_task_record', 'cmd_time_monitor',
    'cmd_device_monitor', 'cmd_alarm', 'cmd_capacity',
    'cmd_efficiency'
  );
