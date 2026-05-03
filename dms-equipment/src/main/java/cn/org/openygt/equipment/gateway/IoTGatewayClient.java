package cn.org.openygt.equipment.gateway;

import java.util.Map;

/**
 * IoT 网关客户端接口
 *
 * <p>通过 HTTP 调用 dms-iot-gateway 的 REST API 完成指令下发。</p>
 */
public interface IoTGatewayClient {

    /**
     * 向指定设备下发指令
     *
     * @param deviceCode   设备编码
     * @param protocolType 协议类型，如 "penglin-mqtt", "weikang-mqtt", "xinyan-tcp"
     * @param commandType  指令类型，如 START_SOAK, START_DECOCT 等
     * @param params       指令参数
     */
    boolean sendCommand(String deviceCode, String protocolType, String commandType, Map<String, Object> params);
}
