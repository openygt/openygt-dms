package cn.org.openygt.production.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class StepDetailDTO {
    private String stepCode;
    private String stepName;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String operatorId;
    private String operatorName;
    private Integer durationMinutes;
    private Integer pauseDuration;
    private String pauseReason;
    private Integer delayMinutes;
    private String delayReason;
    private String result;
    private String abortReason;
    private BigDecimal wasteAmount;
    private String wasteUnit;
    private List<WorkRecordDTO> workRecords;

    @Data
    public static class WorkRecordDTO {
        private String operatorId;
        private String operatorName;
        private String action;
        private Integer workTime;
        private LocalDateTime createdAt;
    }
}
