-- ============================================
-- V6: 设备心跳与生产悲观锁预备
-- ============================================

-- 1. eq_device 新增最后心跳时间
ALTER TABLE eq_device ADD COLUMN last_heartbeat DATETIME;

-- 2. 【MySQL 迁移后生效】为 prod_task 添加悲观锁查询支持
-- SQLite 不支持 SELECT FOR UPDATE，以下 SQL 在 MySQL 环境下执行：
-- 无需 DDL 变更，只需在 Mapper XML/注解中使用 SELECT ... FOR UPDATE
