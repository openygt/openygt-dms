# 架构师审核 - kimi02 详细设计（适配器框架 + TCP）

> 审核日期: 2026-04-25
> 审核结论: **通过** — 可直接开发

---

## 整体评价

详设质量非常高（42KB）。Netty 连接管理、ConnectionPool、指数退避重连、TcpInboundHandler 的实现都很专业。东华原/三延的协议帧定义清晰，CRC16 校验完整。

## 通过项

- NettyTcpClient + ConnectionPool 设计干净，DisposableBean 优雅关闭
- AbstractTcpAdapter 的 onMessageReceived/onDisconnected 回调链路完整
- 东华原/三延帧结构定义合理（AA55 帧头 + 命令字 + 长度 + 数据 + CRC16）
- 指数退避重连策略正确
- 包名使用 `com.decoction`

## 接口对齐方案（已确定）

`DeviceAdapter` 接口由你定义，统一方案：

```java
public interface DeviceAdapter {
    String getManufacturer();
    String getProtocol();
    
    // TCP 侧
    void connect(String deviceCode, DeviceConnectionInfo info);
    void disconnect(String deviceCode);
    void sendCommand(String deviceCode, DeviceCommand command);
    boolean isConnected(String deviceCode);
    DeviceStatusReport parseStatus(byte[] rawData);
    
    // MQTT 侧（default 实现，TCP 适配器无需关心）
    default boolean supports(String topic) { return false; }
    default DeviceStatusReport parseMqttMessage(String topic, String payload) {
        throw new UnsupportedOperationException();
    }
}
```

DTO 统一用你的 `DeviceStatusReport`，加上 `toReportedMap()` 方法。Flyway 脚本你用 V5。
