-- ========== 质检表索引优化 ==========
-- 使用 prepared statement 实现幂等（兼容不支持 DROP INDEX IF EXISTS 的 MySQL 版本）

SET @sql = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'qt_inspection' AND index_name = 'idx_qi_task_id') = 0, 'CREATE INDEX idx_qi_task_id ON qt_inspection(task_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'qt_inspection' AND index_name = 'idx_qi_result') = 0, 'CREATE INDEX idx_qi_result ON qt_inspection(result)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'qt_inspection' AND index_name = 'idx_qi_inspected_at') = 0, 'CREATE INDEX idx_qi_inspected_at ON qt_inspection(inspected_at)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'qt_inspection' AND index_name = 'idx_qi_result_inspected') = 0, 'CREATE INDEX idx_qi_result_inspected ON qt_inspection(result, inspected_at)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'qt_inspection_item' AND index_name = 'idx_qii_inspection_id') = 0, 'CREATE INDEX idx_qii_inspection_id ON qt_inspection_item(inspection_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'qt_inspection_item' AND index_name = 'idx_qii_item_code') = 0, 'CREATE INDEX idx_qii_item_code ON qt_inspection_item(item_code)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
