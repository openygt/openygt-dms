package cn.org.openygt.masterdata.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 煎药方案响应 DTO（屏蔽 deleted / tenantId）。
 */
@Data
public class SchemeResponse {

    private Long id;
    private String name;
    private Integer schemeType;
    private Integer decoctTimes;
    private Integer pressure;
    private BigDecimal upperWater;
    private Integer heatingTime;
    private Integer preHeatingTime;
    private Integer postHeatingTime;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
