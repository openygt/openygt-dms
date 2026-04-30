-- 扩展处方表，支持接收/驳回流程

ALTER TABLE prod_prescription
    ADD COLUMN receive_status VARCHAR(20) DEFAULT 'PENDING' COMMENT '接收状态: PENDING待接收/RECEIVED已接收/REJECTED已驳回',
    ADD COLUMN reject_type VARCHAR(50) COMMENT '驳回类型: LACK_DRUG缺药/NOT_DECOCTION不煎药/OTHER其他',
    ADD COLUMN reject_reason VARCHAR(500) COMMENT '驳回原因',
    ADD COLUMN received_at DATETIME COMMENT '接收时间',
    ADD COLUMN rejected_at DATETIME COMMENT '驳回时间',
    ADD COLUMN task_no VARCHAR(50) COMMENT '关联工单号',
    ADD COLUMN operator_id BIGINT COMMENT '操作人ID',
    ADD COLUMN operator_name VARCHAR(50) COMMENT '操作人姓名';

CREATE INDEX idx_receive_status ON prod_prescription(receive_status);
CREATE INDEX idx_task_no ON prod_prescription(task_no);
