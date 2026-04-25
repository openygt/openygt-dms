package cn.org.openygt.analytics.dto;

import lombok.Data;

@Data
public class TaskStatusDistributionDTO {
    private String status;
    private String statusName;
    private Long count;
}
