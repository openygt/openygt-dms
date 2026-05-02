-- ============================================================
-- Flyway V34: 处方导入异常标记 + 幂等去重
-- 接口/CSV导入时数据校验不通过的处方，标记为异常供人工纠正
-- 增加唯一索引确保HIS推送幂等
-- ============================================================

-- 1. 处方表增加导入异常字段
ALTER TABLE prod_prescription
    ADD COLUMN import_exception TINYINT DEFAULT 0 COMMENT '导入异常标识: 0=正常 1=异常待处理',
    ADD COLUMN exception_reason VARCHAR(500) COMMENT '异常原因（导入校验失败描述）',
    ADD COLUMN raw_import_data TEXT COMMENT '原始导入数据（JSON格式，供人工审查纠正）',
    ADD INDEX idx_import_exception (import_exception),
    ADD INDEX idx_exception_status (import_exception, receive_status);

-- 2. 增加唯一索引，实现HIS推送幂等（同一医院+处方号只能有一个未删除记录）
-- 注意：手动录入的处方 prescription_number 为 null，不受此约束
ALTER TABLE prod_prescription
    ADD UNIQUE INDEX uk_hospital_prescription (hospital_id, prescription_number);
