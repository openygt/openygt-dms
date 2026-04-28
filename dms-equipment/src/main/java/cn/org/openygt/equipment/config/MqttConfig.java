package cn.org.openygt.equipment.config;

import cn.org.openygt.common.service.EquipmentService;
import cn.org.openygt.equipment.dto.DeviceStatusPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.math.BigDecimal;

/**
 * MQTT 配置与消息处理器。
 *
 * <p>Topic 格式: /openygt/{tenantId}/{deviceCode}/{messageType}</p>
 * <p>messageType: status / heartbeat / fault / online / offline</p>
 *
 * <p>★ JSON 解析修复：status 消息使用 ObjectMapper 解析完整 JSON，
 * 替代直接 new BigDecimal(payload.trim())。</p>
 */
@Configuration
public class MqttConfig {

    private static final Logger log = LoggerFactory.getLogger(MqttConfig.class);

    @Value("${mqtt.broker-url}")
    private String brokerUrl;

    @Value("${mqtt.client-id}")
    private String clientId;

    @Value("${mqtt.topic-subscription:+/+/+}")
    private String topicSubscription;

    @Value("${mqtt.username:}")
    private String mqttUsername;

    @Value("${mqtt.password:}")
    private String mqttPassword;

    private final EquipmentService equipmentService;
    private final ObjectMapper objectMapper;
    private MqttClient mqttClient;

    public MqttConfig(EquipmentService equipmentService, ObjectMapper objectMapper) {
        this.equipmentService = equipmentService;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        try {
            mqttClient = new MqttClient(brokerUrl, clientId);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);
            options.setConnectionTimeout(10);
            options.setKeepAliveInterval(20);
            if (mqttUsername != null && !mqttUsername.isEmpty()) {
                options.setUserName(mqttUsername);
                options.setPassword(mqttPassword.toCharArray());
            }
            mqttClient.connect(options);

            // 订阅 /openygt/tenantId/deviceCode/messageType 格式
            String subscribeTopic = "/openygt/" + topicSubscription;
            mqttClient.subscribe(subscribeTopic, (topic, msg) -> {
                String payload = new String(msg.getPayload());
                log.debug("MQTT received: topic={}, payload={}", topic, payload);
                handleMessage(topic, payload);
            });

            log.info("MQTT connected and subscribed to {}", subscribeTopic);
        } catch (Exception e) {
            log.error("MQTT连接失败", e);
        }
    }

    /**
     * 消息处理核心。
     * status 消息使用 ObjectMapper 解析 JSON，其余类型保持 string 处理。
     */
    private void handleMessage(String topic, String payload) {
        try {
            String[] parts = topic.split("/");
            if (parts.length < 5) {
                log.warn("Topic格式不匹配: {}", topic);
                return;
            }
            String tenantId = parts[2];
            String deviceCode = parts[3];
            String messageType = parts[4];

            Long deviceId = equipmentService.getDeviceId(deviceCode);
            if (deviceId == null) {
                log.warn("未知设备: {}, tenantId={}", deviceCode, tenantId);
                // 自动注册设备
                equipmentService.getOrCreateDevice(deviceCode, 1);
                deviceId = equipmentService.getDeviceId(deviceCode);
            }

            switch (messageType) {
                case "status":
                    handleStatusMessage(deviceId, payload);
                    break;
                case "heartbeat":
                    equipmentService.updateDeviceStatus(deviceId, "IDLE");
                    break;
                case "fault":
                    equipmentService.reportFault(deviceId, payload.trim(), "MQTT上报故障");
                    break;
                case "online":
                    equipmentService.updateDeviceStatus(deviceId, "IDLE");
                    break;
                case "offline":
                    equipmentService.updateDeviceStatus(deviceId, "OFFLINE");
                    break;
                default:
                    log.debug("未处理的消息类型: {}", messageType);
            }
        } catch (Exception e) {
            log.error("处理MQTT消息失败: topic={}", topic, e);
        }
    }

    /**
     * 处理状态上报消息。
     * ★ 修复：使用 ObjectMapper 解析 JSON，替代直接 new BigDecimal(payload.trim())
     */
    private void handleStatusMessage(Long deviceId, String payload) {
        try {
            DeviceStatusPayload statusPayload = objectMapper.readValue(payload, DeviceStatusPayload.class);

            if (statusPayload.getTemperature() != null) {
                equipmentService.updateTemperature(deviceId, statusPayload.getTemperature());
            }

            if (statusPayload.getFaultCode() != null && !statusPayload.getFaultCode().isEmpty()) {
                equipmentService.reportFault(deviceId, statusPayload.getFaultCode(), "状态上报故障");
            } else if (statusPayload.getStatus() != null) {
                equipmentService.updateDeviceStatus(deviceId, statusPayload.getStatus());
            }
        } catch (Exception e) {
            log.error("状态消息解析失败: deviceId={}, payload={}", deviceId, payload, e);
            // 兼容旧格式：直接解析为温度值
            try {
                BigDecimal temp = new BigDecimal(payload.trim());
                equipmentService.updateTemperature(deviceId, temp);
            } catch (Exception ignored) {
                // 兼容失败，已记录错误
            }
        }
    }

    @PreDestroy
    public void destroy() {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.disconnect();
                mqttClient.close();
            }
        } catch (Exception e) {
            log.error("MQTT断开失败", e);
        }
    }
}
