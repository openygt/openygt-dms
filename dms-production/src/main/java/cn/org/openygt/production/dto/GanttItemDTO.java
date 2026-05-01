package cn.org.openygt.production.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GanttItemDTO {
    private Long assignmentId;
    private Long taskId;
    private String taskName;
    private Long deviceId;
    private String deviceName;
    private Long employeeId;
    private String employeeName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private Integer progress;
}
