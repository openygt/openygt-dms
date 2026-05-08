-- ============================================
-- demo_cleanup.sql
-- 演示数据清理脚本（幂等，支持重复执行）
-- ============================================
-- 设计原则：
--   1. 仅删除 id >= 900000000 的演示数据，不触碰真实数据
--   2. 先删子表，再删主表，关闭外键检查避免约束冲突
--   3. 执行前校验：确认没有 id < 900000000 的 DEMO 水印数据
--   4. 执行后验证：统计残留记录数，应为 0
-- ============================================

-- --------------------------------------------------
-- Phase 0: 执行前校验
-- --------------------------------------------------

SELECT '【校验1】检查 prod_task 中是否存在 id < 900000000 且 barcode LIKE DEMO-% 的记录' AS check_item;
SELECT COUNT(*) AS dangerous_count FROM prod_task
WHERE id < 900000000 AND (barcode LIKE 'DEMO-%' OR barcode LIKE 'demo-%');
-- 结果必须为 0，否则说明存在小ID演示数据，需人工确认后再执行

SELECT '【校验2】检查 prod_prescription 中是否存在 id < 900000000 且 task_no LIKE DEMO-% 的记录' AS check_item;
SELECT COUNT(*) AS dangerous_count FROM prod_prescription
WHERE id < 900000000 AND (task_no LIKE 'DEMO-%' OR task_no LIKE 'demo-%');
-- 结果必须为 0

SELECT '【校验3】检查当前最大 ID 是否安全（< 900000000）' AS check_item;
SELECT 'prod_task' AS tbl, MAX(id) AS max_id FROM prod_task
UNION ALL SELECT 'prod_prescription', MAX(id) FROM prod_prescription
UNION ALL SELECT 'eq_device', MAX(id) FROM eq_device
UNION ALL SELECT 'prod_step_log', MAX(id) FROM prod_step_log
UNION ALL SELECT 'prod_task_status_history', MAX(id) FROM prod_task_status_history;
-- 所有 MAX(id) 必须 < 900000000（真实数据在安全区）

-- --------------------------------------------------
-- Phase 1: 关闭外键检查
-- --------------------------------------------------
SET FOREIGN_KEY_CHECKS = 0;

-- --------------------------------------------------
-- Phase 2: 删除任务关联子表（按 task_id 关联）
-- --------------------------------------------------

DELETE FROM prod_work_record WHERE task_id >= 900000000;
DELETE FROM prod_handover_detail WHERE task_id >= 900000000;
DELETE FROM prod_step_log WHERE task_id >= 900000000;
DELETE FROM prod_task_status_history WHERE task_id >= 900000000;
DELETE FROM dms_task_assignment WHERE task_id >= 900000000;
DELETE FROM dms_time_monitor WHERE task_id >= 900000000;
DELETE FROM dms_alert_log WHERE task_id >= 900000000;
<<<<<<< HEAD
DELETE FROM eq_wash_record WHERE task_id >= 900000000 OR id >= 900000000;
=======
DELETE FROM eq_wash_record WHERE task_id >= 900000000;
>>>>>>> bf1f1bb9ff4a8c5b9f5c5f0b553587f140bb24a6

-- --------------------------------------------------
-- Phase 3: 删除质检/交接/追溯中间表（按 task_id 关联）
-- --------------------------------------------------

-- 质检子表 → 质检主表
DELETE FROM qt_inspection_item WHERE inspection_id IN (
    SELECT id FROM qt_inspection WHERE task_id >= 900000000
);
DELETE FROM qt_inspection WHERE task_id >= 900000000;

DELETE FROM qt_retain_sample WHERE task_id >= 900000000;
DELETE FROM t_delivery_record WHERE task_id >= 900000000;
DELETE FROM decoction_trace WHERE task_id >= 900000000;

-- --------------------------------------------------
-- Phase 4: 删除任务与处方主链
-- --------------------------------------------------

DELETE FROM prod_task WHERE id >= 900000000;
DELETE FROM prod_prescription_medicine WHERE prescription_id >= 900000000;
DELETE FROM prod_prescription WHERE id >= 900000000;

-- --------------------------------------------------
-- Phase 5: 删除设备关联独立表（按 id 直接删）
-- --------------------------------------------------

DELETE FROM device_command WHERE id >= 900000000;
DELETE FROM eq_device_alarm WHERE id >= 900000000;
DELETE FROM eq_alarm_notification WHERE id >= 900000000;
DELETE FROM eq_device_maintenance WHERE id >= 900000000;
DELETE FROM eq_device_status WHERE id >= 900000000;
DELETE FROM workload_stat WHERE id >= 900000000;
DELETE FROM device_utilization WHERE id >= 900000000;
DELETE FROM eq_device_operator WHERE id >= 900000000;
DELETE FROM eq_device_group_rule WHERE id >= 900000000;

-- --------------------------------------------------
-- Phase 6: 恢复外键检查
-- --------------------------------------------------
SET FOREIGN_KEY_CHECKS = 1;

-- --------------------------------------------------
-- Phase 7: 执行后验证
-- --------------------------------------------------

SELECT '【验证】prod_task 残留演示数据' AS check_item, COUNT(*) AS remain FROM prod_task WHERE id >= 900000000;
SELECT '【验证】prod_prescription 残留演示数据' AS check_item, COUNT(*) AS remain FROM prod_prescription WHERE id >= 900000000;
SELECT '【验证】prod_step_log 残留演示数据' AS check_item, COUNT(*) AS remain FROM prod_step_log WHERE id >= 900000000;
SELECT '【验证】prod_task_status_history 残留演示数据' AS check_item, COUNT(*) AS remain FROM prod_task_status_history WHERE id >= 900000000;
SELECT '【验证】qt_inspection 残留演示数据' AS check_item, COUNT(*) AS remain FROM qt_inspection WHERE task_id >= 900000000;
SELECT '【验证】decoction_trace 残留演示数据' AS check_item, COUNT(*) AS remain FROM decoction_trace WHERE id >= 900000000;
SELECT '【验证】device_command 残留演示数据' AS check_item, COUNT(*) AS remain FROM device_command WHERE id >= 900000000;
SELECT '【验证】eq_device_alarm 残留演示数据' AS check_item, COUNT(*) AS remain FROM eq_device_alarm WHERE id >= 900000000;
SELECT '【验证】eq_device_status 残留演示数据' AS check_item, COUNT(*) AS remain FROM eq_device_status WHERE id >= 900000000;
SELECT '【验证】workload_stat 残留演示数据' AS check_item, COUNT(*) AS remain FROM workload_stat WHERE id >= 900000000;
SELECT '【验证】dms_alert_log 残留演示数据' AS check_item, COUNT(*) AS remain FROM dms_alert_log WHERE id >= 900000000;
SELECT '【验证】dms_time_monitor 残留演示数据' AS check_item, COUNT(*) AS remain FROM dms_time_monitor WHERE id >= 900000000;
SELECT '【验证】device_utilization 残留演示数据' AS check_item, COUNT(*) AS remain FROM device_utilization WHERE id >= 900000000;
-- 以上所有 remain 必须为 0
