-- 删除 EqDevice 冗余字段（告警信息已迁移至 eq_device_alarm 表）
ALTER TABLE eq_device DROP COLUMN IF EXISTS alert_time;
ALTER TABLE eq_device DROP COLUMN IF EXISTS resolved_by;
ALTER TABLE eq_device DROP COLUMN IF EXISTS resolved_at;
