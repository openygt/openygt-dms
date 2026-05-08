-- 设备利用率表查询优化（产能统计评审整改）
-- 覆盖 device_utilization 最常用的查询模式：按设备编码+日期范围查询
CREATE INDEX idx_du_device_date ON device_utilization (device_code, stat_date);
