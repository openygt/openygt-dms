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
    private String schemeName;
    private String code;
    private String schemeCode;
    private Integer schemeType;
    private Integer decoctTimes;
    private Integer pressure;
    private BigDecimal upperWater;
    private Integer heatingTime;
    private Integer decoctTime;
    private Integer preHeatingTime;
    private Integer soakTime;
    private Integer postHeatingTime;
    private String tempRange;
    private String description;
    private String remark;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
