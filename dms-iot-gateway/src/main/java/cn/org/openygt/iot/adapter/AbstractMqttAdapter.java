package cn.org.openygt.iot.adapter;

import cn.org.openygt.iot.protocol.DeviceMessage;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.nio.charset.StandardCharsets;

/**
 * MQTT 协议适配器抽象基类
 *
 * <p>朋霖、卫康等 MQTT 设备厂商继承此类，实现 {@link #decodeMessage(String, String)} 和 {@link #encodeCommand(DeviceCommandDTO)}。</p>
 *
 * <p>厂商实现必须位于私有仓库中；开源仓库仅保留此基类。</p>
 */
@Slf4j
public abstract class AbstractMqttAdapter implements DeviceAdapter {

    protected MqttClient mqttClient;
    protected AdapterStatus status = AdapterStatus.INITIALIZED;

    /** 子类定义 broker 地址 */
    protected abstract String getBrokerUrl();

    /** 子类定义客户端 ID */
    protected abstract String getClientId();

    /** 子类定义订阅主题 */
    protected abstract String[] getSubscribedTopics();

    /** 子类定义消息 QoS */
    protected int getQos() {
        return 1;
    }

    /** 子类实现：将 MQTT 消息解码为统一消息模型 */
    protected abstract DeviceMessage decodeMessage(String topic, String payload);

    /** 子类实现：将指令编码为 MQTT payload */
    protected abstract String encodeCommand(DeviceCommandDTO command);

    /** 子类定义指令发布主题 */
    protected abstract String getCommandTopic(String deviceCode);

    /** 消息解码后进入业务处理 —— 子类可选择性重写 */
    protected abstract void onMessage(DeviceMessage message);

    @Override
    public void start() {
        try {
            mqttClient = new MqttClient(getBrokerUrl(), getClientId(), new MemoryPersistence());
            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(false);
            options.setConnectionTimeout(10);
            options.setKeepAliveInterval(60);
            customizeConnectOptions(options);
            mqttClient.connect(options);

            mqttClient.subscribe(getSubscribedTopics(), new int[getSubscribedTopics().length]);
            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    log.warn("[{}] MQTT 连接丢失: {}", getProtocolType(), cause.getMessage());
                    status = AdapterStatus.ERROR;
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
                    log.debug("[{}] 收到消息 topic={}, payload={}", getProtocolType(), topic, payload);
                    DeviceMessage dm = decodeMessage(topic, payload);
                    if (dm != null) {
                        onMessage(dm);
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                }
            });

            status = AdapterStatus.RUNNING;
            log.info("[{}] MQTT 适配器已启动，订阅 {}", getProtocolType(), String.join(", ", getSubscribedTopics()));
        } catch (MqttException e) {
            log.error("[{}] MQTT 适配器启动失败: {}", getProtocolType(), e.getMessage(), e);
            status = AdapterStatus.ERROR;
        }
    }

    @Override
    public void stop() {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.disconnect();
                mqttClient.close();
            }
            status = AdapterStatus.STOPPED;
            log.info("[{}] MQTT 适配器已停止", getProtocolType());
        } catch (MqttException e) {
            log.error("[{}] MQTT 适配器停止异常: {}", getProtocolType(), e.getMessage(), e);
        }
    }

    @Override
    public void sendCommand(String deviceCode, DeviceCommandDTO command) {
        try {
            String topic = getCommandTopic(deviceCode);
            String payload = encodeCommand(command);
            mqttClient.publish(topic, payload.getBytes(StandardCharsets.UTF_8), getQos(), false);
            log.info("[{}] 下发指令 device={}, topic={}, command={}", getProtocolType(), deviceCode, topic, command.getCommandType());
        } catch (MqttException e) {
            log.error("[{}] 指令下发失败 device={}: {}", getProtocolType(), deviceCode, e.getMessage(), e);
        }
    }

    @Override
    public AdapterStatus getStatus() {
        return status;
    }

    /** 子类可重写以自定义连接参数（用户名/密码等） */
    protected void customizeConnectOptions(MqttConnectOptions options) {
    }
}
