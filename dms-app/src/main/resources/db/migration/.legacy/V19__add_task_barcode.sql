-- V19: 为 prod_task 添加 barcode 字段，支持 PDA 扫码查询
ALTER TABLE prod_task ADD COLUMN barcode VARCHAR(100) NULL COMMENT '任务条形码/二维码';
CREATE INDEX idx_task_barcode ON prod_task(barcode);

-- 为现有任务生成默认条形码（基于 ID）
UPDATE prod_task SET barcode = CONCAT('JY-', LPAD(id, 8, '0')) WHERE barcode IS NULL;
