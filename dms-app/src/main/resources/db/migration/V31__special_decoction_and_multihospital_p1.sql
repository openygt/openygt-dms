-- ============================================================
-- Flyway V31: 特殊煎法全覆盖 + 设备精细状态守卫 + 多医院适配
-- ============================================================

-- 1. 医院药材编码映射表
CREATE TABLE hospital_medicine_mapping (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    hospital_id             BIGINT NOT NULL COMMENT '医院ID',
    hospital_medicine_code  VARCHAR(64) NOT NULL COMMENT '医院药材编码',
    hospital_medicine_name  VARCHAR(100) COMMENT '医院药材名称',
    system_medicine_id      BIGINT NOT NULL COMMENT '系统药材ID',
    system_medicine_name    VARCHAR(100) COMMENT '系统药材名称',
    conversion_ratio        DECIMAL(10,4) DEFAULT 1.0000 COMMENT '换算比例',
    hospital_unit           VARCHAR(20) COMMENT '医院单位',
    system_unit             VARCHAR(20) COMMENT '系统单位',
    is_active               TINYINT DEFAULT 1,
    tenant_id               VARCHAR(32) DEFAULT 'default',
    deleted                 INT DEFAULT 0,
    created_at              DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at              DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_hospital_code (hospital_id, hospital_medicine_code),
    INDEX idx_system_medicine (system_medicine_id),
    INDEX idx_hospital (hospital_id)
) COMMENT='医院药材编码映射';

-- 2. 处方药材表扩展（煎法支持）
ALTER TABLE prod_prescription_medicine
    ADD COLUMN decoction_method VARCHAR(32) DEFAULT 'NORMAL'
        COMMENT '煎法: NORMAL/DECOCT_FIRST/ADD_LATE/WRAP_DECOCT/SEPARATE_DECOCT/DISSOLVE/INFUSE/DECOCT_AS_WATER'
        AFTER toxicity_level,
    ADD COLUMN decoction_params JSON COMMENT '煎法参数'
        AFTER decoction_method,
    ADD INDEX idx_decoction_method (decoction_method);

-- 3. 设备表扩展（当前煎法）
ALTER TABLE eq_device
    ADD COLUMN current_decoction_method VARCHAR(32) COMMENT '当前执行煎法'
        AFTER current_prescription_code;

-- 4. 任务表扩展（煎法计划快照）
ALTER TABLE prod_task
    ADD COLUMN decoction_plan JSON COMMENT '煎法计划(含指令序列)'
        AFTER scheme_snapshot;
