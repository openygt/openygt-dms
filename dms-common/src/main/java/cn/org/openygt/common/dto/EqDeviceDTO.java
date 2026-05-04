package cn.org.openygt.common.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 设备信息传输对象。
 * EquipmentService 所有查询方法的统一返回类型，替代宽松的 Object。
 */
@Data
public class EqDeviceDTO implements Serializable {

    private Long id;
    private String deviceCode;
    private String name;
    private String deviceType;
    private String status;
    private BigDecimal currentTemp;
    private String faultCode;
    private String autoLevel;
    private String location;
    private String protocolType;
    private String communicationId;
    private String vendor;
    private BigDecimal alarmHighTemp;
    private BigDecimal alarmLowTemp;
    private LocalDateTime lastHeartbeat;
}
