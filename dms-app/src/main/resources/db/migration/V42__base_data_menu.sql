-- ============================================
-- V42: 补齐"基础数据"一级菜单及其子菜单
-- 前端 Layout.vue 已存在「基础数据」一级菜单及子菜单，但后端 sys_menu 初始化数据缺失，现补齐。
-- ============================================

-- 1. 插入"基础数据"一级目录（menu_type=0 表示目录，parent_id=0 表示一级菜单）
INSERT IGNORE INTO sys_menu (name, code, path, component, icon, sort_order, menu_type, parent_id, permission, status)
VALUES ('基础数据', 'base_data', '/base', NULL, 'OfficeBuilding', 7, 0, 0, NULL, 'ACTIVE');

-- 2. 插入子菜单：医院管理
INSERT IGNORE INTO sys_menu (name, code, path, component, icon, sort_order, menu_type, parent_id, permission, status)
SELECT '医院管理', 'base_hospital', '/hospitals', 'md/HospitalListView', 'HomeFilled', 1, 1, id, 'md:hospital:view', 'ACTIVE'
FROM sys_menu WHERE code = 'base_data';

-- 3. 插入子菜单：科室管理
INSERT IGNORE INTO sys_menu (name, code, path, component, icon, sort_order, menu_type, parent_id, permission, status)
SELECT '科室管理', 'base_department', '/base/department', 'base/DepartmentManageView', 'OfficeBuilding', 2, 1, id, 'md:dept:view', 'ACTIVE'
FROM sys_menu WHERE code = 'base_data';

-- 4. 插入子菜单：医师管理
INSERT IGNORE INTO sys_menu (name, code, path, component, icon, sort_order, menu_type, parent_id, permission, status)
SELECT '医师管理', 'base_doctor', '/base/doctor', 'base/DoctorManageView', 'UserFilled', 3, 1, id, 'md:doctor:view', 'ACTIVE'
FROM sys_menu WHERE code = 'base_data';

-- 5. 插入子菜单：药材管理
INSERT IGNORE INTO sys_menu (name, code, path, component, icon, sort_order, menu_type, parent_id, permission, status)
SELECT '药材管理', 'base_medicine', '/base/medicine', 'base/MedicineCatalogView', 'FirstAidKit', 4, 1, id, 'md:herb:view', 'ACTIVE'
FROM sys_menu WHERE code = 'base_data';

-- 6. 插入子菜单：毒性药材管理
INSERT IGNORE INTO sys_menu (name, code, path, component, icon, sort_order, menu_type, parent_id, permission, status)
SELECT '毒性药材管理', 'base_toxic_medicine', '/toxic-medicine', 'toxic-medicine/ToxicMedicineView', 'WarningFilled', 5, 1, id, 'base:toxic:manage', 'ACTIVE'
FROM sys_menu WHERE code = 'base_data';

-- 7. 插入子菜单：人员管理
INSERT IGNORE INTO sys_menu (name, code, path, component, icon, sort_order, menu_type, parent_id, permission, status)
SELECT '人员管理', 'base_staff', '/users', 'system/UserListView', 'UserFilled', 6, 1, id, 'sys:user:view', 'ACTIVE'
FROM sys_menu WHERE code = 'base_data';

-- 8. 插入子菜单：身份条码
INSERT IGNORE INTO sys_menu (name, code, path, component, icon, sort_order, menu_type, parent_id, permission, status)
SELECT '身份条码', 'base_id_card', '/employee-barcode', 'system/EmployeeBarcodeView', 'Postcard', 7, 1, id, 'sys:barcode:view', 'ACTIVE'
FROM sys_menu WHERE code = 'base_data';

-- 9. 为管理员角色绑定基础数据全部菜单
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r, sys_menu m
WHERE r.role_code = 'ROLE_ADMIN' AND m.code LIKE 'base_%';

-- 10. 为车间主任绑定基础数据全部菜单（前端 hasPermission 中 DIRECTOR 拥有全部权限）
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r, sys_menu m
WHERE r.role_code = 'ROLE_DIRECTOR' AND m.code LIKE 'base_%';

-- 11. 为班组长绑定基础数据全部菜单（前端 leaderPerms 包含 md:hospital:view / md:dept:view / md:doctor:view / md:herb:view / sys:user:view / sys:barcode:view）
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r, sys_menu m
WHERE r.role_code = 'ROLE_LEADER' AND m.code LIKE 'base_%';
