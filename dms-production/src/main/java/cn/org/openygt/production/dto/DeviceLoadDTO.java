package cn.org.openygt.production.dto;

import lombok.Data;

@Data
public class DeviceLoadDTO {
    private Long deviceId;
    private String deviceCode;
    private String deviceName;
    private Integer assignedCount;
    private Integer runningCount;
    private Integer idleCount;
}
