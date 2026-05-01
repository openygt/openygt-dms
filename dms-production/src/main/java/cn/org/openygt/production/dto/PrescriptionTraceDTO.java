package cn.org.openygt.production.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PrescriptionTraceDTO {
    private Long prescriptionId;
    private String prescriptionNo;
    private String patientName;
    private List<TraceItem> traces;

    @Data
    public static class TraceItem {
        private String stage;
        private String operatorName;
        private LocalDateTime operateTime;
        private String remark;
    }
}
