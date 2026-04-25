package cn.org.openygt.masterdata.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;

/**
 * 煎药方案更新请求。
 */
@Data
public class SchemeUpdateRequest {

    @NotBlank(message = "方案名称不能为空")
    private String name;

    private Integer schemeType;
    private Integer decoctTimes;
    private Integer pressure;
    private BigDecimal upperWater;
    private Integer heatingTime;
    private Integer preHeatingTime;
    private Integer postHeatingTime;
    private String description;
}
