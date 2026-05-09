-- ============================================================================
-- seed_single_prescription_demo.sql
-- 单处方全流程闭环演示数据（严格对齐 TaskServiceImpl 运行时行为）
-- ============================================================================
-- 设计口径：
--   - 状态流：WAIT_SOAK → SOAKING → WAIT_DECOCT → DECOCTING → WAIT_POUR → POURING
--             → WAIT_WRAP → WRAPPING → WAIT_LABEL → WAIT_QC → WAIT_HANDOVER → COMPLETED
--   - 状态历史：11 条（每次 transition() → recordHistory()）
--   - 步骤日志：7 条（每次 createStepLog()：SOAK/DECOCT/POUR/WRAP/LABEL/INSPECT/HANDOVER）
--   - 工序记录：6 条（recordWork()：SOAK×2 / DECOCT×2 / WRAP×2）
--   - 交接明细：1 条（handover() 时写入）
--   - 所有 ID >= 900000000，显式指定
--   - 不 UPDATE 任何真实数据（eq_device 等）
--   - 不创建新角色/用户
-- ============================================================================

SET FOREIGN_KEY_CHECKS = 0;

-- --------------------------------------------------
-- 0. 执行前清理（幂等：删除同 ID 残留数据）
-- --------------------------------------------------

DELETE FROM prod_work_record WHERE task_id = 900000001;
DELETE FROM prod_handover_detail WHERE task_id = 900000001;
DELETE FROM prod_step_log WHERE task_id = 900000001;
DELETE FROM prod_task_status_history WHERE task_id = 900000001;
DELETE FROM dms_task_assignment WHERE task_id = 900000001;
DELETE FROM dms_time_monitor WHERE task_id = 900000001;
DELETE FROM dms_alert_log WHERE task_id = 900000001;
DELETE FROM eq_wash_record WHERE task_id = 900000001;

DELETE FROM qt_inspection_item WHERE inspection_id IN (SELECT id FROM qt_inspection WHERE task_id = 900000001);
DELETE FROM qt_inspection WHERE task_id = 900000001;
DELETE FROM qt_retain_sample WHERE task_id = 900000001;
DELETE FROM qt_retain_sample WHERE sample_no LIKE 'DEMO-SAMPLE%';
DELETE FROM t_delivery_record WHERE task_id = 900000001;
DELETE FROM decoction_trace WHERE task_id = 900000001;

DELETE FROM prod_task WHERE id = 900000001;
DELETE FROM prod_prescription_medicine WHERE prescription_id = 900000001;
DELETE FROM prod_prescription WHERE id = 900000001;

DELETE FROM device_command WHERE id >= 900000001 AND id <= 900000001;
DELETE FROM eq_device_alarm WHERE id >= 900000001 AND id <= 900000001;
DELETE FROM eq_device_maintenance WHERE id >= 900000001 AND id <= 900000001;
DELETE FROM eq_device_status WHERE id >= 900000001 AND id <= 900000002;
DELETE FROM workload_stat WHERE id >= 900000001 AND id <= 900000001;

-- --------------------------------------------------
-- 1. 处方主档 (prod_prescription)
-- --------------------------------------------------
INSERT INTO prod_prescription (
    id, hospital_id, prescription_number, patient_name, patient_phone,
    patient_type, outpatient_no, inpatient_no, bed_no, disease,
    doctor_name, department, medicine_list, repetition,
    bags_per_repetition, bag_capacity, decocting_type, usage_method,
    scheme_id, remark, receive_time, deleted, tenant_id,
    receive_status, task_no, operator_id, operator_name,
    delivery_type, delivery_address, preparation_type, source
) VALUES (
    900000001, 2, 'DEMO-RX-20260507-001', '李明', '13800138001',
    0, 'DEMO-OUT-001', NULL, NULL, '气血两虚',
    '张医生', '中医内科', '黄芪、当归、党参', 7,
    1, 200, 0, '温服，每日两次，每次1袋',
    NULL, '演示用处方：气血两虚调理方', '2026-05-07 08:00:00', 0, 'default',
    'RECEIVED', 'DEMO-TASK-001', 8, '吴主任',
    'SELF_PICKUP', '幸福路88号', 'DECOCTION', 'MANUAL'
);

-- --------------------------------------------------
-- 2. 处方药材明细 (prod_prescription_medicine)
-- --------------------------------------------------
INSERT INTO prod_prescription_medicine (
    id, prescription_id, medicine_id, hospital_code, hospital_name,
    medicine_name, dosage, unit, med_usage, decoction_method,
    batch_no, special_decoction, is_toxic, requires_retain, retain_quantity,
    sort_order, tenant_id, deleted
) VALUES
    (900000001, 900000001, NULL, 'XZZYY', '县中医院', '黄芪', 15.00, 'g', '补气固表', '先煎', NULL, NULL, 0, 0, NULL, 1, 'default', 0),
    (900000002, 900000001, NULL, 'XZZYY', '县中医院', '当归', 10.00, 'g', '补血活血', '常规', NULL, NULL, 0, 0, NULL, 2, 'default', 0),
    (900000003, 900000001, NULL, 'XZZYY', '县中医院', '党参', 12.00, 'g', '补中益气', '常规', NULL, NULL, 0, 0, NULL, 3, 'default', 0);

-- --------------------------------------------------
-- 3. 生产任务 (prod_task) — 最终状态 COMPLETED
-- --------------------------------------------------
INSERT INTO prod_task (
    id, prescription_id, decoct_device_id, package_device_id, scheme_id,
    decoction_plan, operator_id, operator_name, status,
    soak_start_time, soak_end_time,
    decoct_start_time, decoct_end_time,
    pour_start_time, pour_end_time,
    wrap_start_time, wrap_end_time,
    complete_time, current_stage_duration,
    print_status, deleted, created_at, updated_at,
    is_exception, priority,
    standard_cost, actual_cost,
    handover_type, handover_user, handover_time,
    tenant_id, status_enum, barcode
) VALUES (
    900000001, 900000001, 3, 6, NULL,
    '常压煎煮：浸泡30分钟→武火煮沸→文火维持30分钟→灌装', '1', '张三', 'COMPLETED',
    '2026-05-07 08:05:00', '2026-05-07 08:35:00',
    '2026-05-07 08:40:00', '2026-05-07 09:10:00',
    '2026-05-07 09:12:00', '2026-05-07 09:15:00',
    '2026-05-07 09:18:00', '2026-05-07 09:23:00',
    '2026-05-07 09:35:00', 1800,
    'PRINTED', 0, '2026-05-07 08:00:00', '2026-05-07 09:35:00',
    0, 3,
    15.00, 15.00,
    'SELF_PICKUP', '1', '2026-05-07 09:35:00',
    'default', 'COMPLETED', 'DEMO-BARCODE-001'
);

-- --------------------------------------------------
-- 4. 任务分配 (dms_task_assignment)
-- --------------------------------------------------
INSERT INTO dms_task_assignment (
    id, task_id, prescription_id, device_id, employee_id, assign_type,
    assign_reason, scheduled_start_time, scheduled_end_time,
    actual_start_time, actual_end_time, status, created_at, updated_at
) VALUES (
    900000001, 900000001, 900000001, 3, 1, 1,
    '自动分配至朋霖煎药机-03', '2026-05-07 08:05:00', '2026-05-07 10:00:00',
    '2026-05-07 08:05:00', '2026-05-07 09:35:00', 1,
    '2026-05-07 08:00:00', '2026-05-07 09:35:00'
);

-- --------------------------------------------------
-- 5. 任务状态历史 (prod_task_status_history) — 11 条
-- 严格对应 TaskServiceImpl 中每次 transition() → recordHistory()
-- --------------------------------------------------
INSERT INTO prod_task_status_history (
    id, task_id, from_status, to_status, operator_id, operate_time,
    remark, tenant_id, trigger_source, device_code
) VALUES
    -- 1. startSoak: WAIT_SOAK → SOAKING
    (900000001, 900000001, 'WAIT_SOAK', 'SOAKING', '1', '2026-05-07 08:05:00',
     '开始泡药', 'default', 'MANUAL', NULL),
    -- 2. endSoak: SOAKING → WAIT_DECOCT
    (900000002, 900000001, 'SOAKING', 'WAIT_DECOCT', '1', '2026-05-07 08:35:00',
     '泡药结束', 'default', 'MANUAL', NULL),
    -- 3. startDecoct: WAIT_DECOCT → DECOCTING
    (900000003, 900000001, 'WAIT_DECOCT', 'DECOCTING', '1', '2026-05-07 08:40:00',
     '开始煎药，绑定设备: DECOCT_001', 'default', 'MANUAL', 'DECOCT_001'),
    -- 4. endDecoct: DECOCTING → WAIT_POUR
    (900000004, 900000001, 'DECOCTING', 'WAIT_POUR', '1', '2026-05-07 09:10:00',
     '煎药结束', 'default', 'MANUAL', NULL),
    -- 5. startPour: WAIT_POUR → POURING
    (900000005, 900000001, 'WAIT_POUR', 'POURING', '1', '2026-05-07 09:12:00',
     '开始出液', 'default', 'MANUAL', NULL),
    -- 6. endPour: POURING → WAIT_WRAP
    (900000006, 900000001, 'POURING', 'WAIT_WRAP', '1', '2026-05-07 09:15:00',
     '出液结束', 'default', 'MANUAL', NULL),
    -- 7. startWrap: WAIT_WRAP → WRAPPING
    (900000007, 900000001, 'WAIT_WRAP', 'WRAPPING', '1', '2026-05-07 09:18:00',
     '开始包装，绑定包装机: PACK_001', 'default', 'MANUAL', 'PACK_001'),
    -- 8. endWrap: WRAPPING → WAIT_LABEL
    (900000008, 900000001, 'WRAPPING', 'WAIT_LABEL', '1', '2026-05-07 09:23:00',
     '包装结束', 'default', 'MANUAL', NULL),
    -- 9. confirmLabel: WAIT_LABEL → WAIT_QC
    (900000009, 900000001, 'WAIT_LABEL', 'WAIT_QC', '1', '2026-05-07 09:25:00',
     '贴标完成', 'default', 'MANUAL', NULL),
    -- 10. qualityInspect(PASS): WAIT_QC → WAIT_HANDOVER
    (900000010, 900000001, 'WAIT_QC', 'WAIT_HANDOVER', '4', '2026-05-07 09:30:00',
     '质检通过: 外观正常、液量达标、密封良好、标签清晰', 'default', 'MANUAL', NULL),
    -- 11. handover(isFinal=true): WAIT_HANDOVER → COMPLETED
    (900000011, 900000001, 'WAIT_HANDOVER', 'COMPLETED', '1', '2026-05-07 09:35:00',
     '扫码交接: SELF_PICKUP, 袋数=7 (完成)', 'default', 'MANUAL', NULL);

-- --------------------------------------------------
-- 6. 步骤日志 (prod_step_log) — 7 条
-- 严格对应每次 createStepLog() 的 INSERT
-- --------------------------------------------------
INSERT INTO prod_step_log (
    id, task_id, step_type, device_id, operator_id,
    started_at, ended_at, result, tenant_id
) VALUES
    -- 1. SOAK (startSoak create, endSoak close)
    (900000001, 900000001, 'SOAK', '3', '1',
     '2026-05-07 08:05:00', '2026-05-07 08:35:00', '正常', 'default'),
    -- 2. DECOCT (startDecoct create, endDecoct close)
    (900000002, 900000001, 'DECOCT', '3', '1',
     '2026-05-07 08:40:00', '2026-05-07 09:10:00', '正常', 'default'),
    -- 3. POUR (startPour create, endPour close)
    (900000003, 900000001, 'POUR', NULL, '1',
     '2026-05-07 09:12:00', '2026-05-07 09:15:00', '正常', 'default'),
    -- 4. WRAP (startWrap create, endWrap close)
    (900000004, 900000001, 'WRAP', '6', '1',
     '2026-05-07 09:18:00', '2026-05-07 09:23:00', '正常', 'default'),
    -- 5. LABEL (confirmLabel create + close 同方法内)
    (900000005, 900000001, 'LABEL', NULL, '1',
     '2026-05-07 09:25:00', '2026-05-07 09:26:00', '正常', 'default'),
    -- 6. INSPECT (qualityInspect create + close 同方法内)
    (900000006, 900000001, 'INSPECT', NULL, '4',
     '2026-05-07 09:30:00', '2026-05-07 09:31:00', '通过', 'default'),
    -- 7. HANDOVER (handover create + close 同方法内)
    (900000007, 900000001, 'HANDOVER', NULL, '1',
     '2026-05-07 09:35:00', '2026-05-07 09:36:00', '正常', 'default');

-- --------------------------------------------------
-- 7. 工序记录 (prod_work_record) — 6 条
-- recordWork()：SOAK×2 / DECOCT×2 / WRAP×2
-- --------------------------------------------------
INSERT INTO prod_work_record (
    id, task_id, operator_id, operator_name, action, work_time, tenant_id
) VALUES
    (900000001, 900000001, '1', '张三', 'SOAK', 0, 'default'),
    (900000002, 900000001, '1', '张三', 'SOAK', 30, 'default'),
    (900000003, 900000001, '1', '张三', 'DECOCT', 0, 'default'),
    (900000004, 900000001, '1', '张三', 'DECOCT', 30, 'default'),
    (900000005, 900000001, '1', '张三', 'WRAP', 0, 'default'),
    (900000006, 900000001, '1', '张三', 'WRAP', 5, 'default');

-- --------------------------------------------------
-- 8. 交接明细 (prod_handover_detail) — 1 条
-- handover() 时写入
-- --------------------------------------------------
INSERT INTO prod_handover_detail (
    id, task_id, bag_count, handover_type, handover_user, handover_time, remark, tenant_id
) VALUES (
    900000001, 900000001, 7, 'SELF_PICKUP', '1', '2026-05-07 09:35:00',
    '患者自取，7袋，已核对', 'default'
);

-- --------------------------------------------------
-- 9. 质检记录 (qt_inspection) — 1 条
-- --------------------------------------------------
INSERT INTO qt_inspection (
    id, tenant_id, task_id, result, operator_id, remark,
    inspected_at, created_at, updated_at, deleted, is_exception
) VALUES (
    900000001, 'default', 900000001, 'PASS', '4', '外观正常、液量达标、密封良好、标签清晰',
    '2026-05-07 09:30:00', '2026-05-07 09:30:00', '2026-05-07 09:30:00', 0, 0
);

INSERT INTO qt_inspection_item (
    id, inspection_id, item_code, item_name, result, actual_value, remark, sort_order
) VALUES
    (900000001, 900000001, 'APPEARANCE', '外观检查', 'PASS', '澄清、无沉淀', '符合标准', 1),
    (900000002, 900000001, 'VOLUME', '液量检测', 'PASS', '198ml', '200ml±5%', 2),
    (900000003, 900000001, 'SEAL', '密封检查', 'PASS', '无渗漏', '符合标准', 3),
    (900000004, 900000001, 'LABEL', '标签核对', 'PASS', '信息完整', '符合标准', 4);

-- --------------------------------------------------
-- 10. 留样记录 (qt_retain_sample) — 1 条
-- --------------------------------------------------
INSERT INTO qt_retain_sample (
    id, task_id, prescription_id, sample_no, sample_type,
    retain_duration, retain_time, expire_time, status,
    operator_id, remark, tenant_id, deleted
) VALUES (
    900000001, 900000001, 900000001, 'DEMO-SAMPLE-900000001', 1,
    168, '2026-05-07 09:32:00', '2026-05-14 09:32:00', 1,
    4, '气血两虚方留样，保存7天', 'default', 0
);

-- --------------------------------------------------
-- 11. 发药记录 (t_delivery_record) — 1 条
-- --------------------------------------------------
INSERT INTO t_delivery_record (
    id, task_id, prescription_no, patient_name, delivery_type,
    receiver_name, receiver_phone, receiver_address,
    bag_count, status, operator_id, remark, delivered_at, tenant_id, deleted
) VALUES (
    900000001, 900000001, 'DEMO-RX-20260507-001', '李明', 'SELF_PICKUP',
    '李明', '13800138001', '幸福路88号',
    7, 'DELIVERED', '6', '患者自取，已核对身份并签收',
    '2026-05-07 09:35:00', 'default', 0
);

-- --------------------------------------------------
-- 12. 设备指令 (device_command) — 1 条
-- --------------------------------------------------
INSERT INTO device_command (
    id, device_code, command_type, command_payload, status,
    response_payload, retry_count, send_time, ack_time, fail_reason,
    tenant_id, created_at, updated_at, deleted, command_level, risk_level, require_confirm
) VALUES (
    900000001, 'DECOCT_001', 'START',
    '{"taskId":900000001,"scheme":{"soakDuration":30,"decoctDuration":30,"temp":100}}',
    'COMPLETED',
    '{"code":0,"message":"开始煎煮"}', 0,
    '2026-05-07 08:40:00', '2026-05-07 08:40:05', NULL,
    'default', '2026-05-07 08:40:00', '2026-05-07 08:40:05', 0, 'NORMAL', 'LOW', 0
);

-- --------------------------------------------------
-- 13. 设备告警 (eq_device_alarm) — 1 条
-- --------------------------------------------------
INSERT INTO eq_device_alarm (
    id, device_id, alarm_type, alarm_level, message, status,
    resolved_time, is_resolved, resolved_at, deleted, created_at, updated_at, tenant_id
) VALUES (
    900000001, 3, 'TEMP_HIGH', 'MEDIUM',
    '煎药机-03 当前温度105.2°C，超过设定上限100°C',
    1, '2026-05-07 08:45:00', 1, '2026-05-07 08:45:00', 0,
    '2026-05-07 08:42:00', '2026-05-07 08:45:00', 'default'
);

-- --------------------------------------------------
-- 14. 设备维保 (eq_device_maintenance) — 1 条
-- --------------------------------------------------
INSERT INTO eq_device_maintenance (
    id, device_id, maintenance_type, content, parts, cost,
    operator_id, plan_date, finish_date, status, tenant_id, deleted
) VALUES (
    900000001, 3, 'ROUTINE',
    '清洗加热管、检查密封圈、校准温度传感器、润滑传动部件',
    '密封圈×1、润滑油200ml', 120.00,
    '8', '2026-05-07', '2026-05-07', 2, 'default', 0
);

-- --------------------------------------------------
-- 15. 清洗记录 (eq_wash_record) — 1 条
-- --------------------------------------------------
INSERT INTO eq_wash_record (
    id, device_id, device_code, task_id, prescription_id,
    wash_type, standard_duration, start_time, end_time,
    duration_min, result, operator_id, operator_name, remark, tenant_id, deleted
) VALUES (
    900000001, 3, 'DECOCT_001', 900000001, 900000001,
    1, 10, '2026-05-07 09:36:00', '2026-05-07 09:46:00',
    10, 1, 1, '张三', '任务完成后标准清洗流程', 'default', 0
);

-- --------------------------------------------------
-- 16. 设备状态快照 (eq_device_status) — 2 条
-- --------------------------------------------------
INSERT INTO eq_device_status (
    id, device_code, device_type, status, detail_status,
    current_temp, target_temp, water_level, pressure,
    prescription_code, scheme_name, operator_id, operator_name,
    progress_percent, remaining_time, snapshot_time, tenant_id
) VALUES
    (900000001, 'DECOCT_001', 1, 'BUSY', 'DECOCTING',
     98.50, 100.00, 80, 0.15,
     'DEMO-RX-20260507-001', '常压煎煮方案', 1, '张三',
     50, 900, '2026-05-07 08:55:00', 'default'),
    (900000002, 'DECOCT_001', 1, 'IDLE', 'IDLE',
     25.00, 0.00, 0, 0.00,
     NULL, NULL, NULL, NULL,
     0, 0, '2026-05-07 09:50:00', 'default');

-- --------------------------------------------------
-- 17. 工作量统计 (workload_stat) — 1 条
-- --------------------------------------------------
INSERT INTO workload_stat (
    id, stat_date, operator_id, operator_name, work_type, task_count,
    prescription_count, package_count, duration_minutes, efficiency, tenant_id
) VALUES (
    900000001, '2026-05-07', 1, '张三', 'DECOCTION', 1,
    1, 7, 75, 95.00, 'default'
);

-- --------------------------------------------------
-- 18. 煎药追溯主档 (decoction_trace) — 1 条
-- --------------------------------------------------
INSERT INTO decoction_trace (
    id, prescription_no, patient_name, patient_phone, task_id,
    decoct_device_code, decoct_device_name, packer_device_code, labeler_device_code,
    scheme_name, soak_time, first_decoct_time, package_time, package_volume, sample_count,
    receive_time, soak_start_time, soak_end_time,
    first_decoct_start, first_decoct_end,
    package_start_time, package_end_time,
    complete_time,
    receive_operator, soak_operator, decoct_operator, package_operator, deliver_operator,
    max_temp, avg_temp, water_quality_check, status,
    delivery_no, delivery_company, tenant_id, deleted, batch_no
) VALUES (
    900000001, 'DEMO-RX-20260507-001', '李明', '13800138001', 900000001,
    'DECOCT_001', '朋霖煎药机-03', 'PACK_001', 'LBL_PRN_001',
    '常压煎煮方案', 30, 30, 5, 198.00, 1,
    '2026-05-07 08:00:00', '2026-05-07 08:05:00', '2026-05-07 08:35:00',
    '2026-05-07 08:40:00', '2026-05-07 09:10:00',
    '2026-05-07 09:18:00', '2026-05-07 09:23:00',
    '2026-05-07 09:35:00',
    '吴主任', '张三', '张三', '张三', '孙客服',
    105.20, 98.50, '合格', 'COMPLETED',
    NULL, NULL, 'default', 0, 'DEMO-BATCH-001'
);

-- --------------------------------------------------
-- 结束：恢复外键检查
-- --------------------------------------------------
SET FOREIGN_KEY_CHECKS = 1;

-- --------------------------------------------------
-- 执行后快速验证
-- --------------------------------------------------
SELECT '【验证】prod_task' AS item, COUNT(*) AS cnt FROM prod_task WHERE id = 900000001;
SELECT '【验证】prod_prescription' AS item, COUNT(*) AS cnt FROM prod_prescription WHERE id = 900000001;
SELECT '【验证】prod_task_status_history' AS item, COUNT(*) AS cnt FROM prod_task_status_history WHERE task_id = 900000001;
SELECT '【验证】prod_step_log' AS item, COUNT(*) AS cnt FROM prod_step_log WHERE task_id = 900000001;
SELECT '【验证】prod_work_record' AS item, COUNT(*) AS cnt FROM prod_work_record WHERE task_id = 900000001;
SELECT '【验证】prod_handover_detail' AS item, COUNT(*) AS cnt FROM prod_handover_detail WHERE task_id = 900000001;
SELECT '【验证】qt_inspection' AS item, COUNT(*) AS cnt FROM qt_inspection WHERE task_id = 900000001;
SELECT '【验证】decoction_trace' AS item, COUNT(*) AS cnt FROM decoction_trace WHERE task_id = 900000001;
