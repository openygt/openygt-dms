package cn.org.openygt.equipment.service.impl;

import cn.org.openygt.equipment.entity.DeviceUtilization;
import cn.org.openygt.equipment.mapper.DeviceUtilizationMapper;
import cn.org.openygt.equipment.service.DeviceUtilizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceUtilizationServiceImpl implements DeviceUtilizationService {

    private final DeviceUtilizationMapper utilizationMapper;

    @Override
    public List<DeviceUtilization> queryUtilization(String deviceCode, LocalDate startDate, LocalDate endDate) {
        if (deviceCode != null && !deviceCode.isEmpty()) {
            return utilizationMapper.findByDeviceAndDateRange(deviceCode, startDate, endDate);
        }
        List<DeviceUtilization> result = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            result.addAll(utilizationMapper.findByDate(date));
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getTrend(String deviceCode, int days) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(days - 1);

        List<DeviceUtilization> data = utilizationMapper.findByDeviceAndDateRange(deviceCode, start, end);
        List<Map<String, Object>> result = new ArrayList<>();

        for (DeviceUtilization u : data) {
            Map<String, Object> item = new HashMap<>();
            item.put("date", u.getStatDate() != null ? u.getStatDate().toString() : "");
            item.put("utilization", u.getUtilizationRate() != null ? u.getUtilizationRate() : BigDecimal.ZERO);
            result.add(item);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getSummary(LocalDate startDate, LocalDate endDate) {
        List<Map<String, Object>> result = new ArrayList<>();
        List<DeviceUtilization> list = utilizationMapper.aggregateByDevice(startDate, endDate);
        for (DeviceUtilization u : list) {
            Map<String, Object> item = new HashMap<>();
            item.put("deviceCode", u.getDeviceCode());
            item.put("avgUtilization", u.getUtilizationRate());
            result.add(item);
        }
        return result;
    }

    @Override
    public void generateDailyStats(LocalDate date) {
        log.info("生成设备利用率统计: {}", date);
    }
}
