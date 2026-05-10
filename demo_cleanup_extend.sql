-- ============================================
-- demo_cleanup_extend.sql
-- Demo环境补充清理SQL（配合cleanup_prod_command_demo_data.sql使用）
-- 清理范围：日志、追溯、温度记录等现有脚本未覆盖的表
-- 警告：仅用于演示环境，禁止在生产环境执行
-- ============================================

SET FOREIGN_KEY_CHECKS = 0;

-- --------------------------------------------------
-- 1. 系统日志表
-- --------------------------------------------------
DELETE FROM sys_log WHERE created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY);
DELETE FROM sys_log_archive WHERE created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY);

-- --------------------------------------------------
-- 2. 追溯表
-- --------------------------------------------------
DELETE FROM trc_prescription_trace;
DELETE FROM trc_trace_event;

-- --------------------------------------------------
-- 3. 任务状态历史（双重保险，现有脚本已清此处再清）
-- --------------------------------------------------
DELETE FROM prod_task_status_history;

-- --------------------------------------------------
-- 4. 设备温度记录
-- --------------------------------------------------
DELETE FROM eq_temperature_log WHERE created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY);
DELETE FROM eq_temperature_log_archive WHERE created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY);

-- --------------------------------------------------
-- 5. 其他运行数据
-- --------------------------------------------------
DELETE FROM prod_employee_barcode WHERE created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY);
DELETE FROM prod_patient_notify WHERE created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY);
DELETE FROM prod_patient_token WHERE created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY);
DELETE FROM prod_exception_log WHERE created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY);
DELETE FROM prod_handover_detail WHERE created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY);

SET FOREIGN_KEY_CHECKS = 1;
