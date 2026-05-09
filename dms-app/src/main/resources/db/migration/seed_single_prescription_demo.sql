-- ============================================================================
-- 单处方全流程演示数据 (Single Prescription End-to-End Demo)
-- ============================================================================
-- 目标：1条处方贯通全部流程 + 各模块至少1条设备关联数据
-- 基线：县中医院(id=2)、6用户、7设备
-- 执行前：已运行 cleanup_prod_command_demo_data.sql
-- ============================================================================

SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================================
-- 0. 清理残留孤儿数据
-- ============================================================================
DELETE FROM qt_inspection_item WHERE inspection_id IN (
    SELECT id FROM qt_inspection WHERE task_id NOT IN (SELECT id FROM prod_task)
);
DELETE FROM qt_inspection WHERE task_id NOT IN (SELECT id FROM prod_task);

-- ============================================================================
-- 1. 角色与用户分配
-- ============================================================================

-- 1.1 创建"发货员"角色（如果不存在）
INSERT INTO sys_role (id, tenant_id, role_code, role_name, description, sort_order, status, deleted)
VALUES (6, 'default', 'ROLE_SHIPPER', '发货员', '负责发药与配送管理', 6, 'ACTIVE', 0)
ON DUPLICATE KEY UPDATE role_name='发货员', description='负责发药与配送管理', status='ACTIVE', deleted=0;

-- 1.2 给用户分配角色（admin已有ROLE_ADMIN，其余5人分配对应角色）
DELETE FROM sys_user_role WHERE user_id != 9;  -- 保留admin的角色分配

INSERT INTO sys_user_role (user_id, role_id) VALUES
    (8, 2),   -- zr001 吴主任 -> 车间主任
    (3, 3),   -- bz001 王班长 -> 班组长
    (1, 4),   -- jgy001 张三 -> 操作工
    (4, 5),   -- zj001 赵质检 -> 质检员
    (6, 6);   -- kf001 孙客服 -> 发货员

-- ============================================================================
-- 2. 处方主档 (prod_prescription)
-- ============================================================================
INSERT INTO prod_prescription (
    id, hospital_id, prescription_number, patient_name, patient_phone,
    patient_type, outpatient_no, inpatient_no, bed_no, disease,
    doctor_name, department, disease_area, medicine_list, repetition,
    bags_per_repetition, bag_capacity, decocting_type, usage_method,
    scheme_id, remark, receive_time, deleted, tenant_id,
    receive_status, task_no, operator_id, operator_name,
    delivery_type, delivery_address, preparation_type, source
) VALUES (
    1001, 2, 'DEMO-RX-20260505-001', '李明', '13800138001',
    0, 'MZ20260505001', NULL, NULL, '气血两虚',
    '张医生', '中医内科', NULL, '黄芪、当归、党参', 7,
    1, 200, 0, '温服，每日两次，每次1袋',
    NULL, '演示用处方：气血两虚调理方', '2026-05-05 08:00:00', 0, 'default',
    'RECEIVED', 'DEMO-TASK-001', 8, '吴主任',
    'EXPRESS', '幸福路88号', 'DECOCTION', 'MANUAL'
);

-- ============================================================================
-- 3. 处方药材明细 (prod_prescription_medicine)
-- ============================================================================
INSERT INTO prod_prescription_medicine (
    id, prescription_id, medicine_id, hospital_code, hospital_name,
    medicine_name, dosage, unit, med_usage, decoction_method,
    batch_no, special_decoction, is_toxic, requires_retain, retain_quantity,
    sort_order, tenant_id, deleted
) VALUES
    (1001, 1001, NULL, 'XZZYY', '县中医院', '黄芪', 15.00, 'g', '补气固表', '先煎', NULL, NULL, 0, 0, NULL, 1, 'default', 0),
    (1002, 1001, NULL, 'XZZYY', '县中医院', '当归', 10.00, 'g', '补血活血', '常规', NULL, NULL, 0, 0, NULL, 2, 'default', 0),
    (1003, 1001, NULL, 'XZZYY', '县中医院', '党参', 12.00, 'g', '补中益气', '常规', NULL, NULL, 0, 0, NULL, 3, 'default', 0);

-- ============================================================================
-- 4. 生产任务 (prod_task)
-- ============================================================================
INSERT INTO prod_task (
    id, prescription_id, decoct_device_id, package_device_id, scheme_id,
    scheme_snapshot, decoction_plan, operator_id, operator_name, status,
    suspended_from, suspend_reason, suspend_time, expected_resume_time,
    current_temp, target_temp, soak_duration, soak_start_time, soak_end_time,
    decoct_start_time, decoct_end_time, pour_start_time, pour_end_time,
    wrap_start_time, wrap_end_time, complete_time, current_stage_duration,
    print_device_id, print_status, print_time, deleted, created_at, updated_at,
    current_step, pool_id, print_copies, is_exception, is_emergency, priority,
    exception_reason, patient_agreement, standard_cost, actual_cost,
    handover_type, handover_user, handover_time, tenant_id, status_enum, barcode
) VALUES (
    1001, 1001, 3, 6, NULL,
    NULL, '常压煎煮：浸泡30分钟→武火煮沸→文火维持30分钟→灌装', '1', '张三', 'COMPLETED',
    NULL, NULL, NULL, NULL,
    98.50, 100.00, 30, '2026-05-05 08:30:00', '2026-05-05 09:00:00',
    '2026-05-05 09:00:00', '2026-05-05 09:30:00', '2026-05-05 09:35:00', '2026-05-05 09:38:00',
    '2026-05-05 09:40:00', '2026-05-05 09:45:00', '2026-05-05 09:45:00', 1800,
    12, 'COMPLETED', '2026-05-05 09:46:00', 0, '2026-05-05 08:15:00', '2026-05-05 09:46:00',
    'WRAP', NULL, 1, 0, 0, 3,
    NULL, NULL, 15.00, 15.00,
    'SELF_PICKUP', NULL, NULL, 'default', 'COMPLETED', 'DEMO-BARCODE-001'
);

-- ============================================================================
-- 5. 任务分配 (prod_task_assignment)
-- ============================================================================
INSERT INTO prod_task_assignment (
    id, task_id, prescription_id, device_id, employee_id, assign_type,
    assign_reason, scheduled_start_time, scheduled_end_time,
    actual_start_time, actual_end_time, status, stage_breakdown_json,
    created_at, updated_at
) VALUES (
    1001, 1001, 1001, 3, 1, 1,
    '自动分配至朋霖煎药机-03', '2026-05-05 08:30:00', '2026-05-05 10:00:00',
    '2026-05-05 08:30:00', '2026-05-05 09:45:00', 2, NULL,
    '2026-05-05 08:15:00', '2026-05-05 09:45:00'
);

-- ============================================================================
-- 6. 任务状态历史 (prod_task_status_history)
-- ============================================================================
INSERT INTO prod_task_status_history (
    id, task_id, from_status, to_status, operator_id, operate_time,
    remark, tenant_id, trigger_source, device_code
) VALUES
    (1001, 1001, NULL, 'PENDING', '8', '2026-05-05 08:15:00', '处方接收，任务创建', 'default', 'SYSTEM', NULL),
    (1002, 1001, 'PENDING', 'ASSIGNED', '3', '2026-05-05 08:20:00', '班长分配至煎药机-03', 'default', 'MANUAL', 'DECOCT_001'),
    (1003, 1001, 'ASSIGNED', 'SOAKING', '1', '2026-05-05 08:30:00', '开始浸泡', 'default', 'DEVICE', 'DECOCT_001'),
    (1004, 1001, 'SOAKING', 'DECOCTING', '1', '2026-05-05 09:00:00', '浸泡完成，开始煎煮', 'default', 'DEVICE', 'DECOCT_001'),
    (1005, 1001, 'DECOCTING', 'POURING', '1', '2026-05-05 09:30:00', '煎煮完成，开始灌装', 'default', 'DEVICE', 'DECOCT_001'),
    (1006, 1001, 'POURING', 'WRAPPING', '1', '2026-05-05 09:38:00', '灌装完成，开始包装', 'default', 'DEVICE', 'DECOCT_001'),
    (1007, 1001, 'WRAPPING', 'COMPLETED', '1', '2026-05-05 09:45:00', '包装完成，任务结束', 'default', 'DEVICE', 'DECOCT_001');

-- ============================================================================
-- 7. 步骤日志 (prod_step_log)
-- ============================================================================
INSERT INTO prod_step_log (
    id, task_id, parent_id, step_type, device_id, operator_id,
    started_at, ended_at, is_paused, pause_reason, pause_duration,
    delay_minutes, delay_reason, result, abort_reason, waste_amount,
    waste_unit, is_retry, tenant_id
) VALUES
    (1001, 1001, NULL, 'SOAK', '3', '1', '2026-05-05 08:30:00', '2026-05-05 09:00:00', 0, NULL, 0, 0, NULL, '正常', NULL, NULL, NULL, 0, 'default'),
    (1002, 1001, NULL, 'DECOCT', '3', '1', '2026-05-05 09:00:00', '2026-05-05 09:30:00', 0, NULL, 0, 0, NULL, '正常', NULL, NULL, NULL, 0, 'default'),
    (1003, 1001, NULL, 'POUR', '6', '1', '2026-05-05 09:35:00', '2026-05-05 09:38:00', 0, NULL, 0, 0, NULL, '正常', NULL, NULL, NULL, 0, 'default'),
    (1004, 1001, NULL, 'WRAP', '6', '1', '2026-05-05 09:40:00', '2026-05-05 09:45:00', 0, NULL, 0, 0, NULL, '正常', NULL, NULL, NULL, 0, 'default');

-- ============================================================================
-- 8. 质检记录 (qt_inspection)
-- ============================================================================
INSERT INTO qt_inspection (
    id, tenant_id, task_id, result, operator_id, remark,
    inspected_at, created_at, updated_at, deleted, is_exception, exception_reason
) VALUES (
    1001, 'default', 1001, 'PASS', '4', '外观正常、液量达标、密封良好、标签清晰',
    '2026-05-05 09:48:00', '2026-05-05 09:48:00', '2026-05-05 09:48:00', 0, 0, NULL
);

-- 质检明细
INSERT INTO qt_inspection_item (
    id, inspection_id, item_code, item_name, result, actual_value, remark, sort_order
) VALUES
    (1001, 1001, 'APPEARANCE', '外观检查', 'PASS', '澄清、无沉淀', '符合标准', 1),
    (1002, 1001, 'VOLUME', '液量检测', 'PASS', '198ml', '200ml±5%', 2),
    (1003, 1001, 'SEAL', '密封检查', 'PASS', '无渗漏', '符合标准', 3),
    (1004, 1001, 'LABEL', '标签核对', 'PASS', '信息完整', '符合标准', 4);

-- ============================================================================
-- 9. 留样记录 (qt_retain_sample)
-- ============================================================================
INSERT INTO qt_retain_sample (
    id, task_id, prescription_id, sample_no, sample_type,
    retain_duration, retain_time, expire_time, status,
    destroy_time, destroy_by, operator_id, remark, tenant_id, deleted
) VALUES (
    1001, 1001, 1001, 'DEMO-SAMPLE-001', 1,
    168, '2026-05-05 09:50:00', '2026-05-12 09:50:00', 1,
    NULL, NULL, 4, '气血两虚方留样，保存7天', 'default', 0
);

-- ============================================================================
-- 10. 发药记录 (t_delivery_record)
-- ============================================================================
INSERT INTO t_delivery_record (
    id, task_id, prescription_no, patient_name, delivery_type,
    receiver_name, receiver_phone, receiver_address,
    courier_company, courier_no, bag_count, status,
    operator_id, remark, delivered_at, tenant_id, deleted
) VALUES (
    1001, 1001, 'DEMO-RX-20260505-001', '李明', 'EXPRESS',
    '李明', '13800138001', '幸福路88号',
    '顺丰速运', 'SF1234567890', 7, 'DELIVERED',
    '6', '已签收，客户满意', '2026-05-05 14:00:00', 'default', 0
);

-- ============================================================================
-- 11. 设备关联数据
-- ============================================================================

-- 11.1 设备指令 (device_command) - START 指令（已完成）
INSERT INTO device_command (
    id, device_code, command_type, command_payload, status,
    response_payload, retry_count, send_time, ack_time, fail_reason,
    tenant_id, created_at, updated_at, deleted, command_level, risk_level, require_confirm
) VALUES (
    1001, 'DECOCT_001', 'START',
    '{\"taskId\":1001,\"scheme\":{\"soakDuration\":30,\"decoctDuration\":30,\"temp\":100}}',
    'COMPLETED',
    '{\"code\":0,\"message\":\"开始煎煮\"}', 0,
    '2026-05-05 09:00:00', '2026-05-05 09:00:05', NULL,
    'default', '2026-05-05 09:00:00', '2026-05-05 09:00:05', 0, 'NORMAL', 'LOW', 0
);

-- 11.2 设备告警 (eq_device_alarm) - 温度偏高告警（已恢复）
INSERT INTO eq_device_alarm (
    id, device_id, alarm_type, alarm_level, message, status,
    resolved_time, is_resolved, resolved_at, deleted, created_at, updated_at, tenant_id
) VALUES (
    1001, 3, 'TEMP_HIGH', 'MEDIUM', '煎药机-03 当前温度105.2°C，超过设定上限100°C',
    1, '2026-05-05 09:05:00', 1, '2026-05-05 09:05:00', 0,
    '2026-05-05 09:02:00', '2026-05-05 09:05:00', 'default'
);

-- 11.3 设备维保 (eq_device_maintenance) - 定期保养（已完成）
INSERT INTO eq_device_maintenance (
    id, device_id, maintenance_type, content, parts, cost,
    operator_id, plan_date, finish_date, status, tenant_id, deleted
) VALUES (
    1001, 3, 'ROUTINE',
    '清洗加热管、检查密封圈、校准温度传感器、润滑传动部件',
    '密封圈×1、润滑油200ml', 120.00,
    '8', '2026-05-05', '2026-05-05', 2, 'default', 0
);

-- 11.4 清洗记录 (eq_wash_record) - 任务后清洗（已完成）
INSERT INTO eq_wash_record (
    id, device_id, device_code, task_id, prescription_id,
    wash_type, standard_duration, start_time, end_time,
    duration_min, result, operator_id, operator_name, remark, tenant_id, deleted
) VALUES (
    1001, 3, 'DECOCT_001', 1001, 1001,
    1, 10, '2026-05-05 09:46:00', '2026-05-05 09:56:00',
    10, 1, 1, '张三', '任务完成后标准清洗流程', 'default', 0
);

-- ============================================================================
-- 12. 设备状态快照 (eq_device_status) - 2条关键节点快照
-- ============================================================================
INSERT INTO eq_device_status (
    id, device_code, device_type, status, detail_status,
    current_temp, target_temp, water_level, pressure,
    prescription_code, scheme_name, operator_id, operator_name,
    progress_percent, remaining_time, fault_code, fault_message,
    snapshot_time, tenant_id
) VALUES
    (1001, 'DECOCT_001', 1, 'BUSY', 'DECOCTING',
     98.50, 100.00, 80, 0.15,
     'DEMO-RX-20260505-001', '常压煎煮方案', 1, '张三',
     50, 900, NULL, NULL,
     '2026-05-05 09:15:00', 'default'),
    (1002, 'DECOCT_001', 1, 'IDLE', 'IDLE',
     25.00, 0.00, 0, 0.00,
     NULL, NULL, NULL, NULL,
     0, 0, NULL, NULL,
     '2026-05-05 09:50:00', 'default');

-- ============================================================================
-- 13. 工作量统计 (workload_stat) - 1条
-- ============================================================================
INSERT INTO workload_stat (
    id, operator_id, operator_name, work_type, task_count, prescription_count,
    package_count, duration_minutes, efficiency, stat_date, tenant_id, deleted, created_at, updated_at
) VALUES (
    1001, 1, '张三', 'DECOCTION', 1, 1, 7, 75, 95.00, '2026-05-05', 'default', 0,
    '2026-05-05 09:45:00', '2026-05-05 09:45:00'
);

-- ============================================================================
-- 14. 煎药追溯主档 (decoction_trace) - 1条
-- ============================================================================
INSERT INTO decoction_trace (
    id, prescription_no, patient_name, patient_phone, task_id,
    decoct_device_code, decoct_device_name, packer_device_code, labeler_device_code,
    scheme_id, scheme_name, soak_time, pre_decoct_time, first_decoct_time,
    package_time, package_volume, sample_count,
    receive_time, audit_time, audit_pass_time, dispense_time, review_time,
    soak_start_time, soak_end_time, first_decoct_start, first_decoct_end,
    package_start_time, package_end_time, deliver_time, complete_time,
    receive_operator, audit_operator, dispense_operator, review_operator,
    soak_operator, decoct_operator, package_operator, deliver_operator,
    max_temp, avg_temp, water_quality_check, status, exception_reason,
    exception_handle_result, label_print_count, delivery_no, delivery_company,
    tenant_id, deleted, batch_no
) VALUES (
    1001, 'DEMO-RX-20260505-001', '李明', '13800138001', 1001,
    'DECOCT_001', '朋霖煎药机-03', 'PACK_001', 'LBL_PRN_001',
    NULL, '常压煎煮方案', 30, 0, 30,
    5, 198.00, 1,
    '2026-05-05 08:00:00', '2026-05-05 08:05:00', '2026-05-05 08:10:00', '2026-05-05 09:48:00', '2026-05-05 09:50:00',
    '2026-05-05 08:30:00', '2026-05-05 09:00:00', '2026-05-05 09:00:00', '2026-05-05 09:30:00',
    '2026-05-05 09:40:00', '2026-05-05 09:45:00', '2026-05-05 14:00:00', '2026-05-05 14:00:00',
    '吴主任', '赵质检', '孙客服', '赵质检',
    '张三', '张三', '张三', '孙客服',
    105.20, 98.50, '合格', 'COMPLETED', NULL,
    NULL, 7, 'SF1234567890', '顺丰速运',
    'default', 0, 'DEMO-BATCH-001'
);

-- ============================================================================
-- 15. 更新设备当前状态（将 DECOCT_001 设为空闲，关联演示处方信息）
-- ============================================================================
UPDATE eq_device SET
    status = 'IDLE',
    detail_status = 'IDLE',
    current_temp = 25.00,
    water_level = 0,
    pressure = 0.00,
    current_prescription_code = NULL,
    current_operator_id = NULL,
    current_operator_name = NULL,
    estimated_finish_time = NULL,
    remaining_time = 0,
    progress_percent = 0,
    updated_at = NOW()
WHERE id = 3;

SET FOREIGN_KEY_CHECKS = 1;
