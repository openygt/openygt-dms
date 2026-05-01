package cn.org.openygt.production.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TimeRuleRequest {
    private Long id;
    @NotBlank
    private String ruleCode;
    @NotBlank
    private String ruleName;
    private String prescriptionType;
    @NotBlank
    private String stage;
    @NotNull
    private Integer standardDuration;
    private Integer warningThreshold;
    private Integer alertThreshold;
    private Integer criticalThreshold;
    private Integer isDefault;
}
