-- ============================================
-- V5: v1.4 配套迁移脚本
-- ============================================

-- 1. qt_inspection 新增业务字段 inspected_at（与 created_at 解耦）
ALTER TABLE qt_inspection ADD COLUMN inspected_at DATETIME;

-- 2. prt_task 新增 max_retry 字段（打印重试上限）
ALTER TABLE prt_task ADD COLUMN max_retry BIGINT DEFAULT 3;

-- 3. prt_record 新增 operator_id（审计追溯）
-- ALTER TABLE prt_record ADD COLUMN operator_id VARCHAR(50);

-- ============================================
-- 4. prod_task 预埋枚举名列（为 V6 状态值迁移做准备）
-- ============================================
ALTER TABLE prod_task ADD COLUMN status_enum VARCHAR(30);

UPDATE prod_task SET status_enum = CASE status
    WHEN '待泡药' THEN 'WAIT_SOAK'
    WHEN '泡药中' THEN 'SOAKING'
    WHEN '待煎药' THEN 'WAIT_DECOCT'
    WHEN '煎药中' THEN 'DECOCTING'
    WHEN '待出液' THEN 'WAIT_POUR'
    WHEN '出液中' THEN 'POURING'
    WHEN '待包装' THEN 'WAIT_WRAP'
    WHEN '包装中' THEN 'WRAPPING'
    WHEN '待贴标' THEN 'WAIT_LABEL'
    WHEN '待质检' THEN 'WAIT_QC'
    WHEN '待交接' THEN 'WAIT_HANDOVER'
    WHEN '已完成' THEN 'COMPLETED'
    WHEN '已部分完成' THEN 'PARTIAL_COMPLETED'
    WHEN '已报废' THEN 'SCRAPPED'
    ELSE 'UNKNOWN'
END;

-- 校验：确认所有 prod_task.status 都能映射到合法枚举名
-- 如果以下查询返回 0 条，说明存在脏数据，V6 迁移前必须清理
-- SELECT * FROM prod_task WHERE status_enum = 'UNKNOWN';
