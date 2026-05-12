package cn.org.openygt.production.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EmergencyPrescriptionDTO {
    private Long id;
    private Long prescriptionId;
    private String prescriptionNumber;
    private String patientName;
    private String hospitalName;
    private String department;
    private Integer emergencyLevel;
    private LocalDateTime requestTime;
    private LocalDateTime promisedFinishTime;
    private LocalDateTime actualFinishTime;
    private Integer isOnTime;
    private String delayReason;
    private String deliveryType;
    private String deliveryLocation;
    private String nurseName;
    private LocalDateTime nurseSignTime;
    private String status;
}
