-- ========== 质检表索引优化 ==========

-- qt_inspection 高频查询索引
ALTER TABLE qt_inspection ADD INDEX idx_qi_task_id (task_id);
ALTER TABLE qt_inspection ADD INDEX idx_qi_result (result);
ALTER TABLE qt_inspection ADD INDEX idx_qi_inspected_at (inspected_at);
ALTER TABLE qt_inspection ADD INDEX idx_qi_result_inspected (result, inspected_at);

-- qt_inspection_item 高频查询索引
ALTER TABLE qt_inspection_item ADD INDEX idx_qii_inspection_id (inspection_id);
ALTER TABLE qt_inspection_item ADD INDEX idx_qii_item_code (item_code);
