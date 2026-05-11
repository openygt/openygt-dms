package cn.org.openygt.equipment.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
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
    private String reportedAt;
    private Map<String, Object> payload = new HashMap<>();

    /**
     * 获取解析后的 LocalDateTime，兼容带时区偏移的 ISO 格式。
     */
    public LocalDateTime getReportedAt() {
        if (reportedAt == null || reportedAt.trim().isEmpty()) {
            return null;
        }
        String text = reportedAt.trim();
        try {
            return ZonedDateTime.parse(text).toLocalDateTime();
        } catch (DateTimeParseException e) {
            return LocalDateTime.parse(text);
        }
    }
}
