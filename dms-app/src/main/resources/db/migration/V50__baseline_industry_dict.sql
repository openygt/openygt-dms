-- ============================================
-- V50：行业字典基线（子专项A）
-- ============================================
-- 说明：
--   本脚本基于 Day 0 源数据探查结果编写。
--   来源：yylx_prescription.sys_dict_data / sys_dict_type
--   策略：将各医院分散的字典归一化为统一标准行业字典
-- ============================================

-- --------------------------------------------------
-- 1. 煎煮方式字典（md_decoct_method）
-- 来源：yylx_prescription.sys_dict_data (decocting + drug_decocting_*)
-- --------------------------------------------------
CREATE TABLE IF NOT EXISTS md_decoct_method (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    method_code VARCHAR(64) COMMENT '编码',
    method_name VARCHAR(255) NOT NULL COMMENT '名称',
    method_type VARCHAR(64) COMMENT '类型 GROUP/INDIVIDUAL',
    sort INT DEFAULT 0 COMMENT '排序',
    deleted BIGINT DEFAULT 0 COMMENT '逻辑删除',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    tenant_id VARCHAR(64) DEFAULT 'default' COMMENT '租户ID',
    UNIQUE KEY uk_method_name (method_name),
    KEY idx_method_type (method_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='煎煮方式字典';

-- --------------------------------------------------
-- 2. 处方用法字典（md_prescription_usage）
-- 来源：yylx_prescription.sys_dict_data (pre_takemethod_*)
-- --------------------------------------------------
CREATE TABLE IF NOT EXISTS md_prescription_usage (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    usage_code VARCHAR(64) COMMENT '编码',
    usage_name VARCHAR(255) NOT NULL COMMENT '名称',
    sort INT DEFAULT 0 COMMENT '排序',
    deleted BIGINT DEFAULT 0 COMMENT '逻辑删除',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    tenant_id VARCHAR(64) DEFAULT 'default' COMMENT '租户ID',
    UNIQUE KEY uk_usage_name (usage_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='处方用法字典';

-- --------------------------------------------------
-- 3. 药品脚注字典（md_drug_footnote）
-- 来源：yylx_prescription.sys_dict_data (drug_footnote + drug_footnote_*)
-- --------------------------------------------------
CREATE TABLE IF NOT EXISTS md_drug_footnote (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    footnote_code VARCHAR(64) COMMENT '编码',
    footnote_name VARCHAR(255) NOT NULL COMMENT '名称',
    sort INT DEFAULT 0 COMMENT '排序',
    deleted BIGINT DEFAULT 0 COMMENT '逻辑删除',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    tenant_id VARCHAR(64) DEFAULT 'default' COMMENT '租户ID',
    UNIQUE KEY uk_footnote_name (footnote_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='药品脚注字典';

-- --------------------------------------------------
-- 4. 毒性分级字典（md_toxicity_level）
-- 来源：yylx_spd_yangxin_v2.bas_material.drug_level 去重
-- --------------------------------------------------
CREATE TABLE IF NOT EXISTS md_toxicity_level (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    level_code VARCHAR(64) COMMENT '编码',
    level_name VARCHAR(255) NOT NULL COMMENT '名称',
    description VARCHAR(500) COMMENT '说明',
    sort INT DEFAULT 0 COMMENT '排序',
    deleted BIGINT DEFAULT 0 COMMENT '逻辑删除',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    tenant_id VARCHAR(64) DEFAULT 'default' COMMENT '租户ID',
    UNIQUE KEY uk_level_name (level_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='毒性分级字典';

-- ============================================
-- 数据导入：归一化行业字典
-- ============================================

-- --------------------------------------------------
-- 1. 煎煮方式（去重合并各医院编码）
-- --------------------------------------------------
INSERT IGNORE INTO md_decoct_method (method_name, method_type, sort, tenant_id)
SELECT DISTINCT dict_label, 'INDIVIDUAL', CAST(dict_value AS UNSIGNED), 'default'
FROM yylx_prescription.sys_dict_data
WHERE dict_type LIKE 'drug_decocting_%';

INSERT IGNORE INTO md_decoct_method (method_name, method_type, sort, tenant_id)
SELECT DISTINCT dict_label, 'GROUP', CAST(dict_value AS UNSIGNED), 'default'
FROM yylx_prescription.sys_dict_data
WHERE dict_type = 'decocting'
  AND dict_label NOT IN (SELECT method_name FROM md_decoct_method);

-- --------------------------------------------------
-- 2. 处方用法（去重合并各医院编码）
-- --------------------------------------------------
INSERT IGNORE INTO md_prescription_usage (usage_name, usage_code, sort, tenant_id)
SELECT DISTINCT TRIM(dict_label), dict_value, CAST(dict_value AS UNSIGNED), 'default'
FROM yylx_prescription.sys_dict_data
WHERE dict_type LIKE 'pre_takemethod_%';

-- --------------------------------------------------
-- 3. 药品脚注（去重合并各医院编码）
-- --------------------------------------------------
INSERT IGNORE INTO md_drug_footnote (footnote_name, footnote_code, sort, tenant_id)
SELECT DISTINCT TRIM(dict_label), dict_value, CAST(dict_value AS UNSIGNED), 'default'
FROM yylx_prescription.sys_dict_data
WHERE dict_type LIKE 'drug_footnote%';

-- --------------------------------------------------
-- 4. 毒性分级（从药材主数据去重）
-- --------------------------------------------------
INSERT IGNORE INTO md_toxicity_level (level_name, level_code, description, sort, tenant_id)
SELECT DISTINCT 
    drug_level, drug_level, 
    CASE 
        WHEN drug_level = '剧毒' THEN '毒性强烈，需严格控制用量'
        WHEN drug_level = '大毒' THEN '毒性较大，需谨慎使用'
        WHEN drug_level = '有毒' THEN '有毒性，需注意用量'
        WHEN drug_level = '小毒' THEN '毒性较小，仍需注意'
        ELSE '毒性分类'
    END,
    CASE 
        WHEN drug_level = '剧毒' THEN 1
        WHEN drug_level = '大毒' THEN 2
        WHEN drug_level = '有毒' THEN 3
        WHEN drug_level = '小毒' THEN 4
        ELSE 99
    END,
    'default'
FROM yylx_spd_yangxin_v2.bas_material
WHERE drug_level IS NOT NULL AND drug_level != '';

-- ============================================
-- 导入结果校验
-- ============================================
SELECT 'md_decoct_method' AS tbl, COUNT(*) AS cnt FROM md_decoct_method
UNION ALL SELECT 'md_prescription_usage', COUNT(*) FROM md_prescription_usage
UNION ALL SELECT 'md_drug_footnote', COUNT(*) FROM md_drug_footnote
UNION ALL SELECT 'md_toxicity_level', COUNT(*) FROM md_toxicity_level;

-- ============================================
-- GMP 审计：记录本次迁移
-- ============================================
INSERT INTO data_migration_log (migration_id, table_name, step, record_count, executed_by, executed_at)
VALUES ('V50', 'md_decoct_method,md_prescription_usage,md_drug_footnote,md_toxicity_level', 'IMPORT',
    (SELECT COUNT(*) FROM md_decoct_method)
    + (SELECT COUNT(*) FROM md_prescription_usage)
    + (SELECT COUNT(*) FROM md_drug_footnote)
    + (SELECT COUNT(*) FROM md_toxicity_level),
    'SYSTEM', NOW());
