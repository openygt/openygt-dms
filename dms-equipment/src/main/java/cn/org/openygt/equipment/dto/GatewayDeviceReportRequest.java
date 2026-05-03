package cn.org.openygt.equipment.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Data
public class GatewayDeviceReportRequest {

    private String protocolType;
    private String tenantId = "default";
    private String deviceCode;
    private Integer deviceType;
    private String messageType;
    private String status;
    private String detailStatus;
    private BigDecimal currentTemp;
    private BigDecimal targetTemp;
    private Integer waterLevel;
    private BigDecimal pressure;
    private Integer progressPercent;
    private Integer remainingTime;
    private String faultCode;
    private String faultMessage;
    private String commandType;
    private Long commandId;
    private String commandResult;
    private String rawPayload;
    private LocalDateTime reportedAt;
    private Map<String, Object> payload = new HashMap<>();
}
