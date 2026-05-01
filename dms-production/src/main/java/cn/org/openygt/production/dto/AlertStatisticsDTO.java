package cn.org.openygt.production.dto;

import lombok.Data;

import java.util.Map;

@Data
public class AlertStatisticsDTO {
    private Integer totalAlerts;
    private Integer resolvedAlerts;
    private Integer unresolvedAlerts;
    private Map<String, Integer> alertLevelDistribution;
    private Map<String, Integer> alertTypeDistribution;
}
