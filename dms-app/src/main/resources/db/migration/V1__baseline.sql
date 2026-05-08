
/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
SET FOREIGN_KEY_CHECKS = 0;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
DROP TABLE IF EXISTS `alarm_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `alarm_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `alarm_type` varchar(50) NOT NULL COMMENT '告警类型:TEMP_HIGH/TEMP_LOW/TIMEOUT/DEVICE_FAULT',
  `alarm_level` varchar(20) DEFAULT NULL COMMENT '告警级别:INFO/WARNING/CRITICAL',
  `threshold_type` varchar(20) DEFAULT NULL COMMENT '阈值类型:FIXED/PERCENTAGE/DEVIATION',
  `threshold_value` decimal(10,2) DEFAULT NULL COMMENT '阈值',
  `duration_seconds` int DEFAULT NULL COMMENT '持续时间(秒)',
  `notify_type` varchar(50) DEFAULT NULL COMMENT '通知方式:WEB/PUSH/SMS/VOICE/ALL',
  `notify_target` varchar(50) DEFAULT NULL COMMENT '通知对象:OPERATOR/BADMIN/ALL',
  `sound_file` varchar(200) DEFAULT NULL COMMENT '提示音文件',
  `enabled` tinyint(1) DEFAULT '1' COMMENT '是否启用',
  `tenant_id` varchar(32) DEFAULT 'default' COMMENT '租户ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` int DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='告警配置表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `base_toxic_medicine`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `base_toxic_medicine` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `medicine_id` bigint NOT NULL COMMENT '药材ID',
  `medicine_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '药材名称',
  `toxicity_level` tinyint NOT NULL COMMENT '毒性等级: 1小毒 2有毒 3大毒',
  `max_dosage` decimal(10,3) DEFAULT NULL COMMENT '单次最大用量(g)',
  `max_daily_dosage` decimal(10,3) DEFAULT NULL COMMENT '每日最大用量(g)',
  `wash_level` tinyint DEFAULT '1' COMMENT '清洗级别: 1常规 2强化',
  `is_active` tinyint DEFAULT NULL COMMENT '是否启用: 0=禁用 1=启用',
  `remark` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `tenant_id` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '租户ID',
  `deleted` int DEFAULT NULL COMMENT '逻辑删除: 0=正常 1=已删除',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_medicine` (`medicine_id`),
  KEY `idx_toxic_level` (`toxicity_level`),
  KEY `idx_active` (`is_active`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='毒性药材清单';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `data_migration_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `data_migration_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `migration_id` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '迁移标识',
  `table_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '目标表名',
  `step` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'BEFORE / AFTER',
  `record_count` int DEFAULT NULL COMMENT '记录数',
  `executed_by` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '执行人',
  `executed_at` datetime DEFAULT NULL COMMENT '执行时间',
  PRIMARY KEY (`id`),
  KEY `idx_migration_id` (`migration_id`)
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据迁移审计日志表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `decoction_trace`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `decoction_trace` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `prescription_no` varchar(50) NOT NULL COMMENT '处方号（唯一业务键）',
  `patient_name` varchar(50) DEFAULT NULL COMMENT '患者姓名',
  `patient_phone` varchar(20) DEFAULT NULL COMMENT '患者电话（脱敏存储）',
  `task_id` bigint DEFAULT NULL COMMENT '关联任务ID',
  `decoct_device_code` varchar(50) DEFAULT NULL COMMENT '煎药机编码',
  `decoct_device_name` varchar(100) DEFAULT NULL COMMENT '煎药机名称',
  `packer_device_code` varchar(50) DEFAULT NULL COMMENT '包装机编码',
  `labeler_device_code` varchar(50) DEFAULT NULL COMMENT '标签打印机编码',
  `scheme_id` bigint DEFAULT NULL COMMENT '煎药方案ID',
  `scheme_name` varchar(100) DEFAULT NULL COMMENT '方案名称',
  `soak_time` int DEFAULT NULL COMMENT '实际浸泡时间(分钟)',
  `pre_decoct_time` int DEFAULT NULL COMMENT '实际先煎时间(分钟)',
  `first_decoct_time` int DEFAULT NULL COMMENT '实际一煎时间(分钟)',
  `add_late_time` int DEFAULT NULL COMMENT '实际后下时间(分钟)',
  `second_decoct_time` int DEFAULT NULL COMMENT '实际二煎时间(分钟)',
  `package_time` int DEFAULT NULL COMMENT '实际包装时间(分钟)',
  `package_volume` decimal(10,2) DEFAULT NULL COMMENT '实际包装药液量(ml)',
  `sample_count` int DEFAULT NULL COMMENT '实际留样数量',
  `receive_time` datetime DEFAULT NULL COMMENT '接方时间',
  `audit_time` datetime DEFAULT NULL COMMENT '审方时间',
  `audit_pass_time` datetime DEFAULT NULL COMMENT '审方通过时间',
  `dispense_time` datetime DEFAULT NULL COMMENT '调剂时间',
  `review_time` datetime DEFAULT NULL COMMENT '复核时间',
  `soak_start_time` datetime DEFAULT NULL COMMENT '浸泡开始',
  `soak_end_time` datetime DEFAULT NULL COMMENT '浸泡结束',
  `pre_decoct_start` datetime DEFAULT NULL COMMENT '先煎开始',
  `pre_decoct_end` datetime DEFAULT NULL COMMENT '先煎结束',
  `first_decoct_start` datetime DEFAULT NULL COMMENT '一煎开始',
  `first_decoct_end` datetime DEFAULT NULL COMMENT '一煎结束',
  `add_late_time_actual` datetime DEFAULT NULL COMMENT '后下实际时间',
  `second_decoct_start` datetime DEFAULT NULL COMMENT '二煎开始',
  `second_decoct_end` datetime DEFAULT NULL COMMENT '二煎结束',
  `package_start_time` datetime DEFAULT NULL COMMENT '包装开始',
  `package_end_time` datetime DEFAULT NULL COMMENT '包装结束',
  `deliver_time` datetime DEFAULT NULL COMMENT '发货时间',
  `complete_time` datetime DEFAULT NULL COMMENT '全流程完成时间',
  `receive_operator` varchar(50) DEFAULT NULL COMMENT '接方员',
  `audit_operator` varchar(50) DEFAULT NULL COMMENT '审方员',
  `dispense_operator` varchar(50) DEFAULT NULL COMMENT '调剂员',
  `review_operator` varchar(50) DEFAULT NULL COMMENT '复核员',
  `soak_operator` varchar(50) DEFAULT NULL COMMENT '泡药员',
  `decoct_operator` varchar(50) DEFAULT NULL COMMENT '煎药员',
  `package_operator` varchar(50) DEFAULT NULL COMMENT '包装员',
  `deliver_operator` varchar(50) DEFAULT NULL COMMENT '发货员',
  `temp_curve_data` text COMMENT '温度曲线关键点位JSON',
  `max_temp` decimal(5,2) DEFAULT NULL COMMENT '最高温度',
  `avg_temp` decimal(5,2) DEFAULT NULL COMMENT '平均温度',
  `water_quality_check` varchar(200) DEFAULT NULL COMMENT '水质检测结果',
  `status` varchar(20) DEFAULT 'PENDING' COMMENT '追溯状态:PENDING/PROCESSING/COMPLETED/EXCEPTION',
  `exception_reason` varchar(500) DEFAULT NULL COMMENT '异常原因',
  `exception_handle_result` varchar(500) DEFAULT NULL COMMENT '异常处理结果',
  `label_print_count` int DEFAULT '0' COMMENT '标签打印次数',
  `delivery_no` varchar(50) DEFAULT NULL COMMENT '快递单号',
  `delivery_company` varchar(50) DEFAULT NULL COMMENT '快递公司',
  `tenant_id` varchar(32) DEFAULT 'default' COMMENT '租户ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` int DEFAULT '0' COMMENT '逻辑删除',
  `batch_no` varchar(50) DEFAULT NULL COMMENT '生产批次号',
  PRIMARY KEY (`id`),
  KEY `idx_batch_no` (`batch_no`)
) ENGINE=InnoDB AUTO_INCREMENT=900000066 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='煎药过程追溯主表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `decoction_trace_event`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `decoction_trace_event` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `trace_id` bigint NOT NULL COMMENT '关联追溯主表',
  `prescription_no` varchar(50) DEFAULT NULL COMMENT '处方号（冗余）',
  `event_code` varchar(50) DEFAULT NULL COMMENT '事件编码',
  `event_name` varchar(100) DEFAULT NULL COMMENT '事件名称',
  `event_type` varchar(20) DEFAULT NULL COMMENT '事件类型:SYSTEM/MANUAL/DEVICE/AUTO',
  `operator_id` bigint DEFAULT NULL COMMENT '操作人ID',
  `operator_name` varchar(50) DEFAULT NULL COMMENT '操作人姓名',
  `device_code` varchar(50) DEFAULT NULL COMMENT '关联设备编码',
  `device_type` varchar(20) DEFAULT NULL COMMENT '设备类型',
  `event_time` datetime DEFAULT NULL COMMENT '事件发生时间',
  `before_value` varchar(500) DEFAULT NULL COMMENT '变更前值',
  `after_value` varchar(500) DEFAULT NULL COMMENT '变更后值',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `attachment_url` varchar(500) DEFAULT NULL COMMENT '附件URL',
  `tenant_id` varchar(32) DEFAULT 'default' COMMENT '租户ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `deleted` int DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='追溯事件明细表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `device_command`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `device_command` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `device_code` varchar(50) NOT NULL COMMENT '目标设备编码',
  `command_type` varchar(50) NOT NULL COMMENT '指令类型:START_SOAK/START_DECOCT/PAUSE/RESUME/EMERGENCY_STOP/START_PACKAGE/SET_TEMP/ADD_LATE_REMIND/CONFIRM_ADD_LATE',
  `command_payload` text COMMENT '指令参数JSON',
  `status` varchar(20) DEFAULT 'PENDING' COMMENT '状态:PENDING/SENT/ACKED/FAILED/TIMEOUT',
  `response_payload` text COMMENT '设备响应JSON',
  `retry_count` int DEFAULT '0' COMMENT '重试次数',
  `send_time` datetime DEFAULT NULL COMMENT '发送时间',
  `ack_time` datetime DEFAULT NULL COMMENT '确认时间',
  `fail_reason` varchar(500) DEFAULT NULL COMMENT '失败原因',
  `tenant_id` varchar(32) DEFAULT 'default' COMMENT '租户ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` int DEFAULT '0' COMMENT '逻辑删除',
  `command_level` varchar(20) DEFAULT 'NORMAL' COMMENT '指令等级: NORMAL/IMPORTANT/CRITICAL',
  `risk_level` varchar(20) DEFAULT 'LOW' COMMENT '风险等级: LOW/MEDIUM/HIGH',
  `require_confirm` tinyint(1) DEFAULT '0' COMMENT '是否需要确认: 0否/1是',
  PRIMARY KEY (`id`),
  KEY `idx_device_command_code` (`device_code`),
  KEY `idx_device_command_status` (`status`),
  KEY `idx_device_command_type` (`command_type`)
) ENGINE=InnoDB AUTO_INCREMENT=900000072 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='设备指令表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `device_utilization`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `device_utilization` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `device_code` varchar(50) NOT NULL COMMENT '设备编码',
  `stat_date` date NOT NULL COMMENT '统计日期',
  `total_minutes` int DEFAULT '1440' COMMENT '总分钟数',
  `run_minutes` int DEFAULT '0' COMMENT '运行分钟数',
  `idle_minutes` int DEFAULT '0' COMMENT '空闲分钟数',
  `fault_minutes` int DEFAULT '0' COMMENT '故障分钟数',
  `offline_minutes` int DEFAULT '0' COMMENT '离线分钟数',
  `maintenance_minutes` int DEFAULT '0' COMMENT '维护分钟数',
  `utilization_rate` decimal(5,2) DEFAULT NULL COMMENT '利用率%',
  `availability_rate` decimal(5,2) DEFAULT NULL COMMENT '可用率%',
  `fault_count` int DEFAULT '0' COMMENT '故障次数',
  `task_count` int DEFAULT '0' COMMENT '完成任务数',
  `tenant_id` varchar(32) DEFAULT 'default' COMMENT '租户ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` int DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_du_device_date` (`device_code`,`stat_date`)
) ENGINE=InnoDB AUTO_INCREMENT=900000219 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='设备利用率表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `prod_alert_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `prod_alert_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `monitor_id` bigint DEFAULT NULL COMMENT 'monitor id',
  `task_id` bigint DEFAULT NULL COMMENT '任务ID',
  `stage` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '触发预警的生产阶段',
  `alert_level` tinyint DEFAULT NULL COMMENT '等级：1预警 2告警 3严重',
  `alert_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '类型：TIMEOUT-超时, APPROACHING-即将超时',
  `alert_content` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'alert content',
  `notify_channels` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '通知渠道：BOARD,PDA,WECHAT,SMS',
  `notify_targets` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '通知对象ID列表',
  `is_resolved` tinyint DEFAULT NULL COMMENT '是否已处理: 0=否 1=是',
  `resolved_by` bigint DEFAULT NULL COMMENT '处理人',
  `resolved_at` datetime DEFAULT NULL COMMENT '解决时间',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_monitor_id` (`monitor_id`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_alert_level` (`alert_level`),
  KEY `idx_is_resolved` (`is_resolved`),
  KEY `idx_al_resolved_monitor` (`is_resolved`,`monitor_id`)
) ENGINE=InnoDB AUTO_INCREMENT=900000887 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='预警记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `prod_emergency_prescription`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `prod_emergency_prescription` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `prescription_id` bigint NOT NULL COMMENT '关联处方ID',
  `emergency_level` tinyint DEFAULT '1' COMMENT '急诊级别：1普通急诊 2危重急诊 3抢救',
  `request_time` datetime DEFAULT NULL COMMENT '急诊请求时间',
  `promised_finish_time` datetime DEFAULT NULL COMMENT '承诺完成时间',
  `actual_finish_time` datetime DEFAULT NULL COMMENT '实际完成时间',
  `is_on_time` tinyint DEFAULT NULL COMMENT '是否按时完成',
  `delay_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '延迟原因',
  `delivery_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '送达方式：SELF_PICK-自取, DIRECT_DELIVERY-直送急诊室, EXPRESS-快递',
  `delivery_location` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '送达位置',
  `nurse_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '接收护士',
  `nurse_sign_time` datetime DEFAULT NULL COMMENT '护士签收时间',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `prescription_id` (`prescription_id`),
  KEY `idx_prescription_id` (`prescription_id`),
  KEY `idx_emergency_level` (`emergency_level`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='急诊处方表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `prod_employee_barcode`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `prod_employee_barcode` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `employee_id` bigint DEFAULT NULL COMMENT 'employee id',
  `barcode` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '条码内容',
  `barcode_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'QR' COMMENT '类型：CODE128-一维码, QR-二维码',
  `card_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'BADGE' COMMENT '卡片类型：BADGE-工牌, WRIST-腕带, STICKER-贴纸',
  `print_count` int DEFAULT '1' COMMENT '打印次数',
  `last_print_time` datetime DEFAULT NULL COMMENT '最后打印时间',
  `valid_from` datetime DEFAULT NULL COMMENT '有效期起',
  `valid_to` datetime DEFAULT NULL COMMENT '有效期止',
  `is_active` tinyint DEFAULT NULL COMMENT '是否启用: 0=禁用 1=启用',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `barcode` (`barcode`),
  KEY `idx_employee_id` (`employee_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='员工条码表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `prod_employee_skill`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `prod_employee_skill` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `employee_id` bigint NOT NULL COMMENT '员工ID',
  `skill_code` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '技能编码：XIE_BIAO-解表药, ZI_BU-滋补药, WAI_YONG-外用药',
  `skill_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '名称',
  `proficiency_level` tinyint DEFAULT '1' COMMENT '熟练度：1初级 2中级 3高级',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_employee_id` (`employee_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='员工技能标签表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `prod_herb_group_rule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `prod_herb_group_rule` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `group_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '分组编码',
  `group_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '分组名称',
  `group_color` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT '#4488FF' COMMENT '显示颜色',
  `group_icon` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '图标',
  `process_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '处理方式：PRE_DECOCT-先煎, NORMAL-群煎, POST_DECOCT-后下, WRAP-包煎, MELT-烊化, DIRECT-冲服',
  `standard_duration` int DEFAULT NULL COMMENT '标准处理时长（分钟）',
  `special_instruction` text COLLATE utf8mb4_unicode_ci COMMENT '特殊说明',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `is_active` tinyint DEFAULT NULL COMMENT '是否启用: 0=禁用 1=启用',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `group_code` (`group_code`),
  KEY `idx_process_type` (`process_type`)
) ENGINE=InnoDB AUTO_INCREMENT=1003 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='药材分组规则表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `prod_patient_notify`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `prod_patient_notify` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `prescription_id` bigint DEFAULT NULL COMMENT '处方ID',
  `notify_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '类型：START-开始, COMPLETE-完成, DELIVERY-配送, REMIND-服药提醒',
  `channel` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '渠道：SMS-短信, WECHAT-微信, APP-APP推送',
  `content` text COLLATE utf8mb4_unicode_ci COMMENT '内容',
  `is_sent` tinyint DEFAULT NULL COMMENT '是否已发送: 0=否 1=是',
  `sent_at` datetime DEFAULT NULL COMMENT '发送时间',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_prescription_id` (`prescription_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='患者通知记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `prod_patient_token`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `prod_patient_token` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `token` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '查询令牌',
  `prescription_id` bigint DEFAULT NULL COMMENT '处方ID',
  `patient_phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '患者电话',
  `expire_at` datetime DEFAULT NULL COMMENT '过期时间',
  `access_count` int DEFAULT '0' COMMENT '查询次数',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `token` (`token`),
  KEY `idx_token` (`token`),
  KEY `idx_prescription_id` (`prescription_id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='患者查询令牌表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `prod_prescription_herb_group`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `prod_prescription_herb_group` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `prescription_id` bigint NOT NULL COMMENT '关联处方ID',
  `group_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '分组编码：SOAK浸泡/FIRST先煎/MAIN主煎/LAST后下/WRAP包煎',
  `group_seq` int DEFAULT '0' COMMENT '组序号',
  `herbs_json` json DEFAULT NULL COMMENT '本组药材列表',
  `process_status` tinyint DEFAULT '0' COMMENT '处理状态：0待处理 1处理中 2已完成',
  `process_time` datetime DEFAULT NULL COMMENT '实际处理时间',
  `operator_id` bigint DEFAULT NULL COMMENT '操作人',
  `device_id` bigint DEFAULT NULL COMMENT '使用的设备',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_prescription_id` (`prescription_id`),
  KEY `idx_group_code` (`group_code`)
) ENGINE=InnoDB AUTO_INCREMENT=1003 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='处方药材分组实例表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `prod_rollback_reason`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `prod_rollback_reason` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `reason_code` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '编码',
  `reason_name` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '名称',
  `reason_category` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '类别',
  `need_approval` tinyint DEFAULT '0' COMMENT '是否需要审批',
  `approval_level` tinyint DEFAULT NULL COMMENT '审批级别：1班组长 2主任 3医务科',
  `is_active` tinyint DEFAULT NULL COMMENT '是否启用: 0=禁用 1=启用',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `reason_code` (`reason_code`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='回退原因字典表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `prod_shelf`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `prod_shelf` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `shelf_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '货架编码：A-01-03',
  `shelf_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '货架名称',
  `area_code` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '区域编码',
  `area_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '区域名称',
  `row_num` int DEFAULT NULL COMMENT '排号',
  `layer_num` int DEFAULT NULL COMMENT '层号',
  `capacity` int DEFAULT NULL COMMENT '容量（袋数）',
  `current_count` int DEFAULT '0' COMMENT '当前存放数',
  `shelf_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'NORMAL' COMMENT '类型：NORMAL-常温, COLD-冷藏, EXPRESS-快递专区',
  `status` tinyint DEFAULT '1' COMMENT '状态：1启用 2停用 3维护中',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `shelf_code` (`shelf_code`),
  KEY `idx_area_code` (`area_code`),
  KEY `idx_shelf_type` (`shelf_type`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='货架定义表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `prod_shelf_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `prod_shelf_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `prescription_id` bigint DEFAULT NULL COMMENT '处方ID',
  `package_barcode` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '药袋条码',
  `shelf_id` bigint DEFAULT NULL COMMENT 'shelf id',
  `shelf_code` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '货架编码',
  `put_on_time` datetime DEFAULT NULL COMMENT '上架时间',
  `put_on_by` bigint DEFAULT NULL COMMENT '上架人',
  `take_off_time` datetime DEFAULT NULL COMMENT '下架时间',
  `take_off_by` bigint DEFAULT NULL COMMENT '下架人',
  `take_off_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '下架类型：SELF_PICK-自取, DELIVERY-配送, EXPIRED-过期处理',
  `status` tinyint DEFAULT '1' COMMENT '状态：1在架 2已取 3过期',
  `expire_warning_time` datetime DEFAULT NULL COMMENT '过期预警时间',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_prescription_id` (`prescription_id`),
  KEY `idx_shelf_id` (`shelf_id`),
  KEY `idx_package_barcode` (`package_barcode`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='上架记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `prod_task_assignment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `prod_task_assignment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `task_id` bigint NOT NULL COMMENT '煎药任务ID',
  `prescription_id` bigint DEFAULT NULL COMMENT '处方ID',
  `device_id` bigint DEFAULT NULL COMMENT '分配设备ID',
  `employee_id` bigint DEFAULT NULL COMMENT '分配员工ID',
  `assign_type` tinyint DEFAULT '1' COMMENT '分配类型：1自动 2手动 3应急',
  `assign_reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '分配原因/策略说明',
  `scheduled_start_time` datetime DEFAULT NULL COMMENT '计划开始时间',
  `scheduled_end_time` datetime DEFAULT NULL COMMENT '计划结束时间',
  `actual_start_time` datetime DEFAULT NULL COMMENT '实际开始时间',
  `actual_end_time` datetime DEFAULT NULL COMMENT '实际完成时间',
  `status` tinyint DEFAULT '1' COMMENT '状态：1待执行 2执行中 3已完成 4已取消',
  `stage_breakdown_json` text COLLATE utf8mb4_unicode_ci COMMENT '阶段拆分JSON',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_prescription_id` (`prescription_id`),
  KEY `idx_employee_id` (`employee_id`),
  KEY `idx_device_id` (`device_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=900000025 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务智能分配表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `prod_task_rollback`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `prod_task_rollback` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `original_task_id` bigint NOT NULL COMMENT '原任务ID',
  `new_task_id` bigint DEFAULT NULL COMMENT '回退后生成的新任务ID',
  `rollback_from` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '从哪个阶段回退',
  `rollback_to` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '回退到哪个阶段',
  `rollback_reason` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '回退原因',
  `rollback_type` tinyint DEFAULT '1' COMMENT '回退类型：1普通 2跨阶段 3已发货召回',
  `operator_id` bigint NOT NULL COMMENT '操作人',
  `reviewer_id` bigint DEFAULT NULL COMMENT '复核人',
  `approver_id` bigint DEFAULT NULL COMMENT '审批人',
  `approval_status` tinyint DEFAULT '0' COMMENT '审批状态：0待审批 1已通过 2已拒绝',
  `approval_comment` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'approval comment',
  `inventory_reverted` tinyint DEFAULT '0' COMMENT '库存是否已退回',
  `device_reset` tinyint DEFAULT '0' COMMENT '设备是否已重置',
  `patient_notified` tinyint DEFAULT '0' COMMENT '患者是否已通知',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  `approved_at` datetime DEFAULT NULL COMMENT '审批时间',
  PRIMARY KEY (`id`),
  KEY `idx_original_task_id` (`original_task_id`),
  KEY `idx_approval_status` (`approval_status`),
  KEY `idx_rollback_type` (`rollback_type`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务回退记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `prod_time_monitor`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `prod_time_monitor` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `task_id` bigint DEFAULT NULL COMMENT '任务ID',
  `assignment_id` bigint DEFAULT NULL COMMENT '分配记录ID',
  `prescription_id` bigint DEFAULT NULL COMMENT '处方ID',
  `stage` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '当前阶段',
  `planned_start` datetime DEFAULT NULL COMMENT '计划开始',
  `planned_end` datetime DEFAULT NULL COMMENT '计划结束',
  `actual_start` datetime DEFAULT NULL COMMENT '实际开始',
  `actual_end` datetime DEFAULT NULL COMMENT '实际结束',
  `remaining_seconds` int DEFAULT NULL COMMENT '剩余秒数',
  `status` tinyint DEFAULT '1' COMMENT '状态：1正常 2预警中 3已超时 4已处理',
  `warning_count` int DEFAULT '0' COMMENT '预警次数',
  `last_warning_time` datetime DEFAULT NULL COMMENT '上次预警时间',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime DEFAULT NULL COMMENT '更新时间',
  `alert_level` int DEFAULT '0' COMMENT '告警级别:0=正常 1=预警 2=超时 3=告警 4=严重',
  PRIMARY KEY (`id`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_status` (`status`),
  KEY `idx_planned_end` (`planned_end`),
  KEY `idx_tm_task_updated` (`task_id`,`updated_at` DESC),
  KEY `idx_tm_actual_end` (`actual_start`,`actual_end`)
) ENGINE=InnoDB AUTO_INCREMENT=900000645 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='时效监控实例表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `prod_time_rule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `prod_time_rule` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `rule_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '规则编码',
  `rule_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '规则名称',
  `prescription_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'NORMAL' COMMENT '适用处方类型：NORMAL-普通, EMERGENCY-普通急诊, CRITICAL_EMERGENCY-危重急诊',
  `stage` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '阶段：SOAK-泡药, FIRST_DECOCTION-一煎, SECOND_DECOCTION-二煎, DECOCT-煎药, WRAP-包装',
  `standard_duration` int DEFAULT NULL COMMENT '标准时长（分钟）',
  `warning_threshold` int DEFAULT NULL COMMENT '预警阈值（提前X分钟）',
  `alert_threshold` int DEFAULT NULL COMMENT '告警阈值（超时X分钟）',
  `critical_threshold` int DEFAULT NULL COMMENT '严重阈值（超时X分钟）',
  `is_default` tinyint DEFAULT '0' COMMENT '是否默认规则',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `rule_code` (`rule_code`),
  KEY `idx_prescription_type` (`prescription_type`),
  KEY `idx_stage` (`stage`)
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='时效规则表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `prod_voice_setting`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `prod_voice_setting` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `device_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '设备ID（PDA设备标识）',
  `user_id` bigint DEFAULT NULL COMMENT '用户ID',
  `speech_rate` tinyint DEFAULT '50' COMMENT '语速 0-100',
  `volume` tinyint DEFAULT '80' COMMENT '音量 0-100',
  `voice_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'female' COMMENT '语音类型：male-男声, female-女声',
  `enable_voice` tinyint DEFAULT '1' COMMENT '是否启用语音',
  `quiet_start` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT '12:00' COMMENT '免打扰开始时间',
  `quiet_end` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT '13:30' COMMENT '免打扰结束时间',
  `repeat_count` tinyint DEFAULT '1' COMMENT '重复播报次数',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_device_id` (`device_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='PDA语音设置表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `eq_alarm_notification`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `eq_alarm_notification` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` varchar(32) DEFAULT 'default' COMMENT '租户ID',
  `alarm_id` bigint NOT NULL COMMENT '关联告警ID',
  `notify_type` varchar(20) NOT NULL DEFAULT 'IN_APP' COMMENT '通知类型:IN_APP/WS/EMAIL/SMS',
  `notify_target` varchar(200) DEFAULT NULL COMMENT '通知目标',
  `notify_content` varchar(1000) DEFAULT NULL COMMENT '通知内容',
  `send_status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '发送状态:PENDING/SENT/FAILED',
  `send_time` datetime DEFAULT NULL COMMENT '发送时间',
  `error_msg` varchar(500) DEFAULT NULL COMMENT '失败原因',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` int DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_eq_alarm_notification_alarm` (`alarm_id`),
  KEY `idx_eq_alarm_notification_status` (`send_status`),
  KEY `idx_eq_alarm_notification_type` (`notify_type`),
  KEY `idx_eq_alarm_notification_created` (`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=900000007 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='设备告警通知表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `eq_device`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `eq_device` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `device_code` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '设备编码',
  `name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '名称',
  `device_type` bigint DEFAULT NULL COMMENT '设备类型: 1=煎药机 2=包装机 3=打印机',
  `ip_address` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'IP地址',
  `port` bigint DEFAULT NULL COMMENT 'port',
  `protocol_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '协议类型: MQTT/TCP/HTTP',
  `location_x` bigint DEFAULT NULL COMMENT 'X坐标位置',
  `location_y` bigint DEFAULT NULL COMMENT 'Y坐标位置',
  `decoct_mode` bigint DEFAULT NULL COMMENT '煎煮模式',
  `pressure_mode` bigint DEFAULT NULL COMMENT '压力模式: 1=常压 2=高压',
  `slow_fire_time` bigint DEFAULT NULL COMMENT '文火时间(分钟)',
  `package_num` bigint DEFAULT NULL COMMENT '包装数量',
  `package_capacity` bigint DEFAULT NULL COMMENT '包装容量',
  `alarm_min_temp` decimal(5,2) DEFAULT '0.00',
  `alarm_max_temp` decimal(5,2) DEFAULT '120.00',
  `fault_code` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `version` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'IDLE',
  `current_temp` decimal(5,2) DEFAULT '0.00',
  `enabled` bigint DEFAULT '1',
  `deleted` bigint DEFAULT '0',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `auto_level` varchar(16) COLLATE utf8mb4_unicode_ci DEFAULT 'manual',
  `label_mode` varchar(16) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `alert_time` timestamp NULL DEFAULT NULL,
  `resolved_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `resolved_at` timestamp NULL DEFAULT NULL,
  `tenant_id` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT 'default',
  `last_heartbeat` datetime DEFAULT NULL,
  `current_scheme_id` bigint DEFAULT NULL,
  `group_id` bigint DEFAULT NULL,
  `detail_status` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'IDLE' COMMENT '精细状态: 24种设备状态',
  `manufacturer` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '厂商',
  `model_num` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '型号',
  `serial_number` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '序列号',
  `communication_id` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '通信ID',
  `barcode_data` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '条码数据',
  `install_date` date DEFAULT NULL COMMENT '安装日期',
  `warranty_expire` date DEFAULT NULL COMMENT '保修到期日',
  `config_id` bigint DEFAULT NULL COMMENT 'MQTT配置ID(外键)',
  `current_prescription_code` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '当前处方编号',
  `current_operator_id` bigint DEFAULT NULL COMMENT '当前操作人ID',
  `current_operator_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '当前操作人姓名',
  `estimated_finish_time` datetime DEFAULT NULL COMMENT '预计完成时间',
  `remaining_time` int DEFAULT '0' COMMENT '剩余时间(秒)',
  `progress_percent` int DEFAULT '0' COMMENT '当前工序进度百分比',
  `water_level` int DEFAULT '0' COMMENT '水位百分比(0-100)',
  `pressure` decimal(5,2) DEFAULT '0.00' COMMENT '压力值(MPa)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `device_code` (`device_code`),
  KEY `idx_eq_device_detail_status` (`detail_status`),
  KEY `idx_eq_device_manufacturer` (`manufacturer`),
  KEY `idx_eq_device_model` (`model_num`),
  KEY `idx_eq_device_comm_id` (`communication_id`),
  KEY `idx_eq_device_config` (`config_id`)
) ENGINE=InnoDB AUTO_INCREMENT=782 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='设备表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `eq_device_alarm`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `eq_device_alarm` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `device_id` bigint NOT NULL,
  `alarm_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `alarm_level` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `message` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` bigint DEFAULT '0',
  `resolved_time` datetime DEFAULT NULL,
  `is_resolved` bigint DEFAULT '0',
  `resolved_at` datetime DEFAULT NULL,
  `deleted` bigint DEFAULT '0',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `tenant_id` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT 'default',
  PRIMARY KEY (`id`),
  KEY `eq_device_alarm_ibfk_1` (`device_id`),
  CONSTRAINT `eq_device_alarm_ibfk_1` FOREIGN KEY (`device_id`) REFERENCES `eq_device` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900000007 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='设备报警表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `eq_device_connection`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `eq_device_connection` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `decoct_device_id` bigint NOT NULL,
  `package_device_id` bigint NOT NULL,
  `is_primary` bigint DEFAULT '1',
  `deleted` bigint DEFAULT '0',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `tenant_id` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT 'default',
  PRIMARY KEY (`id`),
  UNIQUE KEY `decoct_device_id` (`decoct_device_id`,`package_device_id`),
  KEY `eq_device_connection_ibfk_2` (`package_device_id`),
  CONSTRAINT `eq_device_connection_ibfk_1` FOREIGN KEY (`decoct_device_id`) REFERENCES `eq_device` (`id`),
  CONSTRAINT `eq_device_connection_ibfk_2` FOREIGN KEY (`package_device_id`) REFERENCES `eq_device` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='设备连接表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `eq_device_group`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `eq_device_group` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `group_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `group_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `group_type` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '分组类型: PRODUCTION_LINE/WORKSHOP/AREA/OTHER',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `package_device_id` bigint DEFAULT NULL,
  `printer_device_id` bigint DEFAULT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'ACTIVE',
  `tenant_id` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT 'default',
  `deleted` bigint DEFAULT '0',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `group_code` (`group_code`),
  KEY `eq_device_group_ibfk_1` (`package_device_id`),
  KEY `eq_device_group_ibfk_2` (`printer_device_id`),
  CONSTRAINT `eq_device_group_ibfk_1` FOREIGN KEY (`package_device_id`) REFERENCES `eq_device` (`id`),
  CONSTRAINT `eq_device_group_ibfk_2` FOREIGN KEY (`printer_device_id`) REFERENCES `eq_device` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=100004 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='设备分组表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `eq_device_group_member`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `eq_device_group_member` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `group_id` bigint NOT NULL,
  `decoct_device_id` bigint NOT NULL,
  `sort_order` bigint DEFAULT '0',
  `tenant_id` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT 'default',
  `deleted` bigint DEFAULT '0',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `group_id` (`group_id`,`decoct_device_id`),
  KEY `eq_device_group_member_ibfk_2` (`decoct_device_id`),
  CONSTRAINT `eq_device_group_member_ibfk_1` FOREIGN KEY (`group_id`) REFERENCES `eq_device_group` (`id`),
  CONSTRAINT `eq_device_group_member_ibfk_2` FOREIGN KEY (`decoct_device_id`) REFERENCES `eq_device` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='设备分组成员';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `eq_device_group_rule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `eq_device_group_rule` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `group_id` bigint NOT NULL COMMENT '关联设备分组ID',
  `rule_name` varchar(100) NOT NULL COMMENT '规则名称',
  `trigger_condition` varchar(255) NOT NULL COMMENT '触发条件JSON',
  `action_type` varchar(50) NOT NULL COMMENT '动作类型: START/STOP/ALARM/NOTIFY/EMERGENCY_STOP',
  `target_devices` text COMMENT '目标设备编码列表JSON',
  `enabled` tinyint(1) DEFAULT '1' COMMENT '是否启用: 0禁用/1启用',
  `tenant_id` varchar(32) DEFAULT 'default' COMMENT '租户ID',
  `deleted` int DEFAULT '0' COMMENT '逻辑删除',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_eq_device_group_rule_group_id` (`group_id`),
  KEY `idx_eq_device_group_rule_enabled` (`enabled`)
) ENGINE=InnoDB AUTO_INCREMENT=900000045 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='设备分组联动规则表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `eq_device_maintenance`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `eq_device_maintenance` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `device_id` bigint NOT NULL COMMENT '设备ID',
  `maintenance_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '维护类型: MAINTENANCE保养/REPAIR维修/INSPECTION巡检',
  `content` text COLLATE utf8mb4_unicode_ci COMMENT '维护内容',
  `parts` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '更换配件',
  `cost` decimal(10,2) DEFAULT NULL COMMENT '费用',
  `operator_id` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作人',
  `plan_date` date DEFAULT NULL COMMENT '计划日期',
  `finish_date` date DEFAULT NULL COMMENT '完成日期',
  `status` tinyint DEFAULT '0' COMMENT '状态: 0待执行/1已完成',
  `tenant_id` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'default' COMMENT '租户ID',
  `deleted` bigint DEFAULT '0' COMMENT '逻辑删除',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_device_id` (`device_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=900000050 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='设备维护记录';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `eq_device_mqtt_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `eq_device_mqtt_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `device_code` varchar(50) NOT NULL COMMENT '设备编码',
  `broker_url` varchar(200) DEFAULT NULL COMMENT 'Broker地址',
  `port` int DEFAULT '1883' COMMENT '端口',
  `username` varchar(100) DEFAULT NULL COMMENT '用户名',
  `password_encrypted` varchar(500) DEFAULT NULL COMMENT '加密密码',
  `client_id` varchar(100) DEFAULT NULL COMMENT 'Client ID',
  `qos` tinyint DEFAULT '1' COMMENT 'QoS等级: 0/1/2',
  `clean_session` tinyint DEFAULT '0' COMMENT '保持会话: 0=开启, 1=关闭',
  `publish_topic` varchar(200) DEFAULT NULL COMMENT '设备发布Topic',
  `subscribe_topic` varchar(200) DEFAULT NULL COMMENT '设备订阅Topic',
  `token_secret` varchar(256) DEFAULT NULL COMMENT 'Token密钥',
  `tenant_id` varchar(32) DEFAULT 'default' COMMENT '租户ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` int DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `device_code` (`device_code`),
  UNIQUE KEY `uk_device_code` (`device_code`),
  KEY `idx_eq_mqtt_config_tenant` (`tenant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='设备MQTT配置表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `eq_device_operator`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `eq_device_operator` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `device_code` varchar(50) NOT NULL COMMENT '设备编码',
  `operator_id` bigint NOT NULL COMMENT '操作人ID',
  `operator_name` varchar(50) DEFAULT NULL COMMENT '操作人姓名',
  `shift_start_time` datetime DEFAULT NULL COMMENT '班次开始时间',
  `shift_end_time` datetime DEFAULT NULL COMMENT '班次结束时间',
  `is_current` tinyint(1) DEFAULT '1' COMMENT '是否当前班次:1=是,0=否',
  `tenant_id` varchar(32) DEFAULT 'default' COMMENT '租户ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` int DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_device_shift` (`device_code`,`shift_start_time`),
  KEY `idx_eq_device_operator_code` (`device_code`),
  KEY `idx_eq_device_operator_current` (`device_code`,`is_current`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='设备操作人绑定表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `eq_device_pairing`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `eq_device_pairing` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `pairing_name` varchar(100) NOT NULL COMMENT '配对名称，如：生产线A',
  `decocter_ids` varchar(255) NOT NULL COMMENT '煎药机ID列表，逗号分隔，最多4个',
  `packer_id` bigint NOT NULL COMMENT '包装机ID',
  `labeler_id` bigint NOT NULL COMMENT '标签打印机ID',
  `status` varchar(20) DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE-启用，DISABLED-禁用',
  `tenant_id` varchar(50) DEFAULT 'default',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` int DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='设备生产线配对表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `eq_device_status`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `eq_device_status` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `device_code` varchar(50) NOT NULL COMMENT '设备编码',
  `device_type` int DEFAULT NULL COMMENT '设备类型',
  `status` varchar(20) DEFAULT NULL COMMENT '兼容旧状态',
  `detail_status` varchar(50) DEFAULT NULL COMMENT '精细状态',
  `current_temp` decimal(5,2) DEFAULT '0.00' COMMENT '当前温度',
  `target_temp` decimal(5,2) DEFAULT '0.00' COMMENT '目标温度',
  `water_level` int DEFAULT '0' COMMENT '水位',
  `pressure` decimal(5,2) DEFAULT '0.00' COMMENT '压力',
  `prescription_code` varchar(100) DEFAULT NULL COMMENT '当前处方编号',
  `scheme_name` varchar(100) DEFAULT NULL COMMENT '当前方案名称',
  `operator_id` bigint DEFAULT NULL COMMENT '操作人ID',
  `operator_name` varchar(50) DEFAULT NULL COMMENT '操作人姓名',
  `progress_percent` int DEFAULT '0' COMMENT '进度百分比',
  `remaining_time` int DEFAULT '0' COMMENT '剩余秒数',
  `fault_code` varchar(50) DEFAULT NULL COMMENT '故障码',
  `fault_message` varchar(500) DEFAULT NULL COMMENT '故障描述',
  `snapshot_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '快照时间',
  `tenant_id` varchar(32) DEFAULT 'default' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  KEY `idx_eq_device_status_code` (`device_code`),
  KEY `idx_eq_device_status_time` (`snapshot_time`)
) ENGINE=InnoDB AUTO_INCREMENT=900000073 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='设备实时状态快照表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `eq_device_status_backup_20260507`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `eq_device_status_backup_20260507` (
  `id` bigint NOT NULL DEFAULT '0' COMMENT '主键ID',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'IDLE',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `eq_temperature_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `eq_temperature_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `device_id` bigint DEFAULT NULL,
  `device_code` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `temperature` decimal(5,2) DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `tenant_id` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT 'default',
  `recorded_at` datetime DEFAULT NULL COMMENT '设备上报时间',
  PRIMARY KEY (`id`),
  KEY `idx_eq_temp_log_recorded` (`device_id`,`recorded_at`),
  CONSTRAINT `eq_temperature_log_ibfk_1` FOREIGN KEY (`device_id`) REFERENCES `eq_device` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=37799 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='设备温度记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `eq_temperature_log_archive`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `eq_temperature_log_archive` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `device_id` bigint DEFAULT NULL,
  `device_code` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `temperature` decimal(5,2) DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `tenant_id` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT 'default',
  `recorded_at` datetime DEFAULT NULL COMMENT '设备上报时间',
  `archived_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_eq_temp_log_recorded` (`device_id`,`recorded_at`),
  KEY `idx_archived` (`archived_at`),
  KEY `idx_recorded` (`device_id`,`recorded_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='设备温度存档表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `eq_wash_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `eq_wash_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `device_id` bigint NOT NULL COMMENT '设备ID',
  `device_code` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '设备编码',
  `task_id` bigint DEFAULT NULL COMMENT '关联任务',
  `prescription_id` bigint DEFAULT NULL COMMENT '关联处方',
  `wash_type` tinyint NOT NULL COMMENT '1=常规 2=强化',
  `standard_duration` int NOT NULL COMMENT '标准时长(分钟)',
  `start_time` datetime NOT NULL,
  `end_time` datetime DEFAULT NULL,
  `duration_min` int DEFAULT NULL COMMENT '实际时长(分钟)',
  `result` tinyint DEFAULT NULL COMMENT '1=合格 0=不合格',
  `operator_id` bigint NOT NULL,
  `operator_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `tenant_id` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT 'default',
  `deleted` int DEFAULT '0',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_device` (`device_id`),
  KEY `idx_task` (`task_id`),
  KEY `idx_start_time` (`start_time`)
) ENGINE=InnoDB AUTO_INCREMENT=900000055 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='设备清洗记录';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `eq_wash_standard`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `eq_wash_standard` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `device_type` tinyint NOT NULL COMMENT '1=煎药机 2=包装机',
  `wash_type` tinyint NOT NULL COMMENT '1=常规 2=强化',
  `standard_duration` int NOT NULL COMMENT '标准时长(分钟)',
  `wash_steps` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '步骤说明',
  `alert_threshold` int DEFAULT NULL COMMENT '超时告警阈值(分钟)',
  `is_active` tinyint DEFAULT '1',
  `tenant_id` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT 'default',
  `deleted` int DEFAULT '0',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_type` (`device_type`,`wash_type`),
  KEY `idx_active` (`is_active`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='清洗标准配置';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `exc_escalation_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `exc_escalation_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `exception_id` bigint DEFAULT NULL COMMENT '异常记录ID',
  `from_level` bigint DEFAULT NULL COMMENT '原异常等级',
  `to_level` bigint DEFAULT NULL COMMENT '升级后异常等级',
  `from_handler` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '原处理人',
  `to_handler` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '升级后处理人',
  `escalation_reason` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '升级原因',
  `escalated_at` datetime DEFAULT NULL COMMENT '升级时间',
  PRIMARY KEY (`id`),
  KEY `idx_esc_exc_id` (`exception_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='异常升级日志表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `exc_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `exc_template` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `exc_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '异常类型',
  `exc_level` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '异常等级',
  `root_cause_level1` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '一级根因分类',
  `root_cause_level2` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '二级根因分类',
  `root_cause_template` text COLLATE utf8mb4_unicode_ci COMMENT '根因描述模板',
  `corrective_template` text COLLATE utf8mb4_unicode_ci COMMENT '纠正措施模板',
  `preventive_template` text COLLATE utf8mb4_unicode_ci COMMENT '预防措施模板',
  `suggested_action` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '建议处理动作',
  `is_active` bigint DEFAULT NULL COMMENT '是否启用（1-启用，0-禁用）',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='异常模板表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `hospital_medicine_mapping`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `hospital_medicine_mapping` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `hospital_id` bigint NOT NULL COMMENT '医院ID',
  `hospital_medicine_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '医院药材编码',
  `hospital_medicine_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '医院药材名称',
  `system_medicine_id` bigint NOT NULL COMMENT '系统药材ID',
  `system_medicine_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '系统药材名称',
  `conversion_ratio` decimal(10,4) DEFAULT '1.0000' COMMENT '换算比例',
  `hospital_unit` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '医院单位',
  `system_unit` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '系统单位',
  `is_active` tinyint DEFAULT '1',
  `tenant_id` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT 'default',
  `deleted` int DEFAULT '0',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_hospital_code` (`hospital_id`,`hospital_medicine_code`),
  KEY `idx_system_medicine` (`system_medicine_id`),
  KEY `idx_hospital` (`hospital_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='医院药材编码映射';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `inv_stock_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `inv_stock_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `task_id` bigint DEFAULT NULL COMMENT '关联任务ID',
  `medicine_id` bigint DEFAULT NULL COMMENT '药品ID',
  `medicine_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '药品编码',
  `medicine_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '药品名称',
  `batch_id` bigint DEFAULT NULL COMMENT '批次ID',
  `change_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'CONSUME' COMMENT '变动类型（CONSUME-消耗等）',
  `change_quantity` decimal(12,3) NOT NULL COMMENT '变动数量',
  `before_quantity` decimal(12,3) DEFAULT NULL COMMENT '变动前数量',
  `after_quantity` decimal(12,3) DEFAULT NULL COMMENT '变动后数量',
  `ref_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '关联单据编号',
  `operator_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作人ID',
  `remark` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `tenant_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'default' COMMENT '租户ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_slog_task` (`task_id`),
  KEY `idx_slog_medicine` (`medicine_id`),
  KEY `idx_slog_batch` (`batch_id`),
  KEY `idx_slog_created` (`created_at`),
  KEY `idx_slog_ref` (`ref_no`)
) ENGINE=InnoDB AUTO_INCREMENT=25 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='库存变动日志表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `md_decoct_scheme`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `md_decoct_scheme` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `scheme_type` bigint DEFAULT '0',
  `decoct_times` bigint DEFAULT '0',
  `pressure` bigint DEFAULT '1',
  `upper_water` decimal(6,1) DEFAULT '0.0',
  `heating_time` bigint DEFAULT '30',
  `pre_heating_time` bigint DEFAULT NULL,
  `post_heating_time` bigint DEFAULT NULL,
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `deleted` bigint DEFAULT '0',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `tenant_id` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT 'default',
  `code` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `alarm_high_temp` decimal(5,2) DEFAULT NULL,
  `alarm_low_temp` decimal(5,2) DEFAULT NULL,
  `first_decoct_time` int DEFAULT '30' COMMENT '一煎时间(分钟)',
  `second_decoct_time` int DEFAULT '20' COMMENT '二煎时间(分钟)',
  `soak_time` int DEFAULT '30' COMMENT '浸泡时间(分钟)',
  `drain_time` int DEFAULT '10' COMMENT '出液时间(分钟)',
  `package_time` int DEFAULT '15' COMMENT '包装时间(分钟)',
  `late_add_remind_time` int DEFAULT '5' COMMENT '后下提醒提前时间(分钟)',
  `temp_rise_rate` decimal(3,1) DEFAULT '3.0' COMMENT '升温速率(°C/min)',
  `is_default` tinyint(1) DEFAULT '0' COMMENT '是否默认方案',
  `status` int DEFAULT '1' COMMENT '状态: 1启用 0禁用',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=44 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='煎法表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `md_hospital`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `md_hospital` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `code` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `contact_person` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `phone` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `address` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` int DEFAULT '1',
  `deleted` bigint DEFAULT '0',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `tenant_id` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT 'default',
  `color_code` varchar(16) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '院区标识颜色(如#FF5722)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `code` (`code`)
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='医院表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `medicine_group`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `medicine_group` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `prescription_id` bigint DEFAULT NULL COMMENT '处方ID',
  `prescription_no` varchar(50) DEFAULT NULL COMMENT '处方号',
  `medicine_name` varchar(100) DEFAULT NULL COMMENT '药材名称',
  `group_type` varchar(20) DEFAULT NULL COMMENT '分组类型:PRE_DECOCT/MAIN/ADD_LATE',
  `dosage` decimal(10,2) DEFAULT NULL COMMENT '剂量',
  `unit` varchar(20) DEFAULT NULL COMMENT '单位',
  `sort_order` int DEFAULT '0' COMMENT '组内排序',
  `remark` varchar(200) DEFAULT NULL COMMENT '备注',
  `tenant_id` varchar(32) DEFAULT 'default' COMMENT '租户ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` int DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='药材分组表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `pda_login_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pda_login_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `device_id` bigint DEFAULT NULL,
  `device_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `login_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `logout_time` datetime DEFAULT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ONLINE',
  `online_duration` bigint DEFAULT NULL,
  `ip_address` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `tenant_id` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT 'default',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted` bigint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_pda_login_user` (`user_id`),
  KEY `idx_pda_login_device` (`device_id`),
  KEY `idx_pda_login_status` (`status`),
  KEY `idx_pda_login_time` (`login_time`),
  CONSTRAINT `pda_login_record_chk_1` CHECK ((`status` in (_utf8mb4'ONLINE',_utf8mb4'OFFLINE')))
) ENGINE=InnoDB AUTO_INCREMENT=66 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='pda登录记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `pda_operation_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pda_operation_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `user_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `device_id` bigint DEFAULT NULL,
  `device_code` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `task_id` bigint DEFAULT NULL,
  `prescription_id` bigint DEFAULT NULL,
  `oper_type` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `oper_desc` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ext_data` text COLLATE utf8mb4_unicode_ci,
  `oper_result` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'SUCCESS',
  `error_msg` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `api_path` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `http_method` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `client_ip` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `request_time` bigint DEFAULT NULL,
  `oper_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `tenant_id` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT 'default',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted` bigint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_pda_log_user` (`user_id`),
  KEY `idx_pda_log_device` (`device_code`),
  KEY `idx_pda_log_task` (`task_id`),
  KEY `idx_pda_log_type` (`oper_type`),
  KEY `idx_pda_log_result` (`oper_result`),
  KEY `idx_pda_log_time` (`oper_time`),
  CONSTRAINT `pda_operation_log_chk_1` CHECK ((`oper_result` in (_utf8mb4'SUCCESS',_utf8mb4'FAILED')))
) ENGINE=InnoDB AUTO_INCREMENT=162 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='pda操作日志表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `pda_review_photo`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pda_review_photo` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `task_id` bigint NOT NULL,
  `prescription_id` bigint DEFAULT NULL,
  `photo_url` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `thumbnail_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `photo_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'REVIEW',
  `file_size` bigint DEFAULT NULL,
  `width` bigint DEFAULT NULL,
  `height` bigint DEFAULT NULL,
  `operator_id` bigint NOT NULL,
  `operator_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `review_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `tenant_id` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT 'default',
  `deleted` bigint NOT NULL DEFAULT '0',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_pda_photo_task` (`task_id`),
  KEY `idx_pda_photo_prescription` (`prescription_id`),
  KEY `idx_pda_photo_time` (`review_time`),
  CONSTRAINT `pda_review_photo_chk_1` CHECK ((`photo_type` in (_utf8mb4'REVIEW',_utf8mb4'WEIGHING',_utf8mb4'EXCEPTION')))
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='pda审核图片表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `prod_emergency_dispatch`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `prod_emergency_dispatch` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `emergency_prescription_id` bigint NOT NULL COMMENT '紧急处方ID',
  `target_task_id` bigint DEFAULT NULL COMMENT '被中断任务ID',
  `target_device_id` bigint NOT NULL COMMENT '被占用设备ID',
  `dispatch_result` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'IDLE/INTERRUPT/QUEUE',
  `operator_id` bigint NOT NULL COMMENT '调度操作人',
  `tenant_id` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT 'default',
  `deleted` int DEFAULT '0',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_emergency` (`emergency_prescription_id`),
  KEY `idx_target_task` (`target_task_id`),
  KEY `idx_device` (`target_device_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='紧急处方调度记录';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `prod_exception_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `prod_exception_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `exception_no` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `task_id` bigint NOT NULL,
  `original_task_id` bigint DEFAULT NULL,
  `new_task_id` bigint DEFAULT NULL,
  `prescription_no` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `patient_name` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `exception_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `exception_level` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `source_module` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `current_step` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  `discover_channel` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `discoverer_id` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `is_auto_handled` bigint DEFAULT '0',
  `handle_status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'PENDING',
  `handler_id` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `handler_role` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `assigned_at` datetime DEFAULT NULL,
  `resolution` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `resolution_result` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `resolved_at` datetime DEFAULT NULL,
  `rework_target_step` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `rework_validate_result` text COLLATE utf8mb4_unicode_ci,
  `loss_weight_gram` bigint DEFAULT '0',
  `loss_amount_yuan` double DEFAULT '0',
  `waste_liquid_ml` bigint DEFAULT '0',
  `device_no` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `operator_id` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `shift_type` varchar(16) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `consumable_batch` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `root_cause_level1` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `root_cause_level2` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `root_cause` text COLLATE utf8mb4_unicode_ci,
  `corrective_action` text COLLATE utf8mb4_unicode_ci,
  `preventive_action` text COLLATE utf8mb4_unicode_ci,
  `evidence_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `evidence_barcode` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `evidence_urls` text COLLATE utf8mb4_unicode_ci,
  `handler_sign` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `paper_record_no` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `evidence_hash` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `sla_deadline` datetime DEFAULT NULL,
  `is_timeout` bigint DEFAULT '0',
  `escalation_level` bigint DEFAULT '0',
  `escalated_at` datetime DEFAULT NULL,
  `is_concession` bigint DEFAULT '0',
  `doctor_sign` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `doctor_sign2` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `patient_consent` text COLLATE utf8mb4_unicode_ci,
  `derived_workorder_id` bigint DEFAULT NULL,
  `derived_workorder_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `created_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `last_modified_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `last_modified_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `is_deleted` bigint DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_exc_task_id` (`task_id`),
  KEY `idx_exc_prescription` (`prescription_no`),
  KEY `idx_exc_level_status` (`exception_level`,`handle_status`),
  KEY `idx_exc_created` (`created_at`),
  KEY `idx_exc_handler` (`handler_id`,`handle_status`),
  KEY `idx_exc_device` (`device_no`,`created_at`),
  KEY `idx_exc_operator` (`operator_id`,`created_at`),
  KEY `idx_exc_sla` (`sla_deadline`,`is_timeout`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='异常日志表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `prod_exception_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `prod_exception_order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `exception_no` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '异常单号',
  `task_id` bigint DEFAULT NULL COMMENT '关联任务',
  `device_id` bigint DEFAULT NULL COMMENT '关联设备',
  `exception_type` tinyint NOT NULL COMMENT '1=设备故障 2=停电 3=药材缺货 4=操作异常 5=打印失败 6=其他',
  `exception_level` tinyint NOT NULL COMMENT '1=一般 2=严重 3=紧急',
  `description` varchar(1000) COLLATE utf8mb4_unicode_ci NOT NULL,
  `current_status` tinyint DEFAULT '0' COMMENT '0=待处理 1=处理中 2=已解决 3=已升级',
  `handler_id` bigint DEFAULT NULL COMMENT '处理人',
  `handle_result` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `handle_time` datetime DEFAULT NULL,
  `escalated` tinyint DEFAULT '0',
  `escalate_time` datetime DEFAULT NULL,
  `exception_log_id` bigint DEFAULT NULL COMMENT '关联技术档案',
  `tenant_id` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT 'default',
  `deleted` int DEFAULT '0',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_no` (`exception_no`),
  KEY `idx_status` (`current_status`),
  KEY `idx_task` (`task_id`),
  KEY `idx_device` (`device_id`),
  KEY `idx_handler` (`handler_id`,`current_status`),
  KEY `idx_level` (`exception_level`),
  KEY `idx_created` (`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='异常工单表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `prod_handover_detail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `prod_handover_detail` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `task_id` bigint NOT NULL COMMENT '关联生产任务ID',
  `bag_count` int DEFAULT '0' COMMENT '袋数',
  `handover_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '交接类型',
  `handover_user` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '交接人',
  `handover_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '交接时间',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `tenant_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'default' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  KEY `task_id` (`task_id`),
  CONSTRAINT `prod_handover_detail_ibfk_1` FOREIGN KEY (`task_id`) REFERENCES `prod_task` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900000002 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='生产交接明细表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `prod_prescription`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `prod_prescription` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `hospital_id` bigint DEFAULT NULL COMMENT '医院标识',
  `prescription_number` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '处方号',
  `patient_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '患者姓名',
  `patient_phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '患者电话',
  `patient_type` bigint DEFAULT '0' COMMENT '患者类型',
  `outpatient_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '门诊号',
  `inpatient_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '住院号',
  `bed_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '床号',
  `disease` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '疾病',
  `doctor_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '医生名',
  `department` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '科室',
  `disease_area` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '病区',
  `medicine_list` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '药列表',
  `repetition` bigint DEFAULT '7' COMMENT '剂数（几副药）',
  `bags_per_repetition` bigint DEFAULT '1' COMMENT '备注',
  `bag_capacity` bigint DEFAULT '200' COMMENT '处方接收时间',
  `decocting_type` bigint DEFAULT '0' COMMENT '煎法',
  `usage_method` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用法',
  `scheme_id` bigint DEFAULT NULL,
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `receive_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '接收时间',
  `deleted` bigint DEFAULT '0',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `tenant_id` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT 'default',
  `receive_status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'PENDING' COMMENT '接收状态: PENDING待接收/RECEIVED已接收/REJECTED已驳回',
  `reject_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '驳回类型: REVIEW_REJECTED审方未过/DISPENSING_REJECTED调剂复核未过/OTHER其他',
  `reject_reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '驳回原因',
  `received_at` datetime DEFAULT NULL COMMENT '接收时间',
  `rejected_at` datetime DEFAULT NULL COMMENT '驳回时间',
  `task_no` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '关联工单号',
  `operator_id` bigint DEFAULT NULL COMMENT '操作人ID',
  `operator_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作人姓名',
  `import_exception` tinyint DEFAULT '0' COMMENT '导入异常标识: 0=正常 1=异常待处理',
  `exception_reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '异常原因（导入校验失败描述）',
  `raw_import_data` text COLLATE utf8mb4_unicode_ci COMMENT '原始导入数据（JSON格式，供人工审查纠正）',
  `decoction_plan` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '煎煮计划/工艺路线',
  `delivery_type` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '配送方式: SELF_PICKUP-自取/DELIVERY-配送/IN_HOUSE_DELIVERY-院内配送',
  `delivery_address` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '配送地址',
  `preparation_type` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '制剂类型: DECOCTION-汤剂/.../OTHER-其他',
  `source` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'MANUAL' COMMENT '处方来源: PUSH/PULL/MANUAL/EXCEL/OCR',
  PRIMARY KEY (`id`),
  KEY `scheme_id` (`scheme_id`),
  KEY `idx_receive_status` (`receive_status`),
  KEY `idx_task_no` (`task_no`),
  KEY `idx_import_exception` (`import_exception`),
  KEY `idx_exception_status` (`import_exception`,`receive_status`),
  CONSTRAINT `prod_prescription_ibfk_1` FOREIGN KEY (`scheme_id`) REFERENCES `md_decoct_scheme` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900000032 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='处方表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `prod_prescription_archive`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `prod_prescription_archive` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `hospital_id` bigint DEFAULT NULL,
  `prescription_number` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `patient_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `patient_type` bigint DEFAULT '0',
  `outpatient_no` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `inpatient_no` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `bed_no` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `disease` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `doctor_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `department` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `disease_area` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `medicine_list` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `repetition` bigint DEFAULT '1',
  `bags_per_repetition` bigint DEFAULT '1',
  `bag_capacity` bigint DEFAULT '200',
  `decocting_type` bigint DEFAULT '0',
  `usage_method` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `scheme_id` bigint DEFAULT NULL,
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `receive_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `tenant_id` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT 'default',
  `receive_status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'PENDING' COMMENT '接收状态: PENDING待接收/RECEIVED已接收/REJECTED已驳回',
  `reject_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '驳回类型: LACK_DRUG缺药/NOT_DECOCTION不煎药/OTHER其他',
  `reject_reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '驳回原因',
  `received_at` datetime DEFAULT NULL COMMENT '接收时间',
  `rejected_at` datetime DEFAULT NULL COMMENT '驳回时间',
  `task_no` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '关联工单号',
  `operator_id` bigint DEFAULT NULL COMMENT '操作人ID',
  `operator_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作人姓名',
  `archived_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `scheme_id` (`scheme_id`),
  KEY `idx_receive_status` (`receive_status`),
  KEY `idx_task_no` (`task_no`),
  KEY `idx_archived` (`archived_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='处方存档表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `prod_prescription_default`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `prod_prescription_default` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `setting_key` varchar(100) NOT NULL COMMENT '设置键',
  `setting_name` varchar(100) DEFAULT NULL COMMENT '设置名称',
  `setting_value` varchar(100) DEFAULT NULL COMMENT '设置值',
  `setting_type` varchar(20) DEFAULT 'INT' COMMENT '设置类型:INT/STRING/BOOLEAN/DECIMAL',
  `description` varchar(500) DEFAULT NULL COMMENT '说明',
  `tenant_id` varchar(32) DEFAULT 'default' COMMENT '租户ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` int DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `setting_key` (`setting_key`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='处方默认设置表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `prod_prescription_medicine`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `prod_prescription_medicine` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `prescription_id` bigint NOT NULL COMMENT '处方ID',
  `medicine_id` bigint DEFAULT NULL COMMENT '药材ID',
  `hospital_code` varchar(50) DEFAULT NULL COMMENT '医院自定义编码',
  `hospital_name` varchar(100) DEFAULT NULL COMMENT '医院自定义名称',
  `medicine_name` varchar(100) DEFAULT NULL COMMENT '药材名称',
  `dosage` decimal(10,2) DEFAULT NULL COMMENT '剂量',
  `unit` varchar(20) DEFAULT NULL COMMENT '单位',
  `med_usage` varchar(100) DEFAULT NULL COMMENT '用法',
  `decoction_method` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '煎法',
  `batch_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '批次号',
  `special_decoction` varchar(50) DEFAULT NULL COMMENT '特殊煎法',
  `is_toxic` tinyint(1) DEFAULT '0' COMMENT '是否毒性药材:0=否 1=是',
  `requires_retain` tinyint(1) DEFAULT '0' COMMENT '是否需留样:0=否 1=是',
  `retain_quantity` decimal(10,2) DEFAULT NULL COMMENT '留样量(ml)',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `tenant_id` varchar(32) DEFAULT 'default',
  `deleted` int DEFAULT '0' COMMENT '逻辑删除',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_prescription_id` (`prescription_id`)
) ENGINE=InnoDB AUTO_INCREMENT=900000178 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='处方药材明细表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `prod_prescription_status_backup_20260507`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `prod_prescription_status_backup_20260507` (
  `id` bigint NOT NULL DEFAULT '0',
  `receive_status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'PENDING' COMMENT '接收状态: PENDING待接收/RECEIVED已接收/REJECTED已驳回',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `prod_step_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `prod_step_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `task_id` bigint NOT NULL COMMENT '关联生产任务ID',
  `parent_id` bigint DEFAULT NULL COMMENT '父步骤ID',
  `step_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '步骤类型',
  `device_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '设备ID',
  `operator_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作人ID',
  `started_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '开始时间',
  `ended_at` timestamp NULL DEFAULT NULL COMMENT '结束时间',
  `is_paused` int DEFAULT '0' COMMENT '是否暂停（0-否，1-是）',
  `pause_reason` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '暂停原因',
  `pause_duration` int DEFAULT '0' COMMENT '暂停时长（分钟）',
  `delay_minutes` int DEFAULT '0' COMMENT '延迟时长（分钟）',
  `delay_reason` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '延迟原因',
  `result` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '正常' COMMENT '执行结果（默认：正常）',
  `abort_reason` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '中止原因',
  `waste_amount` decimal(10,2) DEFAULT NULL COMMENT '废弃物数量',
  `waste_unit` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '废弃物单位',
  `is_retry` int DEFAULT '0' COMMENT '是否重试（0-否，1-是）',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `tenant_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'default' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  KEY `task_id` (`task_id`),
  CONSTRAINT `prod_step_log_ibfk_1` FOREIGN KEY (`task_id`) REFERENCES `prod_task` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900000082 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='生产步骤日志表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `prod_task`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `prod_task` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `prescription_id` bigint NOT NULL,
  `decoct_device_id` bigint DEFAULT NULL,
  `package_device_id` bigint DEFAULT NULL,
  `scheme_id` bigint DEFAULT NULL,
  `scheme_snapshot` json DEFAULT NULL COMMENT '工艺参数快照',
  `decoction_plan` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `operator_id` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `operator_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '当前操作人姓名',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'PENDING',
  `suspended_from` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '挂起前状态',
  `suspend_reason` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '挂起原因',
  `suspend_time` datetime DEFAULT NULL COMMENT '挂起时间',
  `expected_resume_time` datetime DEFAULT NULL COMMENT '预计恢复时间',
  `current_temp` decimal(5,2) DEFAULT '0.00',
  `target_temp` decimal(5,2) DEFAULT '100.00',
  `soak_duration` bigint DEFAULT '30',
  `soak_start_time` datetime DEFAULT NULL,
  `soak_end_time` datetime DEFAULT NULL,
  `decoct_start_time` datetime DEFAULT NULL,
  `decoct_end_time` datetime DEFAULT NULL,
  `pour_start_time` datetime DEFAULT NULL,
  `pour_end_time` datetime DEFAULT NULL,
  `wrap_start_time` datetime DEFAULT NULL,
  `wrap_end_time` datetime DEFAULT NULL,
  `complete_time` datetime DEFAULT NULL,
  `current_stage_duration` bigint DEFAULT '0',
  `print_device_id` bigint DEFAULT NULL,
  `print_status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'PENDING',
  `print_time` datetime DEFAULT NULL,
  `deleted` bigint DEFAULT '0',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `current_step` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `pool_id` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `print_copies` int DEFAULT '1',
  `is_exception` int DEFAULT '0',
  `is_emergency` tinyint DEFAULT '0' COMMENT '是否紧急处方',
  `priority` tinyint DEFAULT '3' COMMENT '优先级: 1=最高 2=高 3=普通 4=低',
  `exception_reason` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `patient_agreement` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `standard_cost` decimal(10,2) DEFAULT NULL,
  `actual_cost` decimal(10,2) DEFAULT NULL,
  `handover_type` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `handover_user` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `handover_time` timestamp NULL DEFAULT NULL,
  `tenant_id` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT 'default',
  `status_enum` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `barcode` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '任务条形码/二维码',
  PRIMARY KEY (`id`),
  KEY `prescription_id` (`prescription_id`),
  KEY `scheme_id` (`scheme_id`),
  KEY `idx_task_barcode` (`barcode`),
  KEY `idx_emergency` (`is_emergency`,`priority`),
  KEY `idx_suspended` (`suspended_from`),
  KEY `idx_suspend_time` (`suspend_time`),
  KEY `prod_task_ibfk_2` (`decoct_device_id`),
  KEY `prod_task_ibfk_3` (`package_device_id`),
  CONSTRAINT `prod_task_ibfk_1` FOREIGN KEY (`prescription_id`) REFERENCES `prod_prescription` (`id`),
  CONSTRAINT `prod_task_ibfk_2` FOREIGN KEY (`decoct_device_id`) REFERENCES `eq_device` (`id`),
  CONSTRAINT `prod_task_ibfk_3` FOREIGN KEY (`package_device_id`) REFERENCES `eq_device` (`id`),
  CONSTRAINT `prod_task_ibfk_4` FOREIGN KEY (`scheme_id`) REFERENCES `md_decoct_scheme` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900000032 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `prod_task_archive`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `prod_task_archive` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `prescription_id` bigint NOT NULL,
  `decoct_device_id` bigint DEFAULT NULL,
  `package_device_id` bigint DEFAULT NULL,
  `scheme_id` bigint DEFAULT NULL,
  `operator_id` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `operator_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '当前操作人姓名',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'PENDING',
  `current_temp` decimal(5,2) DEFAULT '0.00',
  `target_temp` decimal(5,2) DEFAULT '100.00',
  `soak_duration` bigint DEFAULT '30',
  `soak_start_time` datetime DEFAULT NULL,
  `soak_end_time` datetime DEFAULT NULL,
  `decoct_start_time` datetime DEFAULT NULL,
  `decoct_end_time` datetime DEFAULT NULL,
  `pour_start_time` datetime DEFAULT NULL,
  `pour_end_time` datetime DEFAULT NULL,
  `wrap_start_time` datetime DEFAULT NULL,
  `wrap_end_time` datetime DEFAULT NULL,
  `complete_time` datetime DEFAULT NULL,
  `current_stage_duration` bigint DEFAULT '0',
  `print_device_id` bigint DEFAULT NULL,
  `print_status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'PENDING',
  `print_time` datetime DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `current_step` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `pool_id` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `print_copies` int DEFAULT '1',
  `is_exception` int DEFAULT '0',
  `is_emergency` tinyint DEFAULT '0' COMMENT '是否紧急处方',
  `priority` tinyint DEFAULT '3' COMMENT '优先级: 1=最高 2=高 3=普通 4=低',
  `exception_reason` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `patient_agreement` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `standard_cost` decimal(10,2) DEFAULT NULL,
  `actual_cost` decimal(10,2) DEFAULT NULL,
  `handover_type` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `handover_user` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `handover_time` timestamp NULL DEFAULT NULL,
  `tenant_id` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT 'default',
  `status_enum` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `barcode` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '任务条形码/二维码',
  `archived_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '归档时间',
  PRIMARY KEY (`id`),
  KEY `prescription_id` (`prescription_id`),
  KEY `decoct_device_id` (`decoct_device_id`),
  KEY `package_device_id` (`package_device_id`),
  KEY `scheme_id` (`scheme_id`),
  KEY `idx_task_barcode` (`barcode`),
  KEY `idx_emergency` (`is_emergency`,`priority`),
  KEY `idx_archived` (`archived_at`),
  KEY `idx_complete_time` (`complete_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务存档表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `prod_task_status_backup_20260507`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `prod_task_status_backup_20260507` (
  `id` bigint NOT NULL DEFAULT '0',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'PENDING',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `prod_task_status_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `prod_task_status_history` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `task_id` bigint NOT NULL,
  `from_status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `to_status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `operator_id` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `operate_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `tenant_id` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT 'default',
  `trigger_source` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `device_code` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `task_id` (`task_id`),
  CONSTRAINT `prod_task_status_history_ibfk_1` FOREIGN KEY (`task_id`) REFERENCES `prod_task` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900000082 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务状态历史表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `prod_work_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `prod_work_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `operator_id` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `operator_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `task_id` bigint NOT NULL,
  `action` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `work_time` bigint DEFAULT '0',
  `deleted` bigint DEFAULT '0',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `tenant_id` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT 'default',
  PRIMARY KEY (`id`),
  KEY `task_id` (`task_id`),
  CONSTRAINT `prod_work_record_ibfk_1` FOREIGN KEY (`task_id`) REFERENCES `prod_task` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900000012 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工作记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `prt_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `prt_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'default' COMMENT '租户ID',
  `print_task_id` bigint NOT NULL COMMENT '关联打印任务ID',
  `result` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '打印结果',
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '错误信息',
  `printed_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '打印时间',
  `deleted` bigint DEFAULT '0' COMMENT '逻辑删除标志（0-未删除，1-已删除）',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='打印记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `prt_task`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `prt_task` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'default' COMMENT '租户ID',
  `task_id` bigint NOT NULL COMMENT '关联生产任务ID',
  `device_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '打印设备编码',
  `operator_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作人ID',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'PENDING' COMMENT '任务状态（PENDING-待处理等）',
  `copies` bigint DEFAULT '1' COMMENT '打印份数（默认1）',
  `retry_count` bigint DEFAULT '0' COMMENT '已重试次数',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bigint DEFAULT '0' COMMENT '逻辑删除标志（0-未删除，1-已删除）',
  `max_retry` bigint DEFAULT '3' COMMENT '最大重试次数（默认3）',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='打印任务表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `qt_inspection`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qt_inspection` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'default' COMMENT '租户ID',
  `task_id` bigint NOT NULL COMMENT '关联生产任务ID',
  `result` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '检验结果',
  `operator_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '检验人ID',
  `remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '备注',
  `inspected_at` datetime DEFAULT NULL COMMENT '检验时间',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bigint DEFAULT '0' COMMENT '逻辑删除标志（0-未删除，1-已删除）',
  `is_exception` bigint DEFAULT '0' COMMENT '是否有异常（0-否，1-是）',
  `exception_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '异常原因',
  PRIMARY KEY (`id`),
  KEY `idx_qi_task_id` (`task_id`),
  KEY `idx_qi_result` (`result`),
  KEY `idx_qi_created` (`created_at`),
  KEY `idx_qi_inspected_at` (`inspected_at`),
  KEY `idx_qi_result_inspected` (`result`,`inspected_at`)
) ENGINE=InnoDB AUTO_INCREMENT=900000002 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='质检检验表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `qt_inspection_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qt_inspection_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `inspection_id` bigint NOT NULL COMMENT '质检记录ID',
  `item_code` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '检查项编码：APPEARANCE/ODOR/DOSE/SEAL/LABEL/SAMPLE',
  `item_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '检查项名称',
  `result` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'PASS/FAIL/NA',
  `actual_value` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '实际值（如剂量ml）',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_inspection_id` (`inspection_id`),
  KEY `idx_qii_inspection_id` (`inspection_id`),
  KEY `idx_qii_item_code` (`item_code`)
) ENGINE=InnoDB AUTO_INCREMENT=900000005 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='质检检查项明细';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `qt_retain_sample`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qt_retain_sample` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `task_id` bigint NOT NULL COMMENT '任务ID',
  `prescription_id` bigint NOT NULL COMMENT '处方ID',
  `sample_no` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '留样编号',
  `sample_type` tinyint NOT NULL COMMENT '7天留样固定为4',
  `retain_duration` int DEFAULT NULL COMMENT '留样时长(小时)',
  `retain_time` datetime NOT NULL COMMENT '留样时间',
  `expire_time` datetime DEFAULT NULL COMMENT '销毁时间',
  `status` tinyint DEFAULT '1' COMMENT '1=留样中 2=已复检 3=可销毁 4=已销毁',
  `destroy_time` datetime DEFAULT NULL COMMENT '销毁时间',
  `destroy_by` bigint DEFAULT NULL COMMENT '销毁人ID',
  `operator_id` bigint NOT NULL COMMENT '留样操作人',
  `remark` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `tenant_id` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT 'default',
  `deleted` int DEFAULT '0',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_qrs_sample_no` (`sample_no`),
  KEY `idx_task` (`task_id`),
  KEY `idx_prescription` (`prescription_id`),
  KEY `idx_expire` (`expire_time`),
  KEY `idx_status` (`status`),
  KEY `idx_sample_no` (`sample_no`),
  KEY `idx_qrs_task_id` (`task_id`),
  KEY `idx_qrs_status` (`status`),
  KEY `idx_qrs_expire` (`expire_time`,`status`),
  KEY `idx_qrs_sample_no` (`sample_no`)
) ENGINE=InnoDB AUTO_INCREMENT=900000002 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='留样记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `shedlock`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `shedlock` (
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '锁名称（主键）',
  `lock_until` timestamp(3) NOT NULL COMMENT '锁定截止时间',
  `locked_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '锁定时间',
  `locked_by` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '锁定方标识',
  PRIMARY KEY (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='分布式锁表（ShedLock定时任务锁）';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT 'default',
  `config_key` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `config_value` text COLLATE utf8mb4_unicode_ci,
  `description` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `deleted` bigint DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `config_key` (`config_key`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_exc_threshold`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_exc_threshold` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `exc_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `param_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `param_value` bigint NOT NULL,
  `unit` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  `is_active` bigint DEFAULT '1',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT 'default',
  `user_id` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `action` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `module` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `result` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作结果: SUCCESS/FAILED',
  `target_id` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作对象ID',
  `target_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作对象名称',
  `detail` text COLLATE utf8mb4_unicode_ci,
  `ip_address` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=38901 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='日志表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_log_archive`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_log_archive` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT 'default',
  `user_id` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `action` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `module` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `detail` text COLLATE utf8mb4_unicode_ci,
  `ip_address` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='日志存档表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_menu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_menu` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'default',
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '页面功能描述，用于标题区副标题展示',
  `code` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `path` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `component` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `icon` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `sort_order` bigint DEFAULT '0',
  `menu_type` bigint DEFAULT '1',
  `parent_id` bigint DEFAULT '0',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'ACTIVE',
  `permission` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `deleted` bigint DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `code` (`code`),
  KEY `idx_sys_menu_parent` (`parent_id`),
  KEY `idx_sys_menu_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=85 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜单权限表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_migration`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_migration` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `script` varchar(100) NOT NULL,
  `executed_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `script` (`script`)
) ENGINE=InnoDB AUTO_INCREMENT=72 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='数据库升级sql';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'default',
  `role_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `role_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `sort_order` bigint DEFAULT '0',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'ACTIVE',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `deleted` bigint DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `role_code` (`role_code`),
  KEY `idx_sys_role_code` (`role_code`),
  KEY `idx_sys_role_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_role_menu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role_menu` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `role_id` bigint NOT NULL,
  `menu_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `role_id` (`role_id`,`menu_id`),
  KEY `idx_sys_role_menu_role` (`role_id`),
  KEY `idx_sys_role_menu_menu` (`menu_id`)
) ENGINE=InnoDB AUTO_INCREMENT=191 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色菜单关联表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_signature`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_signature` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `biz_type` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '业务类型: QC_INSPECT/HANDOVER/APPROVE_ROLLBACK/EXCEPTION_HANDLE',
  `biz_id` bigint NOT NULL COMMENT '业务单据ID',
  `signer_id` bigint NOT NULL COMMENT '签名人ID',
  `signer_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '签名人姓名',
  `sign_image_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '签名图片URL',
  `sign_hash` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '签名内容哈希(防篡改)',
  `sign_time` datetime NOT NULL COMMENT '签名时间',
  `sign_device` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '签名设备: PDA/WEB',
  `tenant_id` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT 'default',
  `deleted` int DEFAULT '0',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_biz` (`biz_type`,`biz_id`),
  KEY `idx_signer` (`signer_id`,`sign_time`),
  KEY `idx_time` (`sign_time`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='电子签名表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT 'default',
  `username` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `password` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `real_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `barcode` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '员工扫码条码',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'ACTIVE',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `deleted` bigint DEFAULT '0',
  `role` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`),
  KEY `idx_sys_user_barcode` (`barcode`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_user_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user_role` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `role_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `user_id` (`user_id`,`role_id`),
  KEY `idx_sys_user_role_user` (`user_id`),
  KEY `idx_sys_user_role_role` (`role_id`)
) ENGINE=InnoDB AUTO_INCREMENT=37 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `prod_delivery_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `prod_delivery_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `task_id` bigint NOT NULL COMMENT '关联工单ID',
  `prescription_no` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '处方号',
  `patient_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '患者姓名',
  `delivery_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '交付类型: SELF自取/EXPRESS快递/DELIVERY配送',
  `receiver_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '接收人',
  `receiver_phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '接收电话',
  `receiver_address` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '接收地址',
  `courier_company` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '快递公司',
  `courier_no` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '快递单号',
  `bag_count` int DEFAULT '0' COMMENT '袋数',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'PENDING' COMMENT '状态: PENDING待交付/DELIVERED已交付',
  `operator_id` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作人',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `delivered_at` datetime DEFAULT NULL COMMENT '交付时间',
  `tenant_id` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'default' COMMENT '租户ID',
  `deleted` bigint DEFAULT '0' COMMENT '逻辑删除',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_status` (`status`),
  KEY `idx_prescription_no` (`prescription_no`)
) ENGINE=InnoDB AUTO_INCREMENT=900000002 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='交付记录';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `base_department`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `base_department` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `dept_code` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '科室编码',
  `dept_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '科室名称',
  `hospital_id` bigint DEFAULT NULL COMMENT '所属医院ID',
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '科室描述',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `status` tinyint DEFAULT '1' COMMENT '状态：0禁用 1启用',
  `tenant_id` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'default' COMMENT '租户ID',
  `deleted` bigint DEFAULT '0' COMMENT '逻辑删除',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `dept_code` (`dept_code`),
  KEY `idx_hospital_id` (`hospital_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='科室管理';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `base_doctor`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `base_doctor` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `doctor_code` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '医师编码',
  `doctor_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '医师姓名',
  `title` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '职称',
  `department_id` bigint DEFAULT NULL COMMENT '所属科室ID',
  `hospital_id` bigint DEFAULT NULL COMMENT '所属医院ID',
  `phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '联系电话',
  `status` tinyint DEFAULT '1' COMMENT '状态：0禁用 1启用',
  `tenant_id` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'default' COMMENT '租户ID',
  `deleted` bigint DEFAULT '0' COMMENT '逻辑删除',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `doctor_code` (`doctor_code`),
  KEY `idx_department_id` (`department_id`),
  KEY `idx_hospital_id` (`hospital_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='医师管理';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_interface_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_interface_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `interface_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '接口编码',
  `interface_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '接口名称',
  `interface_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'HIS/DEVICE/THIRD_PARTY',
  `protocol` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '协议：REST/WEBSERVICE/HL7/MQTT/MODBUS/TCP',
  `base_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '基础URL/地址',
  `auth_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '认证方式：NONE/BASIC/TOKEN/OAUTH2',
  `auth_config` text COLLATE utf8mb4_unicode_ci COMMENT '认证配置JSON',
  `status` tinyint DEFAULT '1' COMMENT '0禁用 1启用',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `tenant_id` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT 'default',
  `deleted` int DEFAULT '0',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `interface_code` (`interface_code`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='接口配置';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_interface_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_interface_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `interface_id` bigint NOT NULL COMMENT '接口配置ID',
  `interface_code` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '接口编码',
  `direction` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'REQUEST/RESPONSE',
  `method` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'HTTP方法/操作类型',
  `url` text COLLATE utf8mb4_unicode_ci COMMENT '请求URL',
  `request_body` text COLLATE utf8mb4_unicode_ci COMMENT '请求内容',
  `response_body` text COLLATE utf8mb4_unicode_ci COMMENT '响应内容',
  `status_code` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '状态码',
  `result` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'SUCCESS/FAIL/TIMEOUT',
  `duration_ms` int DEFAULT NULL COMMENT '耗时毫秒',
  `error_msg` text COLLATE utf8mb4_unicode_ci COMMENT '错误信息',
  `tenant_id` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT 'default',
  `deleted` int DEFAULT '0',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_interface_id` (`interface_id`),
  KEY `idx_result` (`result`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='接口调用日志';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `prt_label_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `prt_label_template` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `template_code` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '模板编码',
  `template_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '模板名称',
  `template_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '模板类型: SOAK泡药/DECOCT煎煮/PACKAGE包装/DELIVER交付',
  `width_mm` int DEFAULT NULL COMMENT '标签宽度(mm)',
  `height_mm` int DEFAULT NULL COMMENT '标签高度(mm)',
  `hospital_color` varchar(16) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '关联院区颜色',
  `content` text COLLATE utf8mb4_unicode_ci COMMENT '模板内容(JSON格式字段配置)',
  `preview_image` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '预览图URL',
  `status` tinyint DEFAULT '1' COMMENT '状态：0禁用 1启用',
  `tenant_id` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'default' COMMENT '租户ID',
  `deleted` bigint DEFAULT '0' COMMENT '逻辑删除',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `template_code` (`template_code`),
  KEY `idx_type` (`template_type`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='标签模板';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `base_medicine`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `base_medicine` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `medicine_code` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '药材编码',
  `medicine_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '药材名称',
  `aliases` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '别名，逗号分隔',
  `his_code` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'HIS系统编码',
  `national_code` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '国标编码',
  `spec` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '规格',
  `unit` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '单位',
  `stock_warning` decimal(10,2) DEFAULT NULL COMMENT '库存预警值',
  `status` tinyint DEFAULT '1' COMMENT '状态：0禁用 1启用',
  `tenant_id` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'default' COMMENT '租户ID',
  `deleted` bigint DEFAULT '0' COMMENT '逻辑删除',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `medicine_code` (`medicine_code`),
  KEY `idx_name` (`medicine_name`),
  KEY `idx_his_code` (`his_code`),
  KEY `idx_national_code` (`national_code`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='药材目录';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `md_package_spec`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `md_package_spec` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `spec_code` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '规格编码',
  `spec_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '规格名称',
  `volume_ml` int NOT NULL COMMENT '单袋容量(ml)',
  `bag_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '袋型：普通/真空/铝箔',
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '描述',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `status` tinyint DEFAULT '1' COMMENT '状态：0禁用 1启用',
  `tenant_id` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'default' COMMENT '租户ID',
  `deleted` bigint DEFAULT '0' COMMENT '逻辑删除',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `spec_code` (`spec_code`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='包装规格';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `time_check_rule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `time_check_rule` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `rule_code` varchar(50) NOT NULL COMMENT '规则编码',
  `rule_name` varchar(100) DEFAULT NULL COMMENT '规则名称',
  `from_step` varchar(50) DEFAULT NULL COMMENT '起始步骤编码',
  `to_step` varchar(50) DEFAULT NULL COMMENT '目标步骤编码',
  `min_duration` int DEFAULT NULL COMMENT '最小间隔（秒），-1表示不限制',
  `max_duration` int DEFAULT NULL COMMENT '最大间隔（秒），-1表示不限制',
  `check_type` varchar(20) DEFAULT 'BLOCK' COMMENT '校验类型:BLOCK/WARN/PASS',
  `warning_message` varchar(200) DEFAULT NULL COMMENT '警告提示文案',
  `block_message` varchar(200) DEFAULT NULL COMMENT '阻断提示文案',
  `enabled` tinyint(1) DEFAULT '1' COMMENT '是否启用',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `tenant_id` varchar(32) DEFAULT 'default' COMMENT '租户ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` int DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `rule_code` (`rule_code`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='时间校验规则表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `water_formula`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `water_formula` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `formula_code` varchar(50) NOT NULL COMMENT '公式编码',
  `formula_name` varchar(100) DEFAULT NULL COMMENT '公式名称',
  `expression` varchar(500) DEFAULT NULL COMMENT '公式表达式',
  `expression_desc` varchar(500) DEFAULT NULL COMMENT '公式说明',
  `variables` varchar(500) DEFAULT NULL COMMENT '变量说明JSON',
  `is_default` tinyint(1) DEFAULT '0' COMMENT '是否默认',
  `enabled` tinyint(1) DEFAULT '1' COMMENT '是否启用',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `tenant_id` varchar(32) DEFAULT 'default' COMMENT '租户ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` int DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `formula_code` (`formula_code`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='加水量公式表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `workload_stat`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `workload_stat` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `stat_date` date NOT NULL COMMENT '统计日期',
  `operator_id` bigint DEFAULT NULL COMMENT '操作人ID',
  `operator_name` varchar(50) DEFAULT NULL COMMENT '操作人姓名',
  `work_type` varchar(20) DEFAULT NULL COMMENT '工作类型:DISPENSE/REVIEW/SOAK/DECOCT/PACKAGE/DELIVER',
  `task_count` int DEFAULT '0' COMMENT '任务数',
  `prescription_count` int DEFAULT '0' COMMENT '处方数',
  `package_count` int DEFAULT '0' COMMENT '包装数',
  `duration_minutes` int DEFAULT '0' COMMENT '工作时长(分钟)',
  `efficiency` decimal(5,2) DEFAULT NULL COMMENT '效率(处方数/小时)',
  `tenant_id` varchar(32) DEFAULT 'default' COMMENT '租户ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` int DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900000061 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工作量统计表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
SET FOREIGN_KEY_CHECKS = 1;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

