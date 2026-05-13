package cn.org.openygt.equipment.service.impl;

import cn.org.openygt.equipment.entity.DeviceUtilization;
import cn.org.openygt.equipment.entity.EqDevice;
import cn.org.openygt.equipment.entity.EqDeviceStatus;
import cn.org.openygt.equipment.mapper.DeviceUtilizationMapper;
import cn.org.openygt.equipment.mapper.EqDeviceMapper;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceUtilizationServiceImpl implements DeviceUtilizationService {

    private final DeviceUtilizationMapper utilizationMapper;
    private final EqDeviceStatusMapper statusMapper;
    private final EqDeviceMapper eqDeviceMapper;

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
        // 按设备分组处理（包含当天无快照的设备，后续会补前序状态）
        Map<String, List<EqDeviceStatus>> byDevice = new HashMap<>();
        if (snapshots != null) {
            for (EqDeviceStatus s : snapshots) {
                byDevice.computeIfAbsent(s.getDeviceCode(), k -> new ArrayList<>()).add(s);
            }
        }

        int totalMinutesPerDay = 24 * 60; // 1440

        // 从 eq_device 获取所有设备编码，确保当天无快照但有前序状态的设备也被处理
        List<EqDevice> allDevices = eqDeviceMapper.findAllDeviceCodes();
        Set<String> devicesToProcess = new HashSet<>();
        for (EqDevice d : allDevices) {
            devicesToProcess.add(d.getDeviceCode());
        }

        // 批量查询当天各设备的完成任务数
        Map<String, Integer> taskCountMap = new HashMap<>();
        List<Map<String, Object>> taskCountRows = utilizationMapper.selectTaskCountByDevice(date.toString());
        if (taskCountRows != null) {
            for (Map<String, Object> row : taskCountRows) {
                String code = (String) row.get("device_code");
                Object cnt = row.get("task_count");
                if (code != null && cnt != null) {
                    taskCountMap.put(code, ((Number) cnt).intValue());
                }
            }
        }

        for (String deviceCode : devicesToProcess) {
            List<EqDeviceStatus> list = byDevice.getOrDefault(deviceCode, new ArrayList<>());

            // 补查当天开始前最后一条快照，作为初始状态
            EqDeviceStatus preStatus = statusMapper.findLatestBeforeTime(deviceCode, startStr);
            if (preStatus != null) {
                // 将前序状态作为当天 00:00 的虚拟快照插入头部
                EqDeviceStatus virtualStart = new EqDeviceStatus();
                virtualStart.setDeviceCode(deviceCode);
                virtualStart.setDeviceType(preStatus.getDeviceType());
                virtualStart.setStatus(preStatus.getStatus());
                virtualStart.setDetailStatus(preStatus.getDetailStatus());
                virtualStart.setSnapshotTime(dayStart);
                List<EqDeviceStatus> extended = new ArrayList<>();
                extended.add(virtualStart);
                extended.addAll(list);
                list = extended;
            }

            // 如果当天无任何快照且无前序状态，则跳过
            if (list.isEmpty()) {
                continue;
            }

            int runMinutes = 0;
            int idleMinutes = 0;
            int faultMinutes = 0;
            int offlineMinutes = 0;
            int maintenanceMinutes = 0;
            int faultCount = 0;

            String prevDetail = null;

            for (int i = 0; i < list.size(); i++) {
                EqDeviceStatus current = list.get(i);
                String detail = current.getDetailStatus() != null ? current.getDetailStatus().toUpperCase() : "UNKNOWN";
                LocalDateTime currentTime = current.getSnapshotTime();
                LocalDateTime nextTime = (i + 1 < list.size()) ? list.get(i + 1).getSnapshotTime() : dayEnd;

                if (nextTime.isAfter(dayEnd)) {
                    nextTime = dayEnd;
                }
                long minutes = ChronoUnit.MINUTES.between(currentTime, nextTime);
                if (minutes < 0) minutes = 0;
                if (minutes > totalMinutesPerDay) minutes = totalMinutesPerDay;

                // 故障次数：仅当首次进入 FAULT/ERROR 时计数
                boolean isFault = "FAULT".equals(detail) || "ERROR".equals(detail);
                boolean wasFault = "FAULT".equals(prevDetail) || "ERROR".equals(prevDetail);
                if (isFault && !wasFault) {
                    faultCount++;
                }
                prevDetail = detail;

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
                        break;
                    case "MAINTENANCE":
                        maintenanceMinutes += minutes;
                        break;
                    default:
                        idleMinutes += minutes;
                }
            }

            // 利用率 = 运行分钟 / 480（8小时标准工作班次）
            int workMinutesPerDay = 8 * 60;
            BigDecimal utilizationRate = totalMinutesPerDay > 0
                    ? BigDecimal.valueOf(runMinutes * 100L)
                        .divide(BigDecimal.valueOf(workMinutesPerDay), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            // 可用率 = (运行 + 空闲) / 480
            int availableMinutes = runMinutes + idleMinutes;
            BigDecimal availabilityRate = totalMinutesPerDay > 0
                    ? BigDecimal.valueOf(availableMinutes * 100L)
                        .divide(BigDecimal.valueOf(workMinutesPerDay), 2, RoundingMode.HALF_UP)
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
            util.setTaskCount(taskCountMap.getOrDefault(deviceCode, 0));
            util.setCreatedAt(LocalDateTime.now());
            util.setUpdatedAt(LocalDateTime.now());
            util.setDeleted(0);
            utilizationMapper.insert(util);
        }

        log.info("完成设备利用率统计: {} 设备数={}", date, devicesToProcess.size());
    }
}
