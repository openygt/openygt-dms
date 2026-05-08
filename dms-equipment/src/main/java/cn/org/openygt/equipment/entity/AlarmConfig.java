package cn.org.openygt.equipment.entity;

import cn.org.openygt.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("eq_alarm_config")
public class AlarmConfig extends BaseEntity {

    @NotBlank(message = "告警类型不能为空")
    private String alarmType;

    @NotBlank(message = "告警级别不能为空")
    private String alarmLevel;

    private String thresholdType;

    @NotNull(message = "阈值不能为空")
    private BigDecimal thresholdValue;

    private Integer durationSeconds;
    private String notifyType;
    private String notifyTarget;
    private String soundFile;

    @NotNull(message = "启用状态不能为空")
    private Boolean enabled;
}
