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
    /** NORMAL-普通, EMERGENCY-普通急诊, CRITICAL_EMERGENCY-危重急诊 */
    private String prescriptionType;
    /** SOAK-泡药, FIRST_DECOCTION-一煎, SECOND_DECOCTION-二煎, DECOCT-煎药, WRAP-包装 */
    @NotBlank
    private String stage;
    @NotNull
    private Integer standardDuration;
    private Integer warningThreshold;
    private Integer alertThreshold;
    private Integer criticalThreshold;
    private Integer isDefault;
}
