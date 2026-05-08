-- ============================================================
-- Flyway V32: 状态机补全 + 追溯增强 + 紧急处方 + 电子签名
-- ============================================================

-- 1. 电子签名表
CREATE TABLE sys_signature (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    biz_type        VARCHAR(32) NOT NULL COMMENT '业务类型: QC_INSPECT/HANDOVER/APPROVE_ROLLBACK/EXCEPTION_HANDLE',
    biz_id          BIGINT NOT NULL COMMENT '业务单据ID',
    signer_id       BIGINT NOT NULL COMMENT '签名人ID',
    signer_name     VARCHAR(50) COMMENT '签名人姓名',
    sign_image_url  VARCHAR(500) COMMENT '签名图片URL',
    sign_hash       VARCHAR(64) COMMENT '签名内容哈希(防篡改)',
    sign_time       DATETIME NOT NULL COMMENT '签名时间',
    sign_device     VARCHAR(20) COMMENT '签名设备: PDA/WEB',
    tenant_id       VARCHAR(32) DEFAULT 'default',
    deleted         INT DEFAULT 0,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_biz (biz_type, biz_id),
    INDEX idx_signer (signer_id, sign_time),
    INDEX idx_time (sign_time)
) COMMENT='电子签名表';

-- 2. 紧急处方插队记录表（审计用）
CREATE TABLE prod_emergency_dispatch (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    emergency_prescription_id BIGINT NOT NULL COMMENT '紧急处方ID',
    target_task_id      BIGINT COMMENT '被中断任务ID',
    target_device_id    BIGINT NOT NULL COMMENT '被占用设备ID',
    dispatch_result     VARCHAR(20) NOT NULL COMMENT 'IDLE/INTERRUPT/QUEUE',
    operator_id         BIGINT NOT NULL COMMENT '调度操作人',
    tenant_id           VARCHAR(32) DEFAULT 'default',
    deleted             INT DEFAULT 0,
    created_at          DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_emergency (emergency_prescription_id),
    INDEX idx_target_task (target_task_id),
    INDEX idx_device (target_device_id)
) COMMENT='紧急处方调度记录';

-- 3. 任务表扩展（紧急处方标记 + 优先级）
ALTER TABLE prod_task
    ADD COLUMN is_emergency TINYINT DEFAULT 0 COMMENT '是否紧急处方'
        AFTER is_exception,
    ADD COLUMN priority TINYINT DEFAULT 3 COMMENT '优先级: 1=最高 2=高 3=普通 4=低'
        AFTER is_emergency,
    ADD INDEX idx_emergency (is_emergency, priority);
