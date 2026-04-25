package cn.org.openygt.equipment.entity;

import cn.org.openygt.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("eq_device")
public class EqDevice extends BaseEntity {

    private String deviceCode;
    private String name;
    private Integer deviceType;
    private String ipAddress;
    private Integer port;
    private String protocolType;
    private Integer locationX;
    private Integer locationY;
    private Integer decoctMode;
    private Integer pressureMode;
    private Integer slowFireTime;
    private Integer packageNum;
    private Integer packageCapacity;
    private BigDecimal alarmMinTemp;
    private BigDecimal alarmMaxTemp;
    private String faultCode;
    private String version;
    private String status;
    private BigDecimal currentTemp;
    private Integer enabled;
    private String autoLevel;
    private String labelMode;
    private Long currentSchemeId;
    private Date alertTime;
    private String resolvedBy;
    private Date resolvedAt;
    private LocalDateTime lastHeartbeat;
}
