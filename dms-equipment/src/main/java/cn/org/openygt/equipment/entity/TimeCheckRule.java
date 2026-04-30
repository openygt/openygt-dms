package cn.org.openygt.equipment.entity;

import cn.org.openygt.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("time_check_rule")
public class TimeCheckRule extends BaseEntity {

    private String ruleCode;
    private String ruleName;
    private String fromStep;
    private String toStep;
    private Integer minDuration;
    private Integer maxDuration;
    private String checkType;
    private String warningMessage;
    private String blockMessage;
    private Boolean enabled;
    private Integer sortOrder;
}
