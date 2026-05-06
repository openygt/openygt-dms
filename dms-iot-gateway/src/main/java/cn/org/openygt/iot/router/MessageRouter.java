package cn.org.openygt.iot.router;

import cn.org.openygt.iot.config.GatewayProperties;
import cn.org.openygt.iot.gateway.session.GatewayDeviceSessionController;
import cn.org.openygt.iot.protocol.DeviceMessage;
import cn.org.openygt.iot.protocol.MessageType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

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
    private final Executor asyncExecutor;

    public MessageRouter(RestTemplate restTemplate,
                         GatewayProperties properties,
                         GatewayDeviceSessionController gatewayDeviceSessionController,
                         @Qualifier("gatewayAsyncExecutor") Executor asyncExecutor) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.gatewayDeviceSessionController = gatewayDeviceSessionController;
        this.asyncExecutor = asyncExecutor;
    }

    /**
     * 路由设备消息
     */
    public void route(DeviceMessage message) {
        if (message == null || message.getMessageType() == null) {
            log.warn("收到空设备消息，跳过路由");
            return;
        }

        // 1. 消息类型校验 + 日志记录
        MessageType msgType = MessageType.from(message.getMessageType());
        switch (msgType) {
            case TELEMETRY:
                log.info("[TELEMETRY] device={}, payload={}", message.getDeviceCode(), message.getPayload());
                break;
            case STATUS:
                log.info("[STATUS] device={}, payload={}", message.getDeviceCode(), message.getPayload());
                break;
            case ALARM:
                log.warn("[ALARM] device={}, payload={}", message.getDeviceCode(), message.getPayload());
                break;
            case COMMAND_ACK:
                log.info("[ACK] device={}, payload={}", message.getDeviceCode(), message.getPayload());
                break;
            default:
                log.warn("未知消息类型: {}", message.getMessageType());
                return;
        }

        // 2. 先更新本地在线状态（不依赖后端回写是否成功）
        gatewayDeviceSessionController.touchOnlineDevice(
                message.getDeviceCode(), message.getProtocolType(), message.getSourceIp());

        // 3. 再异步回写后端（失败不影响在线状态，仅记录告警）
        final DeviceMessage msg = message;
        asyncExecutor.execute(() -> {
            try {
                boolean ok = postToEquipment(msg);
                if (!ok) {
                    log.warn("异步回写后端失败（不影响设备在线状态）: device={}, type={}",
                            msg.getDeviceCode(), msg.getMessageType());
                }
            } catch (Exception e) {
                log.error("异步回写后端异常（不影响设备在线状态）: device={}, type={}",
                        msg.getDeviceCode(), msg.getMessageType(), e);
            }
        });
    }

    // handleXxx 方法已内联到 route() 中，postToEquipment 统一在 route() 末尾异步调用

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
