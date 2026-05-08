-- ============================================
-- seed_static_demo_data.sql
-- 静态演示数据种子脚本（28条任务，覆盖13种状态）
-- ============================================
-- 设计原则：
--   - 所有 ID >= 900000000，显式指定
--   - 状态值使用 V47 统一后的英文编码
--   - 不 UPDATE 任何真实数据
--   - 支持重复执行（先清理再插入）
-- ============================================

SET FOREIGN_KEY_CHECKS = 0;

-- 【已移除】设备分组主档（避免影响真实设备分组绑定）

-- 【已移除】eq_device_group 不在本次交付范围内

-- 更新设备分组绑定（演示数据分组）
-- 【已移除】禁止 UPDATE 真实设备
-- 【已移除】禁止 UPDATE 真实设备
-- 【已移除】禁止 UPDATE 真实设备

-- --------------------------------------------------
-- 2. 演示任务主链（24条处方 + 24条任务）
-- --------------------------------------------------
DELETE FROM prod_prescription WHERE id >= 900000000;

INSERT INTO prod_prescription (id, hospital_id, prescription_number, patient_name, patient_phone, patient_type, outpatient_no, inpatient_no, bed_no, disease, doctor_name, department, disease_area, medicine_list, repetition, bags_per_repetition, bag_capacity, decocting_type, usage_method, scheme_id, remark, receive_time, deleted, created_at, updated_at, tenant_id, receive_status, reject_type, reject_reason, received_at, rejected_at, task_no, operator_id, operator_name, import_exception, exception_reason, raw_import_data, decoction_plan, delivery_type, delivery_address, preparation_type, source) VALUES
(900000001, 2, 'DEMO-RX-20260506-0001', '张伟', '13800138001', 0, 'OUT0001', NULL, NULL, '风寒感冒', '张医生', '内科', NULL, '黄芪、当归、党参', 7, 1, 200, 0, '水煎服', NULL, NULL, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'MANUAL'),
(900000002, 2, 'DEMO-RX-20260506-0002', '李娜', '13800138002', 0, 'OUT0002', NULL, NULL, '风寒感冒', '李医生', '外科', NULL, '当归、党参、白术', 7, 1, 200, 0, '水煎服', NULL, NULL, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'MANUAL'),
(900000003, 2, 'DEMO-RX-20260506-0003', '王芳', '13800138003', 0, 'OUT0003', NULL, NULL, '风寒感冒', '王医生', '妇科', NULL, '党参、白术、茯苓', 7, 1, 200, 0, '水煎服', NULL, NULL, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'MANUAL'),
(900000004, 2, 'DEMO-RX-20260506-0004', '刘洋', '13800138004', 0, 'OUT0004', NULL, NULL, '风寒感冒', '刘医生', '儿科', NULL, '白术、茯苓、甘草', 7, 1, 200, 0, '水煎服', NULL, NULL, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'MANUAL'),
(900000005, 2, 'DEMO-RX-20260506-0005', '陈静', '13800138005', 0, 'OUT0005', NULL, NULL, '风寒感冒', '陈医生', '骨科', NULL, '茯苓、甘草、川芎', 7, 1, 200, 0, '水煎服', NULL, NULL, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'MANUAL'),
(900000006, 2, 'DEMO-RX-20260506-0006', '杨强', '13800138006', 0, 'OUT0006', NULL, NULL, '风寒感冒', '张医生', '肿瘤科', NULL, '甘草、川芎、熟地黄', 7, 1, 200, 0, '水煎服', NULL, NULL, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'MANUAL'),
(900000007, 2, 'DEMO-RX-20260506-0007', '赵敏', '13800138007', 0, 'OUT0007', NULL, NULL, '风寒感冒', '李医生', '中医科', NULL, '川芎、熟地黄、白芍', 7, 1, 200, 0, '水煎服', NULL, NULL, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'MANUAL'),
(900000008, 2, 'DEMO-RX-20260506-0008', '黄磊', '13800138008', 0, 'OUT0008', NULL, NULL, '风寒感冒', '王医生', '康复科', NULL, '熟地黄、白芍、桂枝', 7, 1, 200, 0, '水煎服', NULL, NULL, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'MANUAL'),
(900000009, 2, 'DEMO-RX-20260506-0009', '周杰', '13800138009', 0, 'OUT0009', NULL, NULL, '风寒感冒', '刘医生', '内科', NULL, '白芍、桂枝', 7, 1, 200, 0, '水煎服', NULL, NULL, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'MANUAL'),
(900000010, 2, 'DEMO-RX-20260506-0010', '吴倩', '13800138010', 0, 'OUT0010', NULL, NULL, '风寒感冒', '陈医生', '外科', NULL, '桂枝', 7, 1, 200, 0, '水煎服', NULL, NULL, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'MANUAL'),
(900000011, 2, 'DEMO-RX-20260506-0011', '徐磊', '13800138011', 0, 'OUT0011', NULL, NULL, '风寒感冒', '张医生', '妇科', NULL, '黄芪、当归、党参', 7, 1, 200, 0, '水煎服', NULL, NULL, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'MANUAL'),
(900000012, 2, 'DEMO-RX-20260506-0012', '孙丽', '13800138012', 0, 'OUT0012', NULL, NULL, '风寒感冒', '李医生', '儿科', NULL, '当归、党参、白术', 7, 1, 200, 0, '水煎服', NULL, NULL, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'MANUAL'),
(900000013, 2, 'DEMO-RX-20260506-0013', '马超', '13800138013', 0, 'OUT0013', NULL, NULL, '风寒感冒', '王医生', '骨科', NULL, '党参、白术、茯苓', 7, 1, 200, 0, '水煎服', NULL, NULL, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'MANUAL'),
(900000014, 2, 'DEMO-RX-20260506-0014', '朱琳', '13800138014', 0, 'OUT0014', NULL, NULL, '风寒感冒', '刘医生', '肿瘤科', NULL, '白术、茯苓、甘草', 7, 1, 200, 0, '水煎服', NULL, NULL, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'MANUAL'),
(900000015, 2, 'DEMO-RX-20260506-0015', '胡军', '13800138015', 0, 'OUT0015', NULL, NULL, '风寒感冒', '陈医生', '中医科', NULL, '茯苓、甘草、川芎', 7, 1, 200, 0, '水煎服', NULL, NULL, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'MANUAL'),
(900000016, 2, 'DEMO-RX-20260506-0016', '郭鑫', '13800138016', 0, 'OUT0016', NULL, NULL, '风寒感冒', '张医生', '康复科', NULL, '甘草、川芎、熟地黄', 7, 1, 200, 0, '水煎服', NULL, NULL, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'MANUAL'),
(900000017, 2, 'DEMO-RX-20260506-0017', '何静', '13800138017', 0, 'OUT0017', NULL, NULL, '风寒感冒', '李医生', '内科', NULL, '川芎、熟地黄、白芍', 7, 1, 200, 0, '水煎服', NULL, NULL, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'MANUAL'),
(900000018, 2, 'DEMO-RX-20260506-0018', '高明', '13800138018', 0, 'OUT0018', NULL, NULL, '风寒感冒', '王医生', '外科', NULL, '熟地黄、白芍、桂枝', 7, 1, 200, 0, '水煎服', NULL, NULL, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'MANUAL'),
(900000019, 2, 'DEMO-RX-20260506-0019', '林涛', '13800138019', 0, 'OUT0019', NULL, NULL, '风寒感冒', '刘医生', '妇科', NULL, '白芍、桂枝', 7, 1, 200, 0, '水煎服', NULL, NULL, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'MANUAL'),
(900000020, 2, 'DEMO-RX-20260506-0020', '郑洁', '13800138020', 0, 'OUT0020', NULL, NULL, '风寒感冒', '陈医生', '儿科', NULL, '桂枝', 7, 1, 200, 0, '水煎服', NULL, NULL, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'MANUAL'),
(900000021, 2, 'DEMO-RX-20260506-0021', '谢峰', '13800138021', 0, 'OUT0021', NULL, NULL, '风寒感冒', '张医生', '骨科', NULL, '黄芪、当归、党参', 7, 1, 200, 0, '水煎服', NULL, NULL, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'MANUAL'),
(900000022, 2, 'DEMO-RX-20260506-0022', '韩梅', '13800138022', 0, 'OUT0022', NULL, NULL, '风寒感冒', '李医生', '肿瘤科', NULL, '当归、党参、白术', 7, 1, 200, 0, '水煎服', NULL, NULL, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'MANUAL'),
(900000023, 2, 'DEMO-RX-20260506-0023', '唐勇', '13800138023', 0, 'OUT0023', NULL, NULL, '风寒感冒', '王医生', '中医科', NULL, '党参、白术、茯苓', 7, 1, 200, 0, '水煎服', NULL, NULL, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'MANUAL'),
(900000024, 2, 'DEMO-RX-20260506-0024', '曹颖', '13800138024', 0, 'OUT0024', NULL, NULL, '风寒感冒', '刘医生', '康复科', NULL, '白术、茯苓、甘草', 7, 1, 200, 0, '水煎服', NULL, NULL, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'MANUAL'),
(900000026, 2, 'DEMO-RX-20260506-0026', '吴丽', '13800138026', 0, 'OUT0026', NULL, NULL, '风寒感冒', '李医生', '外科', NULL, '当归、党参', 7, 1, 200, 0, '水煎服', NULL, NULL, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'MANUAL'),
(900000027, 2, 'DEMO-RX-20260506-0027', '郑强', '13800138027', 0, 'OUT0027', NULL, NULL, '风寒感冒', '王医生', '妇科', NULL, '党参、白术', 7, 1, 200, 0, '水煎服', NULL, NULL, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'MANUAL'),
(900000028, 2, 'DEMO-RX-20260506-0028', '孙芳', '13800138028', 0, 'OUT0028', NULL, NULL, '风寒感冒', '刘医生', '儿科', NULL, '白术、茯苓', 7, 1, 200, 0, '水煎服', NULL, NULL, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'MANUAL');
DELETE FROM prod_task WHERE id >= 900000000;
INSERT INTO prod_task (id, prescription_id, decoct_device_id, package_device_id, scheme_id, scheme_snapshot, decoction_plan, operator_id, operator_name, status, suspended_from, suspend_reason, suspend_time, expected_resume_time, current_temp, target_temp, soak_duration, soak_start_time, soak_end_time, decoct_start_time, decoct_end_time, pour_start_time, pour_end_time, wrap_start_time, wrap_end_time, complete_time, current_stage_duration, print_device_id, print_status, print_time, deleted, created_at, updated_at, current_step, pool_id, print_copies, is_exception, is_emergency, priority, exception_reason, patient_agreement, standard_cost, actual_cost, handover_type, handover_user, handover_time, tenant_id, status_enum, barcode) VALUES
(900000001, 900000001, 1, 5, NULL, NULL, NULL, NULL, NULL, 'WAIT_SOAK', NULL, NULL, NULL, NULL, 0.00, 100.00, 30, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, 'WAIT_SOAK', NULL, 0, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'WAIT_SOAK', NULL),
(900000002, 900000002, 2, 6, NULL, NULL, NULL, NULL, NULL, 'WAIT_SOAK', NULL, NULL, NULL, NULL, 0.00, 100.00, 30, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, 'WAIT_SOAK', NULL, 0, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'WAIT_SOAK', NULL),
(900000003, 900000003, 3, 7, NULL, NULL, NULL, NULL, NULL, 'WAIT_SOAK', NULL, NULL, NULL, NULL, 0.00, 100.00, 30, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, 'WAIT_SOAK', NULL, 0, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'WAIT_SOAK', NULL),
(900000004, 900000004, 4, 5, NULL, NULL, NULL, NULL, NULL, 'WAIT_SOAK', NULL, NULL, NULL, NULL, 0.00, 100.00, 30, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, 'WAIT_SOAK', NULL, 0, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'WAIT_SOAK', NULL),
(900000005, 900000005, 774, 6, NULL, NULL, NULL, NULL, NULL, 'SOAKING', NULL, NULL, NULL, NULL, 0.00, 100.00, 30, '2026-05-06 07:40:00', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, 'WAIT_SOAK', NULL, 0, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'SOAKING', NULL),
(900000006, 900000006, 1, 7, NULL, NULL, NULL, NULL, NULL, 'SOAKING', NULL, NULL, NULL, NULL, 0.00, 100.00, 30, '2026-05-06 07:40:00', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, 'WAIT_SOAK', NULL, 0, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'SOAKING', NULL),
(900000007, 900000007, 2, 5, NULL, NULL, NULL, NULL, NULL, 'SOAKING', NULL, NULL, NULL, NULL, 0.00, 100.00, 30, '2026-05-06 07:40:00', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, 'WAIT_SOAK', NULL, 0, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'SOAKING', NULL),
(900000008, 900000008, 3, 6, NULL, NULL, NULL, NULL, NULL, 'SOAKING', NULL, NULL, NULL, NULL, 0.00, 100.00, 30, '2026-05-06 07:40:00', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, 'WAIT_SOAK', NULL, 0, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'SOAKING', NULL),
(900000009, 900000009, 4, 5, NULL, NULL, NULL, NULL, NULL, 'DECOCTING', NULL, NULL, NULL, NULL, 0.00, 100.00, 30, '2026-05-06 07:40:00', '2026-05-06 07:30:00', '2026-05-06 08:00:00', NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, 'WAIT_SOAK', NULL, 0, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'DECOCTING', NULL),
(900000010, 900000010, 774, 6, NULL, NULL, NULL, NULL, NULL, 'DECOCTING', NULL, NULL, NULL, NULL, 0.00, 100.00, 30, '2026-05-06 07:40:00', '2026-05-06 07:30:00', '2026-05-06 08:00:00', NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, 'WAIT_SOAK', NULL, 0, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'DECOCTING', NULL),
(900000011, 900000011, 1, 7, NULL, NULL, NULL, NULL, NULL, 'DECOCTING', NULL, NULL, NULL, NULL, 0.00, 100.00, 30, '2026-05-06 07:40:00', '2026-05-06 07:30:00', '2026-05-06 08:00:00', NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, 'WAIT_SOAK', NULL, 0, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'DECOCTING', NULL),
(900000012, 900000012, 2, 5, NULL, NULL, NULL, NULL, NULL, 'DECOCTING', NULL, NULL, NULL, NULL, 0.00, 100.00, 30, '2026-05-06 07:40:00', '2026-05-06 07:30:00', '2026-05-06 08:00:00', NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, 'WAIT_SOAK', NULL, 0, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'DECOCTING', NULL),
(900000013, 900000013, 3, 6, NULL, NULL, NULL, NULL, NULL, 'DECOCTING', NULL, NULL, NULL, NULL, 0.00, 100.00, 30, '2026-05-06 07:40:00', '2026-05-06 07:30:00', '2026-05-06 08:00:00', NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, 'WAIT_SOAK', NULL, 0, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'DECOCTING', NULL),
(900000014, 900000014, 4, 7, NULL, NULL, NULL, NULL, NULL, 'DECOCTING', NULL, NULL, NULL, NULL, 0.00, 100.00, 30, '2026-05-06 07:40:00', '2026-05-06 07:30:00', '2026-05-06 08:00:00', NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, 'WAIT_SOAK', NULL, 0, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'DECOCTING', NULL),
(900000015, 900000015, 774, 5, NULL, NULL, NULL, NULL, NULL, 'WRAPPING', NULL, NULL, NULL, NULL, 0.00, 100.00, 30, '2026-05-06 07:40:00', '2026-05-06 07:30:00', '2026-05-06 08:00:00', NULL, NULL, NULL, '2026-05-06 07:30:00', '2026-05-06 08:00:00', NULL, 0, NULL, 'WAIT_SOAK', NULL, 0, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'WRAPPING', NULL),
(900000016, 900000016, 1, 6, NULL, NULL, NULL, NULL, NULL, 'WRAPPING', NULL, NULL, NULL, NULL, 0.00, 100.00, 30, '2026-05-06 07:40:00', '2026-05-06 07:30:00', '2026-05-06 08:00:00', NULL, NULL, NULL, '2026-05-06 07:30:00', '2026-05-06 08:00:00', NULL, 0, NULL, 'WAIT_SOAK', NULL, 0, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'WRAPPING', NULL),
(900000017, 900000017, 2, 5, NULL, NULL, NULL, NULL, NULL, 'WRAPPING', NULL, NULL, NULL, NULL, 0.00, 100.00, 30, '2026-05-06 07:40:00', '2026-05-06 07:30:00', '2026-05-06 08:00:00', NULL, NULL, NULL, '2026-05-06 07:30:00', '2026-05-06 08:00:00', NULL, 0, NULL, 'WAIT_SOAK', NULL, 0, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'WRAPPING', NULL),
(900000018, 900000018, 3, 6, NULL, NULL, NULL, NULL, NULL, 'WAIT_QC', NULL, NULL, NULL, NULL, 0.00, 100.00, 30, '2026-05-06 07:40:00', '2026-05-06 07:30:00', '2026-05-06 08:00:00', NULL, '2026-05-06 07:30:00', '2026-05-06 08:00:00', '2026-05-06 07:30:00', '2026-05-06 08:00:00', NULL, 0, NULL, 'WAIT_SOAK', NULL, 0, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'WAIT_QC', NULL),
(900000019, 900000019, 4, 7, NULL, NULL, NULL, NULL, NULL, 'WAIT_QC', NULL, NULL, NULL, NULL, 0.00, 100.00, 30, '2026-05-06 07:40:00', '2026-05-06 07:30:00', '2026-05-06 08:00:00', NULL, '2026-05-06 07:30:00', '2026-05-06 08:00:00', '2026-05-06 07:30:00', '2026-05-06 08:00:00', NULL, 0, NULL, 'WAIT_SOAK', NULL, 0, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'WAIT_QC', NULL),
(900000020, 900000020, 774, 5, NULL, NULL, NULL, NULL, NULL, 'WAIT_QC', NULL, NULL, NULL, NULL, 0.00, 100.00, 30, '2026-05-06 07:40:00', '2026-05-06 07:30:00', '2026-05-06 08:00:00', NULL, '2026-05-06 07:30:00', '2026-05-06 08:00:00', '2026-05-06 07:30:00', '2026-05-06 08:00:00', NULL, 0, NULL, 'WAIT_SOAK', NULL, 0, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'WAIT_QC', NULL),
(900000021, 900000021, 1, 6, NULL, NULL, NULL, NULL, NULL, 'COMPLETED', NULL, NULL, NULL, NULL, 0.00, 100.00, 30, '2026-05-06 07:40:00', '2026-05-06 07:30:00', '2026-05-06 08:00:00', NULL, '2026-05-06 07:30:00', '2026-05-06 08:00:00', '2026-05-06 07:30:00', '2026-05-06 08:00:00', '2026-05-05 08:00:00', 0, NULL, 'WAIT_SOAK', NULL, 0, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'COMPLETED', NULL),
(900000022, 900000022, 2, 7, NULL, NULL, NULL, NULL, NULL, 'COMPLETED', NULL, NULL, NULL, NULL, 0.00, 100.00, 30, '2026-05-06 07:40:00', '2026-05-06 07:30:00', '2026-05-06 08:00:00', NULL, '2026-05-06 07:30:00', '2026-05-06 08:00:00', '2026-05-06 07:30:00', '2026-05-06 08:00:00', '2026-05-05 08:00:00', 0, NULL, 'WAIT_SOAK', NULL, 0, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'COMPLETED', NULL),
(900000023, 900000023, 3, 5, NULL, NULL, NULL, NULL, NULL, 'COMPLETED', NULL, NULL, NULL, NULL, 0.00, 100.00, 30, '2026-05-06 07:40:00', '2026-05-06 07:30:00', '2026-05-06 08:00:00', NULL, '2026-05-06 07:30:00', '2026-05-06 08:00:00', '2026-05-06 07:30:00', '2026-05-06 08:00:00', '2026-05-05 08:00:00', 0, NULL, 'WAIT_SOAK', NULL, 0, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'COMPLETED', NULL),
(900000024, 900000024, 4, 6, NULL, NULL, NULL, NULL, NULL, 'COMPLETED', NULL, NULL, NULL, NULL, 0.00, 100.00, 30, '2026-05-06 07:40:00', '2026-05-06 07:30:00', '2026-05-06 08:00:00', NULL, '2026-05-06 07:30:00', '2026-05-06 08:00:00', '2026-05-06 07:30:00', '2026-05-06 08:00:00', '2026-05-05 08:00:00', 0, NULL, 'WAIT_SOAK', NULL, 0, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'COMPLETED', NULL),
(900000025, 900000025, 5, 1, NULL, NULL, NULL, NULL, NULL, 'WAIT_DECOCT', NULL, NULL, NULL, NULL, 0.00, 100.00, 30, '2026-05-06 07:20:00', '2026-05-06 07:30:00', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, 'WAIT_SOAK', NULL, 0, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'WAIT_DECOCT', NULL),
(900000026, 900000026, 6, 2, NULL, NULL, NULL, NULL, NULL, 'WAIT_POUR', NULL, NULL, NULL, NULL, 0.00, 100.00, 30, '2026-05-06 07:20:00', '2026-05-06 07:30:00', '2026-05-06 07:30:00', '2026-05-06 08:00:00', NULL, NULL, NULL, NULL, NULL, 0, NULL, 'WAIT_SOAK', NULL, 0, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'WAIT_POUR', NULL),
(900000027, 900000027, 7, 3, NULL, NULL, NULL, NULL, NULL, 'POURING', NULL, NULL, NULL, NULL, 0.00, 100.00, 30, '2026-05-06 07:20:00', '2026-05-06 07:30:00', '2026-05-06 07:30:00', '2026-05-06 08:00:00', '2026-05-06 08:00:00', NULL, NULL, NULL, NULL, 0, NULL, 'WAIT_SOAK', NULL, 0, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'POURING', NULL),
(900000028, 900000028, 774, 4, NULL, NULL, NULL, NULL, NULL, 'WAIT_LABEL', NULL, NULL, NULL, NULL, 0.00, 100.00, 30, '2026-05-06 07:20:00', '2026-05-06 07:30:00', '2026-05-06 07:30:00', '2026-05-06 08:00:00', '2026-05-06 08:00:00', '2026-05-06 08:10:00', '2026-05-06 08:10:00', '2026-05-06 08:20:00', NULL, 0, NULL, 'WAIT_SOAK', NULL, 0, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'WAIT_LABEL', NULL);

-- --------------------------------------------------
-- 3. 任务分配
-- --------------------------------------------------
DELETE FROM dms_task_assignment WHERE task_id >= 900000001;

INSERT INTO dms_task_assignment (id, task_id, prescription_id, device_id, employee_id, assign_type, assign_reason, scheduled_start_time, scheduled_end_time, actual_start_time, actual_end_time, status, stage_breakdown_json, created_at, updated_at) VALUES
(900000001, 900000001, 900000001, 1, 1, 1, '自动分配', NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(900000002, 900000002, 900000002, 2, 1, 1, '自动分配', NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(900000003, 900000003, 900000003, 3, 1, 1, '自动分配', NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(900000004, 900000004, 900000004, 4, 1, 1, '自动分配', NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(900000005, 900000005, 900000005, 774, 1, 1, '自动分配', NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(900000006, 900000006, 900000006, 1, 1, 1, '自动分配', NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(900000007, 900000007, 900000007, 2, 1, 1, '自动分配', NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(900000008, 900000008, 900000008, 3, 1, 1, '自动分配', NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(900000009, 900000009, 900000009, 4, 1, 2, '自动分配', NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(900000010, 900000010, 900000010, 774, 1, 2, '自动分配', NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(900000011, 900000011, 900000011, 1, 1, 2, '自动分配', NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(900000012, 900000012, 900000012, 2, 1, 2, '自动分配', NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(900000013, 900000013, 900000013, 3, 1, 2, '自动分配', NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(900000014, 900000014, 900000014, 4, 1, 2, '自动分配', NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(900000015, 900000015, 900000015, 774, 1, 2, '自动分配', NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(900000016, 900000016, 900000016, 1, 1, 2, '自动分配', NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(900000017, 900000017, 900000017, 2, 1, 3, '自动分配', NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(900000018, 900000018, 900000018, 3, 1, 3, '自动分配', NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(900000019, 900000019, 900000019, 4, 1, 3, '自动分配', NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(900000020, 900000020, 900000020, 774, 1, 3, '自动分配', NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(900000021, 900000021, 900000021, 1, 1, 3, '自动分配', NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(900000022, 900000022, 900000022, 2, 1, 3, '自动分配', NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(900000023, 900000023, 900000023, 3, 1, 3, '自动分配', NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(900000024, 900000024, 900000024, 4, 1, 3, '自动分配', NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL);

-- --------------------------------------------------
-- 4. 时效监控
-- --------------------------------------------------
DELETE FROM dms_time_monitor WHERE task_id >= 900000001;

INSERT INTO dms_time_monitor (id, task_id, assignment_id, prescription_id, stage, planned_start, planned_end, actual_start, actual_end, remaining_seconds, status, warning_count, last_warning_time, created_at, updated_at, alert_level) VALUES
(900000001, 900000001, 900000001, 900000001, 'RECEIVE', '2026-05-06 06:00:00', '2026-05-06 07:00:00', NULL, NULL, 3600, 1, 0, NULL, NOW(), NULL, NULL),
(900000002, 900000002, 900000002, 900000002, 'RECEIVE', '2026-05-06 07:00:00', '2026-05-06 08:00:00', NULL, NULL, 3600, 1, 0, NULL, NOW(), NULL, NULL),
(900000003, 900000003, 900000003, 900000003, 'RECEIVE', '2026-05-06 08:00:00', '2026-05-06 09:00:00', NULL, NULL, 3600, 1, 0, NULL, NOW(), NULL, NULL),
(900000004, 900000004, 900000004, 900000004, 'RECEIVE', '2026-05-06 09:00:00', '2026-05-06 10:00:00', NULL, NULL, 3600, 1, 0, NULL, NOW(), NULL, NULL),
(900000005, 900000005, 900000005, 900000005, 'SOAK', '2026-05-06 10:00:00', '2026-05-06 011:00:00', '2026-05-06 10:30:00', NULL, 1800, 1, 1, NOW(), NULL, NULL, NULL),
(900000006, 900000006, 900000006, 900000006, 'SOAK', '2026-05-06 011:00:00', '2026-05-06 012:00:00', '2026-05-06 011:30:00', NULL, 1800, 1, 1, NOW(), NULL, NULL, NULL),
(900000007, 900000007, 900000007, 900000007, 'SOAK', '2026-05-06 012:00:00', '2026-05-06 013:00:00', '2026-05-06 012:30:00', NULL, 1800, 1, 1, NOW(), NULL, NULL, NULL),
(900000008, 900000008, 900000008, 900000008, 'SOAK', '2026-05-06 013:00:00', '2026-05-06 014:00:00', '2026-05-06 013:30:00', NULL, 1800, 1, 1, NOW(), NULL, NULL, NULL),
(900000009, 900000009, 900000009, 900000009, 'DECOCT', '2026-05-06 06:00:00', '2026-05-06 07:00:00', '2026-05-06 06:30:00', NULL, 1800, 1, 1, NOW(), NULL, NULL, NULL),
(900000010, 900000010, 900000010, 900000010, 'DECOCT', '2026-05-06 07:00:00', '2026-05-06 08:00:00', '2026-05-06 07:30:00', NULL, 1800, 1, 1, NOW(), NULL, NULL, NULL),
(900000011, 900000011, 900000011, 900000011, 'DECOCT', '2026-05-06 08:00:00', '2026-05-06 09:00:00', '2026-05-06 08:30:00', NULL, 1800, 1, 1, NOW(), NULL, NULL, NULL),
(900000012, 900000012, 900000012, 900000012, 'DECOCT', '2026-05-06 09:00:00', '2026-05-06 10:00:00', '2026-05-06 09:30:00', NULL, 1800, 1, 1, NOW(), NULL, NULL, NULL);

-- --------------------------------------------------
-- 5. 预警记录
-- --------------------------------------------------
DELETE FROM dms_alert_log WHERE task_id >= 900000001;

INSERT INTO dms_alert_log (id, monitor_id, task_id, stage, alert_level, alert_type, alert_content, notify_channels, notify_targets, is_resolved, resolved_by, resolved_at, created_at) VALUES
(900000001, 900000001, 900000001, 'RECEIVE', 1, 'DELAY', '处方接收超时', 'SMS,APP', 'OPERATOR', 0, NULL, NULL, NOW()),
(900000002, 900000003, 900000003, 'SOAK', 2, 'APPROACHING', '泡药即将超时', 'APP', 'OPERATOR', 0, NULL, NULL, NOW()),
(900000003, 900000005, 900000005, 'DECOCT', 3, 'OVERTIME', '煎煮已超时30分钟', 'SMS,APP,PHONE', 'ADMIN,OPERATOR', 1, 9, NOW(), NOW()),
(900000004, 900000007, 900000007, 'DECOCT', 2, 'APPROACHING', '煎煮即将超时', 'APP', 'OPERATOR', 0, NULL, NULL, NOW()),
(900000005, 900000009, 900000009, 'PACKAGE', 1, 'DELAY', '包装延迟', 'APP', 'OPERATOR', 0, NULL, NULL, NOW()),
(900000006, 900000011, 900000011, 'QC', 2, 'QUALITY', '质检不合格', 'APP', 'QC,OPERATOR', 1, 4, NOW(), NOW()),
(900000007, 900000012, 900000012, 'COMPLETE', 0, 'INFO', '任务已完成', 'APP', 'OPERATOR', 1, NULL, NOW(), NOW());

-- --------------------------------------------------
-- 6. 设备告警
-- --------------------------------------------------
DELETE FROM eq_device_alarm WHERE id >= 900000001;

INSERT INTO eq_device_alarm (id, device_id, alarm_type, alarm_level, message, status, resolved_time, is_resolved, resolved_at, deleted, created_at, updated_at, tenant_id) VALUES
(900000001, 1, 'HIGH_TEMP', 'CRITICAL', '煎药机温度过高: 125°C', 1, NULL, 0, NULL, 0, NOW(), NULL, NULL),
(900000002, 2, 'LOW_WATER', 'WARNING', '煎药机水位过低', 1, NULL, 0, NULL, 0, NOW(), NULL, NULL),
(900000003, 3, 'PRESSURE_ERR', 'ERROR', '压力异常: 0.35MPa', 0, NOW(), NULL, NULL, NULL, NULL, NULL, NULL),
(900000004, 774, 'OFFLINE', 'CRITICAL', '设备离线超过10分钟', 1, NULL, 0, NULL, 0, NOW(), NULL, NULL),
(900000005, 5, 'FAULT', 'WARNING', '包装机故障E101', 1, NULL, 0, NULL, 0, NOW(), NULL, NULL),
(900000006, 12, 'MAINTENANCE', 'INFO', '打印机需保养', 0, NOW(), NULL, NULL, NULL, NULL, NULL, NULL);

-- --------------------------------------------------
-- 7. 告警通知
-- --------------------------------------------------
DELETE FROM eq_alarm_notification WHERE id >= 900000001;

INSERT INTO eq_alarm_notification (id, tenant_id, alarm_id, notify_type, notify_target, notify_content, send_status, send_time, error_msg, created_at, updated_at, deleted) VALUES
(900000001, 'default', 900000001, 'IN_APP', 'OPERATOR', '煎药机温度过高: 125°C', 'SENT', NOW(), NULL, NULL, NULL, NULL),
(900000002, 'default', 900000001, 'SMS', 'ADMIN', '煎药机温度过高: 125°C', 'SENT', NOW(), NULL, NULL, NULL, NULL),
(900000003, 'default', 900000002, 'IN_APP', 'OPERATOR', '煎药机水位过低', 'SENT', NOW(), NULL, NULL, NULL, NULL),
(900000004, 'default', 900000004, 'IN_APP', 'ADMIN', '设备离线超过10分钟', 'FAILED', NULL, '网络超时', NOW(), NULL, NULL),
(900000005, 'default', 900000005, 'SMS', 'OPERATOR', '包装机故障E101', 'SENT', NOW(), NULL, NULL, NULL, NULL),
(900000006, 'default', 900000006, 'IN_APP', 'OPERATOR', '打印机需保养', 'SENT', NOW(), NULL, NULL, NULL, NULL);

-- --------------------------------------------------
-- 8. 设备状态快照
-- --------------------------------------------------
DELETE FROM eq_device_status WHERE id >= 900000001;

INSERT INTO eq_device_status (id, device_code, device_type, status, detail_status, current_temp, target_temp, water_level, pressure, prescription_code, scheme_name, operator_id, operator_name, progress_percent, remaining_time, fault_code, fault_message, snapshot_time, tenant_id) VALUES
(900000001, 'SIM-DECOCT-02', 1, 'ONLINE', 'SOAKING', 65.50, 70.00, 3, 0.00, 'DEMO-RX-20260506-0001', '标准煎药方案', 1, '张师傅', 45, 1800, '', '', NOW(), NOW()),
(900000025, 'PDA_001', 1, 'ONLINE', 'IDLE', 25.00, 0.00, 0, 0.00, '', '', 0, '', 0, 0, '', '', NOW(), NOW()),
(900000026, 'DECOCT_001', 1, 'ONLINE', 'FIRST_DECOCTING', 98.50, 100.00, 2, 0.15, 'DEMO-RX-20260506-0003', '标准煎药方案', 3, '李师傅', 65, 1200, '', '', NOW(), NOW()),
(900000027, 'SIM-DECOCT-01', 1, 'ONLINE', 'SECOND_DECOCTING', 96.00, 100.00, 2, 0.12, 'DEMO-RX-20260506-0004', '标准煎药方案', 4, '王师傅', 70, 900, '', '', NOW(), NOW()),
(900000028, 'SIM-DECOCT-TCP-01', 1, 'ONLINE', 'BUSY', 88.00, 90.00, 2, 0.10, 'DEMO-RX-20260506-0005', '标准煎药方案', 5, '刘师傅', 55, 1500, '', '', NOW(), NOW()),
(900000029, 'SIM-WRAP-01', 2, 'ONLINE', 'IDLE', 30.00, 0.00, 0, 0.00, '', '', 0, '', 0, 0, '', '', NOW(), NOW()),
(900000030, 'PACK_001', 2, 'OFFLINE', 'OFFLINE', 0.00, 0.00, 0, 0.00, '', '', 0, '', 0, 0, '', '通信超时', NOW(), NOW()),
(900000031, 'PACK_002', 2, 'ONLINE', 'FAULT', 0.00, 0.00, 0, 0.00, '', '', 0, '', 0, 0, '', 'E101-温度传感器故障', NOW(), NOW()),
(900000032, 'LBL_PRN_001', 3, 'ONLINE', 'MAINTENANCE', 0.00, 0.00, 0, 0.00, '', '', 0, '', 0, 0, '', '', NOW(), NOW()),
(900000033, 'LASER_PRN_001', 4, 'ONLINE', 'READY', 28.00, 0.00, 0, 0.00, '', '', 0, '', 0, 0, '', '', NOW(), NOW());

-- --------------------------------------------------
-- 9. 设备指令
-- --------------------------------------------------
DELETE FROM device_command WHERE id >= 900000001;

INSERT INTO device_command (id, device_code, command_type, command_payload, status, response_payload, retry_count, send_time, ack_time, fail_reason, tenant_id, created_at, updated_at, deleted, command_level, risk_level, require_confirm) VALUES
(900000001, 'SIM-DECOCT-02', 'START_SOAK', '{}', 'ACKED', '{}', 0, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(900000034, 'PDA_001', 'PAUSE', '{}', 'ACKED', '{}', 0, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(900000035, 'DECOCT_001', 'EMERGENCY_STOP', '{}', 'WAIT_SOAK', NULL, 1, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(900000036, 'SIM-DECOCT-01', 'RESUME', '{}', 'ACKED', '{}', 0, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(900000037, 'SIM-DECOCT-TCP-01', 'START_DECOCT', '{}', 'ACKED', '{}', 0, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(900000038, 'SIM-WRAP-01', 'START_PACK', '{}', 'ACKED', '{}', 0, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(900000039, 'PACK_001', 'PAUSE_PRINT', '{}', 'FAILED', NULL, 2, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(900000040, 'PACK_002', 'STOP', '{}', 'ACKED', '{}', 0, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(900000041, 'LBL_PRN_001', 'RESET', '{}', 'ACKED', '{}', 0, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(900000042, 'LASER_PRN_001', 'START_PRINT', '{}', 'ACKED', '{}', 0, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);

-- --------------------------------------------------
-- 10. 设备分组联动规则
-- --------------------------------------------------
DELETE FROM eq_device_group_rule WHERE id >= 900000001;

INSERT INTO eq_device_group_rule (id, group_id, rule_name, trigger_condition, action_type, target_devices, enabled, tenant_id, deleted, created_at, updated_at) VALUES
(900000001, 900000001, '浸泡完成提醒', '{"stage":"SOAK_DONE","durationMin":30}', 'NOTIFY', '[5,6]', 1, 'default', 0, NOW(), NOW()),
(900000043, 900000002, '煎煮超时告警', '{"stage":"DECOCTING","durationMin":60}', 'ALARM', '[1,2,3,4,774]', 1, 'default', 0, NOW(), NOW()),
(900000044, 900000003, '包装完成自动打印', '{"stage":"PACK_DONE","autoPrint":true}', 'COMMAND', '[12,15]', 1, 'default', 0, NOW(), NOW());

-- --------------------------------------------------
-- 11. 设备维保
-- --------------------------------------------------
DELETE FROM eq_device_maintenance WHERE id >= 900000001;

INSERT INTO eq_device_maintenance (id, device_id, maintenance_type, content, parts, cost, operator_id, plan_date, finish_date, status, tenant_id, deleted, created_at, updated_at) VALUES
(900000001, 1, 'MAINTENANCE', '清洗加热管，检查温控器', NULL, 120.00, 1, '2026-05-01', '2026-05-02', 2, 'default', 0, NOW(), NOW()),
(900000045, 2, 'REPAIR', '更换水位传感器', '传感器组件', 85.00, 2, '2026-05-03', '2026-05-03', 2, 'default', 0, NOW(), NOW()),
(900000046, 3, 'MAINTENANCE', '全面清洗与校准', NULL, 200.00, 3, '2026-05-05', NULL, 0, 'default', 0, NOW(), NOW()),
(900000047, 774, 'INSPECTION', '月度巡检，固件升级', NULL, 0.00, 4, '2026-05-06', NULL, 0, 'default', 0, NOW(), NOW()),
(900000048, 5, 'REPAIR', '更换传送带', '传送带组件', 350.00, 5, '2026-05-04', '2026-05-04', 2, 'default', 0, NOW(), NOW()),
(900000049, 12, 'MAINTENANCE', '打印头清洁，导轨润滑', NULL, 80.00, 6, '2026-05-02', '2026-05-02', 2, 'default', 0, NOW(), NOW());

-- --------------------------------------------------
-- 12. 设备清洗
-- --------------------------------------------------
DELETE FROM eq_wash_record WHERE id >= 900000001;

INSERT INTO eq_wash_record (id, device_id, device_code, task_id, prescription_id, wash_type, standard_duration, start_time, end_time, duration_min, result, operator_id, operator_name, remark, tenant_id, deleted, created_at) VALUES
(900000001, 1, 'SIM-DECOCT-02', NULL, NULL, 1, 15, NOW(), NULL, NULL, NULL, 1, '张三', NULL, NULL, NULL, NULL),
(900000050, 2, 'PDA_001', NULL, NULL, 1, 10, NOW(), NULL, NULL, NULL, 1, '张三', NULL, NULL, NULL, NULL),
(900000051, 3, 'DECOCT_001', 900000003, 900000003, 2, 30, NOW(), NULL, NULL, NULL, 1, '张三', NULL, NULL, NULL, NULL),
(900000052, 774, 'SIM-DECOCT-TCP-01', 900000005, 900000005, 2, 30, NOW(), NULL, NULL, NULL, 1, '张三', NULL, NULL, NULL, NULL),
(900000053, 5, 'SIM-WRAP-01', NULL, NULL, 1, 15, NOW(), NULL, NULL, NULL, 1, '张三', NULL, NULL, NULL, NULL),
(900000054, 12, 'LBL_PRN_001', NULL, NULL, 1, 10, NOW(), NULL, NULL, NULL, 1, '张三', NULL, NULL, NULL, NULL);

-- --------------------------------------------------
-- 13. 工作量统计（近7天）
-- --------------------------------------------------
DELETE FROM workload_stat WHERE id >= 900000001;

INSERT INTO workload_stat (id, stat_date, operator_id, operator_name, work_type, task_count, prescription_count, package_count, duration_minutes, efficiency, tenant_id, created_at, updated_at, deleted) VALUES
(900000001, '2026-04-30', 1, '张师傅', 'DECOCT', 15, 12, 45, 360, 85.50, 'default', NOW(), NULL, NULL),
(900000055, '2026-05-01', 2, '李师傅', 'SOAK', 18, 14, 50, 390, 85.50, 'default', NOW(), NULL, NULL),
(900000056, '2026-05-02', 3, '王师傅', 'PACKAGE', 21, 16, 55, 420, 85.50, 'default', NOW(), NULL, NULL),
(900000057, '2026-05-03', 4, '刘师傅', 'QC', 24, 18, 60, 450, 85.50, 'default', NOW(), NULL, NULL),
(900000058, '2026-05-04', 5, '陈师傅', 'DELIVER', 27, 20, 65, 480, 85.50, 'default', NOW(), NULL, NULL),
(900000059, '2026-05-05', 1, '张师傅', 'DECOCT', 30, 22, 70, 510, 85.50, 'default', NOW(), NULL, NULL),
(900000060, '2026-05-06', 2, '李师傅', 'PACKAGE', 33, 24, 75, 540, 85.50, 'default', NOW(), NULL, NULL);

-- --------------------------------------------------
-- 14. 煎煮追溯
-- --------------------------------------------------
DELETE FROM decoction_trace WHERE id >= 900000001;

INSERT INTO decoction_trace (id, prescription_no, patient_name, patient_phone, task_id, decoct_device_code, decoct_device_name, packer_device_code, labeler_device_code, scheme_id, scheme_name, soak_time, pre_decoct_time, first_decoct_time, add_late_time, second_decoct_time, package_time, package_volume, sample_count, receive_time, audit_time, audit_pass_time, dispense_time, review_time, soak_start_time, soak_end_time, pre_decoct_start, pre_decoct_end, first_decoct_start, first_decoct_end, add_late_time_actual, second_decoct_start, second_decoct_end, package_start_time, package_end_time, deliver_time, complete_time, receive_operator, audit_operator, dispense_operator, review_operator, soak_operator, decoct_operator, package_operator, deliver_operator, temp_curve_data, max_temp, avg_temp, water_quality_check, status, exception_reason, exception_handle_result, label_print_count, delivery_no, delivery_company, tenant_id, created_at, updated_at, deleted, batch_no) VALUES
(900000001, 'DEMO-RX-20260506-0001', '张伟', '13800138001', 900000001, 'SIM-DECOCT-02', '朋霖煎药机-01', 'PACK_001', 'LBL_PRN_001', NULL, '标准煎药方案', 30, 0, 45, 0, 0, 10, 200.0, 1, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(900000061, 'DEMO-RX-20260506-0002', '李娜', '13800138002', 900000002, 'PDA_001', '朋霖煎药机-02', 'PACK_001', 'LBL_PRN_001', NULL, '标准煎药方案', 30, 0, 45, 0, 0, 10, 200.0, 1, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(900000062, 'DEMO-RX-20260506-0003', '王芳', '13800138003', 900000003, 'DECOCT_001', '朋霖煎药机-03', 'PACK_001', 'LBL_PRN_001', NULL, '标准煎药方案', 30, 0, 45, 0, 0, 10, 200.0, 1, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(900000063, 'DEMO-RX-20260506-0004', '刘洋', '13800138004', 900000004, 'SIM-DECOCT-01', '威康煎药机-01', 'PACK_001', 'LBL_PRN_001', NULL, '标准煎药方案', 30, 0, 45, 0, 0, 10, 200.0, 1, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(900000064, 'DEMO-RX-20260506-0005', '陈静', '13800138005', 900000005, 'SIM-DECOCT-TCP-01', '信研煎药机-01', 'PACK_001', 'LBL_PRN_001', NULL, '标准煎药方案', 30, 0, 45, 0, 0, 10, 200.0, 1, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(900000065, 'DEMO-RX-20260506-0006', '杨强', '13800138006', 900000006, 'SIM-WRAP-01', '朋霖包装机-01', 'PACK_001', 'LBL_PRN_001', NULL, '标准煎药方案', 30, 0, 45, 0, 0, 10, 200.0, 1, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);

-- --------------------------------------------------
-- 15. 任务状态历史
-- --------------------------------------------------
DELETE FROM prod_task_status_history WHERE task_id >= 900000001;

INSERT INTO prod_task_status_history (id, task_id, from_status, to_status, operator_id, operate_time, remark, tenant_id, trigger_source, device_code) VALUES
(900000001, 900000001, NULL, 'WAIT_SOAK', 'system', NOW(), NULL, NULL, NULL, NULL),
(900000066, 900000002, 'WAIT_SOAK', 'SOAKING', 'operator01', NOW(), NULL, NULL, NULL, NULL),
(900000067, 900000003, 'SOAKING', 'DECOCTING', 'operator01', NOW(), NULL, NULL, NULL, NULL),
(900000068, 900000004, 'DECOCTING', 'WRAPPING', 'operator01', NOW(), NULL, NULL, NULL, NULL),
(900000069, 900000005, 'WRAPPING', 'WAIT_QC', 'operator01', NOW(), NULL, NULL, NULL, NULL),
(900000070, 900000006, 'WAIT_QC', 'COMPLETED', 'operator01', NOW(), NULL, NULL, NULL, NULL),
(900000071, 900000001, NULL, 'WAIT_SOAK', 'system', NOW(), NULL, NULL, NULL, NULL),
(900000072, 900000002, 'WAIT_SOAK', 'SOAKING', 'operator01', NOW(), NULL, NULL, NULL, NULL),
(900000073, 900000003, 'SOAKING', 'DECOCTING', 'operator01', NOW(), NULL, NULL, NULL, NULL),
(900000074, 900000004, 'DECOCTING', 'WRAPPING', 'operator01', NOW(), NULL, NULL, NULL, NULL),
(900000075, 900000005, 'WRAPPING', 'WAIT_QC', 'operator01', NOW(), NULL, NULL, NULL, NULL),
(900000076, 900000006, 'WAIT_QC', 'COMPLETED', 'operator01', NOW(), NULL, NULL, NULL, NULL);

-- --------------------------------------------------
-- 16. 任务步骤日志
-- --------------------------------------------------
DELETE FROM prod_step_log WHERE id >= 900000001;

INSERT INTO prod_step_log (id, task_id, parent_id, step_type, device_id, operator_id, started_at, ended_at, is_paused, pause_reason, pause_duration, delay_minutes, delay_reason, result, abort_reason, waste_amount, waste_unit, is_retry, created_at, updated_at, tenant_id) VALUES
(900000001, 900000001, NULL, 'SOAK', 'PDA_001', 1, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(900000077, 900000003, NULL, 'DECOCT', 'DECOCT_001', 3, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(900000078, 900000005, NULL, 'DECOCT', 'SIM-DECOCT-TCP-01', 5, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(900000079, 900000009, NULL, 'PACKAGE', 'SIM-WRAP-01', 2, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(900000080, 900000015, NULL, 'QC', 'PACK_001', 4, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(900000081, 900000021, NULL, 'DELIVER', 'LASER_PRN_001', 6, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);

-- --------------------------------------------------
-- 17. 设备利用率（10台设备 x 7天）
-- --------------------------------------------------
DELETE FROM device_utilization WHERE tenant_id = 'default';

INSERT INTO device_utilization (id, device_code, stat_date, total_minutes, run_minutes, idle_minutes, fault_minutes, offline_minutes, maintenance_minutes, utilization_rate, availability_rate, fault_count, task_count, tenant_id, created_at, updated_at, deleted) VALUES
(900000001, 'SIM-DECOCT-02', '2026-04-30', 1440, 200, 800, 20, 120, 10, 13.89, 69.44, 1, 4, 'default', NOW(), NULL, NULL),
(900000082, 'PDA_001', '2026-04-30', 1440, 210, 795, 21, 115, 0, 14.58, 69.79, 1, 4, 'default', NOW(), NULL, NULL),
(900000083, 'DECOCT_001', '2026-04-30', 1440, 220, 790, 22, 110, 0, 15.28, 70.14, 1, 4, 'default', NOW(), NULL, NULL),
(900000084, 'SIM-DECOCT-01', '2026-04-30', 1440, 230, 785, 23, 105, 0, 15.97, 70.49, 1, 5, 'default', NOW(), NULL, NULL),
(900000085, 'SIM-DECOCT-TCP-01', '2026-04-30', 1440, 240, 780, 24, 100, 0, 16.67, 70.83, 1, 5, 'default', NOW(), NULL, NULL),
(900000086, 'SIM-WRAP-01', '2026-04-30', 1440, 250, 775, 25, 95, 0, 17.36, 71.18, 1, 5, 'default', NOW(), NULL, NULL),
(900000087, 'PACK_001', '2026-04-30', 1440, 260, 770, 26, 90, 0, 18.06, 71.53, 1, 5, 'default', NOW(), NULL, NULL),
(900000088, 'PACK_002', '2026-04-30', 1440, 270, 765, 27, 85, 0, 18.75, 71.88, 1, 6, 'default', NOW(), NULL, NULL),
(900000089, 'LBL_PRN_001', '2026-04-30', 1440, 280, 760, 28, 80, 0, 19.44, 72.22, 1, 6, 'default', NOW(), NULL, NULL),
(900000090, 'LASER_PRN_001', '2026-04-30', 1440, 290, 755, 29, 75, 0, 20.14, 72.57, 1, 6, 'default', NOW(), NULL, NULL),
(900000091, 'SIM-DECOCT-02', '2026-05-01', 1440, 230, 820, 17, 110, 0, 15.97, 72.92, 1, 5, 'default', NOW(), NULL, NULL),
(900000092, 'PDA_001', '2026-05-01', 1440, 240, 815, 18, 105, 10, 16.67, 73.26, 1, 5, 'default', NOW(), NULL, NULL),
(900000093, 'DECOCT_001', '2026-05-01', 1440, 250, 810, 19, 100, 0, 17.36, 73.61, 1, 5, 'default', NOW(), NULL, NULL),
(900000094, 'SIM-DECOCT-01', '2026-05-01', 1440, 260, 805, 20, 95, 0, 18.06, 73.96, 1, 5, 'default', NOW(), NULL, NULL),
(900000095, 'SIM-DECOCT-TCP-01', '2026-05-01', 1440, 270, 800, 21, 90, 0, 18.75, 74.31, 1, 6, 'default', NOW(), NULL, NULL),
(900000096, 'SIM-WRAP-01', '2026-05-01', 1440, 280, 795, 22, 85, 0, 19.44, 74.65, 1, 6, 'default', NOW(), NULL, NULL),
(900000097, 'PACK_001', '2026-05-01', 1440, 290, 790, 23, 80, 0, 20.14, 75.0, 1, 6, 'default', NOW(), NULL, NULL),
(900000098, 'PACK_002', '2026-05-01', 1440, 300, 785, 24, 75, 0, 20.83, 75.35, 1, 6, 'default', NOW(), NULL, NULL),
(900000099, 'LBL_PRN_001', '2026-05-01', 1440, 310, 780, 25, 70, 0, 21.53, 75.69, 1, 6, 'default', NOW(), NULL, NULL),
(900000100, 'LASER_PRN_001', '2026-05-01', 1440, 320, 775, 26, 65, 0, 22.22, 76.04, 1, 7, 'default', NOW(), NULL, NULL),
(900000101, 'SIM-DECOCT-02', '2026-05-02', 1440, 260, 840, 14, 100, 0, 18.06, 76.39, 1, 5, 'default', NOW(), NULL, NULL),
(900000102, 'PDA_001', '2026-05-02', 1440, 270, 835, 15, 95, 0, 18.75, 76.74, 1, 6, 'default', NOW(), NULL, NULL),
(900000103, 'DECOCT_001', '2026-05-02', 1440, 280, 830, 16, 90, 10, 19.44, 77.08, 1, 6, 'default', NOW(), NULL, NULL),
(900000104, 'SIM-DECOCT-01', '2026-05-02', 1440, 290, 825, 17, 85, 0, 20.14, 77.43, 1, 6, 'default', NOW(), NULL, NULL),
(900000105, 'SIM-DECOCT-TCP-01', '2026-05-02', 1440, 300, 820, 18, 80, 0, 20.83, 77.78, 1, 6, 'default', NOW(), NULL, NULL),
(900000106, 'SIM-WRAP-01', '2026-05-02', 1440, 310, 815, 19, 75, 0, 21.53, 78.12, 1, 6, 'default', NOW(), NULL, NULL),
(900000107, 'PACK_001', '2026-05-02', 1440, 320, 810, 20, 70, 0, 22.22, 78.47, 1, 7, 'default', NOW(), NULL, NULL),
(900000108, 'PACK_002', '2026-05-02', 1440, 330, 805, 21, 65, 0, 22.92, 78.82, 1, 7, 'default', NOW(), NULL, NULL),
(900000109, 'LBL_PRN_001', '2026-05-02', 1440, 340, 800, 22, 60, 0, 23.61, 79.17, 1, 7, 'default', NOW(), NULL, NULL),
(900000110, 'LASER_PRN_001', '2026-05-02', 1440, 350, 795, 23, 55, 0, 24.31, 79.51, 1, 7, 'default', NOW(), NULL, NULL),
(900000111, 'SIM-DECOCT-02', '2026-05-03', 1440, 290, 860, 11, 90, 0, 20.14, 79.86, 1, 6, 'default', NOW(), NULL, NULL),
(900000112, 'PDA_001', '2026-05-03', 1440, 300, 855, 12, 85, 0, 20.83, 80.21, 1, 6, 'default', NOW(), NULL, NULL),
(900000113, 'DECOCT_001', '2026-05-03', 1440, 310, 850, 13, 80, 0, 21.53, 80.56, 1, 6, 'default', NOW(), NULL, NULL),
(900000114, 'SIM-DECOCT-01', '2026-05-03', 1440, 320, 845, 14, 75, 10, 22.22, 80.9, 1, 7, 'default', NOW(), NULL, NULL),
(900000115, 'SIM-DECOCT-TCP-01', '2026-05-03', 1440, 330, 840, 15, 70, 0, 22.92, 81.25, 1, 7, 'default', NOW(), NULL, NULL),
(900000116, 'SIM-WRAP-01', '2026-05-03', 1440, 340, 835, 16, 65, 0, 23.61, 81.6, 1, 7, 'default', NOW(), NULL, NULL),
(900000117, 'PACK_001', '2026-05-03', 1440, 350, 830, 17, 60, 0, 24.31, 81.94, 1, 7, 'default', NOW(), NULL, NULL),
(900000118, 'PACK_002', '2026-05-03', 1440, 360, 825, 18, 55, 0, 25.0, 82.29, 1, 8, 'default', NOW(), NULL, NULL),
(900000119, 'LBL_PRN_001', '2026-05-03', 1440, 370, 820, 19, 50, 0, 25.69, 82.64, 1, 8, 'default', NOW(), NULL, NULL),
(900000120, 'LASER_PRN_001', '2026-05-03', 1440, 380, 815, 20, 45, 0, 26.39, 82.99, 1, 8, 'default', NOW(), NULL, NULL),
(900000121, 'SIM-DECOCT-02', '2026-05-04', 1440, 320, 880, 8, 80, 0, 22.22, 83.33, 1, 7, 'default', NOW(), NULL, NULL),
(900000122, 'PDA_001', '2026-05-04', 1440, 330, 875, 9, 75, 0, 22.92, 83.68, 1, 7, 'default', NOW(), NULL, NULL),
(900000123, 'DECOCT_001', '2026-05-04', 1440, 340, 870, 10, 70, 0, 23.61, 84.03, 1, 7, 'default', NOW(), NULL, NULL),
(900000124, 'SIM-DECOCT-01', '2026-05-04', 1440, 350, 865, 11, 65, 0, 24.31, 84.38, 1, 7, 'default', NOW(), NULL, NULL),
(900000125, 'SIM-DECOCT-TCP-01', '2026-05-04', 1440, 360, 860, 12, 60, 10, 25.0, 84.72, 1, 8, 'default', NOW(), NULL, NULL),
(900000126, 'SIM-WRAP-01', '2026-05-04', 1440, 370, 855, 13, 55, 0, 25.69, 85.07, 1, 8, 'default', NOW(), NULL, NULL),
(900000127, 'PACK_001', '2026-05-04', 1440, 380, 850, 14, 50, 0, 26.39, 85.42, 1, 8, 'default', NOW(), NULL, NULL),
(900000128, 'PACK_002', '2026-05-04', 1440, 390, 845, 15, 45, 0, 27.08, 85.76, 1, 8, 'default', NOW(), NULL, NULL),
(900000129, 'LBL_PRN_001', '2026-05-04', 1440, 400, 840, 16, 40, 0, 27.78, 86.11, 1, 8, 'default', NOW(), NULL, NULL),
(900000130, 'LASER_PRN_001', '2026-05-04', 1440, 410, 835, 17, 35, 0, 28.47, 86.46, 1, 9, 'default', NOW(), NULL, NULL),
(900000131, 'SIM-DECOCT-02', '2026-05-05', 1440, 350, 900, 5, 70, 0, 24.31, 86.81, 1, 7, 'default', NOW(), NULL, NULL),
(900000132, 'PDA_001', '2026-05-05', 1440, 360, 895, 6, 65, 0, 25.0, 87.15, 1, 8, 'default', NOW(), NULL, NULL),
(900000133, 'DECOCT_001', '2026-05-05', 1440, 370, 890, 7, 60, 0, 25.69, 87.5, 1, 8, 'default', NOW(), NULL, NULL),
(900000134, 'SIM-DECOCT-01', '2026-05-05', 1440, 380, 885, 8, 55, 0, 26.39, 87.85, 1, 8, 'default', NOW(), NULL, NULL),
(900000135, 'SIM-DECOCT-TCP-01', '2026-05-05', 1440, 390, 880, 9, 50, 0, 27.08, 88.19, 1, 8, 'default', NOW(), NULL, NULL),
(900000136, 'SIM-WRAP-01', '2026-05-05', 1440, 400, 875, 10, 45, 10, 27.78, 88.54, 1, 8, 'default', NOW(), NULL, NULL),
(900000137, 'PACK_001', '2026-05-05', 1440, 410, 870, 11, 40, 0, 28.47, 88.89, 1, 9, 'default', NOW(), NULL, NULL),
(900000138, 'PACK_002', '2026-05-05', 1440, 420, 865, 12, 35, 0, 29.17, 89.24, 1, 9, 'default', NOW(), NULL, NULL),
(900000139, 'LBL_PRN_001', '2026-05-05', 1440, 430, 860, 13, 30, 0, 29.86, 89.58, 1, 9, 'default', NOW(), NULL, NULL),
(900000140, 'LASER_PRN_001', '2026-05-05', 1440, 440, 855, 14, 25, 0, 30.56, 89.93, 1, 9, 'default', NOW(), NULL, NULL),
(900000141, 'SIM-DECOCT-02', '2026-05-06', 1440, 380, 920, 2, 60, 0, 26.39, 90.28, 1, 8, 'default', NOW(), NULL, NULL),
(900000142, 'PDA_001', '2026-05-06', 1440, 390, 915, 3, 55, 0, 27.08, 90.62, 1, 8, 'default', NOW(), NULL, NULL),
(900000143, 'DECOCT_001', '2026-05-06', 1440, 400, 910, 4, 50, 0, 27.78, 90.97, 1, 8, 'default', NOW(), NULL, NULL),
(900000144, 'SIM-DECOCT-01', '2026-05-06', 1440, 410, 905, 5, 45, 0, 28.47, 91.32, 1, 9, 'default', NOW(), NULL, NULL),
(900000145, 'SIM-DECOCT-TCP-01', '2026-05-06', 1440, 420, 900, 6, 40, 0, 29.17, 91.67, 1, 9, 'default', NOW(), NULL, NULL),
(900000146, 'SIM-WRAP-01', '2026-05-06', 1440, 430, 895, 7, 35, 0, 29.86, 92.01, 1, 9, 'default', NOW(), NULL, NULL),
(900000147, 'PACK_001', '2026-05-06', 1440, 440, 890, 8, 30, 10, 30.56, 92.36, 1, 9, 'default', NOW(), NULL, NULL),
(900000148, 'PACK_002', '2026-05-06', 1440, 450, 885, 9, 25, 0, 31.25, 92.71, 1, 10, 'default', NOW(), NULL, NULL),
(900000149, 'LBL_PRN_001', '2026-05-06', 1440, 460, 880, 10, 20, 0, 31.94, 93.06, 1, 10, 'default', NOW(), NULL, NULL),
(900000150, 'LASER_PRN_001', '2026-05-06', 1440, 470, 875, 11, 15, 0, 32.64, 93.4, 1, 10, 'default', NOW(), NULL, NULL);

-- --------------------------------------------------
-- 18. 处方药材明细
-- --------------------------------------------------
DELETE FROM prod_prescription_medicine WHERE prescription_id >= 900000001;

INSERT INTO prod_prescription_medicine (id, prescription_id, medicine_id, hospital_code, hospital_name, medicine_name, dosage, unit, med_usage, decoction_method, batch_no, special_decoction, is_toxic, requires_retain, retain_quantity, sort_order, tenant_id, deleted, created_at) VALUES
(900000151, 900000001, 1, 'HOSP002', '市中医院', '黄芪', '10.0', 'g', '水煎服', '常规煎煮', 'BATCH20260506001', NULL, 0, 0, NULL, 1, 'default', 0, NOW()),
(900000152, 900000002, 2, 'HOSP002', '市中医院', '当归', '10.0', 'g', '水煎服', '常规煎煮', 'BATCH20260506002', NULL, 0, 0, NULL, 1, 'default', 0, NOW()),
(900000153, 900000003, 3, 'HOSP002', '市中医院', '党参', '10.0', 'g', '水煎服', '常规煎煮', 'BATCH20260506003', NULL, 0, 0, NULL, 1, 'default', 0, NOW()),
(900000154, 900000004, 4, 'HOSP002', '市中医院', '白术', '10.0', 'g', '水煎服', '常规煎煮', 'BATCH20260506004', NULL, 0, 0, NULL, 1, 'default', 0, NOW()),
(900000155, 900000005, 5, 'HOSP002', '市中医院', '茯苓', '10.0', 'g', '水煎服', '常规煎煮', 'BATCH20260506005', NULL, 0, 0, NULL, 1, 'default', 0, NOW()),
(900000156, 900000006, 6, 'HOSP002', '市中医院', '甘草', '10.0', 'g', '水煎服', '常规煎煮', 'BATCH20260506006', NULL, 0, 0, NULL, 1, 'default', 0, NOW()),
(900000157, 900000007, 7, 'HOSP002', '市中医院', '川芎', '10.0', 'g', '水煎服', '常规煎煮', 'BATCH20260506007', NULL, 0, 0, NULL, 1, 'default', 0, NOW()),
(900000158, 900000008, 8, 'HOSP002', '市中医院', '熟地黄', '10.0', 'g', '水煎服', '常规煎煮', 'BATCH20260506008', NULL, 0, 0, NULL, 1, 'default', 0, NOW()),
(900000159, 900000009, 9, 'HOSP002', '市中医院', '黄芪', '10.0', 'g', '水煎服', '常规煎煮', 'BATCH20260506009', NULL, 0, 0, NULL, 1, 'default', 0, NOW()),
(900000160, 900000010, 10, 'HOSP002', '市中医院', '当归', '10.0', 'g', '水煎服', '常规煎煮', 'BATCH20260506010', NULL, 0, 0, NULL, 1, 'default', 0, NOW()),
(900000161, 900000011, 11, 'HOSP002', '市中医院', '党参', '10.0', 'g', '水煎服', '常规煎煮', 'BATCH20260506011', NULL, 0, 0, NULL, 1, 'default', 0, NOW()),
(900000162, 900000012, 12, 'HOSP002', '市中医院', '白术', '10.0', 'g', '水煎服', '常规煎煮', 'BATCH20260506012', NULL, 0, 0, NULL, 1, 'default', 0, NOW()),
(900000163, 900000013, 13, 'HOSP002', '市中医院', '茯苓', '10.0', 'g', '水煎服', '常规煎煮', 'BATCH20260506013', NULL, 0, 0, NULL, 1, 'default', 0, NOW()),
(900000164, 900000014, 14, 'HOSP002', '市中医院', '甘草', '10.0', 'g', '水煎服', '常规煎煮', 'BATCH20260506014', NULL, 0, 0, NULL, 1, 'default', 0, NOW()),
(900000165, 900000015, 15, 'HOSP002', '市中医院', '川芎', '10.0', 'g', '水煎服', '常规煎煮', 'BATCH20260506015', NULL, 0, 0, NULL, 1, 'default', 0, NOW()),
(900000166, 900000016, 16, 'HOSP002', '市中医院', '熟地黄', '10.0', 'g', '水煎服', '常规煎煮', 'BATCH20260506016', NULL, 0, 0, NULL, 1, 'default', 0, NOW()),
(900000167, 900000017, 17, 'HOSP002', '市中医院', '黄芪', '10.0', 'g', '水煎服', '常规煎煮', 'BATCH20260506017', NULL, 0, 0, NULL, 1, 'default', 0, NOW()),
(900000168, 900000018, 18, 'HOSP002', '市中医院', '当归', '10.0', 'g', '水煎服', '常规煎煮', 'BATCH20260506018', NULL, 0, 0, NULL, 1, 'default', 0, NOW()),
(900000169, 900000019, 19, 'HOSP002', '市中医院', '党参', '10.0', 'g', '水煎服', '常规煎煮', 'BATCH20260506019', NULL, 0, 0, NULL, 1, 'default', 0, NOW()),
(900000170, 900000020, 20, 'HOSP002', '市中医院', '白术', '10.0', 'g', '水煎服', '常规煎煮', 'BATCH20260506020', NULL, 0, 0, NULL, 1, 'default', 0, NOW()),
(900000171, 900000021, 21, 'HOSP002', '市中医院', '茯苓', '10.0', 'g', '水煎服', '常规煎煮', 'BATCH20260506021', NULL, 0, 0, NULL, 1, 'default', 0, NOW()),
(900000172, 900000022, 22, 'HOSP002', '市中医院', '甘草', '10.0', 'g', '水煎服', '常规煎煮', 'BATCH20260506022', NULL, 0, 0, NULL, 1, 'default', 0, NOW()),
(900000173, 900000023, 23, 'HOSP002', '市中医院', '川芎', '10.0', 'g', '水煎服', '常规煎煮', 'BATCH20260506023', NULL, 0, 0, NULL, 1, 'default', 0, NOW()),
(900000174, 900000024, 24, 'HOSP002', '市中医院', '熟地黄', '10.0', 'g', '水煎服', '常规煎煮', 'BATCH20260506024', NULL, 0, 0, NULL, 1, 'default', 0, NOW());

SET FOREIGN_KEY_CHECKS = 1;
