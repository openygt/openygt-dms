package cn.org.openygt.equipment.service.impl;

import cn.org.openygt.equipment.entity.DeviceUtilization;
import cn.org.openygt.equipment.entity.EqDeviceStatus;
import cn.org.openygt.equipment.mapper.DeviceUtilizationMapper;
import cn.org.openygt.equipment.mapper.EqDeviceStatusMapper;
import cn.org.openygt.equipment.service.DeviceUtilizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceUtilizationServiceImpl implements DeviceUtilizationService {

    private final DeviceUtilizationMapper utilizationMapper;
    private final EqDeviceStatusMapper statusMapper;

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
    @Transactional
    public void generateDailyStats(LocalDate date) {
        log.info("生成设备利用率统计: {}", date);
        if (date == null) {
            return;
        }
        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.atTime(LocalTime.MAX);
        String startStr = dayStart.toString();
        String endStr = dayEnd.toString();

        List<EqDeviceStatus> snapshots = statusMapper.findAllByTimeRange(startStr, endStr);
        if (snapshots == null || snapshots.isEmpty()) {
            log.warn("{} 无设备状态快照数据", date);
            return;
        }

        // 按设备分组处理
        Map<String, List<EqDeviceStatus>> byDevice = new HashMap<>();
        for (EqDeviceStatus s : snapshots) {
            byDevice.computeIfAbsent(s.getDeviceCode(), k -> new ArrayList<>()).add(s);
        }

        int totalMinutesPerDay = 24 * 60; // 1440

        for (Map.Entry<String, List<EqDeviceStatus>> entry : byDevice.entrySet()) {
            String deviceCode = entry.getKey();
            List<EqDeviceStatus> list = entry.getValue();

            int runMinutes = 0;
            int idleMinutes = 0;
            int faultMinutes = 0;
            int offlineMinutes = 0;
            int maintenanceMinutes = 0;
            int faultCount = 0;

            for (int i = 0; i < list.size(); i++) {
                EqDeviceStatus current = list.get(i);
                String detail = current.getDetailStatus() != null ? current.getDetailStatus().toUpperCase() : "UNKNOWN";
                LocalDateTime currentTime = current.getSnapshotTime();
                LocalDateTime nextTime = (i + 1 < list.size()) ? list.get(i + 1).getSnapshotTime() : dayEnd;

                // 最后一条记录持续到当天结束，但不超过当天
                if (nextTime.isAfter(dayEnd)) {
                    nextTime = dayEnd;
                }
                long minutes = ChronoUnit.MINUTES.between(currentTime, nextTime);
                if (minutes < 0) minutes = 0;
                if (minutes > totalMinutesPerDay) minutes = totalMinutesPerDay;

                switch (detail) {
                    case "RUNNING":
                    case "PACKAGING":
                    case "PRINTING":
                    case "BUSY":
                    case "WORKING":
                        runMinutes += minutes;
                        break;
                    case "IDLE":
                    case "READY":
                    case "STANDBY":
                        idleMinutes += minutes;
                        break;
                    case "OFFLINE":
                        offlineMinutes += minutes;
                        break;
                    case "FAULT":
                    case "ERROR":
                        faultMinutes += minutes;
                        faultCount++;
                        break;
                    case "MAINTENANCE":
                        maintenanceMinutes += minutes;
                        break;
                    default:
                        idleMinutes += minutes; // 未知状态按空闲处理
                }
            }

            // 利用率 = 运行分钟 / 1440
            BigDecimal utilizationRate = totalMinutesPerDay > 0
                    ? BigDecimal.valueOf(runMinutes * 100L)
                        .divide(BigDecimal.valueOf(totalMinutesPerDay), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            // 可用率 = (运行 + 空闲) / 1440
            int availableMinutes = runMinutes + idleMinutes;
            BigDecimal availabilityRate = totalMinutesPerDay > 0
                    ? BigDecimal.valueOf(availableMinutes * 100L)
                        .divide(BigDecimal.valueOf(totalMinutesPerDay), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            // 先删除旧记录（幂等）
            utilizationMapper.delete(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DeviceUtilization>()
                            .eq(DeviceUtilization::getDeviceCode, deviceCode)
                            .eq(DeviceUtilization::getStatDate, date));

            DeviceUtilization util = new DeviceUtilization();
            util.setDeviceCode(deviceCode);
            util.setStatDate(date);
            util.setTotalMinutes(totalMinutesPerDay);
            util.setRunMinutes(runMinutes);
            util.setIdleMinutes(idleMinutes);
            util.setFaultMinutes(faultMinutes);
            util.setOfflineMinutes(offlineMinutes);
            util.setMaintenanceMinutes(maintenanceMinutes);
            util.setUtilizationRate(utilizationRate);
            util.setAvailabilityRate(availabilityRate);
            util.setFaultCount(faultCount);
            util.setTaskCount(0); // 任务数后续可从 prod_task 补充
            util.setCreatedAt(LocalDateTime.now());
            util.setUpdatedAt(LocalDateTime.now());
            util.setDeleted(0);
            utilizationMapper.insert(util);
        }

        log.info("完成设备利用率统计: {} 设备数={}", date, byDevice.size());
    }
}
