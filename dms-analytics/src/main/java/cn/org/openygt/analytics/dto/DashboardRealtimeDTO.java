package cn.org.openygt.analytics.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 监控大屏实时数据。
 */
@Data
public class DashboardRealtimeDTO {
    private Long todayTotalTasks;
    private Long todayCompletedTasks;
    private Long todayInProgressTasks;
    private Long todayPendingTasks;
    private Long onlineDeviceCount;
    private Long offlineDeviceCount;
    private Long activeAlarmCount;
    private Long todayInspectionCount;
    private BigDecimal todayPassRate;
    private List<TaskStatusDistributionDTO> taskStatusDistribution;
    private List<DeviceTypeDistributionDTO> deviceTypeDistribution;
}
