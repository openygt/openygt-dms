package cn.org.openygt.common.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 日产能统计传输对象。
 */
@Data
public class CapacityDailyDTO implements Serializable {

    private LocalDate statDate;
    private String tenantId;
    private Long totalTasks;
    private Long completedTasks;
    private Long scrappedTasks;

    // 前端展示扩展字段
    private Integer taskCount;
    private Integer completedCount;
    private Integer doseCount;
    private Double avgDuration;
    private Double deviceUtilization;
}
