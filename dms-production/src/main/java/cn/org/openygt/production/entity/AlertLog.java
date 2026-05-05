package cn.org.openygt.production.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("dms_alert_log")
public class AlertLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long monitorId;
    private Long taskId;
    private String stage;
    private Integer alertLevel;
    private String alertType;
    private String alertContent;
    private String notifyChannels;
    private String notifyTargets;
    private Integer isResolved;
    private Long resolvedBy;
    private LocalDateTime resolvedAt;
    private LocalDateTime createdAt;
}
