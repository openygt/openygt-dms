package cn.org.openygt.equipment.entity;

import cn.org.openygt.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("eq_alarm_notification")
public class EqAlarmNotification extends BaseEntity {

    private Long alarmId;

    /** IN_APP / WS / EMAIL / SMS（V2.0 不再支持 VOICE） */
    private String notifyType;

    private String notifyTarget;

    private String notifyContent;

    /** PENDING / SENT / FAILED */
    private String sendStatus;

    private LocalDateTime sendTime;
    private String errorMsg;
}
