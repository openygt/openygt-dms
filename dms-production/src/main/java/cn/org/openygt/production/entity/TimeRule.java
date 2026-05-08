package cn.org.openygt.production.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("prod_time_rule")
public class TimeRule {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String ruleCode;
    private String ruleName;
    /** NORMAL-普通, EMERGENCY-普通急诊, CRITICAL_EMERGENCY-危重急诊 */
    private String prescriptionType;
    /** SOAK-泡药, FIRST_DECOCTION-一煎, SECOND_DECOCTION-二煎, DECOCT-煎药, WRAP-包装 */
    private String stage;
    private Integer standardDuration;
    private Integer warningThreshold;
    private Integer alertThreshold;
    private Integer criticalThreshold;
    private Integer isDefault;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
