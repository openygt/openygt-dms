-- ========== 接口中心 ==========
CREATE TABLE IF NOT EXISTS t_interface_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    interface_code VARCHAR(50) UNIQUE NOT NULL COMMENT '接口编码',
    interface_name VARCHAR(100) NOT NULL COMMENT '接口名称',
    interface_type VARCHAR(20) NOT NULL COMMENT 'HIS/DEVICE/THIRD_PARTY',
    protocol VARCHAR(20) COMMENT '协议：REST/WEBSERVICE/HL7/MQTT/MODBUS/TCP',
    base_url VARCHAR(500) COMMENT '基础URL/地址',
    auth_type VARCHAR(20) COMMENT '认证方式：NONE/BASIC/TOKEN/OAUTH2',
    auth_config TEXT COMMENT '认证配置JSON',
    status TINYINT DEFAULT 1 COMMENT '0禁用 1启用',
    remark VARCHAR(500) COMMENT '备注',
    tenant_id VARCHAR(32) DEFAULT 'default',
    deleted INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT='接口配置';

CREATE TABLE IF NOT EXISTS t_interface_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    interface_id BIGINT NOT NULL COMMENT '接口配置ID',
    interface_code VARCHAR(50) COMMENT '接口编码',
    direction VARCHAR(10) COMMENT 'REQUEST/RESPONSE',
    method VARCHAR(10) COMMENT 'HTTP方法/操作类型',
    url TEXT COMMENT '请求URL',
    request_body TEXT COMMENT '请求内容',
    response_body TEXT COMMENT '响应内容',
    status_code VARCHAR(10) COMMENT '状态码',
    result VARCHAR(20) COMMENT 'SUCCESS/FAIL/TIMEOUT',
    duration_ms INT COMMENT '耗时毫秒',
    error_msg TEXT COMMENT '错误信息',
    tenant_id VARCHAR(32) DEFAULT 'default',
    deleted INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_interface_id (interface_id),
    INDEX idx_result (result),
    INDEX idx_created_at (created_at)
) COMMENT='接口调用日志';

-- E2E测试数据
INSERT INTO t_interface_config (interface_code, interface_name, interface_type, protocol, base_url, auth_type, status, remark) VALUES
('HIS_001', 'HIS处方接口', 'HIS', 'REST', 'http://his.hospital.com/api', 'TOKEN', 1, '接收处方数据'),
('DEV_001', '煎药机MQTT', 'DEVICE', 'MQTT', 'mqtt://broker.local:1883', 'NONE', 1, '煎药机设备通信'),
('DEV_002', '包装机TCP', 'DEVICE', 'TCP', 'tcp://192.168.1.100:502', 'NONE', 1, '包装机Modbus通信'),
('THIRD_001', '顺丰物流接口', 'THIRD_PARTY', 'REST', 'https://api.sf-express.com', 'TOKEN', 1, '物流单号查询');

INSERT INTO t_interface_log (interface_id, interface_code, direction, method, url, result, duration_ms, status_code) VALUES
(1, 'HIS_001', 'REQUEST', 'POST', '/api/v1/prescription', 'SUCCESS', 120, '200'),
(1, 'HIS_001', 'RESPONSE', 'POST', '/api/v1/prescription', 'SUCCESS', 0, '200'),
(2, 'DEV_001', 'REQUEST', 'PUBLISH', '/device/001/start', 'SUCCESS', 45, '200'),
(3, 'DEV_002', 'REQUEST', 'READ', '/modbus/100', 'FAIL', 5000, 'TIMEOUT'),
(4, 'THIRD_001', 'REQUEST', 'GET', '/api/route', 'SUCCESS', 230, '200');
