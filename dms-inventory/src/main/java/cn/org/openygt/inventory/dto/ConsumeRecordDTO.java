package cn.org.openygt.inventory.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ConsumeRecordDTO {
    private Long id;
    private Long taskId;
    private Long medicineId;
    private String medicineCode;
    private String medicineName;
    private String changeType;
    private BigDecimal changeQuantity;
    private BigDecimal beforeQuantity;
    private BigDecimal afterQuantity;
    private String refNo;
    private String operatorId;
    private String remark;
    private LocalDateTime createdAt;
}
