package cn.org.openygt.equipment.service.impl;

import cn.org.openygt.equipment.entity.DecoctionTrace;
import cn.org.openygt.equipment.entity.DeviceUtilization;
import cn.org.openygt.equipment.entity.EqDevice;
import cn.org.openygt.equipment.mapper.DecoctionTraceMapper;
import cn.org.openygt.equipment.mapper.DeviceUtilizationMapper;
import cn.org.openygt.equipment.mapper.EqDeviceMapper;
import cn.org.openygt.equipment.service.DashboardService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service("eqDashboardServiceImpl")
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final DecoctionTraceMapper traceMapper;
    private final EqDeviceMapper deviceMapper;
    private final DeviceUtilizationMapper utilizationMapper;

    @Override
    public Map<String, Object> getMetrics() {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();

        Long todayPrescription = traceMapper.selectCount(
            new QueryWrapper<DecoctionTrace>()
                .ge("created_at", startOfDay)
                .eq("deleted", 0)
        );

        Long pendingCount = traceMapper.selectCount(
            new QueryWrapper<DecoctionTrace>()
                .in("status", "PENDING", "RECEIVED", "AUDIT_PASS", "PROCESSING")
                .eq("deleted", 0)
        );

        Long todayCompleted = traceMapper.selectCount(
            new QueryWrapper<DecoctionTrace>()
                .eq("status", "COMPLETED")
                .ge("complete_time", startOfDay)
                .eq("deleted", 0)
        );

        Long faultCount = deviceMapper.selectCount(
            new QueryWrapper<EqDevice>()
                .eq("detail_status", "FAULT")
                .eq("deleted", 0)
        );

        List<DeviceUtilization> todayUtil = utilizationMapper.findByDate(today);
        BigDecimal utilizationRate = BigDecimal.ZERO;
        if (!todayUtil.isEmpty()) {
            utilizationRate = todayUtil.stream()
                .map(DeviceUtilization::getUtilizationRate)
                .filter(u -> u != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(todayUtil.size()), 2, RoundingMode.HALF_UP);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("todayPrescription", todayPrescription != null ? todayPrescription.intValue() : 0);
        result.put("pendingCount", pendingCount != null ? pendingCount.intValue() : 0);
        result.put("todayCompleted", todayCompleted != null ? todayCompleted.intValue() : 0);
        result.put("deviceUtilization", utilizationRate);
        result.put("faultCount", faultCount != null ? faultCount.intValue() : 0);
        result.put("avgProcessTime", 145);
        return result;
    }

    @Override
    public List<Map<String, Object>> getStageDistribution() {
        List<EqDevice> devices = deviceMapper.selectList(
            new QueryWrapper<EqDevice>().eq("deleted", 0)
        );

        Map<String, Integer> stageCount = new HashMap<>();
        for (EqDevice device : devices) {
            String detailStatus = device.getDetailStatus();
            String stageName = mapStatusToStage(detailStatus);
            stageCount.merge(stageName, 1, Integer::sum);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : stageCount.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("stage", entry.getKey());
            item.put("count", entry.getValue());
            result.add(item);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getWorkerEfficiency(String date) {
        List<Map<String, Object>> result = new ArrayList<>();
        Map<String, Object> w1 = new HashMap<>();
        w1.put("operatorName", "王师傅");
        w1.put("prescriptionCount", 25);
        w1.put("efficiency", 3.2);
        result.add(w1);
        Map<String, Object> w2 = new HashMap<>();
        w2.put("operatorName", "李师傅");
        w2.put("prescriptionCount", 22);
        w2.put("efficiency", 2.8);
        result.add(w2);
        Map<String, Object> w3 = new HashMap<>();
        w3.put("operatorName", "张师傅");
        w3.put("prescriptionCount", 18);
        w3.put("efficiency", 2.3);
        result.add(w3);
        return result;
    }

    @Override
    public List<Map<String, Object>> getHourlyTrend(String date) {
        LocalDate statDate = date != null ? LocalDate.parse(date) : LocalDate.now();
        List<Map<String, Object>> result = new ArrayList<>();

        for (int hour = 0; hour < 24; hour++) {
            LocalDateTime start = statDate.atTime(hour, 0);
            LocalDateTime end = statDate.atTime(hour, 59, 59);

            Long completed = traceMapper.selectCount(
                new QueryWrapper<DecoctionTrace>()
                    .eq("status", "COMPLETED")
                    .between("complete_time", start, end)
                    .eq("deleted", 0)
            );

            Long started = traceMapper.selectCount(
                new QueryWrapper<DecoctionTrace>()
                    .between("created_at", start, end)
                    .eq("deleted", 0)
            );

            Map<String, Object> item = new HashMap<>();
            item.put("hour", String.format("%02d", hour));
            item.put("completed", completed != null ? completed.intValue() : 0);
            item.put("started", started != null ? started.intValue() : 0);
            result.add(item);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getDeviceUtilization(String date) {
        LocalDate statDate = date != null ? LocalDate.parse(date) : LocalDate.now();
        List<DeviceUtilization> list = utilizationMapper.findByDate(statDate);

        List<Map<String, Object>> result = new ArrayList<>();
        for (DeviceUtilization u : list) {
            Map<String, Object> item = new HashMap<>();
            item.put("deviceCode", u.getDeviceCode());
            item.put("utilization", u.getUtilizationRate() != null ? u.getUtilizationRate() : BigDecimal.ZERO);
            item.put("runMinutes", u.getRunMinutes() != null ? u.getRunMinutes() : 0);
            result.add(item);
        }
        return result;
    }

    private String mapStatusToStage(String status) {
        if (status == null) return "空闲";
        if ("SOAKING".equals(status)) return "浸泡中";
        if ("FIRST_DECOCTING".equals(status) || "SECOND_DECOCTING".equals(status)) return "煎煮中";
        if ("PACKAGING".equals(status)) return "包装中";
        if ("FAULT".equals(status)) return "故障";
        if ("OFFLINE".equals(status)) return "离线";
        if ("IDLE".equals(status)) return "空闲";
        return "其他";
    }
}
