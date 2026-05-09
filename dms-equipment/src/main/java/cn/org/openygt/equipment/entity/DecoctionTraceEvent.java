package cn.org.openygt.equipment.entity;

import cn.org.openygt.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("decoction_trace_event")
public class DecoctionTraceEvent extends BaseEntity {

    private Long traceId;
    private String prescriptionNo;
    private String eventCode;
    private String eventName;
    private String eventType;
    private Long operatorId;
    private String operatorName;
    private String deviceCode;
    private String deviceType;
    private LocalDateTime eventTime;
    private String beforeValue;
    private String afterValue;
    private String remark;
    private String attachmentUrl;
}
