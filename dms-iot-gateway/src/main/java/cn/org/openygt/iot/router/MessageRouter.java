package cn.org.openygt.iot.router;

import cn.org.openygt.iot.config.GatewayProperties;
import cn.org.openygt.iot.gateway.session.GatewayDeviceSessionController;
import cn.org.openygt.iot.protocol.DeviceMessage;
import cn.org.openygt.iot.protocol.MessageType;
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

    private final RestTemplate restTemplate;
    private final GatewayProperties properties;
    private final GatewayDeviceSessionController gatewayDeviceSessionController;

    public MessageRouter(RestTemplate restTemplate,
                         GatewayProperties properties,
                         GatewayDeviceSessionController gatewayDeviceSessionController) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.gatewayDeviceSessionController = gatewayDeviceSessionController;
    }

    /**
     * 路由设备消息
     */
    public void route(DeviceMessage message) {
        if (message == null || message.getMessageType() == null) {
            log.warn("收到空设备消息，跳过路由");
            return;
        }

        boolean accepted;
        switch (MessageType.from(message.getMessageType())) {
            case TELEMETRY:
                accepted = handleTelemetry(message);
                break;
            case STATUS:
                accepted = handleStatus(message);
                break;
            case ALARM:
                accepted = handleAlarm(message);
                break;
            case COMMAND_ACK:
                accepted = handleCommandAck(message);
                break;
            default:
                log.warn("未知消息类型: {}", message.getMessageType());
                accepted = false;
        }

        if (accepted) {
            gatewayDeviceSessionController.touchOnlineDevice(
                    message.getDeviceCode(), message.getProtocolType(), message.getSourceIp());
        }
    }

    private boolean handleTelemetry(DeviceMessage message) {
        log.info("[TELEMETRY] device={}, payload={}", message.getDeviceCode(), message.getPayload());
        return postToEquipment(message);
    }

    private boolean handleStatus(DeviceMessage message) {
        log.info("[STATUS] device={}, payload={}", message.getDeviceCode(), message.getPayload());
        return postToEquipment(message);
    }

    private boolean handleAlarm(DeviceMessage message) {
        log.warn("[ALARM] device={}, payload={}", message.getDeviceCode(), message.getPayload());
        return postToEquipment(message);
    }

    private boolean handleCommandAck(DeviceMessage message) {
        log.info("[ACK] device={}, payload={}", message.getDeviceCode(), message.getPayload());
        return postToEquipment(message);
    }

    private boolean postToEquipment(DeviceMessage message) {
        String url = properties.getBackendUrl() + "/api/v1/eq/gateway/report";
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

        int maxAttempts = Math.max(1, properties.getRetry().getMaxAttempts());
        long backoffMillis = Math.max(0L, properties.getRetry().getBackoffMillis());
        Exception lastError = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                restTemplate.postForEntity(url, entity, Object.class);
                if (attempt > 1) {
                    log.info("消息回写成功: device={}, type={}, attempt={}/{}",
                            message.getDeviceCode(), message.getMessageType(), attempt, maxAttempts);
                }
                return true;
            } catch (Exception e) {
                lastError = e;
                if (attempt < maxAttempts) {
                    log.warn("消息回写失败，准备重试: device={}, type={}, attempt={}/{}",
                            message.getDeviceCode(), message.getMessageType(), attempt, maxAttempts, e);
                    sleepQuietly(backoffMillis);
                }
            }
        }

        log.error("消息回写 dms-equipment 失败，已达到最大重试次数: device={}, type={}, url={}",
                message.getDeviceCode(), message.getMessageType(), url, lastError);
        return false;
    }

    private void sleepQuietly(long backoffMillis) {
        if (backoffMillis <= 0L) {
            return;
        }
        try {
            Thread.sleep(backoffMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("消息回写重试被中断", e);
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
