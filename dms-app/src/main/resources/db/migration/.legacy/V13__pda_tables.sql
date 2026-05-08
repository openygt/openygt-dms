-- ============================================================
-- V13: PDA 手持终端相关表
-- 日期: 2026-04-26
-- 说明: 新增 PDA 设备登录记录、配药复核照片、PDA 操作日志三张表
-- 参考: spd-yangxin scale 模块 + xinyan PDA 系统 + DMS PDA API 设计文档
-- ============================================================

-- ------------------------------------------------------------
-- 1. PDA 设备登录记录（员工 ↔ PDA 设备绑定，借鉴 spd-yangxin ScaleLoginRecords）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS pda_login_record (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT         NOT NULL,
    device_id       BIGINT         NOT NULL,
    device_code     VARCHAR(50)     NOT NULL,
    user_code       VARCHAR(50)     NOT NULL,
    login_time      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    logout_time     DATETIME,
    status          VARCHAR(20)     NOT NULL DEFAULT 'ONLINE' CHECK (status IN ('ONLINE', 'OFFLINE')),
    online_duration BIGINT,                    -- 在线时长（秒），logout 时计算
    ip_address      VARCHAR(50),
    tenant_id       VARCHAR(32)     DEFAULT 'default',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_pda_login_user ON pda_login_record(user_id);
CREATE INDEX idx_pda_login_device ON pda_login_record(device_id);
CREATE INDEX idx_pda_login_status ON pda_login_record(status);
CREATE INDEX idx_pda_login_time ON pda_login_record(login_time);

-- ------------------------------------------------------------
-- 2. PDA 配药复核照片（强制拍照留档，借鉴 xinyan uploadImg + spd-yangxin 称重照片）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS pda_review_photo (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id         BIGINT         NOT NULL,
    prescription_id BIGINT,
    photo_url       VARCHAR(500)    NOT NULL,   -- 原图路径，如 /uploads/pda/20260426/FH_1001_xxx.jpg
    thumbnail_url   VARCHAR(500),               -- 缩略图路径
    photo_type      VARCHAR(20)     NOT NULL DEFAULT 'REVIEW' CHECK (photo_type IN ('REVIEW', 'WEIGHING', 'EXCEPTION')),
    file_size       BIGINT,                    -- 文件大小（字节）
    width           BIGINT,                    -- 图片宽度
    height          BIGINT,                    -- 图片高度
    operator_id     BIGINT         NOT NULL,
    operator_name   VARCHAR(50),
    review_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    remark          VARCHAR(500),
    tenant_id       VARCHAR(32)     DEFAULT 'default',
    deleted         BIGINT         NOT NULL DEFAULT 0,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_pda_photo_task ON pda_review_photo(task_id);
CREATE INDEX idx_pda_photo_prescription ON pda_review_photo(prescription_id);
CREATE INDEX idx_pda_photo_time ON pda_review_photo(review_time);

-- ------------------------------------------------------------
-- 3. PDA 操作日志（审计追溯，借鉴 spd-yangxin @AutoLog + xinyan 操作记录）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS pda_operation_log (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT         NOT NULL,
    user_name       VARCHAR(50),
    device_id       BIGINT,
    device_code     VARCHAR(50),
    task_id         BIGINT,
    prescription_id BIGINT,
    oper_type       VARCHAR(32)     NOT NULL,   -- START_SOAK, END_DECOCT, HANDOVER, REPORT_EXCEPTION, SCAN_QUERY, LOGIN, BIND_DEVICE...
    oper_desc       VARCHAR(200),               -- 人类可读描述，如"结束煎药-JY-001"
    ext_data        TEXT,                       -- JSON 格式扩展数据
    oper_result     VARCHAR(20)     NOT NULL DEFAULT 'SUCCESS' CHECK (oper_result IN ('SUCCESS', 'FAILED')),
    error_msg       VARCHAR(500),
    api_path        VARCHAR(200),               -- 调用的 API 路径，如 /api/v1/pda/tasks/1001/decoct/end
    http_method     VARCHAR(10),                -- POST, GET, PUT...
    client_ip       VARCHAR(50),
    request_time    BIGINT,                    -- 请求耗时（毫秒）
    oper_time       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    tenant_id       VARCHAR(32)     DEFAULT 'default',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_pda_log_user ON pda_operation_log(user_id);
CREATE INDEX idx_pda_log_device ON pda_operation_log(device_code);
CREATE INDEX idx_pda_log_task ON pda_operation_log(task_id);
CREATE INDEX idx_pda_log_type ON pda_operation_log(oper_type);
CREATE INDEX idx_pda_log_result ON pda_operation_log(oper_result);
CREATE INDEX idx_pda_log_time ON pda_operation_log(oper_time);

-- ------------------------------------------------------------
-- 4. 初始化数据
-- ------------------------------------------------------------

-- 系统配置：PDA 相关参数（动态可调）
INSERT IGNORE INTO sys_config (config_key, config_value, description) VALUES
('pda.heartbeat.interval', '30', 'PDA 心跳间隔（秒）'),
('pda.heartbeat.timeout.idle', '120', 'PDA 空闲状态超时阈值（秒）'),
('pda.heartbeat.timeout.transport', '600', 'PDA 搬运状态超时阈值（秒）'),
('pda.photo.max_size', '5242880', 'PDA 单张照片最大大小（字节，默认5MB）'),
('pda.photo.max_count', '3', 'PDA 单次最多上传照片数量'),
('pda.offline.sync_batch_size', '50', 'PDA 离线同步批量大小'),
('pda.recent.limit', '20', 'PDA 首页最近记录数量'),
('pda.device.recent.limit', '5', 'PDA 最近使用设备数量');
