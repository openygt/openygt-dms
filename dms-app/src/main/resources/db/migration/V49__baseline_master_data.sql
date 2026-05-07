-- ============================================
-- V49：基础主数据基线（子专项A）
-- ============================================
-- 说明：
--   本脚本依赖 Day 0 源数据探查结果（三库数据源）。
--   当前为框架版本，待探查完成后填充具体 INSERT 语句。
--   三库来源：yylx_spd_yangxin_v2 / yylx_prescription / jybz
-- ============================================

-- --------------------------------------------------
-- 1. 医院主数据扩展字段（如需要）
-- --------------------------------------------------
-- TODO: 根据 Day 0 探查结果，确认是否需要扩展 md_hospital 字段
-- DESCRIBE md_hospital;

-- --------------------------------------------------
-- 2. 药材标准字典（md_medicine_dict）
-- --------------------------------------------------
-- TODO: 待 Day 0 探查后，从 yylx_spd_yangxin_v2 导入
-- CREATE TABLE IF NOT EXISTS md_medicine_dict (
--     id BIGINT PRIMARY KEY AUTO_INCREMENT,
--     medicine_code VARCHAR(50) NOT NULL COMMENT '药材编码',
--     medicine_name VARCHAR(100) NOT NULL COMMENT '药材名称',
--     pinyin_code VARCHAR(100) COMMENT '拼音码',
--     category VARCHAR(50) COMMENT '分类',
--     is_toxic TINYINT DEFAULT 0 COMMENT '是否毒性药材',
--     standard_dosage DECIMAL(10,2) COMMENT '标准剂量',
--     unit VARCHAR(20) COMMENT '单位',
--     source_db VARCHAR(50) COMMENT '来源数据库',
--     tenant_id VARCHAR(32) DEFAULT 'default',
--     deleted BIGINT DEFAULT 0,
--     created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
--     updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
--     UNIQUE KEY uk_medicine_code (medicine_code)
-- ) COMMENT='药材标准字典';

-- --------------------------------------------------
-- 3. 医院科室字典（t_department 补充）
-- --------------------------------------------------
-- TODO: 待 Day 0 探查后，从 yylx_prescription 导入
-- INSERT INTO t_department (id, department_name, hospital_id, ...) VALUES (...);

-- --------------------------------------------------
-- 4. 煎药方案模板（md_decoct_scheme 补充）
-- --------------------------------------------------
-- TODO: 待 Day 0 探查后，从 jybz 导入标准方案
-- INSERT INTO md_decoct_scheme (...) VALUES (...);

-- --------------------------------------------------
-- 5. GMP 审计：记录本次迁移
-- --------------------------------------------------
INSERT INTO data_migration_log (version, migration_type, target_table, record_count, executed_at, executed_by, status, remark)
VALUES ('V49', 'BASELINE_MASTER_DATA', 'md_medicine_dict,t_department,md_decoct_scheme', 0, NOW(), 'SYSTEM', 'FRAMEWORK', 'V49 框架已落地，待 Day 0 探查后填充具体数据');
