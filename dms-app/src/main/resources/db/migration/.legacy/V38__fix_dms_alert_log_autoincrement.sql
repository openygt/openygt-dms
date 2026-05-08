-- V38: 修复时效预警日志主键自增，避免定时任务插入失败

ALTER TABLE dms_alert_log
    MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID';
