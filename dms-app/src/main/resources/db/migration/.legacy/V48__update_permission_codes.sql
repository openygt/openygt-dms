-- V48__update_permission_codes.sql
-- 作者：开发执行人
-- 日期：2026-05-07
-- 说明：统一权限码，list → view
-- 依据：第一批系统基础治理专项 - 权限码统一

-- ==================== 菜单权限更新 ====================
UPDATE sys_menu SET permission = 'eq:device:view' WHERE permission = 'eq:device:list';
UPDATE sys_menu SET permission = 'eq:device:view:mine' WHERE permission = 'eq:device:list:mine';
UPDATE sys_menu SET permission = 'prod:prescription:view' WHERE permission = 'prod:prescription:list';
UPDATE sys_menu SET permission = 'qt:inspect:view' WHERE permission = 'qt:inspect:list';
UPDATE sys_menu SET permission = 'sys:user:view' WHERE permission = 'sys:user:list';
UPDATE sys_menu SET permission = 'sys:role:view' WHERE permission = 'sys:role:list';
UPDATE sys_menu SET permission = 'sys:menu:view' WHERE permission = 'sys:menu:list';
UPDATE sys_menu SET permission = 'sys:config:view' WHERE permission = 'sys:config:list';
UPDATE sys_menu SET permission = 'sys:log:view' WHERE permission = 'sys:log:list';
UPDATE sys_menu SET permission = 'inv:log:view' WHERE permission = 'inv:log:list';

-- ==================== 校验 ====================
SELECT permission, COUNT(*) AS count FROM sys_menu WHERE permission LIKE '%:list' GROUP BY permission;
-- 预期返回 0 行
