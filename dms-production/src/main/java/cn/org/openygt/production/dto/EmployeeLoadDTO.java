package cn.org.openygt.production.dto;

import lombok.Data;

@Data
public class EmployeeLoadDTO {
    private Long employeeId;
    private String employeeName;
    private Integer assignedCount;
    private Integer completedCount;
    private Integer pendingCount;
}
