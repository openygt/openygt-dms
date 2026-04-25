package cn.org.openygt.equipment.adapter;

import cn.org.openygt.equipment.dto.DeviceCommandDTO;
import lombok.extern.slf4j.Slf4j;

/**
 * MQTT 协议适配器抽象基类。
 *
 * <p>当前由 MqttConfig 直接处理 JSON 消息，保留此类作为未来多厂商 MQTT 扩展的基类。</p>
 */
@Slf4j
public abstract class AbstractMqttAdapter implements DeviceAdapter {

    @Override
    public void connect() {
        log.info("MQTT adapter connected: vendor={}", getVendor());
    }

    @Override
    public void disconnect() {
        log.info("MQTT adapter disconnected: vendor={}", getVendor());
    }

    @Override
    public void sendCommand(String deviceCode, DeviceCommandDTO command) {
        doSendCommand(deviceCode, command);
    }

    protected abstract void doSendCommand(String deviceCode, DeviceCommandDTO command);

    @Override
    public String getProtocolType() {
        return "MQTT";
    }

    /**
     * 处理设备上报消息 —— 由 MqttConfig 路由调用。
     */
    public abstract void onMessage(String topic, String payload);
}
