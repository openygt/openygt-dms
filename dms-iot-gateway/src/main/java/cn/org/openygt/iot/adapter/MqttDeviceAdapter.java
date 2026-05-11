package cn.org.openygt.iot.adapter;

import cn.org.openygt.iot.config.GatewayProperties;
import cn.org.openygt.iot.protocol.DeviceMessage;
import cn.org.openygt.iot.router.MessageRouter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * MQTT 设备适配器。
 * 订阅 /openygt/+/+/status、/openygt/+/+/online、/openygt/+/+/offline 等主题，
 * 将消息转换为 DeviceMessage 后路由到后端。
 */
@Slf4j
@Component
public class MqttDeviceAdapter implements DeviceAdapter {

    private final GatewayProperties properties;
    private final MessageRouter messageRouter;
    private MqttClient client;

    public MqttDeviceAdapter(GatewayProperties properties, MessageRouter messageRouter) {
        this.properties = properties;
        this.messageRouter = messageRouter;
    }

    @Override
    public String getProtocolType() {
        return "mqtt";
    }

    @Override
    public void start() {
        try {
            String broker = properties.getMqtt().getBroker();
            String clientId = properties.getMqtt().getClientId() + "-" + System.currentTimeMillis();
            client = new MqttClient(broker, clientId, new MemoryPersistence());

            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);
            options.setConnectionTimeout(10);
            options.setKeepAliveInterval(20);

            client.connect(options);

            // 订阅通配符主题
            String[] topics = {
                "/openygt/+/+/status",
                "/openygt/+/+/online",
                "/openygt/+/+/offline",
                "/openygt/+/+/fault"
            };
            int[] qos = {1, 1, 1, 1};
            client.subscribe(topics, qos);

            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    log.warn("MQTT连接断开: {}", cause.getMessage());
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    try {
                        handleMessage(topic, message);
                    } catch (Exception e) {
                        log.error("处理MQTT消息异常: topic={}", topic, e);
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    // ignore
                }
            });

            log.info("MQTT适配器启动成功: broker={}, clientId={}", broker, clientId);
        } catch (Exception e) {
            throw new IllegalStateException("MQTT适配器启动失败: " + e.getMessage(), e);
        }
    }

    private void handleMessage(String topic, MqttMessage mqttMessage) {
        String payload = new String(mqttMessage.getPayload(), StandardCharsets.UTF_8);
        log.debug("收到MQTT消息: topic={}, payload={}", topic, payload);

        // 解析topic: /openygt/{tenantId}/{deviceCode}/{messageType}
        String[] parts = topic.split("/");
        if (parts.length < 5) {
            log.warn("MQTT主题格式不正确: {}", topic);
            return;
        }
        String tenantId = parts[2];
        String deviceCode = parts[3];
        String messageType = parts[4];

        DeviceMessage msg = new DeviceMessage();
        msg.setProtocolType("mqtt");
        msg.setTenantId(tenantId);
        msg.setDeviceCode(deviceCode);
        msg.setMessageType(messageType);
        msg.setRawPayload(payload);
        msg.setTimestamp(LocalDateTime.now());

        Map<String, Object> data = parsePayload(payload);
        msg.setPayload(data);

        messageRouter.route(msg);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parsePayload(String payload) {
        if (payload == null || payload.trim().isEmpty()) {
            return new HashMap<>();
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(payload, HashMap.class);
        } catch (Exception e) {
            log.warn("JSON解析失败，使用原始payload: {}", payload);
            Map<String, Object> map = new HashMap<>();
            map.put("raw", payload);
            return map;
        }
    }

    @Override
    public void stop() {
        try {
            if (client != null && client.isConnected()) {
                client.disconnect();
                client.close();
            }
            log.info("MQTT适配器已停止");
        } catch (Exception e) {
            log.error("MQTT适配器停止异常", e);
        }
    }

    @Override
    public void sendCommand(String deviceCode, DeviceCommandDTO command) {
        try {
            String topic = "/openygt/default/" + deviceCode + "/command";
            String payload = command.getCommandType();
            client.publish(topic, payload.getBytes(StandardCharsets.UTF_8), 1, false);
            log.info("MQTT指令下发: deviceCode={}, topic={}, command={}", deviceCode, topic, command.getCommandType());
        } catch (Exception e) {
            log.error("MQTT指令下发失败: deviceCode={}", deviceCode, e);
            throw new IllegalStateException("指令下发失败: " + e.getMessage(), e);
        }
    }

    @Override
    public AdapterStatus getStatus() {
        if (client == null) return AdapterStatus.STOPPED;
        if (client.isConnected()) return AdapterStatus.RUNNING;
        return AdapterStatus.ERROR;
    }
}
