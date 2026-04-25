package cn.org.openygt.common.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 设备利用率统计 DTO。
 */
@Data
public class DeviceUtilizationDTO implements Serializable {

    private Long deviceId;
    private String deviceCode;
    private String deviceName;
    private BigDecimal utilizationRate;
    private Integer totalMinutes;
    private Integer runningMinutes;
    private Integer idleMinutes;
    private Integer faultMinutes;
}
