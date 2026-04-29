package cn.org.openygt.analytics.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 监控大屏实时数据。
 */
@Data
public class DashboardRealtimeDTO {
    // 任务核心KPI
    private Long todayTotalTasks;
    private Long todayEndedTasks;
    private Long todayInProgressTasks;
    private Long todayAlertingTasks;
    private Long todayAlertedTasks;

    // 设备统计
    private Long onlineDeviceCount;
    private Long offlineDeviceCount;

    // 质检统计
    private Long todayInspectionCount;
    private BigDecimal todayPassRate;

    // 趋势（环比百分比）
    private Integer taskTrend;
    private Integer endedTrend;
    private Integer inProgressTrend;
    private Integer alertingTrend;

    // 分布
    private List<TaskStatusDistributionDTO> taskStatusDistribution;
    private List<DeviceTypeDistributionDTO> deviceTypeDistribution;
}
