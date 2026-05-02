package cn.org.openygt.iot.protocol;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 设备上报的统一消息模型（协议无关）
 *
 * <p>任何协议适配器解析原始报文后，必须转换为 DeviceMessage 再进入业务层。</p>
 */
@Data
public class DeviceMessage {

    /** 消息唯一标识 */
    private String messageId;

    /** 协议类型，如 penglin-mqtt, xinyan-tcp */
    private String protocolType;

    /** 设备编号 */
    private String deviceCode;

    /** 租户编号 */
    private String tenantId;

    /** 消息类型：TELEMETRY(遥测), STATUS(状态), ALARM(告警), COMMAND_ACK(指令响应) */
    private String messageType;

    /** 原始报文（Base64 或 HEX，用于调试审计） */
    private String rawPayload;

    /** 结构化数据载荷 */
    private Map<String, Object> payload = new HashMap<>();

    /** 时间戳 */
    private LocalDateTime timestamp;

    /** 消息来源 IP */
    private String sourceIp;
}
