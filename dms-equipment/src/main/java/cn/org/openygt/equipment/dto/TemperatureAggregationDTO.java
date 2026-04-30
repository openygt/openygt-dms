package cn.org.openygt.equipment.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 温度聚合数据DTO
 */
@Data
public class TemperatureAggregationDTO {

    /** 时间窗口起始时间 */
    private LocalDateTime windowStart;

    /** 平均温度 */
    private BigDecimal avgTemp;

    /** 最高温度 */
    private BigDecimal maxTemp;

    /** 最低温度 */
    private BigDecimal minTemp;

    /** 样本数量 */
    private Integer sampleCount;
}
