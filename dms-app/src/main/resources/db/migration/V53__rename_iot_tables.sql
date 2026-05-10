-- ============================================================
-- iot 表结构初始化（配合表名规范化）
-- 变更日期: 2026-05-09
-- ============================================================

CREATE TABLE IF NOT EXISTS `iot_device_command` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `device_code` varchar(64) DEFAULT NULL COMMENT '设备编码',
  `command_type` varchar(32) DEFAULT NULL COMMENT '命令类型',
  `command_payload` text COMMENT '命令内容',
  `status` tinyint DEFAULT '0' COMMENT '状态：0待发送 1已发送 2执行成功 3执行失败',
  `sent_at` datetime DEFAULT NULL COMMENT '发送时间',
  `executed_at` datetime DEFAULT NULL COMMENT '执行时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除标志',
  PRIMARY KEY (`id`),
  KEY `idx_idc_device_code` (`device_code`),
  KEY `idx_idc_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='IoT设备命令表';

CREATE TABLE IF NOT EXISTS `iot_device_connection` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `device_code` varchar(64) NOT NULL COMMENT '设备编码',
  `connection_status` tinyint DEFAULT '0' COMMENT '连接状态：0离线 1在线 2断开',
  `last_heartbeat` datetime DEFAULT NULL COMMENT '最后心跳时间',
  `ip_address` varchar(32) DEFAULT NULL COMMENT 'IP地址',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除标志',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_idc_device_code` (`device_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='IoT设备连接表';

CREATE TABLE IF NOT EXISTS `iot_device_mqtt_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `device_code` varchar(64) NOT NULL COMMENT '设备编码',
  `mqtt_topic` varchar(128) DEFAULT NULL COMMENT 'MQTT主题',
  `qos_level` tinyint DEFAULT '1' COMMENT 'QoS等级',
  `is_active` tinyint DEFAULT '1' COMMENT '是否启用',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除标志',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_idmc_device_code` (`device_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='IoT设备MQTT配置表';
