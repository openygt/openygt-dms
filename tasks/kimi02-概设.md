# kimi02 概要设计 - 设备适配器框架 + TCP 适配器

> 对应需求: EQ-007(适配器框架), EQ-008(东华原TCP), EQ-009(三延TCP)  
> 日期: 2026-04-25  
> 作者: kimi02

---

## 1. 需求范围

### 1.1 对应需求编号

| 编号 | 需求 | 优先级 |
|------|------|--------|
| EQ-007 | 设备适配器框架（统一接口 + 注册中心） | P0 |
| EQ-008 | 东华原煎药机 TCP 二进制适配器 | P0 |
| EQ-009 | 三延煎药机 TCP 二进制适配器 | P0 |

### 1.2 功能边界

**在范围内：**
- 定义统一的 `DeviceAdapter` 接口（connect/disconnect/sendCommand/parseStatus）
- 实现 `AdapterRegistry` 注册中心，按 `manufacturer + protocolType` 自动路由适配器
- 基于 Netty 的 TCP 连接管理（连接池、心跳、重连）
- 东华原煎药机 TCP 二进制协议适配器（帧结构解析 + 指令组装）
- 三延煎药机 TCP 二进制协议适配器（帧结构解析 + 指令组装）
- 设备实体新增 `manufacturer` 字段，数据库迁移脚本

**不在范围内（由 kimi03 负责）：**
- MQTT 适配器（仟方/厚达）
- 设备影子（reported/desired）
- 心跳超时自动离线（心跳调度器主体，但 TCP 层心跳保活在范围内）

### 1.3 与现有代码的关系

当前 `MqttConfig.java` 中硬编码了 MQTT 消息处理逻辑（状态/故障/温度解析）。本设计引入 `DeviceAdapter` 抽象后，**第一阶段**保持 MQTT 逻辑不变（避免回归风险），**第二阶段**（Sprint 3 或后续）将 MQTT 消息处理也迁移到 Adapter 模式，由 `MqttConfig` 通过 `AdapterRegistry` 委托给对应的 MQTT 适配器（kimi03 实现）。

---

## 2. 模块设计

### 2.1 新增包结构

```
com.decoction.equipment.adapter/          # 新增包
├── DeviceAdapter.java                    # 统一适配器接口
├── AdapterRegistry.java                  # 适配器注册中心
├── connection/                           # TCP连接管理
│   ├── NettyTcpClient.java              # Netty客户端封装
│   ├── ConnectionPool.java              # 连接池（按deviceCode管理Channel）
│   ├── TcpMessageDecoder.java           # 通用TCP帧解码器基类
│   └── TcpMessageEncoder.java           # 通用TCP帧编码器基类
├── tcp/                                  # TCP协议适配器
│   ├── AbstractTcpAdapter.java          # TCP适配器抽象基类
│   ├── DonghuaYuanAdapter.java          # 东华原适配器
│   └── SanyanAdapter.java               # 三延适配器
└── protocol/                             # 协议解析工具
    ├── FrameParser.java                 # 帧解析接口
    └── ByteUtils.java                   # 字节操作工具（BCD、CRC16、高低位转换）
```

### 2.2 修改现有文件

| 文件 | 修改内容 |
|------|---------|
| `com.decoction.entity.Device` | 新增 `manufacturer` 字段 |
| `com.decoction.service.DeviceService` / `impl` | 新增 `connectDevice(deviceCode)`、`disconnectDevice(deviceCode)`、`sendCommand(deviceCode, command)` 方法 |
| `com.decoction.controller.DeviceController` | 新增 REST API：连接/断开/指令下发 |
| `db/migration/V5__adapter.sql` | 新增 manufacturer 字段 + 适配器配置表（可选） |

### 2.3 核心类设计

#### DeviceAdapter 接口

```java
public interface DeviceAdapter {
    /** 厂商编码，如 "donghuayuan", "sanyan" */
    String getManufacturer();
    
    /** 协议类型：TCP / MQTT */
    String getProtocol();
    
    /** 建立连接 */
    void connect(String deviceCode, DeviceConnectionInfo info);
    
    /** 断开连接 */
    void disconnect(String deviceCode);
    
    /** 发送指令 */
    void sendCommand(String deviceCode, DeviceCommand command);
    
    /** 解析原始数据为设备状态 */
    DeviceStatusReport parseStatus(byte[] rawData);
    
    /** 是否已连接 */
    boolean isConnected(String deviceCode);
}
```

#### AdapterRegistry 注册中心

```java
@Component
public class AdapterRegistry {
    // Map<manufacturer, Map<protocol, DeviceAdapter>>
    private final Map<String, Map<String, DeviceAdapter>> registry = new ConcurrentHashMap<>();
    
    public void register(DeviceAdapter adapter);
    public DeviceAdapter getAdapter(String manufacturer, String protocol);
    public DeviceAdapter getAdapterForDevice(String deviceCode); // 从数据库查 manufacturer+protocol
}
```

#### AbstractTcpAdapter（TCP 基类）

```java
public abstract class AbstractTcpAdapter implements DeviceAdapter {
    @Autowired protected ConnectionPool connectionPool;
    @Autowired protected DeviceMapper deviceMapper;
    
    @Override
    public void connect(String deviceCode, DeviceConnectionInfo info) {
        // 1. 从数据库获取设备 IP/Port
        // 2. 通过 NettyTcpClient 建立连接
        // 3. 注册 ChannelInboundHandler（含协议特定的帧解码器）
        // 4. 连接成功后上报 EquipmentService.updateDeviceStatus(IDLE)
    }
    
    @Override
    public void disconnect(String deviceCode) {
        connectionPool.close(deviceCode);
    }
    
    @Override
    public boolean isConnected(String deviceCode) {
        return connectionPool.isActive(deviceCode);
    }
    
    /** 子类实现：指令编码为字节 */
    protected abstract byte[] encodeCommand(DeviceCommand command);
    
    /** 子类实现：帧解析 */
    protected abstract FrameParser getFrameParser();
}
```

---

## 3. 接口设计

### 3.1 新增 REST API

| 方法 | 路径 | 说明 | 请求体 | 响应 |
|------|------|------|--------|------|
| POST | `/api/devices/{deviceCode}/connect` | 手动触发设备连接 | 无 | Device |
| POST | `/api/devices/{deviceCode}/disconnect` | 断开设备连接 | 无 | Device |
| POST | `/api/devices/{deviceCode}/command` | 下发指令 | `{"commandType":"START","params":{}}` | Device |
| GET | `/api/devices/{deviceCode}/connection` | 查询连接状态 | 无 | `{"connected":true,"lastHeartbeat":"2026-04-25T10:00:00"}` |

### 3.2 请求体示例

**下发指令:**
```json
{
  "commandType": "START",
  "params": {
    "targetTemp": 100,
    "duration": 30
  }
}
```

---

## 4. 数据库设计

### 4.1 修改现有表

**device 表新增字段:**

```sql
ALTER TABLE device ADD COLUMN manufacturer VARCHAR(32) DEFAULT NULL;
```

用途：标识设备厂商（`donghuayuan`, `sanyan`, `qianfang`, `houda` 等），供 `AdapterRegistry` 路由。

### 4.2 新增适配器配置表（可选，Sprint 1 建议预留）

```sql
CREATE TABLE device_adapter_config (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    manufacturer VARCHAR(32) NOT NULL,      -- 厂商编码
    protocol_type VARCHAR(16) NOT NULL,     -- TCP / MQTT
    frame_header VARCHAR(32),               -- 帧头字节（HEX）
    frame_tail VARCHAR(32),                 -- 帧尾字节（HEX）
    heartbeat_interval INT DEFAULT 30,      -- 心跳间隔（秒）
    reconnect_interval INT DEFAULT 10,      -- 重连间隔（秒）
    timeout_ms INT DEFAULT 5000,            -- 指令超时（毫秒）
    config_json TEXT,                       -- 扩展配置JSON
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

**是否必须？** Sprint 1 建议先不创建，将协议参数硬编码在适配器类中。待验证稳定后再抽取到配置表，避免过早抽象。

---

## 5. 与其他模块的交互

### 5.1 调用现有接口

| 调用方 | 被调用方 | 接口 | 场景 |
|--------|---------|------|------|
| `AdapterRegistry` | `DeviceMapper` | `selectByDeviceCode` | 查询设备 manufacturer/protocolType |
| `AbstractTcpAdapter` | `EquipmentService` | `updateDeviceStatus` | 连接成功后设为 IDLE，断开设为 OFFLINE |
| `AbstractTcpAdapter` | `EquipmentService` | `updateTemperature` | 解析状态报文后上报温度 |
| `AbstractTcpAdapter` | `EquipmentService` | `reportFault` | 解析故障报文后上报故障 |
| `DeviceController` | `DeviceService` | 新增 connect/disconnect/command | REST 层委托 |

### 5.2 被调用关系

| 调用方 | 场景 |
|--------|------|
| `DeviceController` | 用户手动触发连接/断开/指令 |
| `SoakTimeoutScheduler`（未来） | 泡药超时后自动调用 `sendCommand(START_DECOCT)` |
| `TaskServiceImpl`（未来） | `startDecoct` 时自动连接煎药机并下发启动指令 |
| `MqttConfig`（未来，kimi03） | MQTT 消息路由到对应 MQTT 适配器 |

---

## 6. 技术方案

### 6.1 TCP 连接管理选型：Netty vs NIO vs Spring Integration

| 方案 | 优点 | 缺点 | 结论 |
|------|------|------|------|
| **Netty 4.1** | ByteBuf 高效处理二进制；内置粘包/拆包解决（LengthFieldBasedFrameDecoder）；成熟稳定；支持 JDK 1.8 | 引入新依赖（~2MB） | **✅ 推荐** |
| Java NIO | 无额外依赖 | 代码复杂度高；需自行处理粘包/拆包；调试困难 | ❌ 不推荐 |
| Spring Integration TCP | Spring 生态整合好 | 过重；文档少；二进制协议支持弱 | ❌ 不推荐 |

**结论：引入 Netty 4.1.x（`io.netty:netty-all:4.1.94.Final`，支持 JDK 6+）。**

### 6.2 TCP 粘包/拆包处理

东华原和三延均为**固定长度帧**或**长度字段+变长 payload** 格式。使用 Netty 内置解码器：

```java
// 示例：LengthFieldBasedFrameDecoder（假设帧结构：2字节头 + 2字节长度 + N字节payload + 2字节CRC）
new LengthFieldBasedFrameDecoder(
    maxFrameLength,     // 最大帧长 1024
    lengthFieldOffset,  // 长度字段偏移（如 2）
    lengthFieldLength,  // 长度字段长度（如 2）
    lengthAdjustment,   // 长度补偿
    initialBytesToStrip // 是否剥离长度字段
);
```

子类（东华原/三延）各自配置自己的帧参数。

### 6.3 连接生命周期管理

```
[应用启动] → AdapterRegistry 加载所有 Adapter Bean
    ↓
[connect(deviceCode)] → NettyTcpClient.connect(ip, port)
    ↓
[ChannelActive] → EquipmentService.updateDeviceStatus(IDLE)
    ↓
[收到状态帧] → parseStatus(raw) → EquipmentService.updateTemperature()
    ↓
[ChannelInactive] → EquipmentService.updateDeviceStatus(OFFLINE)
    ↓
[异常断开] → 触发重连（指数退避：10s → 20s → 40s → 最大60s）
```

### 6.4 与现有 MQTT 处理的共存策略

当前 `MqttConfig` 直接调用 `EquipmentService` 更新状态。引入 Adapter 后：

- **Sprint 3 第一阶段**：TCP 设备走 Adapter，MQTT 设备仍走 `MqttConfig` 硬编码逻辑（互不干扰）。
- **后续迭代**：kimi03 实现 MQTT Adapter 后，`MqttConfig` 改为通过 `AdapterRegistry` 路由到 `QianfangAdapter` / `HoudaAdapter`。

### 6.5 难点与风险

| 难点 | 缓解措施 |
|------|---------|
| 厂商协议文档可能不完整 | 预留 `config_json` 扩展字段；协议解析部分用策略模式，方便热替换 |
| 并发指令下发（同一设备） | Netty Channel 天然单线程写入，无需额外同步 |
| TCP 长连接与 Spring 生命周期 | `NettyTcpClient` 实现 `DisposableBean`，应用关闭时优雅释放所有 Channel |
| 测试环境无真实设备 | 提供 `MockTcpAdapter` + `TcpSimulator`（基于 Netty ServerBootstrap），供单元测试和 simulator.py 使用 |

### 6.6 Mock 测试策略

```java
@Test
void donghuaYuanAdapter_shouldParseStatus() {
    // Mock Netty ConnectionPool（不建立真实TCP连接）
    when(connectionPool.getChannel("DHY001")).thenReturn(mockChannel);
    
    // 直接测试 parseStatus
    byte[] raw = HexUtils.decode("AA550102..."); // 模拟状态帧
    DeviceStatusReport report = adapter.parseStatus(raw);
    
    assertEquals(100, report.getTemperature());
    assertEquals("IDLE", report.getStatus());
}
```

---

## 7. 开发任务拆分（Sprint 3 参考）

| 子任务 | 优先级 | 预估工时 |
|--------|--------|---------|
| 引入 Netty 依赖 + ByteUtils 工具 | P0 | 0.5d |
| DeviceAdapter 接口 + AdapterRegistry | P0 | 1d |
| NettyTcpClient + ConnectionPool | P0 | 1.5d |
| AbstractTcpAdapter + 帧解码基类 | P0 | 1d |
| DonghuaYuanAdapter（协议解析） | P0 | 2d |
| SanyanAdapter（协议解析） | P0 | 2d |
| DeviceController REST API | P1 | 0.5d |
| MockTcpAdapter + TcpSimulator | P1 | 1d |
| 单元测试 + 集成测试 | P1 | 1.5d |

---

## 8. 需要架构师确认的问题

1. **Netty 依赖引入是否同意？** 版本建议 `4.1.94.Final`，与 Spring Boot 2.7.18 兼容。
2. **设备表新增 `manufacturer` 字段是否接受？** 类型 `VARCHAR(32)`，允许 NULL（存量设备逐步补充）。
3. **TCP 连接是否由业务层主动触发？** 当前设计为 `startDecoct` 时主动 `connect`，`endDecoct` 后 `disconnect`。是否改为应用启动时预连接所有 TCP 设备？
4. **适配器配置表是否 Sprint 1 就创建？** 建议 Sprint 3 先硬编码，稳定后再抽取。
