package cn.org.openygt.analytics.dto;

import lombok.Data;

@Data
public class DeviceTypeDistributionDTO {
    private String deviceType;
    private String typeName;
    private Long count;
    private Long onlineCount;
    private Long totalCount;
}
