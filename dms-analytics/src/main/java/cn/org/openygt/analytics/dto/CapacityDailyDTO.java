package cn.org.openygt.analytics.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 日产能统计项。
 */
@Data
public class CapacityDailyDTO {
    private LocalDate statDate;
    private Long totalTasks;
    private Long completedTasks;
    private Long totalBags;
    private Long scrappedTasks;
    private BigDecimal completionRate;
}
