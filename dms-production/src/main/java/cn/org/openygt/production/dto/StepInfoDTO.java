package cn.org.openygt.production.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StepInfoDTO {
    private String stepCode;
    private String stepName;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String operatorId;
    private String operatorName;
    private Integer durationMinutes;
    private String result;
}
