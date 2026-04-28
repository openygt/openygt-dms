-- ============================================
-- V11: 设备组队管理（煎药机-包装机-打印机）
-- ============================================

-- 1. 设备组主表
CREATE TABLE IF NOT EXISTS eq_device_group (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_code VARCHAR(50) NOT NULL UNIQUE,
    group_name VARCHAR(100) NOT NULL,
    package_device_id BIGINT,
    printer_device_id BIGINT,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    tenant_id VARCHAR(32) DEFAULT 'default',
    deleted BIGINT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (package_device_id) REFERENCES eq_device(id),
    FOREIGN KEY (printer_device_id) REFERENCES eq_device(id)
);

-- 2. 设备组成员表（煎药机入组关系）
CREATE TABLE IF NOT EXISTS eq_device_group_member (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_id BIGINT NOT NULL,
    decoct_device_id BIGINT NOT NULL,
    sort_order BIGINT DEFAULT 0,
    tenant_id VARCHAR(32) DEFAULT 'default',
    deleted BIGINT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (group_id) REFERENCES eq_device_group(id),
    FOREIGN KEY (decoct_device_id) REFERENCES eq_device(id),
    UNIQUE(group_id, decoct_device_id)
);

-- 3. 为 eq_device 增加 group_id 冗余字段，便于快速查询设备所属组
ALTER TABLE eq_device ADD COLUMN group_id BIGINT DEFAULT NULL;
