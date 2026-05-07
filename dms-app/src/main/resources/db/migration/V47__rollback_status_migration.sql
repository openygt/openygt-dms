-- V47__rollback_status_migration.sql
-- 作者：开发执行人
-- 日期：2026-05-07
-- 说明：状态字典迁移回滚脚本（基于备份表恢复原始值）
-- 警告：仅在迁移出现问题时手动执行，不作为 Flyway 正向迁移脚本

-- ==================== prod_task 回滚 ====================
UPDATE prod_task t
  JOIN prod_task_status_backup_20260507 b ON t.id = b.id
  SET t.status = b.status;

-- ==================== prod_prescription 回滚 ====================
UPDATE prod_prescription t
  JOIN prod_prescription_status_backup_20260507 b ON t.id = b.id
  SET t.receive_status = b.receive_status;

-- ==================== eq_device 回滚 ====================
UPDATE eq_device t
  JOIN eq_device_status_backup_20260507 b ON t.id = b.id
  SET t.status = b.status;
