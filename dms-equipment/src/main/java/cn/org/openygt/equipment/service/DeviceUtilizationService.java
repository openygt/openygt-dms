package cn.org.openygt.equipment.service;

import cn.org.openygt.equipment.entity.DeviceUtilization;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface DeviceUtilizationService {

    List<DeviceUtilization> queryUtilization(String deviceCode, LocalDate startDate, LocalDate endDate);

    List<Map<String, Object>> getTrend(String deviceCode, int days);

    List<Map<String, Object>> getSummary(LocalDate startDate, LocalDate endDate);

    void generateDailyStats(LocalDate date);
}
