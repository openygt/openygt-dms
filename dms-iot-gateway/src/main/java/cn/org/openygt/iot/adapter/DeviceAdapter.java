package cn.org.openygt.iot.adapter;

/**
 * 设备适配器通用接口
 *
 * <p>所有厂商协议适配器（MQTT/TCP二进制）必须实现此接口，通过 SPI 或手动注册到 {@link AdapterRegistry}。</p>
 *
 * <p>厂商实现类必须位于私有仓库中；开源仓库仅保留此接口及抽象基类。</p>
 *
 * @since 1.0.0
 */
public interface DeviceAdapter {

    /**
     * 协议类型标识，如 "penglin-mqtt", "weikang-mqtt", "xinyan-tcp"
     */
    String getProtocolType();

    /**
     * 启动适配器监听（启动 MQTT 订阅或 TCP 端口监听）
     */
    void start();

    /**
     * 停止适配器
     */
    void stop();

    /**
     * 向指定设备下发指令
     */
    void sendCommand(String deviceCode, DeviceCommandDTO command);

    /**
     * 获取适配器状态
     */
    AdapterStatus getStatus();
}
