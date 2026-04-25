package cn.org.openygt.equipment.adapter;

import cn.org.openygt.equipment.dto.DeviceCommandDTO;

/**
 * 设备适配器接口 —— 所有协议适配器必须实现。
 *
 * <p>MVP 阶段仅定义接口和注册中心，东华原 TCP 二进制实现留阶段二。</p>
 */
public interface DeviceAdapter {

    /**
     * 建立与设备的连接（或启动服务端监听）。
     */
    void connect();

    /**
     * 断开连接（或停止服务端）。
     */
    void disconnect();

    /**
     * 向指定设备发送控制指令。
     */
    void sendCommand(String deviceCode, DeviceCommandDTO command);

    /**
     * 获取支持的协议类型。
     */
    String getProtocolType();

    /**
     * 获取厂商标识。
     */
    String getVendor();

    /**
     * 初始化注册到 AdapterRegistry。
     */
    default void register(AdapterRegistry registry) {
        registry.register(getProtocolType(), getVendor(), this);
    }
}
