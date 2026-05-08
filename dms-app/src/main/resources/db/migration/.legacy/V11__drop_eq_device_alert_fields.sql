-- 删除 EqDevice 冗余字段（告警信息已迁移至 eq_device_alarm 表）
-- 注：MySQL 8.0 不支持 DROP COLUMN IF EXISTS，且当前基线中这些列从未存在过，故跳过
SELECT 1;
