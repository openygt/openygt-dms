-- ============================================================
-- openygt-dms Demo Data Rebuild v2
-- 基于用户给定的自洽演示数据规格 (2026-05-12)
-- 确保所有页面数据一致、可演示
-- ============================================================

SET @demo_date = '2026-05-12';

-- ============================================================
-- 1. 处方数据 (prod_prescription) — 8条普通 + 1条异常
-- ============================================================

-- 删除多余的处方 11 (沈建雷的重复异常)
UPDATE prod_prescription SET deleted = 1 WHERE id = 11;

-- 更新8条普通处方
UPDATE prod_prescription SET
  patient_name = '郑晓薇',
  hospital_id = NULL,
  doctor_name = '张医生',
  department = '中医内科',
  receive_status = 'RECEIVED',
  repetition = 7,
  created_at = '2026-05-12 06:06:00',
  updated_at = NOW(),
  source = 'MANUAL'
WHERE id = 1;

UPDATE prod_prescription SET
  patient_name = '朱秀芳',
  hospital_id = NULL,
  doctor_name = '李医生',
  department = '中医内科',
  receive_status = 'RECEIVED',
  repetition = 7,
  created_at = '2026-05-12 07:06:00',
  updated_at = NOW()
WHERE id = 2;

UPDATE prod_prescription SET
  patient_name = '孙文博',
  hospital_id = NULL,
  doctor_name = '王医生',
  department = '社区全科',
  receive_status = 'RECEIVED',
  repetition = 14,
  created_at = '2026-05-12 07:06:00',
  updated_at = NOW()
WHERE id = 3;

UPDATE prod_prescription SET
  patient_name = '冯德明',
  hospital_id = NULL,
  doctor_name = '赵医生',
  department = '中医内科',
  receive_status = 'RECEIVED',
  repetition = 7,
  created_at = '2026-05-12 07:06:00',
  updated_at = NOW()
WHERE id = 4;

UPDATE prod_prescription SET
  patient_name = '刘桂云',
  hospital_id = NULL,
  doctor_name = '张医生',
  department = '中医内科',
  receive_status = 'RECEIVED',
  repetition = 7,
  created_at = '2026-05-12 08:06:00',
  updated_at = NOW()
WHERE id = 5;

UPDATE prod_prescription SET
  patient_name = '赵大伟',
  hospital_id = NULL,
  doctor_name = '王医生',
  department = '社区全科',
  receive_status = 'RECEIVED',
  repetition = 7,
  created_at = '2026-05-12 09:06:00',
  updated_at = NOW()
WHERE id = 6;

UPDATE prod_prescription SET
  patient_name = '陶琴',
  hospital_id = NULL,
  doctor_name = '王医生',
  department = '社区全科',
  receive_status = 'RECEIVED',
  repetition = 7,
  created_at = '2026-05-12 11:06:00',
  updated_at = NOW()
WHERE id = 7;

UPDATE prod_prescription SET
  patient_name = '孙文博',
  hospital_id = NULL,
  doctor_name = '王医生',
  department = '社区全科',
  receive_status = 'RECEIVED',
  repetition = 14,
  created_at = '2026-05-12 12:06:00',
  updated_at = NOW()
WHERE id = 8;

-- 异常处方 RX20260512010 — 缺少用法用量
UPDATE prod_prescription SET
  patient_name = '沈建雷',
  receive_status = 'RECEIVED',
  import_exception = 1,
  exception_reason = '缺少用法用量',
  source = 'INTERFACE',
  repetition = 14,
  created_at = '2026-05-12 13:06:00',
  updated_at = NOW()
WHERE id = 10;

-- ============================================================
-- 2. 任务数据 (prod_task) — 8条
-- ============================================================

-- 确保任务ID 1 存在（如果没有则插入）
INSERT INTO prod_task (id, prescription_id, decoct_device_id, package_device_id, status, operator_id, operator_name, soak_duration, target_temp, is_exception, is_emergency, priority, created_at, updated_at, deleted)
SELECT 1, 1, 1, NULL, 'COMPLETED', '1', '系统管理员', 30, 100, 0, 0, 3, '2026-05-12 06:06:00', NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM prod_task WHERE id = 1);

-- 若已存在则更新
UPDATE prod_task SET
  prescription_id = 1,
  status = 'COMPLETED',
  operator_id = '1',
  operator_name = '系统管理员',
  complete_time = '2026-05-12 10:30:00',
  created_at = '2026-05-12 06:06:00',
  is_exception = 0,
  updated_at = NOW()
WHERE id = 1;

UPDATE prod_task SET
  prescription_id = 2,
  status = 'COMPLETED',
  operator_id = '1',
  operator_name = '系统管理员',
  complete_time = '2026-05-12 14:00:00',
  created_at = '2026-05-12 07:06:00',
  is_exception = 0,
  is_emergency = 1,
  priority = 1,
  updated_at = NOW()
WHERE id = 2;

UPDATE prod_task SET
  prescription_id = 3,
  status = 'WAIT_SOAK',
  operator_id = '2',
  operator_name = '张建国',
  created_at = '2026-05-12 07:06:00',
  is_exception = 0,
  updated_at = NOW()
WHERE id = 3;

UPDATE prod_task SET
  prescription_id = 4,
  status = 'DECOCTING',
  operator_id = '1',
  operator_name = '系统管理员',
  decoct_start_time = '2026-05-12 09:00:00',
  created_at = '2026-05-12 07:06:00',
  is_exception = 0,
  updated_at = NOW()
WHERE id = 4;

UPDATE prod_task SET
  prescription_id = 5,
  status = 'WAIT_POUR',
  operator_id = '2',
  operator_name = '张建国',
  pour_start_time = NULL,
  created_at = '2026-05-12 08:06:00',
  is_exception = 0,
  updated_at = NOW()
WHERE id = 5;

UPDATE prod_task SET
  prescription_id = 6,
  status = 'WAIT_SOAK',
  operator_id = '1',
  operator_name = '系统管理员',
  created_at = '2026-05-12 09:06:00',
  is_exception = 0,
  updated_at = NOW()
WHERE id = 6;

UPDATE prod_task SET
  prescription_id = 7,
  status = 'DECOCTING',
  operator_id = '2',
  operator_name = '张建国',
  decoct_start_time = '2026-05-12 12:00:00',
  created_at = '2026-05-12 11:06:00',
  is_exception = 0,
  updated_at = NOW()
WHERE id = 7;

UPDATE prod_task SET
  prescription_id = 8,
  status = 'WAIT_SOAK',
  operator_id = '1',
  operator_name = '系统管理员',
  created_at = '2026-05-12 12:06:00',
  is_exception = 0,
  updated_at = NOW()
WHERE id = 8;

-- ============================================================
-- 3. 设备数据 (eq_device) — 只更新用户提到的6台关键设备
-- ============================================================

UPDATE eq_device SET status = 'ONLINE', detail_status = 'RUNNING', last_heartbeat = NOW()
WHERE device_code = 'DECOCT_01';

UPDATE eq_device SET status = 'ONLINE', detail_status = 'RUNNING', last_heartbeat = NOW()
WHERE device_code = 'DECOCT_02';

UPDATE eq_device SET status = 'OFFLINE', detail_status = 'OFFLINE'
WHERE device_code = 'DECOCT_03';

UPDATE eq_device SET status = 'MAINTENANCE', detail_status = 'MAINTENANCE'
WHERE device_code = 'DECOCT_04';

UPDATE eq_device SET status = 'ONLINE', detail_status = 'IDLE'
WHERE device_code = 'LABEL_01';

UPDATE eq_device SET status = 'ONLINE', detail_status = 'IDLE'
WHERE device_code = 'PACK_01';

-- 其他设备设为在线空闲
UPDATE eq_device SET status = 'ONLINE', detail_status = 'IDLE'
WHERE device_code IN ('DECOCT_05', 'DECOCT_06', 'DECOCT_07', 'DECOCT_08',
  'PACK_02', 'PACK_03', 'LABEL_02', 'LABEL_03', 'LASER_01', 'PDA_01', 'PDA_02');

-- ============================================================
-- 4. 设备告警 — PACK_01 有心跳超时告警
-- ============================================================

-- 先清除旧告警
DELETE FROM eq_device_alarm WHERE device_id = (SELECT id FROM eq_device WHERE device_code = 'PACK_01');

INSERT INTO eq_device_alarm (device_id, alarm_type, message, is_resolved, created_at, updated_at, deleted, tenant_id)
SELECT id, 'HEARTBEAT_TIMEOUT', '设备心跳超时（超过5分钟未上报）', 0, '2026-05-12 14:30:00', NOW(), 0, 'default'
FROM eq_device WHERE device_code = 'PACK_01';

-- ============================================================
-- 5. 任务分配 (prod_task_assignment) — 重建
-- ============================================================

-- 清除旧分配记录
DELETE FROM prod_task_assignment;

-- 任务分配：按用户指定的时间段和设备/员工
-- 时间段映射：任务创建时间 → 分配开始/结束时间

INSERT INTO prod_task_assignment (task_id, prescription_id, device_id, employee_id, assign_type, assign_reason,
  scheduled_start_time, scheduled_end_time, actual_start_time, actual_end_time, status, created_at, updated_at)
VALUES
-- 任务1 (RX001) → 煎药机1-1, 系统管理员(1), 0:00-1:00 时段, 已完成
(1, 1, 1, 1, 1, '自动分配',
 '2026-05-12 00:00:00', '2026-05-12 01:00:00',
 '2026-05-12 00:00:00', '2026-05-12 01:00:00', 2, '2026-05-12 06:06:00', NOW()),

-- 任务2 (RX002) → 煎药机1-2, 系统管理员(1), 1:00-2:00 时段, 已完成
(2, 2, 2, 1, 1, '自动分配',
 '2026-05-12 01:00:00', '2026-05-12 02:00:00',
 '2026-05-12 01:00:00', '2026-05-12 02:00:00', 2, '2026-05-12 07:06:00', NOW()),

-- 任务3 (RX003) → 煎药机1-1, 张建国(2), 2:00-3:00, 待执行
(3, 3, 1, 2, 1, '自动分配',
 '2026-05-12 02:00:00', '2026-05-12 03:00:00',
 NULL, NULL, 0, '2026-05-12 07:06:00', NOW()),

-- 任务4 (RX004) → 煎药机1-2, 系统管理员(1), 3:00-4:00, 执行中
(4, 4, 2, 1, 1, '自动分配',
 '2026-05-12 03:00:00', '2026-05-12 04:00:00',
 '2026-05-12 03:00:00', NULL, 1, '2026-05-12 07:06:00', NOW()),

-- 任务5 (RX005) → 煎药机1-3, 张建国(2), 4:00-5:00, 待执行
(5, 5, 3, 2, 1, '自动分配',
 '2026-05-12 04:00:00', '2026-05-12 05:00:00',
 NULL, NULL, 0, '2026-05-12 08:06:00', NOW()),

-- 任务6 (RX006) → 煎药机1-2, 系统管理员(1), 5:00-6:00, 待执行
(6, 6, 2, 1, 1, '自动分配',
 '2026-05-12 05:00:00', '2026-05-12 06:00:00',
 NULL, NULL, 0, '2026-05-12 09:06:00', NOW()),

-- 任务7 (RX007) → 煎药机1-1, 张建国(2), 无实际开始, 执行中
(7, 7, 1, 2, 1, '自动分配',
 '2026-05-12 06:00:00', '2026-05-12 08:00:00',
 '2026-05-12 06:00:00', NULL, 1, '2026-05-12 11:06:00', NOW()),

-- 任务8 (RX008) → 煎药机1-2, 系统管理员(1), 待执行
(8, 8, 2, 1, 1, '自动分配',
 '2026-05-12 08:00:00', '2026-05-12 10:00:00',
 NULL, NULL, 0, '2026-05-12 12:06:00', NOW());

-- ============================================================
-- 6. 紧急处方 (prod_emergency_prescription) — RX20260512002
-- ============================================================

DELETE FROM prod_emergency_prescription;

INSERT INTO prod_emergency_prescription (prescription_id, emergency_level, request_time,
  promised_finish_time, actual_finish_time, is_on_time, delay_reason,
  delivery_type, delivery_location, created_at, updated_at)
VALUES (2, 3, '2026-05-12 07:30:00',
  '2026-05-12 11:00:00', '2026-05-12 14:00:00', 0,
  '药材准备延迟，浸泡时间不足',
  'IN_HOUSE_DELIVERY', '住院部5楼', '2026-05-12 07:30:00', NOW());

-- ============================================================
-- 7. 异常工单 (prod_exception_log + prod_exception_order)
-- ============================================================

DELETE FROM prod_exception_order;
DELETE FROM prod_exception_log;

-- 异常1: RX20260512010 — 缺少用法用量
INSERT INTO prod_exception_log (exception_no, task_id, prescription_no, patient_name,
  exception_type, exception_level, source_module, current_step, description,
  discover_channel, handle_status, created_at, created_by, is_deleted)
VALUES ('EXC-20260512-010', 10, 'RX20260512010', '沈建雷',
  'MISSING_USAGE', 'MEDIUM', 'PRESCRIPTION', '审方',
  '缺少用法用量：处方未填写用药方法和用量说明',
  'AUTO_SCAN', 'PENDING', '2026-05-12 13:06:00', 'admin', 0);

INSERT INTO prod_exception_order (exception_no, task_id, exception_type, exception_level,
  description, current_status, exception_log_id, tenant_id, deleted, created_at, updated_at)
VALUES ('EXC-20260512-010', 10, 1, 2,
  '缺少用法用量：处方未填写用药方法和用量说明',
  0, 1, 'default', 0, '2026-05-12 13:06:00', '2026-05-12 13:06:00');

-- 异常2: 药材缺货（保留演示第二个异常类型）
INSERT INTO prod_exception_log (exception_no, task_id, prescription_no, patient_name,
  exception_type, exception_level, source_module, current_step, description,
  discover_channel, handle_status, created_at, created_by, is_deleted)
VALUES ('EXC-20260512-011', 11, 'RX20260512011', '周丽华',
  'HERB_SHORTAGE', 'HIGH', 'PRESCRIPTION', '审方',
  '药材缺货：附子库存不足，无法按处方配药',
  'AUTO_SCAN', 'PENDING', '2026-05-12 14:06:00', 'admin', 0);

INSERT INTO prod_exception_order (exception_no, task_id, exception_type, exception_level,
  description, current_status, exception_log_id, tenant_id, deleted, created_at, updated_at)
VALUES ('EXC-20260512-011', 11, 3, 3,
  '药材缺货：附子库存不足，无法按处方配药',
  0, 2, 'default', 0, '2026-05-12 14:06:00', '2026-05-12 14:06:00');

-- ============================================================
-- 8. 返工处理记录 — 如果 prod_rework 表不存在则跳过
-- ============================================================

-- 检查返工表是否存在
-- (如果不存在，返工页面将显示空数据或无数据，这是预期行为)

-- ============================================================
-- 9. 产能统计 — 设备利用率 (device_utilization)
-- ============================================================

DELETE FROM device_utilization WHERE stat_date = @demo_date;

-- 煎药机1-1: 3个任务, 60% 利用率
INSERT INTO device_utilization (device_code, stat_date, total_minutes, run_minutes, idle_minutes,
  fault_minutes, offline_minutes, maintenance_minutes, utilization_rate, availability_rate,
  fault_count, task_count, tenant_id, created_at, updated_at, deleted)
VALUES ('DECOCT_01', @demo_date, 1440, 864, 576, 0, 0, 0, 60.00, 100.00, 0, 3, 'default', NOW(), NOW(), 0);

-- 煎药机1-2: 4个任务, 80% 利用率
INSERT INTO device_utilization (device_code, stat_date, total_minutes, run_minutes, idle_minutes,
  fault_minutes, offline_minutes, maintenance_minutes, utilization_rate, availability_rate,
  fault_count, task_count, tenant_id, created_at, updated_at, deleted)
VALUES ('DECOCT_02', @demo_date, 1440, 1152, 288, 0, 0, 0, 80.00, 100.00, 0, 4, 'default', NOW(), NOW(), 0);

-- 煎药机1-3: 2个任务, 40% 利用率 (offline)
INSERT INTO device_utilization (device_code, stat_date, total_minutes, run_minutes, idle_minutes,
  fault_minutes, offline_minutes, maintenance_minutes, utilization_rate, availability_rate,
  fault_count, task_count, tenant_id, created_at, updated_at, deleted)
VALUES ('DECOCT_03', @demo_date, 1440, 576, 0, 0, 864, 0, 40.00, 40.00, 0, 2, 'default', NOW(), NOW(), 0);

-- 煎药机1-4: 0个任务, 0% 利用率 (maintenance)
INSERT INTO device_utilization (device_code, stat_date, total_minutes, run_minutes, idle_minutes,
  fault_minutes, offline_minutes, maintenance_minutes, utilization_rate, availability_rate,
  fault_count, task_count, tenant_id, created_at, updated_at, deleted)
VALUES ('DECOCT_04', @demo_date, 1440, 0, 0, 0, 0, 1440, 0.00, 0.00, 0, 0, 'default', NOW(), NOW(), 0);

-- PACK_01: 2个任务, 50% 利用率
INSERT INTO device_utilization (device_code, stat_date, total_minutes, run_minutes, idle_minutes,
  fault_minutes, offline_minutes, maintenance_minutes, utilization_rate, availability_rate,
  fault_count, task_count, tenant_id, created_at, updated_at, deleted)
VALUES ('PACK_01', @demo_date, 1440, 720, 720, 0, 0, 0, 50.00, 100.00, 0, 2, 'default', NOW(), NOW(), 0);

-- LABEL_01: 0个任务, 0% 利用率
INSERT INTO device_utilization (device_code, stat_date, total_minutes, run_minutes, idle_minutes,
  fault_minutes, offline_minutes, maintenance_minutes, utilization_rate, availability_rate,
  fault_count, task_count, tenant_id, created_at, updated_at, deleted)
VALUES ('LABEL_01', @demo_date, 1440, 0, 1440, 0, 0, 0, 0.00, 100.00, 0, 0, 'default', NOW(), NOW(), 0);

-- ============================================================
-- 10. 工作量统计 (ops_workload_stat)
-- ============================================================

DELETE FROM ops_workload_stat;

-- 张三 (id=1, 系统管理员): 7条工作类型记录
INSERT INTO ops_workload_stat (stat_date, operator_id, operator_name, work_type, task_count, prescription_count, package_count, duration_minutes, efficiency, tenant_id, created_at, updated_at, deleted)
VALUES
(@demo_date, 1, '系统管理员', 'DISPENSE', 2, 3, 7, 140, 1.28, 'default', NOW(), NOW(), 0),
(@demo_date, 1, '系统管理员', 'REVIEW', 2, 3, 7, 90, 2.00, 'default', NOW(), NOW(), 0),
(@demo_date, 1, '系统管理员', 'SOAK', 2, 3, 7, 110, 1.64, 'default', NOW(), NOW(), 0),
(@demo_date, 1, '系统管理员', 'DECOCT', 3, 4, 14, 180, 1.33, 'default', NOW(), NOW(), 0),
(@demo_date, 1, '系统管理员', 'QC', 3, 4, 14, 80, 3.00, 'default', NOW(), NOW(), 0),
(@demo_date, 1, '系统管理员', 'PACKAGE', 3, 4, 14, 60, 4.00, 'default', NOW(), NOW(), 0),
(@demo_date, 1, '系统管理员', 'DELIVER', 3, 4, 14, 80, 3.00, 'default', NOW(), NOW(), 0);

-- 李四 (id=3, 李秀芳): 1条记录 (历史数据 2026-05-11)
INSERT INTO ops_workload_stat (stat_date, operator_id, operator_name, work_type, task_count, prescription_count, package_count, duration_minutes, efficiency, tenant_id, created_at, updated_at, deleted)
VALUES
('2026-05-11', 3, '李秀芳', 'DECOCT', 1, 7, 7, 21, 2.00, 'default', NOW(), NOW(), 0);

-- ============================================================
-- 11. 设备状态快照 (eq_device_status) — 与 eq_device 同步
-- ============================================================

-- 清除今天已存在的记录
DELETE FROM eq_device_status WHERE DATE(snapshot_time) = @demo_date;

-- 从 eq_device 同步状态
INSERT INTO eq_device_status (device_code, device_type, status, detail_status, current_temp, target_temp, snapshot_time, tenant_id)
SELECT device_code, device_type, status, detail_status, current_temp, 100.00, NOW(), 'default'
FROM eq_device WHERE deleted = 0 AND status IN ('ONLINE', 'OFFLINE', 'MAINTENANCE', 'FAULT');

-- ============================================================
-- 12. 任务状态历史 (prod_task_status_history)
-- ============================================================

-- 清除今天的历史记录
DELETE FROM prod_task_status_history WHERE DATE(operate_time) = @demo_date OR task_id IN (1,2,3,4,5,6,7,8);

-- 为已完成的任务1插入完整流转
INSERT INTO prod_task_status_history (task_id, from_status, to_status, operator_id, operate_time, remark, tenant_id)
VALUES
(1, 'WAIT_SOAK', 'SOAKING', '1', '2026-05-12 06:20:00', '开始浸泡', 'default'),
(1, 'SOAKING', 'DECOCTING', '1', '2026-05-12 06:50:00', '开始煎药', 'default'),
(1, 'DECOCTING', 'WAIT_POUR', '1', '2026-05-12 08:20:00', '煎药完成，准备出汤', 'default'),
(1, 'WAIT_POUR', 'POURING', '1', '2026-05-12 08:30:00', '开始出汤', 'default'),
(1, 'POURING', 'WAIT_WRAP', '1', '2026-05-12 09:00:00', '出汤完成', 'default'),
(1, 'WAIT_WRAP', 'WRAPPING', '1', '2026-05-12 09:10:00', '开始包装', 'default'),
(1, 'WRAPPING', 'COMPLETED', '1', '2026-05-12 10:30:00', '包装完成，任务结束', 'default');

-- 为已完成的任务2插入完整流转
INSERT INTO prod_task_status_history (task_id, from_status, to_status, operator_id, operate_time, remark, tenant_id)
VALUES
(2, 'WAIT_SOAK', 'SOAKING', '1', '2026-05-12 07:20:00', '开始浸泡', 'default'),
(2, 'SOAKING', 'DECOCTING', '1', '2026-05-12 07:50:00', '开始煎药', 'default'),
(2, 'DECOCTING', 'WAIT_POUR', '1', '2026-05-12 10:20:00', '煎药完成', 'default'),
(2, 'WAIT_POUR', 'POURING', '1', '2026-05-12 10:30:00', '开始出汤', 'default'),
(2, 'POURING', 'WAIT_WRAP', '1', '2026-05-12 12:00:00', '出汤完成', 'default'),
(2, 'WAIT_WRAP', 'WRAPPING', '1', '2026-05-12 12:10:00', '开始包装', 'default'),
(2, 'WRAPPING', 'COMPLETED', '1', '2026-05-12 14:00:00', '包装完成，任务结束', 'default');

-- 为进行中的任务插入部分流转
INSERT INTO prod_task_status_history (task_id, from_status, to_status, operator_id, operate_time, remark, tenant_id)
VALUES
(4, 'WAIT_SOAK', 'SOAKING', '1', '2026-05-12 07:20:00', '开始浸泡', 'default'),
(4, 'SOAKING', 'DECOCTING', '1', '2026-05-12 09:00:00', '开始煎药', 'default'),
(7, 'WAIT_SOAK', 'SOAKING', '2', '2026-05-12 11:20:00', '开始浸泡', 'default'),
(7, 'SOAKING', 'DECOCTING', '2', '2026-05-12 12:00:00', '开始煎药', 'default');

-- ============================================================
-- 验证查询
-- ============================================================
SELECT '=== 处方数 ===' as check_item;
SELECT COUNT(*) as count FROM prod_prescription WHERE deleted = 0;

SELECT '=== 任务数 ===' as check_item;
SELECT COUNT(*) as count FROM prod_task WHERE deleted = 0;

SELECT '=== 任务状态分布 ===' as check_item;
SELECT status, COUNT(*) as count FROM prod_task WHERE deleted = 0 GROUP BY status;

SELECT '=== 紧急处方 ===' as check_item;
SELECT prescription_id, emergency_level, is_on_time, promised_finish_time, actual_finish_time FROM prod_emergency_prescription;

SELECT '=== 异常工单 ===' as check_item;
SELECT exception_no, prescription_no, exception_type, description FROM prod_exception_log WHERE is_deleted = 0;

SELECT '=== 任务分配数 ===' as check_item;
SELECT COUNT(*) as count FROM prod_task_assignment;

SELECT '=== 设备状态 ===' as check_item;
SELECT device_code, name, status, detail_status FROM eq_device WHERE device_code IN ('DECOCT_01','DECOCT_02','DECOCT_03','DECOCT_04','PACK_01','LABEL_01');

SELECT '=== 设备利用率 ===' as check_item;
SELECT device_code, stat_date, utilization_rate, task_count FROM device_utilization WHERE stat_date = @demo_date;

SELECT '=== 工作量统计 ===' as check_item;
SELECT stat_date, operator_name, work_type, prescription_count, task_count, duration_minutes, efficiency FROM ops_workload_stat ORDER BY stat_date DESC, operator_name, work_type;

SELECT '=== 设备告警 ===' as check_item;
SELECT d.device_code, a.alarm_type, a.message, a.is_resolved FROM eq_device_alarm a JOIN eq_device d ON a.device_id = d.id;
