package cn.org.openygt.iot.router;

import cn.org.openygt.iot.protocol.DeviceMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 消息路由器
 *
 * <p>将统一消息 DeviceMessage 路由到 DMS 后端或本地处理。</p>
 */
@Slf4j
@Component
public class MessageRouter {

    /**
     * 路由设备消息
     */
    public void route(DeviceMessage message) {
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
        // TODO: 转发至 DMS 后端 /api/v1/iot/telemetry
    }

    private void handleStatus(DeviceMessage message) {
        log.info("[STATUS] device={}, payload={}", message.getDeviceCode(), message.getPayload());
        // TODO: 转发至 DMS 后端 /api/v1/iot/status
    }

    private void handleAlarm(DeviceMessage message) {
        log.warn("[ALARM] device={}, payload={}", message.getDeviceCode(), message.getPayload());
        // TODO: 转发至 DMS 后端 /api/v1/iot/alarm
    }

    private void handleCommandAck(DeviceMessage message) {
        log.info("[ACK] device={}, payload={}", message.getDeviceCode(), message.getPayload());
        // TODO: 更新指令执行状态
    }
}
