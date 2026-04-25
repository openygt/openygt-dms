package cn.org.openygt.equipment.config;

import cn.org.openygt.common.service.EquipmentService;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.math.BigDecimal;

@Slf4j
@Configuration
public class MqttConfig {

    @Value("${mqtt.broker-url}")
    private String brokerUrl;

    @Value("${mqtt.client-id}")
    private String clientId;

    @Value("${mqtt.topic-subscription}")
    private String topicSubscription;

    private final EquipmentService equipmentService;
    private MqttClient mqttClient;

    public MqttConfig(EquipmentService equipmentService) {
        this.equipmentService = equipmentService;
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
            mqttClient.connect(options);

            mqttClient.subscribe(topicSubscription, (topic, msg) -> {
                String payload = new String(msg.getPayload());
                log.info("MQTT received: topic={}, payload={}", topic, payload);
                handleMessage(topic, payload);
            });

            log.info("MQTT subscribed to {}", topicSubscription);
        } catch (Exception e) {
            log.error("MQTT连接失败", e);
        }
    }

    private void handleMessage(String topic, String payload) {
        try {
            String[] parts = topic.split("/");
            if (parts.length < 3) {
                return;
            }
            String deviceCode = parts[1];
            String messageType = parts[2];

            Long deviceId = equipmentService.getDeviceId(deviceCode);
            if (deviceId == null) {
                log.warn("未知设备: {}", deviceCode);
                return;
            }

            switch (messageType) {
                case "status":
                    BigDecimal temp = new BigDecimal(payload.trim());
                    equipmentService.updateTemperature(deviceId, temp);
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
