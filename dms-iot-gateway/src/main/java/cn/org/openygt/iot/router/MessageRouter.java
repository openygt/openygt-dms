package cn.org.openygt.iot.router;

import cn.org.openygt.iot.protocol.DeviceMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 消息路由器
 *
 * <p>将统一消息 DeviceMessage 路由到 DMS 后端或本地处理。</p>
 */
@Slf4j
@Component
public class MessageRouter {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${iot.gateway.backend-url:http://localhost:8080}")
    private String backendUrl;

    /**
     * 路由设备消息
     */
    public void route(DeviceMessage message) {
        if (message == null || message.getMessageType() == null) {
            log.warn("收到空设备消息，跳过路由");
            return;
        }

        switch (message.getMessageType()) {
            case "TELEMETRY":
                handleTelemetry(message);
                break;
            case "STATUS":
                handleStatus(message);
                break;
            case "ALARM":
                handleAlarm(message);
                break;
            case "COMMAND_ACK":
                handleCommandAck(message);
                break;
            default:
                log.warn("未知消息类型: {}", message.getMessageType());
        }
    }

    private void handleTelemetry(DeviceMessage message) {
        log.info("[TELEMETRY] device={}, payload={}", message.getDeviceCode(), message.getPayload());
        postToEquipment(message);
    }

    private void handleStatus(DeviceMessage message) {
        log.info("[STATUS] device={}, payload={}", message.getDeviceCode(), message.getPayload());
        postToEquipment(message);
    }

    private void handleAlarm(DeviceMessage message) {
        log.warn("[ALARM] device={}, payload={}", message.getDeviceCode(), message.getPayload());
        postToEquipment(message);
    }

    private void handleCommandAck(DeviceMessage message) {
        log.info("[ACK] device={}, payload={}", message.getDeviceCode(), message.getPayload());
        postToEquipment(message);
    }

    private void postToEquipment(DeviceMessage message) {
        String url = backendUrl + "/api/v1/eq/gateway/report";
        Map<String, Object> request = new HashMap<>();
        request.put("protocolType", message.getProtocolType());
        request.put("tenantId", message.getTenantId());
        request.put("deviceCode", message.getDeviceCode());
        request.put("messageType", message.getMessageType());
        request.put("rawPayload", message.getRawPayload());
        request.put("reportedAt", message.getTimestamp() != null ? message.getTimestamp() : LocalDateTime.now());
        request.put("payload", message.getPayload());

        Map<String, Object> payload = message.getPayload() != null ? message.getPayload() : new HashMap<>();
        request.put("deviceType", payload.get("deviceType"));
        request.put("status", firstString(payload, "status"));
        request.put("detailStatus", firstString(payload, "detailStatus"));
        request.put("currentTemp", firstValue(payload, "currentTemp", "temperature", "rtp"));
        request.put("targetTemp", firstValue(payload, "targetTemp", "stp"));
        request.put("waterLevel", firstValue(payload, "waterLevel"));
        request.put("pressure", firstValue(payload, "pressure"));
        request.put("progressPercent", firstValue(payload, "progressPercent"));
        request.put("remainingTime", firstValue(payload, "remainingTime", "jt", "ft", "ht", "bt", "st"));
        request.put("faultCode", firstString(payload, "faultCode", "wn"));
        request.put("faultMessage", firstString(payload, "faultMessage", "description"));
        request.put("commandType", firstString(payload, "commandType"));
        request.put("commandId", firstValue(payload, "commandId"));
        request.put("commandResult", firstString(payload, "result"));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        try {
            restTemplate.postForEntity(url, entity, Object.class);
        } catch (Exception e) {
            log.error("消息回写 dms-equipment 失败: device={}, type={}, url={}",
                    message.getDeviceCode(), message.getMessageType(), url, e);
        }
    }

    private Object firstValue(Map<String, Object> payload, String... keys) {
        for (String key : keys) {
            Object value = payload.get(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String firstString(Map<String, Object> payload, String... keys) {
        Object value = firstValue(payload, keys);
        return value != null ? value.toString() : null;
    }
}
