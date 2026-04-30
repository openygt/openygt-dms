package cn.org.openygt.equipment.entity;

import cn.org.openygt.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("alarm_config")
public class AlarmConfig extends BaseEntity {

    private String alarmType;
    private String alarmLevel;
    private String thresholdType;
    private BigDecimal thresholdValue;
    private Integer durationSeconds;
    private String notifyType;
    private String notifyTarget;
    private String soundFile;
    private Boolean enabled;
}
