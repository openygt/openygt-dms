-- Phase 4: 质量追溯 + 打印配置

-- ==================== 批次追溯支持 ====================
-- 为煎药追溯表增加批次号字段
ALTER TABLE decoction_trace
    ADD COLUMN batch_no VARCHAR(50) COMMENT '生产批次号',
    ADD INDEX idx_batch_no (batch_no);

-- ==================== 标签模板 ====================
CREATE TABLE t_label_template (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_code VARCHAR(32) NOT NULL UNIQUE COMMENT '模板编码',
    template_name VARCHAR(100) NOT NULL COMMENT '模板名称',
    template_type VARCHAR(20) NOT NULL COMMENT '模板类型: SOAK泡药/DECOCT煎煮/PACKAGE包装/DELIVER交付',
    width_mm INT COMMENT '标签宽度(mm)',
    height_mm INT COMMENT '标签高度(mm)',
    content TEXT COMMENT '模板内容(JSON格式字段配置)',
    preview_image VARCHAR(500) COMMENT '预览图URL',
    status TINYINT DEFAULT 1 COMMENT '状态：0禁用 1启用',
    tenant_id VARCHAR(50) DEFAULT 'default' COMMENT '租户ID',
    deleted BIGINT DEFAULT 0 COMMENT '逻辑删除',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_type (template_type),
    INDEX idx_status (status)
) COMMENT='标签模板';

-- 插入默认标签模板
INSERT INTO t_label_template (template_code, template_name, template_type, width_mm, height_mm, content, status, tenant_id, deleted) VALUES
('LBL_SOAK_001', '泡药标签', 'SOAK', 80, 50, '[{"field":"patientName","label":"患者"},{"field":"prescriptionNo","label":"处方号"},{"field":"soakTime","label":"浸泡时间"},{"field":"createTime","label":"打印时间"}]', 1, 'default', 0),
('LBL_DECOCT_001', '煎煮标签', 'DECOCT', 80, 50, '[{"field":"patientName","label":"患者"},{"field":"prescriptionNo","label":"处方号"},{"field":"decoctTime","label":"煎煮时间"},{"field":"operatorName","label":"操作人"}]', 1, 'default', 0),
('LBL_PKG_001', '包装标签', 'PACKAGE', 80, 50, '[{"field":"patientName","label":"患者"},{"field":"prescriptionNo","label":"处方号"},{"field":"doseCount","label":"剂数"},{"field":"packageVolume","label":"容量"},{"field":"expireDate","label":"有效期"}]', 1, 'default', 0),
('LBL_DELIVER_001', '交付标签', 'DELIVER', 80, 50, '[{"field":"patientName","label":"患者"},{"field":"phone","label":"电话"},{"field":"address","label":"地址"},{"field":"deliveryNo","label":"快递单号"}]', 1, 'default', 0);
