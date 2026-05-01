-- Phase 5: 交付管理 + 打印记录 + 设备维护

-- ==================== 交付记录 ====================
CREATE TABLE t_delivery_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL COMMENT '关联工单ID',
    prescription_no VARCHAR(50) COMMENT '处方号',
    patient_name VARCHAR(50) COMMENT '患者姓名',
    delivery_type VARCHAR(20) COMMENT '交付类型: SELF自取/EXPRESS快递/DELIVERY配送',
    receiver_name VARCHAR(50) COMMENT '接收人',
    receiver_phone VARCHAR(20) COMMENT '接收电话',
    receiver_address VARCHAR(255) COMMENT '接收地址',
    courier_company VARCHAR(50) COMMENT '快递公司',
    courier_no VARCHAR(50) COMMENT '快递单号',
    bag_count INT DEFAULT 0 COMMENT '袋数',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态: PENDING待交付/DELIVERED已交付',
    operator_id VARCHAR(50) COMMENT '操作人',
    remark VARCHAR(500) COMMENT '备注',
    delivered_at DATETIME COMMENT '交付时间',
    tenant_id VARCHAR(50) DEFAULT 'default' COMMENT '租户ID',
    deleted BIGINT DEFAULT 0 COMMENT '逻辑删除',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_task_id (task_id),
    INDEX idx_status (status),
    INDEX idx_prescription_no (prescription_no)
) COMMENT='交付记录';

-- ==================== 设备维护记录 ====================
CREATE TABLE eq_device_maintenance (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_id BIGINT NOT NULL COMMENT '设备ID',
    maintenance_type VARCHAR(20) COMMENT '维护类型: MAINTENANCE保养/REPAIR维修/INSPECTION巡检',
    content TEXT COMMENT '维护内容',
    parts VARCHAR(255) COMMENT '更换配件',
    cost DECIMAL(10,2) COMMENT '费用',
    operator_id VARCHAR(50) COMMENT '操作人',
    plan_date DATE COMMENT '计划日期',
    finish_date DATE COMMENT '完成日期',
    status TINYINT DEFAULT 0 COMMENT '状态: 0待执行/1已完成',
    tenant_id VARCHAR(50) DEFAULT 'default' COMMENT '租户ID',
    deleted BIGINT DEFAULT 0 COMMENT '逻辑删除',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_device_id (device_id),
    INDEX idx_status (status)
) COMMENT='设备维护记录';

-- 插入交付记录测试数据
INSERT INTO t_delivery_record (task_id, prescription_no, patient_name, delivery_type, receiver_name, receiver_phone, receiver_address, status, operator_id, bag_count, tenant_id, deleted) VALUES
(1, 'P20250428001', '张三', 'SELF', '张三', '13800138001', '门诊大厅', 'DELIVERED', 'admin', 7, 'default', 0),
(2, 'P20250428002', '李四', 'EXPRESS', '李四', '13800138002', '北京市海淀区xxx路', 'DELIVERED', 'admin', 14, 'default', 0),
(3, 'P20250428003', '王五', 'DELIVERY', '王五', '13800138003', '住院部3楼', 'PENDING', NULL, 7, 'default', 0),
(4, 'P20250428004', '赵六', 'SELF', '赵六', '13800138004', '门诊大厅', 'PENDING', NULL, 7, 'default', 0);

-- 插入设备维护记录测试数据
INSERT INTO eq_device_maintenance (device_id, maintenance_type, content, parts, cost, operator_id, plan_date, finish_date, status, tenant_id, deleted) VALUES
(1, 'MAINTENANCE', '清洗加热管，检查温控器', NULL, 0.00, 'jx001', '2026-04-01', '2026-04-01', 1, 'default', 0),
(1, 'REPAIR', '更换压力传感器', '压力传感器', 350.00, 'jx001', '2026-04-15', '2026-04-15', 1, 'default', 0),
(2, 'INSPECTION', '月度巡检，检查传送带', NULL, 0.00, 'jx001', '2026-05-01', NULL, 0, 'default', 0);
