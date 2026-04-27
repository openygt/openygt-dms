package cn.org.openygt.analytics.service.impl;

import cn.org.openygt.analytics.dto.DashboardRealtimeDTO;
import cn.org.openygt.analytics.dto.DeviceTypeDistributionDTO;
import cn.org.openygt.analytics.dto.TaskStatusDistributionDTO;
import cn.org.openygt.analytics.mapper.DashboardStatMapper;
import cn.org.openygt.analytics.service.DashboardService;
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

        // 今日任务统计
        Map<String, Object> taskStats = statMapper.selectTodayTaskStats();
        dto.setTodayTotalTasks(toLong(taskStats.get("total")));
        dto.setTodayCompletedTasks(toLong(taskStats.get("completed")));
        dto.setTodayInProgressTasks(toLong(taskStats.get("inProgress")));
        dto.setTodayPendingTasks(toLong(taskStats.get("pending")));

        // 设备统计
        dto.setOnlineDeviceCount(statMapper.selectOnlineDeviceCount());
        dto.setOfflineDeviceCount(statMapper.selectOfflineDeviceCount());

        // 告警统计
        dto.setActiveAlarmCount(statMapper.selectActiveAlarmCount());

        // 质检统计
        Long inspectionCount = statMapper.selectTodayInspectionCount();
        dto.setTodayInspectionCount(inspectionCount);
        dto.setTodayPassRate(BigDecimal.valueOf(100)); // 简化：默认100%

        // 任务状态分布
        List<Map<String, Object>> taskDist = statMapper.selectTaskStatusDistribution();
        dto.setTaskStatusDistribution(taskDist.stream().map(m -> {
            TaskStatusDistributionDTO d = new TaskStatusDistributionDTO();
            d.setStatus((String) m.get("status"));
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
}
