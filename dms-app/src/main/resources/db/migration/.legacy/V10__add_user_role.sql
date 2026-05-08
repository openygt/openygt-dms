-- 煎药室人员角色数据
-- 给 sys_user 加 role 字段（SQLite 允许）
ALTER TABLE sys_user ADD COLUMN role VARCHAR(32);

-- 更新 admin 为管理员
UPDATE sys_user SET role = '主任' WHERE username = 'admin';

-- 插入测试用户（各角色）
INSERT INTO sys_user (tenant_id, username, password, real_name, phone, status, role) VALUES
('default', 'jgy001', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EO', '张三', '13800138001', 'ACTIVE', '煎药工'),
('default', 'jgy002', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EO', '李四', '13800138002', 'ACTIVE', '煎药工'),
('default', 'bz001', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EO', '王班长', '13800138003', 'ACTIVE', '班长'),
('default', 'zj001', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EO', '赵质检', '13800138004', 'ACTIVE', '质检员'),
('default', 'zj002', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EO', '钱质检', '13800138005', 'ACTIVE', '质检员'),
('default', 'kf001', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EO', '孙客服', '13800138006', 'ACTIVE', '客服'),
('default', 'jx001', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EO', '周机修', '13800138007', 'ACTIVE', '机修工'),
('default', 'zr001', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EO', '吴主任', '13800138008', 'ACTIVE', '主任');
