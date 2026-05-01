-- ========== 质检管理流程化：检查项明细表 ==========
CREATE TABLE IF NOT EXISTS qt_inspection_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    inspection_id BIGINT NOT NULL COMMENT '质检记录ID',
    item_code VARCHAR(32) NOT NULL COMMENT '检查项编码：APPEARANCE/ODOR/DOSE/SEAL/LABEL/SAMPLE',
    item_name VARCHAR(50) NOT NULL COMMENT '检查项名称',
    result VARCHAR(20) NOT NULL COMMENT 'PASS/FAIL/NA',
    actual_value VARCHAR(100) COMMENT '实际值（如剂量ml）',
    remark VARCHAR(500) COMMENT '备注',
    sort_order INT DEFAULT 0 COMMENT '排序',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_inspection_id (inspection_id)
) COMMENT='质检检查项明细';

-- E2E测试数据：为已有的质检记录补充检查项
-- 假设已有质检记录 id=1 (PASS), id=2 (FAIL)
INSERT INTO qt_inspection_item (inspection_id, item_code, item_name, result, actual_value, remark, sort_order) VALUES
(1, 'APPEARANCE', '外观检查', 'PASS', '', '颜色正常，澄清度良好', 1),
(1, 'ODOR', '气味检查', 'PASS', '', '药味正常', 2),
(1, 'DOSE', '剂量检查', 'PASS', '200', '实际200ml，符合标准', 3),
(1, 'SEAL', '密封检查', 'PASS', '', '无漏液', 4),
(1, 'LABEL', '标签核对', 'PASS', '', '处方号、患者名、剂数正确', 5),
(2, 'APPEARANCE', '外观检查', 'PASS', '', '颜色正常', 1),
(2, 'ODOR', '气味检查', 'PASS', '', '药味正常', 2),
(2, 'DOSE', '剂量检查', 'FAIL', '180', '实际180ml，低于标准200ml', 3),
(2, 'SEAL', '密封检查', 'PASS', '', '无漏液', 4),
(2, 'LABEL', '标签核对', 'PASS', '', '标签信息正确', 5);
