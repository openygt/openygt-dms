-- ========== 留样编号唯一约束（防并发重复） ==========

-- 1. 先给已逻辑删除的记录的 sample_no 加后缀，避免与有效记录冲突
UPDATE qt_retain_sample
SET sample_no = CONCAT(sample_no, '_DEL', id),
    updated_at = CURRENT_TIMESTAMP
WHERE deleted = 1;

-- 2. sample_no 唯一约束
ALTER TABLE qt_retain_sample ADD UNIQUE INDEX uk_qrs_sample_no (sample_no);
