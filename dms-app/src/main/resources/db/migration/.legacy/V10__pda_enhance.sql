-- V10: PDA 移动端增强
-- 1. sys_user 增加 barcode 字段（员工扫码条码）
ALTER TABLE sys_user ADD COLUMN barcode VARCHAR(50) DEFAULT NULL COMMENT '员工扫码条码' AFTER phone;
CREATE INDEX idx_sys_user_barcode ON sys_user(barcode);

-- 2. prod_task 增加操作人姓名字段（避免每次关联查询）
ALTER TABLE prod_task ADD COLUMN operator_name VARCHAR(50) DEFAULT NULL COMMENT '当前操作人姓名' AFTER operator_id;

-- 3. 初始化测试员工条码数据
UPDATE sys_user SET barcode = CONCAT('EMP', LPAD(id, 6, '0')) WHERE barcode IS NULL;
