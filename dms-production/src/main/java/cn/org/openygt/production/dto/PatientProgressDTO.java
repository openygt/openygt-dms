package cn.org.openygt.production.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PatientProgressDTO {
    private Long prescriptionId;
    private String patientName;
    private String patientPhone;
    private String currentStatus;
    private Integer progressPercent;
    private LocalDateTime estimatedFinishTime;
    private List<StepItem> steps;

    @Data
    public static class StepItem {
        private String stepName;
        private String status;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
    }
}
