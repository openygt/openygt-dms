-- ============================================
-- V50：行业字典基线（子专项A）
-- ============================================
-- 说明：
--   本脚本依赖 Day 0 源数据探查结果。
--   当前为框架版本，待探查完成后填充具体 INSERT 语句。
--   行业字典包括：药材分类、煎药方法、包装规格、配送方式等。
-- ============================================

-- --------------------------------------------------
-- 1. 药材分类字典（md_medicine_category）
-- --------------------------------------------------
-- TODO: 待 Day 0 探查后建表并导入
-- CREATE TABLE IF NOT EXISTS md_medicine_category (
--     id BIGINT PRIMARY KEY AUTO_INCREMENT,
--     category_code VARCHAR(50) NOT NULL COMMENT '分类编码',
--     category_name VARCHAR(100) NOT NULL COMMENT '分类名称',
--     parent_id BIGINT DEFAULT 0 COMMENT '父级ID',
--     sort_order INT DEFAULT 0,
--     tenant_id VARCHAR(32) DEFAULT 'default',
--     deleted BIGINT DEFAULT 0,
--     created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
--     UNIQUE KEY uk_category_code (category_code)
-- ) COMMENT='药材分类字典';

-- --------------------------------------------------
-- 2. 煎药方法字典（md_decoct_method）
-- --------------------------------------------------
-- TODO: 待 Day 0 探查后建表并导入
-- CREATE TABLE IF NOT EXISTS md_decoct_method (
--     id BIGINT PRIMARY KEY AUTO_INCREMENT,
--     method_code VARCHAR(50) NOT NULL COMMENT '方法编码',
--     method_name VARCHAR(100) NOT NULL COMMENT '方法名称',
--     description VARCHAR(500) COMMENT '说明',
--     sort_order INT DEFAULT 0,
--     tenant_id VARCHAR(32) DEFAULT 'default',
--     deleted BIGINT DEFAULT 0,
--     created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
--     UNIQUE KEY uk_method_code (method_code)
-- ) COMMENT='煎药方法字典';

-- --------------------------------------------------
-- 3. 包装规格字典（t_package_spec 补充）
-- --------------------------------------------------
-- TODO: 待 Day 0 探查后补充 INSERT
-- INSERT INTO t_package_spec (id, spec_name, capacity_ml, ...) VALUES (...);

-- --------------------------------------------------
-- 4. 配送方式字典（t_delivery_type）
-- --------------------------------------------------
-- TODO: 待 Day 0 探查后建表或补充现有表

-- --------------------------------------------------
-- 5. GMP 审计：记录本次迁移
-- --------------------------------------------------
INSERT INTO data_migration_log (version, migration_type, target_table, record_count, executed_at, executed_by, status, remark)
VALUES ('V50', 'BASELINE_INDUSTRY_DICT', 'md_medicine_category,md_decoct_method,t_package_spec', 0, NOW(), 'SYSTEM', 'FRAMEWORK', 'V50 框架已落地，待 Day 0 探查后填充具体数据');
