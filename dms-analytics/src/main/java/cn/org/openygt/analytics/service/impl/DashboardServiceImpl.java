package cn.org.openygt.analytics.service.impl;

import cn.org.openygt.analytics.dto.DashboardRealtimeDTO;
import cn.org.openygt.analytics.dto.DeviceTypeDistributionDTO;
import cn.org.openygt.analytics.dto.TaskStatusDistributionDTO;
import cn.org.openygt.analytics.mapper.DashboardStatMapper;
import cn.org.openygt.analytics.service.DashboardService;
import cn.org.openygt.common.enums.TaskStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 监控大屏服务实现。
 */
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final DashboardStatMapper statMapper;

    @Override
    public DashboardRealtimeDTO getRealtime() {
        DashboardRealtimeDTO dto = new DashboardRealtimeDTO();

        // 今日任务核心KPI统计
        Map<String, Object> taskStats = statMapper.selectTodayTaskStats();
        long todayTotal = toLong(taskStats.get("total"));
        long todayEnded = toLong(taskStats.get("ended"));
        long todayInProgress = toLong(taskStats.get("inProgress"));
        long todayAlerting = toLong(taskStats.get("alertingNow"));
        long todayAlerted = toLong(taskStats.get("everAlerted"));

        dto.setTodayTotalTasks(todayTotal);
        dto.setTodayEndedTasks(todayEnded);
        dto.setTodayInProgressTasks(todayInProgress);
        dto.setTodayAlertingTasks(todayAlerting);
        dto.setTodayAlertedTasks(todayAlerted);

        // 昨日任务统计（用于趋势）
        Map<String, Object> yestStats = statMapper.selectYesterdayTaskStats();
        long yestTotal = yestStats != null ? toLong(yestStats.get("total")) : 0L;
        long yestEnded = yestStats != null ? toLong(yestStats.get("ended")) : 0L;
        long yestInProgress = yestStats != null ? toLong(yestStats.get("inProgress")) : 0L;
        long yestAlerting = yestStats != null ? toLong(yestStats.get("alertingNow")) : 0L;

        // 计算趋势（环比百分比，保留整数）
        dto.setTaskTrend(calcTrend(yestTotal, todayTotal));
        dto.setEndedTrend(calcTrend(yestEnded, todayEnded));
        dto.setInProgressTrend(calcTrend(yestInProgress, todayInProgress));
        dto.setAlertingTrend(calcTrend(yestAlerting, todayAlerting));

        // 设备统计
        dto.setOnlineDeviceCount(statMapper.selectOnlineDeviceCount());
        dto.setOfflineDeviceCount(statMapper.selectOfflineDeviceCount());

        // 质检统计
        Long inspectionCount = statMapper.selectTodayInspectionCount();
        dto.setTodayInspectionCount(inspectionCount);
        dto.setTodayPassRate(BigDecimal.valueOf(100)); // 简化：默认100%

        // 任务状态分布
        List<Map<String, Object>> taskDist = statMapper.selectTaskStatusDistribution();
        dto.setTaskStatusDistribution(taskDist.stream().map(m -> {
            TaskStatusDistributionDTO d = new TaskStatusDistributionDTO();
            String statusCode = (String) m.get("status");
            TaskStatus enumStatus = TaskStatus.fromCode(statusCode);
            d.setStatus(enumStatus != null ? enumStatus.getLabel() : statusCode);
            d.setCount(toLong(m.get("count")));
            return d;
        }).collect(Collectors.toList()));

        // 设备类型分布
        List<Map<String, Object>> deviceDist = statMapper.selectDeviceTypeDistribution();
        dto.setDeviceTypeDistribution(deviceDist.stream().map(m -> {
            DeviceTypeDistributionDTO d = new DeviceTypeDistributionDTO();
            Object type = m.get("device_type");
            d.setDeviceType(type != null ? type.toString() : "未知");
            d.setCount(toLong(m.get("count")));
            d.setOnlineCount(toLong(m.get("onlineCount")));
            d.setOnlineWithAlarmCount(toLong(m.get("onlineWithAlarmCount")));
            d.setOfflineCount(toLong(m.get("offlineCount")));
            d.setTotalCount(toLong(m.get("count")));
            return d;
        }).collect(Collectors.toList()));

        return dto;
    }

    private Long toLong(Object obj) {
        if (obj == null) return 0L;
        if (obj instanceof Number) return ((Number) obj).longValue();
        try {
            return Long.valueOf(obj.toString());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    /**
     * 计算环比趋势（百分比整数）。<br>
     * 昨日为0时返回0（避免"从无到有"显示夸张的100%）。
     */
    private Integer calcTrend(long yesterday, long today) {
        if (yesterday == 0) {
            return 0;
        }
        return BigDecimal.valueOf(today - yesterday)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(yesterday), 0, RoundingMode.HALF_UP)
                .intValue();
    }
}
