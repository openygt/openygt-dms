package cn.org.openygt.production.dto;

import lombok.Data;

@Data
public class DeviceDailyDTO {
    private Long deviceId;
    private String deviceCode;
    private String deviceName;
    private Integer taskCount;
    private Integer runningMinutes;
    private Integer idleMinutes;
    private Integer utilization;
}
