-- 留样类型扩展：4=7天留样（业务上每条任务仅一条，替代原 1+2+3 三条）
ALTER TABLE qt_retain_sample
    MODIFY COLUMN sample_type TINYINT NOT NULL COMMENT '1=质检样(历史) 2=24h(历史) 3=72h(历史) 4=7天留样';
