package cn.org.openygt.equipment.service;

import java.util.List;
import java.util.Map;

public interface DashboardService {

    Map<String, Object> getMetrics();

    List<Map<String, Object>> getStageDistribution();

    List<Map<String, Object>> getWorkerEfficiency(String date);

    List<Map<String, Object>> getHourlyTrend(String date);

    List<Map<String, Object>> getDeviceUtilization(String date);
}
