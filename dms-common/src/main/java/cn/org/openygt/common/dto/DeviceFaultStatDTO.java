package cn.org.openygt.common.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 设备故障统计 DTO。
 */
@Data
public class DeviceFaultStatDTO implements Serializable {

    private Long deviceId;
    private String deviceCode;
    private String deviceName;
    private Integer faultCount;
    private String latestFaultCode;
    private String latestFaultMessage;
}
