# 架构师审核 - kimi02 概要设计（适配器框架 + TCP）

> 审核日期: 2026-04-25
> 审核结论: **有条件通过** — 3 个问题确认后可进入详设

---

## 整体评价

概设质量优秀。Netty 选型正确，连接生命周期管理完整，与现有 MQTT 的共存策略务实（先不动 MQTT，避免回归风险）。

## 通过项

- Netty 4.1 选型合理，JDK8 兼容
- AbstractTcpAdapter 抽象层次恰当
- ConnectionPool 按 deviceCode 管理 Channel 的设计正确
- 与 EquipmentService 的集成方式清晰
- 测试策略（MockTcpAdapter + TcpSimulator）考虑周全

## 回答 4 个问题

1. **Netty 依赖**：同意引入 `io.netty:netty-all:4.1.94.Final`。

2. **manufacturer 字段**：同意。但注意当前 decoction 项目的表名是 `device`（无前缀），openygt-dms 是 `eq_device`。在 decoction 项目中开发时用 `device` 表，迁移脚本写 `ALTER TABLE device ADD COLUMN manufacturer VARCHAR(32)`。

3. **TCP 连接时机**：采用**按需连接**（`startDecoct` 时 connect，任务完成后 disconnect）。不要预连接——设备可能不在线，预连接会产生大量重连噪音。但要支持手动 connect API（调试用）。

4. **适配器配置表**：同意先不创建，硬编码在适配器类中。

## 需修正（1项）

**DeviceAdapter 接口需要兼容 TCP 和 MQTT 两种签名**。kimi03 提出了同样的问题。统一方案：

```java
public interface DeviceAdapter {
    String getManufacturer();
    String getProtocol();  // "TCP" or "MQTT"
    
    // TCP 侧使用
    void connect(String deviceCode, DeviceConnectionInfo info);
    void disconnect(String deviceCode);
    boolean isConnected(String deviceCode);
    void sendCommand(String deviceCode, DeviceCommand command);
    DeviceStatusReport parseStatus(byte[] rawData);
    
    // MQTT 侧使用
    default boolean supports(String topic) { return false; }
    default DeviceStatusReport parseMqttStatus(String topic, String payload) { 
        throw new UnsupportedOperationException(); 
    }
}
```

TCP 适配器实现 `parseStatus(byte[])`，MQTT 适配器实现 `supports()` + `parseMqttStatus()`。用 `default` 方法避免强制实现不相关的方法。

请在详设中与 kimi03 对齐这个接口定义。
