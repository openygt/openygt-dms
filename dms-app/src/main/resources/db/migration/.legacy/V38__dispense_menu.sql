-- V38: 补充发药管理菜单数据
-- 前端 Layout.vue 已存在「发药管理」一级菜单及子菜单，但后端 sys_menu 初始化数据缺失，现补齐。

-- 1. 插入「发药管理」一级菜单（目录）
INSERT IGNORE INTO sys_menu (name, code, path, component, icon, sort_order, menu_type, parent_id, permission, status)
VALUES ('发药管理', 'dispense', '/dispense', NULL, 'MedicineBoxOutlined', 6, 0, 0, NULL, 'ACTIVE');

-- 2. 插入子菜单：成品暂存
INSERT IGNORE INTO sys_menu (name, code, path, component, icon, sort_order, menu_type, parent_id, permission, status)
SELECT '成品暂存', 'shelf_manage', '/shelf-manage', 'warehouse/ShelfManageView', 'AppstoreOutlined', 1, 1, id, 'inv:storage:view', 'ACTIVE'
FROM sys_menu WHERE code = 'dispense';

-- 3. 插入子菜单：发药确认
INSERT IGNORE INTO sys_menu (name, code, path, component, icon, sort_order, menu_type, parent_id, permission, status)
SELECT '发药确认', 'delivery_manage', '/prod/delivery', 'prod/DeliveryManageView', 'CheckCircleOutlined', 2, 1, id, 'prod:dispatch:view', 'ACTIVE'
FROM sys_menu WHERE code = 'dispense';

-- 4. 插入子菜单：进度查询
INSERT IGNORE INTO sys_menu (name, code, path, component, icon, sort_order, menu_type, parent_id, permission, status)
SELECT '进度查询', 'patient_query', '/patient-query', 'patient/PatientQueryView', 'SearchOutlined', 3, 1, id, 'prod:progress:view', 'ACTIVE'
FROM sys_menu WHERE code = 'dispense';

-- 5. 为管理员角色绑定发药管理全部菜单
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r, sys_menu m
WHERE r.role_code = 'ROLE_ADMIN' AND m.code IN ('dispense', 'shelf_manage', 'delivery_manage', 'patient_query');

-- 6. 为车间主任绑定发药管理全部菜单（前端 hasPermission 中 DIRECTOR 拥有全部权限）
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r, sys_menu m
WHERE r.role_code = 'ROLE_DIRECTOR' AND m.code IN ('dispense', 'shelf_manage', 'delivery_manage', 'patient_query');

-- 7. 为班组长绑定发药管理全部菜单（前端 leaderPerms 包含 inv:storage:view / prod:dispatch:view / prod:progress:view）
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r, sys_menu m
WHERE r.role_code = 'ROLE_LEADER' AND m.code IN ('dispense', 'shelf_manage', 'delivery_manage', 'patient_query');
