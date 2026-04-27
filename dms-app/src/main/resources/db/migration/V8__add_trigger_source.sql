-- V8: 给状态历史表增加触发来源和设备码字段
ALTER TABLE prod_task_status_history ADD COLUMN trigger_source VARCHAR(20);
ALTER TABLE prod_task_status_history ADD COLUMN device_code VARCHAR(50);

-- 更新现有数据的默认值
UPDATE prod_task_status_history SET trigger_source = '手动' WHERE trigger_source IS NULL;
