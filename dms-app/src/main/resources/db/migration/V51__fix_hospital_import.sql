-- --------------------------------------------------
-- V51: 修复医院数据导入
-- 问题：V49 中 bas_hospital.is_del 全为 NULL，导致 WHERE bh.is_del = 0 过滤了所有记录
--       同时 system_code 全为 NULL，无法作为去重依据
-- 修复：使用 (is_del = 0 OR is_del IS NULL) 条件，改为按 hospital_name 去重
-- --------------------------------------------------

INSERT IGNORE INTO md_hospital (name, code, contact_person, phone, address, status, tenant_id)
SELECT 
    hospital_name, 
    system_code, 
    contact, 
    mobile, 
    address, 
    CASE WHEN status = 1 THEN 1 ELSE 0 END, 
    'default'
FROM yylx_spd_yangxin_v2.bas_hospital bh
WHERE (bh.is_del = 0 OR bh.is_del IS NULL)
  AND NOT EXISTS (
      SELECT 1 FROM md_hospital mh 
      WHERE mh.name = CONVERT(bh.hospital_name USING utf8mb4) COLLATE utf8mb4_unicode_ci
  );
