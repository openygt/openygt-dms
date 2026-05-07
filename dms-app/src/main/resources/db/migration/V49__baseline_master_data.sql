-- ============================================
-- V49：基础主数据基线（子专项A）
-- ============================================
-- 说明：
--   本脚本基于 Day 0 源数据探查结果编写。
--   三库来源：yylx_spd_yangxin_v2 / yylx_prescription / jybz
--   目标：新建 11 张标准表 + 向 2 张已有表补充数据
-- ============================================

-- --------------------------------------------------
-- 1. 药材分类（md_medicine_category）
-- 来源：yylx_spd_yangxin_v2.bas_material_category
-- --------------------------------------------------
CREATE TABLE IF NOT EXISTS md_medicine_category (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    source_id VARCHAR(64) COMMENT '源库原始ID',
    pid VARCHAR(64) COMMENT '父节点ID',
    code VARCHAR(64) COMMENT '编码',
    name VARCHAR(255) NOT NULL COMMENT '名称',
    fullname VARCHAR(255) COMMENT '全名',
    sort INT DEFAULT 0 COMMENT '排序',
    is_enabled TINYINT DEFAULT 1 COMMENT '是否启用 1:是 0:否',
    deleted BIGINT DEFAULT 0 COMMENT '逻辑删除',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    tenant_id VARCHAR(64) DEFAULT 'default' COMMENT '租户ID',
    UNIQUE KEY uk_source_id (source_id),
    KEY idx_code (code),
    KEY idx_pid (pid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='药材分类';

-- --------------------------------------------------
-- 2. 计量单位（md_measure_unit）
-- 来源：yylx_spd_yangxin_v2.bas_measure_unit
-- --------------------------------------------------
CREATE TABLE IF NOT EXISTS md_measure_unit (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    source_id VARCHAR(64) COMMENT '源库原始ID',
    name VARCHAR(64) NOT NULL COMMENT '名称',
    symbol VARCHAR(32) COMMENT '符号',
    pid VARCHAR(64) COMMENT '父节点ID',
    factor DECIMAL(18,6) COMMENT '换算系数',
    is_enabled TINYINT DEFAULT 1 COMMENT '是否启用',
    deleted BIGINT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    tenant_id VARCHAR(64) DEFAULT 'default',
    UNIQUE KEY uk_source_id (source_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='计量单位';

-- --------------------------------------------------
-- 3. 药材主数据（md_medicine）
-- 来源：yylx_spd_yangxin_v2.bas_material
-- --------------------------------------------------
CREATE TABLE IF NOT EXISTS md_medicine (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    source_id VARCHAR(64) COMMENT '源库原始ID',
    code VARCHAR(64) COMMENT '编码',
    name VARCHAR(255) NOT NULL COMMENT '名称',
    category_id BIGINT COMMENT '分类ID（关联md_medicine_category）',
    unit_id VARCHAR(64) COMMENT '单位ID',
    unit_name VARCHAR(64) COMMENT '单位名称',
    sale_price DECIMAL(18,4) COMMENT '销售价格',
    model VARCHAR(255) COMMENT '规格型号',
    drug_type TINYINT DEFAULT 0 COMMENT '药品类型 0:饮片 1:颗粒',
    is_enabled TINYINT DEFAULT 1 COMMENT '是否启用 1:是 0:否',
    pinyin VARCHAR(255) COMMENT '拼音',
    english_name VARCHAR(255) COMMENT '英文名',
    medicinal_part VARCHAR(255) COMMENT '药用部位',
    processing_method VARCHAR(255) COMMENT '炮制方法',
    froms_family VARCHAR(255) COMMENT '药材科来源',
    froms_species VARCHAR(255) COMMENT '药材种来源',
    drug_level VARCHAR(64) COMMENT '毒性分类',
    efficacy_category VARCHAR(64) COMMENT '功效分类',
    main_usage VARCHAR(1000) COMMENT '用法用量',
    storage VARCHAR(255) COMMENT '贮藏条件',
    attention VARCHAR(1000) COMMENT '注意事项',
    drug_img VARCHAR(500) COMMENT '药品图片URL',
    remark VARCHAR(500) COMMENT '备注',
    deleted BIGINT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    tenant_id VARCHAR(64) DEFAULT 'default',
    UNIQUE KEY uk_source_id (source_id),
    KEY idx_code (code),
    KEY idx_name (name),
    KEY idx_category (category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='药材主数据';

-- --------------------------------------------------
-- 4. 医生主数据（md_doctor）
-- 来源：yylx_prescription.ylx_doctor_info
-- --------------------------------------------------
CREATE TABLE IF NOT EXISTS md_doctor (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    source_id VARCHAR(64) COMMENT '源库原始ID',
    hospital_id BIGINT COMMENT '医院ID',
    name VARCHAR(64) COMMENT '姓名',
    role INT COMMENT '角色 1:医生 2:抓药师 3:药房员工',
    sex INT COMMENT '性别 1:男 2:女',
    mobile VARCHAR(32) COMMENT '联系电话',
    department VARCHAR(64) COMMENT '科室',
    certificate_practice VARCHAR(64) COMMENT '执业证书号',
    phy_qualification VARCHAR(64) COMMENT '医师资格证书号',
    status INT DEFAULT 1 COMMENT '状态 1:有效 2:无效',
    his_doctor_id VARCHAR(64) COMMENT 'HIS系统医生编号',
    deleted BIGINT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    tenant_id VARCHAR(64) DEFAULT 'default',
    UNIQUE KEY uk_source_id (source_id),
    KEY idx_hospital (hospital_id),
    KEY idx_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='医生主数据';

-- --------------------------------------------------
-- 5. 处方药品字典（md_drug）
-- 来源：yylx_prescription.ylx_drug
-- --------------------------------------------------
CREATE TABLE IF NOT EXISTS md_drug (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    source_id VARCHAR(64) COMMENT '源库原始ID',
    drug_type VARCHAR(32) COMMENT '药品种类',
    unit VARCHAR(32) COMMENT '单位',
    drug_name VARCHAR(255) COMMENT '药品名称',
    drug_specificat VARCHAR(255) COMMENT '规格',
    mnemonic VARCHAR(255) COMMENT '助记符',
    bas_material_id VARCHAR(64) COMMENT '关联药房药品ID',
    retail_price DECIMAL(18,6) COMMENT '零售价格',
    sys_org_code VARCHAR(64) COMMENT '药房编码',
    deleted BIGINT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    tenant_id VARCHAR(64) DEFAULT 'default',
    UNIQUE KEY uk_source_id (source_id),
    KEY idx_name (drug_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='处方药品字典';

-- --------------------------------------------------
-- 6. 用药规则（md_prescription_rule）
-- 来源：yylx_prescription.ylx_prescription_rule
-- --------------------------------------------------
CREATE TABLE IF NOT EXISTS md_prescription_rule (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    source_id VARCHAR(64) COMMENT '源库原始ID',
    drug_name VARCHAR(255) COMMENT '药品名称',
    rule VARCHAR(1000) COMMENT '规则内容',
    rule_type TINYINT COMMENT '规则类型 1:相反 2:相畏 3:妊娠禁用 4:妊娠慎用 5:毒麻品内服超量 6:毒麻品外用超量 7:普通饮品内服超量 8:普通饮品外用超量',
    status TINYINT DEFAULT 1 COMMENT '状态 1:可用 2:注销',
    deleted BIGINT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    tenant_id VARCHAR(64) DEFAULT 'default',
    UNIQUE KEY uk_source_id (source_id),
    KEY idx_drug_name (drug_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用药规则';

-- --------------------------------------------------
-- 7. 科室（md_department）
-- 来源：jybz.department
-- --------------------------------------------------
CREATE TABLE IF NOT EXISTS md_department (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    source_id VARCHAR(64) COMMENT '源库原始ID',
    name VARCHAR(64) NOT NULL COMMENT '名称',
    parent_id VARCHAR(64) COMMENT '父节点ID',
    sort INT DEFAULT 0 COMMENT '排序',
    leader VARCHAR(64) COMMENT '负责人',
    chairman VARCHAR(64) COMMENT '主管',
    deleted BIGINT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    tenant_id VARCHAR(64) DEFAULT 'default',
    UNIQUE KEY uk_source_id (source_id),
    KEY idx_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='科室';

-- --------------------------------------------------
-- 8. 岗位（md_duty）
-- 来源：jybz.duty
-- --------------------------------------------------
CREATE TABLE IF NOT EXISTS md_duty (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    source_id VARCHAR(64) COMMENT '源库原始ID',
    name VARCHAR(64) NOT NULL COMMENT '名称',
    deleted BIGINT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    tenant_id VARCHAR(64) DEFAULT 'default',
    UNIQUE KEY uk_source_id (source_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='岗位';

-- --------------------------------------------------
-- 9. 告警级别（md_alarm_level）
-- 来源：jybz.dic_alarmlevel
-- --------------------------------------------------
CREATE TABLE IF NOT EXISTS md_alarm_level (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    source_id VARCHAR(64) COMMENT '源库原始ID',
    name VARCHAR(64) NOT NULL COMMENT '名称',
    color VARCHAR(32) COMMENT '显示颜色',
    is_upgrade TINYINT DEFAULT 0 COMMENT '是否升级',
    upgrade_times INT COMMENT '每告警多少次升一次级',
    alarm_mode VARCHAR(32) COMMENT '告警方式',
    deleted BIGINT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    tenant_id VARCHAR(64) DEFAULT 'default',
    UNIQUE KEY uk_source_id (source_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警级别';

-- --------------------------------------------------
-- 10. 告警类型（md_alarm_type）
-- 来源：jybz.dicalarmtype
-- --------------------------------------------------
CREATE TABLE IF NOT EXISTS md_alarm_type (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    source_id VARCHAR(64) COMMENT '源库原始ID',
    name VARCHAR(64) NOT NULL COMMENT '名称',
    deleted BIGINT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    tenant_id VARCHAR(64) DEFAULT 'default',
    UNIQUE KEY uk_source_id (source_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警类型';

-- --------------------------------------------------
-- 11. 快递公司（md_express_company）
-- 来源：jybz.expresscompany
-- --------------------------------------------------
CREATE TABLE IF NOT EXISTS md_express_company (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    source_id VARCHAR(64) COMMENT '源库原始ID',
    name VARCHAR(64) NOT NULL COMMENT '名称',
    fee DECIMAL(18,2) COMMENT '费用',
    deleted BIGINT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    tenant_id VARCHAR(64) DEFAULT 'default',
    UNIQUE KEY uk_source_id (source_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='快递公司';

-- ============================================
-- 数据导入：从三库导入基础主数据
-- ============================================

-- --------------------------------------------------
-- 1. 药材分类
-- --------------------------------------------------
INSERT IGNORE INTO md_medicine_category (source_id, pid, code, name, fullname, sort, is_enabled, tenant_id)
SELECT id, pid, code, name, fullname, 0, is_enabled, COALESCE(tenant_id, 'default')
FROM yylx_spd_yangxin_v2.bas_material_category;

-- --------------------------------------------------
-- 2. 计量单位
-- --------------------------------------------------
INSERT IGNORE INTO md_measure_unit (source_id, name, symbol, pid, factor, is_enabled, tenant_id)
SELECT id, name, symbol, pid, factor, is_enabled, COALESCE(tenant_id, 'default')
FROM yylx_spd_yangxin_v2.bas_measure_unit;

-- --------------------------------------------------
-- 3. 药材主数据
-- --------------------------------------------------
INSERT IGNORE INTO md_medicine (
    source_id, code, name, category_id, unit_id, unit_name, sale_price, model,
    drug_type, is_enabled, pinyin, english_name, medicinal_part, processing_method,
    froms_family, froms_species, drug_level, efficacy_category, main_usage, storage,
    attention, drug_img, remark, tenant_id
)
SELECT 
    bm.id, bm.code, bm.name, mc.id, bm.unit_id, bm.unit_name, bm.sale_price, bm.model,
    bm.drug_type, bm.is_enabled, bm.pinyin, bm.english_name, bm.medicinal_part, bm.processing_method,
    bm.froms_family, bm.froms_species, bm.drug_level, bm.efficacy_category, bm.main_usage, bm.storage,
    bm.attention, bm.drug_img, bm.remark, COALESCE(bm.tenant_id, 'default')
FROM yylx_spd_yangxin_v2.bas_material bm
LEFT JOIN md_medicine_category mc ON mc.source_id = CONVERT(bm.category_id USING utf8mb4) COLLATE utf8mb4_unicode_ci;

-- --------------------------------------------------
-- 4. 医生主数据
-- --------------------------------------------------
INSERT IGNORE INTO md_doctor (source_id, hospital_id, name, role, sex, mobile, department, certificate_practice, phy_qualification, status, his_doctor_id, tenant_id)
SELECT 
    id, h_id, name, role, sex, mobile, department, certificate_practice, phy_qualification, status, his_doctor_id, 'default'
FROM yylx_prescription.ylx_doctor_info
WHERE status = 1;

-- --------------------------------------------------
-- 5. 处方药品字典
-- --------------------------------------------------
INSERT IGNORE INTO md_drug (source_id, drug_type, unit, drug_name, drug_specificat, mnemonic, bas_material_id, retail_price, sys_org_code, tenant_id)
SELECT 
    id, drug_type, unit, drug_name, drug_specificat, mnemonic, bas_material_id, retail_price, sys_org_code, 'default'
FROM yylx_prescription.ylx_drug;

-- --------------------------------------------------
-- 6. 用药规则
-- --------------------------------------------------
INSERT IGNORE INTO md_prescription_rule (source_id, drug_name, rule, rule_type, status, tenant_id)
SELECT 
    id, drug_name, rule, type, status, 'default'
FROM yylx_prescription.ylx_prescription_rule
WHERE status = 1;

-- --------------------------------------------------
-- 7. 科室
-- --------------------------------------------------
INSERT IGNORE INTO md_department (source_id, name, parent_id, sort, leader, chairman, tenant_id)
SELECT 
    id, name, parentid, CAST(indexnum AS UNSIGNED), depleader, chairman, 'default'
FROM jybz.department;

-- --------------------------------------------------
-- 8. 岗位
-- --------------------------------------------------
INSERT IGNORE INTO md_duty (source_id, name, tenant_id)
SELECT 
    id, name, 'default'
FROM jybz.duty;

-- --------------------------------------------------
-- 9. 告警级别
-- --------------------------------------------------
INSERT IGNORE INTO md_alarm_level (source_id, name, color, is_upgrade, upgrade_times, alarm_mode, tenant_id)
SELECT 
    ID, NAME, COLOR, CASE WHEN ISUPGRADE = 'Y' THEN 1 ELSE 0 END, 
    CAST(NULLIF(UPGRADETIMES, '') AS UNSIGNED), ALARMMODE, 'default'
FROM jybz.dic_alarmlevel;

-- --------------------------------------------------
-- 10. 告警类型
-- --------------------------------------------------
INSERT IGNORE INTO md_alarm_type (source_id, name, tenant_id)
SELECT 
    ID, NAME, 'default'
FROM jybz.dicalarmtype;

-- --------------------------------------------------
-- 11. 快递公司
-- --------------------------------------------------
INSERT IGNORE INTO md_express_company (source_id, name, fee, tenant_id)
SELECT 
    id, name, fee, 'default'
FROM jybz.expresscompany;

-- --------------------------------------------------
-- 12. 医院主数据补充（向已有表 md_hospital 追加）
-- 来源：yylx_spd_yangxin_v2.bas_hospital
-- 策略：跳过已有编码，避免重复
-- --------------------------------------------------
INSERT IGNORE INTO md_hospital (name, code, contact_person, phone, address, status, tenant_id)
SELECT 
    hospital_name, system_code, contact, mobile, address, 
    CASE WHEN status = 1 THEN 1 ELSE 0 END, 'default'
FROM yylx_spd_yangxin_v2.bas_hospital bh
WHERE (bh.is_del = 0 OR bh.is_del IS NULL)
  AND NOT EXISTS (
      SELECT 1 FROM md_hospital mh 
      WHERE mh.name = CONVERT(bh.hospital_name USING utf8mb4) COLLATE utf8mb4_unicode_ci
  );

-- --------------------------------------------------
-- 13. 煎药方案补充（向已有表 md_decoct_scheme 追加）
-- 来源：jybz.prescriptdecoctscheme
-- 策略：按 code 去重
-- --------------------------------------------------
INSERT IGNORE INTO md_decoct_scheme (name, code, scheme_type, decoct_times, pressure, upper_water, heating_time, pre_heating_time, post_heating_time, description, status, tenant_id)
SELECT 
    name, ID, type, times, pressure, 
    CAST(NULLIF(upperwater, '') AS DECIMAL(10,2)), 
    CAST(NULLIF(heatingTime, '') AS UNSIGNED), 
    CAST(NULLIF(xjheatingTime, '') AS UNSIGNED), 
    CAST(NULLIF(hxheatingTime, '') AS UNSIGNED), 
    detail, 1, 'default'
FROM jybz.prescriptdecoctscheme ps
WHERE NOT EXISTS (SELECT 1 FROM md_decoct_scheme ds WHERE ds.code = CONVERT(ps.ID USING utf8mb4) COLLATE utf8mb4_unicode_ci);

-- ============================================
-- 导入结果校验
-- ============================================
SELECT 'md_medicine_category' AS tbl, COUNT(*) AS cnt FROM md_medicine_category
UNION ALL SELECT 'md_measure_unit', COUNT(*) FROM md_measure_unit
UNION ALL SELECT 'md_medicine', COUNT(*) FROM md_medicine
UNION ALL SELECT 'md_doctor', COUNT(*) FROM md_doctor
UNION ALL SELECT 'md_drug', COUNT(*) FROM md_drug
UNION ALL SELECT 'md_prescription_rule', COUNT(*) FROM md_prescription_rule
UNION ALL SELECT 'md_department', COUNT(*) FROM md_department
UNION ALL SELECT 'md_duty', COUNT(*) FROM md_duty
UNION ALL SELECT 'md_alarm_level', COUNT(*) FROM md_alarm_level
UNION ALL SELECT 'md_alarm_type', COUNT(*) FROM md_alarm_type
UNION ALL SELECT 'md_express_company', COUNT(*) FROM md_express_company
UNION ALL SELECT 'md_hospital', COUNT(*) FROM md_hospital
UNION ALL SELECT 'md_decoct_scheme', COUNT(*) FROM md_decoct_scheme;

-- ============================================
-- GMP 审计：记录本次迁移
-- ============================================
INSERT IGNORE INTO data_migration_log (migration_id, table_name, step, record_count, executed_by, executed_at)
VALUES ('V49', 'md_medicine_category,md_measure_unit,md_medicine,md_doctor,md_drug,md_prescription_rule,md_department,md_duty,md_alarm_level,md_alarm_type,md_express_company,md_hospital,md_decoct_scheme', 'IMPORT', 
    (SELECT COUNT(*) FROM md_medicine_category)
    + (SELECT COUNT(*) FROM md_measure_unit)
    + (SELECT COUNT(*) FROM md_medicine)
    + (SELECT COUNT(*) FROM md_doctor)
    + (SELECT COUNT(*) FROM md_drug)
    + (SELECT COUNT(*) FROM md_prescription_rule)
    + (SELECT COUNT(*) FROM md_department)
    + (SELECT COUNT(*) FROM md_duty)
    + (SELECT COUNT(*) FROM md_alarm_level)
    + (SELECT COUNT(*) FROM md_alarm_type)
    + (SELECT COUNT(*) FROM md_express_company)
    + (SELECT COUNT(*) FROM md_hospital)
    + (SELECT COUNT(*) FROM md_decoct_scheme),
    'SYSTEM', NOW());
