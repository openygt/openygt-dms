-- ============================================
-- cleanup_prod_command_demo_data.sql
-- 生产指挥模块垃圾数据清理脚本
-- 警告：此脚本仅用于演示环境数据重建，禁止在生产正式环境直接执行
-- 执行前请先备份数据库
-- ============================================

SET FOREIGN_KEY_CHECKS = 0;

-- --------------------------------------------------
-- 第一步：删除强依赖子表
-- --------------------------------------------------

-- 1.1 告警通知子表
DELETE FROM eq_alarm_notification WHERE alarm_id IN (SELECT id FROM eq_device_alarm WHERE tenant_id = 'default');

-- 1.2 时效预警记录表（无 tenant_id 字段，直接清空）
DELETE FROM dms_alert_log;

-- 1.3 煎煮追溯事件子表
DELETE FROM decoction_trace_event WHERE tenant_id = 'default';

-- 1.4 任务步骤日志
DELETE FROM prod_step_log WHERE tenant_id = 'default';

-- 1.5 任务状态历史
DELETE FROM prod_task_status_history WHERE tenant_id = 'default';

-- --------------------------------------------------
-- 第二步：删除监控、指令、快照、统计表
-- --------------------------------------------------

-- 2.1 时效监控实例表（无 tenant_id 字段，直接清空）
DELETE FROM dms_time_monitor;

-- 2.2 设备指令表（保留 V44 之前的旧数据可选清理）
DELETE FROM device_command WHERE tenant_id = 'default';

-- 2.3 设备实时状态快照表
DELETE FROM eq_device_status WHERE tenant_id = 'default';

-- 2.4 设备利用率统计表
DELETE FROM device_utilization WHERE tenant_id = 'default';

-- 2.5 工作量统计表
DELETE FROM workload_stat WHERE tenant_id = 'default';

-- 2.6 设备操作人绑定表
DELETE FROM eq_device_operator WHERE tenant_id = 'default';

-- --------------------------------------------------
-- 第三步：删除设备过程表
-- --------------------------------------------------

-- 3.1 设备分组联动规则表
DELETE FROM eq_device_group_rule WHERE tenant_id = 'default';

-- 3.2 设备维保记录表
DELETE FROM eq_device_maintenance WHERE tenant_id = 'default';

-- 3.3 设备清洗记录表
DELETE FROM eq_wash_record WHERE tenant_id = 'default';

-- --------------------------------------------------
-- 第四步：删除任务关联过程表
-- --------------------------------------------------

-- 4.1 任务智能分配表（无 tenant_id 字段，直接清空）
DELETE FROM dms_task_assignment;

-- 4.2 煎煮追溯主表
DELETE FROM decoction_trace WHERE tenant_id = 'default';

-- 4.3 设备告警主表
DELETE FROM eq_device_alarm WHERE tenant_id = 'default';

-- --------------------------------------------------
-- 第五步：按条件删除演示任务主链（谨慎操作）
-- 仅删除 source='MANUAL' 且 task_no 以 DEMO- 开头或为空的历史测试任务
-- 若当前库无 DEMO- 前缀任务，则此步影响为 0，是安全的
-- --------------------------------------------------

-- 5.1 先删除子表：处方药材
DELETE pm FROM prod_prescription_medicine pm
INNER JOIN prod_prescription p ON pm.prescription_id = p.id
WHERE p.tenant_id = 'default'
  AND (p.task_no LIKE 'DEMO-%' OR p.task_no IS NULL OR p.task_no = '');

-- 5.2 删除任务表（通过 prescription_id 关联）
DELETE t FROM prod_task t
INNER JOIN prod_prescription p ON t.prescription_id = p.id
WHERE p.tenant_id = 'default'
  AND (p.task_no LIKE 'DEMO-%' OR p.task_no IS NULL OR p.task_no = '');

-- 5.3 删除处方主档
DELETE FROM prod_prescription
WHERE tenant_id = 'default'
  AND (task_no LIKE 'DEMO-%' OR task_no IS NULL OR task_no = '');

SET FOREIGN_KEY_CHECKS = 1;
