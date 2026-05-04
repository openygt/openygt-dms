package cn.org.openygt.iot.gateway;

import cn.org.openygt.iot.protocol.DeviceMessage;
import cn.org.openygt.iot.protocol.MessageType;
import cn.org.openygt.iot.router.MessageRouter;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/iot/debug")
public class GatewayDebugController {

    private final MessageRouter messageRouter;

    public GatewayDebugController(MessageRouter messageRouter) {
        this.messageRouter = messageRouter;
    }

    @PostMapping("/report")
    public ResponseEntity<Map<String, Object>> report(@RequestBody DebugReportRequest request) {
        DeviceMessage message = new DeviceMessage();
        message.setMessageId(String.valueOf(System.currentTimeMillis()));
        message.setProtocolType(defaultString(request.getProtocolType(), "debug-http"));
        message.setTenantId(defaultString(request.getTenantId(), "default"));
        message.setDeviceCode(request.getDeviceCode());
        message.setMessageType(request.getMessageType() != null ? request.getMessageType() : MessageType.STATUS);
        message.setTimestamp(request.getReportedAt() != null ? request.getReportedAt() : LocalDateTime.now());
        message.setSourceIp(defaultString(request.getSourceIp(), "127.0.0.1"));
        message.setRawPayload(request.getRawPayload());
        message.setPayload(request.getPayload() != null ? request.getPayload() : new HashMap<>());

        messageRouter.route(message);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("deviceCode", message.getDeviceCode());
        result.put("messageType", message.getMessageType());
        return ResponseEntity.ok(result);
    }

    private String defaultString(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    @Data
    public static class DebugReportRequest {
        private String protocolType;
        private String tenantId;
        private String deviceCode;
        private MessageType messageType;
        private String sourceIp;
        private String rawPayload;
        private LocalDateTime reportedAt;
        private Map<String, Object> payload;
    }
}
