-- ============================================
-- demo_cleanup_pre.sql
-- 清理前预处理：创建cleanup脚本中引用但可能不存在的空表
-- 仅用于演示环境
-- ============================================

CREATE TABLE IF NOT EXISTS decoction_trace_event (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id VARCHAR(32) DEFAULT 'default'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS decoction_trace (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id VARCHAR(32) DEFAULT 'default'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS device_command (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id VARCHAR(32) DEFAULT 'default'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS device_utilization (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id VARCHAR(32) DEFAULT 'default'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS workload_stat (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id VARCHAR(32) DEFAULT 'default'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
