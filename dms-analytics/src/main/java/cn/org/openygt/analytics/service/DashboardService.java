package cn.org.openygt.analytics.service;

import cn.org.openygt.analytics.dto.DashboardRealtimeDTO;

/**
 * 监控大屏服务。
 */
public interface DashboardService {

    /**
     * 获取实时大屏数据。
     */
    DashboardRealtimeDTO getRealtime();
}
