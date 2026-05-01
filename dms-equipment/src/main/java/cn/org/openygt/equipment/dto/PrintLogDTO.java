package cn.org.openygt.equipment.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PrintLogDTO {
    private Long id;
    private Long taskId;
    private String deviceCode;
    private String operatorId;
    private String status;
    private Integer copies;
    private Integer retryCount;
    private Integer maxRetry;
    private String result;
    private String errorMessage;
    private String printerCode;
    private LocalDateTime printedAt;
    private LocalDateTime createdAt;
}
