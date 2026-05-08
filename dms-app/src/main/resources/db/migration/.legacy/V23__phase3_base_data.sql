-- Phase 3: 基础数据 + 工艺配置
-- 药材目录 / 科室管理 / 医师管理 / 包装规格

-- ==================== 药材目录 ====================
CREATE TABLE base_medicine (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    medicine_code VARCHAR(32) NOT NULL UNIQUE COMMENT '药材编码',
    medicine_name VARCHAR(100) NOT NULL COMMENT '药材名称',
    aliases VARCHAR(500) COMMENT '别名，逗号分隔',
    his_code VARCHAR(50) COMMENT 'HIS系统编码',
    national_code VARCHAR(32) COMMENT '国标编码',
    spec VARCHAR(50) COMMENT '规格',
    unit VARCHAR(20) COMMENT '单位',
    stock_warning DECIMAL(10,2) COMMENT '库存预警值',
    status TINYINT DEFAULT 1 COMMENT '状态：0禁用 1启用',
    tenant_id VARCHAR(50) DEFAULT 'default' COMMENT '租户ID',
    deleted BIGINT DEFAULT 0 COMMENT '逻辑删除',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_name (medicine_name),
    INDEX idx_his_code (his_code),
    INDEX idx_national_code (national_code),
    INDEX idx_status (status)
) COMMENT='药材目录';

-- ==================== 科室管理 ====================
CREATE TABLE base_department (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    dept_code VARCHAR(32) NOT NULL UNIQUE COMMENT '科室编码',
    dept_name VARCHAR(100) NOT NULL COMMENT '科室名称',
    hospital_id BIGINT COMMENT '所属医院ID',
    description VARCHAR(500) COMMENT '科室描述',
    sort_order INT DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 1 COMMENT '状态：0禁用 1启用',
    tenant_id VARCHAR(50) DEFAULT 'default' COMMENT '租户ID',
    deleted BIGINT DEFAULT 0 COMMENT '逻辑删除',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_hospital_id (hospital_id),
    INDEX idx_status (status)
) COMMENT='科室管理';

-- ==================== 医师管理 ====================
CREATE TABLE base_doctor (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    doctor_code VARCHAR(32) NOT NULL UNIQUE COMMENT '医师编码',
    doctor_name VARCHAR(100) NOT NULL COMMENT '医师姓名',
    title VARCHAR(50) COMMENT '职称',
    department_id BIGINT COMMENT '所属科室ID',
    hospital_id BIGINT COMMENT '所属医院ID',
    phone VARCHAR(20) COMMENT '联系电话',
    status TINYINT DEFAULT 1 COMMENT '状态：0禁用 1启用',
    tenant_id VARCHAR(50) DEFAULT 'default' COMMENT '租户ID',
    deleted BIGINT DEFAULT 0 COMMENT '逻辑删除',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_department_id (department_id),
    INDEX idx_hospital_id (hospital_id),
    INDEX idx_status (status)
) COMMENT='医师管理';

-- ==================== 包装规格 ====================
CREATE TABLE md_package_spec (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    spec_code VARCHAR(32) NOT NULL UNIQUE COMMENT '规格编码',
    spec_name VARCHAR(50) NOT NULL COMMENT '规格名称',
    volume_ml INT NOT NULL COMMENT '单袋容量(ml)',
    bag_type VARCHAR(20) COMMENT '袋型：普通/真空/铝箔',
    description VARCHAR(500) COMMENT '描述',
    sort_order INT DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 1 COMMENT '状态：0禁用 1启用',
    tenant_id VARCHAR(50) DEFAULT 'default' COMMENT '租户ID',
    deleted BIGINT DEFAULT 0 COMMENT '逻辑删除',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status (status)
) COMMENT='包装规格';
