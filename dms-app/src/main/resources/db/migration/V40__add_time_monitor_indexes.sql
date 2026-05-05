-- 时效监控模块索引优化（评审整改）
-- 覆盖 getEffectiveMonitors() 的 WHERE 条件与排序需求

-- dms_time_monitor: 支持 task_id 去重与 updated_at 排序
CREATE INDEX idx_tm_task_updated ON dms_time_monitor (task_id, updated_at DESC);

-- dms_time_monitor: 支持 actual_start/actual_end 组合过滤
CREATE INDEX idx_tm_actual_end ON dms_time_monitor (actual_start, actual_end);

-- dms_alert_log: 支持 is_resolved + monitor_id 组合查询（resolveAlert LIMIT 1）
CREATE INDEX idx_al_resolved_monitor ON dms_alert_log (is_resolved, monitor_id);
