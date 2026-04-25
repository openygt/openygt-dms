# kimi03 概要设计 - MQTT 适配器 + 设备影子 + 心跳

| 文档信息 | |
|---------|---|
| 版本 | V1.0 |
| 日期 | 2026-04-25 |
| 作者 | kimi03 |
| 对应需求 | EQ-010, EQ-011, EQ-013, EQ-014 |
| 所属模块 | dms-equipment |

---

## 1. 需求范围

### 1.1 功能边界

| 编号 | 功能 | 说明 | 优先级 |
|------|------|------|--------|
| EQ-010 | 仟方煎药机 MQTT 适配器 | 解析仟方 MQTT Topic 与 Payload | P0 |
| EQ-011 | 厚达煎药机 MQTT 适配器 | 解析厚达 MQTT Topic 与 Payload | P0 |
| EQ-013 | 设备影子（reported/desired） | 双态缓存，支持查询与下发 | P1 |
| EQ-014 | 心跳超时自动离线 | 检测设备心跳，超时标记 OFFLINE | P1 |

### 1.2 非本设计范围

- TCP 二进制适配器（东华原/三延）→ kimi02 负责
- 适配器框架接口定义（`DeviceAdapter` / `AdapterRegistry`）→ kimi02 负责，本设计复用
- 设备认证 Token（EQ-015）→ 预留，本期不实现

---

## 2. 模块设计

### 2.1 新增包结构

```
cn.org.openygt.equipment
├── adapter/                    # 设备适配器（与 kimi02 共享包）
│   ├── DeviceAdapter.java      # 已由 kimi02 定义
│   ├── mqtt/
│   │   ├── AbstractMqttAdapter.java   # MQTT 适配器抽象基类
│   │   ├── QianfangAdapter.java       # 仟方 MQTT 适配器
│   │   └── HoudaAdapter.java          # 厚达 MQTT 适配器
│   └── tcp/                    # kimi02 负责
├── shadow/                     # 设备影子
│   ├── DeviceShadowService.java
│   ├── DeviceShadow.java       # Entity
│   └── DeviceShadowMapper.java
├── heartbeat/                  # 心跳管理
│   ├── HeartbeatScheduler.java
│   └── HeartbeatService.java
└── config/
    └── MqttConfig.java         # 现有，需改造为适配器路由模式
```

### 2.2 类职责

| 类 | 职责 |
|---|------|
| `AbstractMqttAdapter` | 提供 MQTT 通用解析逻辑（Topic 匹配、JSON 提取、异常处理） |
| `QianfangAdapter` | 仟方协议：Topic `qianfang/{code}/telemetry`，Payload 含 temp/status/fault |
| `HoudaAdapter` | 厚达协议：Topic `houda/devices/{code}/properties/report`，Payload 含 temperature/state/errorCode |
| `DeviceShadowService` | 影子读写、版本控制、reported/desired 合并 |
| `HeartbeatScheduler` | `@Scheduled` 定时扫描，检测超时设备 |
| `HeartbeatService` | 记录心跳时间、更新设备 lastHeartbeatAt |

---

## 3. 接口设计

### 3.1 复用 kimi02 的 DeviceAdapter 接口

```java
public interface DeviceAdapter {
    String getManufacturer();          // "仟方", "厚达"
    String getProtocol();              // "MQTT", "TCP"
    boolean supports(String topic);    // MQTT 侧：根据 Topic 判断是否支持
    DeviceStatus parseStatus(String topic, String payload);  // 解析为统一状态对象
    String encodeCommand(DeviceCommand command);  // 将指令编码为 MQTT Payload
}
```

> **与 kimi02 协调点**：`parseStatus` 方法签名。kimi02 的 TCP 适配器签名可能是 `parseStatus(byte[] rawData)`，建议 MQTT 侧重载为 `parseStatus(String topic, String payload)`，或统一为 `parseStatus(Object rawData, Map<String, Object> context)`。推荐前者，简单直接。

### 3.2 MQTT 适配器抽象基类

```java
public abstract class AbstractMqttAdapter implements DeviceAdapter {
    @Override
    public String getProtocol() { return "MQTT"; }
    
    // 公共：提取 JSON 字段
    protected String jsonExtract(String payload, String path) { ... }
    
    // 公共：温度转换（仟方可能用摄氏度整数，厚达可能用浮点）
    protected BigDecimal parseTemperature(Object raw) { ... }
}
```

### 3.3 设备影子服务接口

```java
public interface DeviceShadowService {
    // 更新 reported 状态（设备上报时调用）
    void updateReported(Long deviceId, Map<String, Object> reported);
    
    // 设置 desired 状态（业务系统/用户下发指令时调用）
    void updateDesired(Long deviceId, Map<String, Object> desired);
    
    // 查询完整影子
    DeviceShadow getShadow(Long deviceId);
    
    // 获取某个 reported 字段
    Object getReportedValue(Long deviceId, String key);
    
    // desired 被设备确认后，合并到 reported 并清空 desired
    void mergeDesiredToReported(Long deviceId);
}
```

### 3.4 REST API（新增）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/eq/devices/{deviceId}/shadow` | 查询设备影子 |
| PUT | `/api/v1/eq/devices/{deviceId}/shadow/desired` | 设置 desired 状态 |
| GET | `/api/v1/eq/devices/{deviceId}/heartbeat` | 查询心跳状态 |

---

## 4. 数据库设计

### 4.1 新增表：eq_device_shadow（设备影子）

```sql
CREATE TABLE IF NOT EXISTS eq_device_shadow (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    tenant_id VARCHAR(32) DEFAULT 'default',
    device_id INTEGER NOT NULL UNIQUE,
    reported_state TEXT,              -- JSON: {"temp": 95.5, "status": "BUSY"}
    desired_state TEXT,               -- JSON: {"targetTemp": 100}
    version INTEGER DEFAULT 0,        -- 乐观锁版本
    last_reported_at DATETIME,
    last_desired_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);
```

### 4.2 eq_device 表扩展（心跳字段）

在现有 `eq_device` 表增加一列（通过 Flyway 迁移脚本）：

```sql
ALTER TABLE eq_device ADD COLUMN last_heartbeat_at DATETIME;
ALTER TABLE eq_device ADD COLUMN heartbeat_timeout_sec INTEGER DEFAULT 180; -- 3分钟，可按设备配置
```

> **设计理由**：心跳是设备属性，与设备强关联，无需独立表。影子是独立概念（reported/desired 双态），需要独立表。

### 4.3 实体类

```java
@Data
@TableName("eq_device_shadow")
public class DeviceShadow extends BaseEntity {
    private Long deviceId;
    private String reportedState;   // JSON 字符串，业务层序列化/反序列化
    private String desiredState;    // JSON 字符串
    private Integer version;
    private Date lastReportedAt;
    private Date lastDesiredAt;
}
```

---

## 5. 与其他模块的交互

### 5.1 调用关系图

```
[dms-equipment]
  │
  ├── MqttConfig (改造后)
  │     │
  │     ├── 收到 MQTT 消息
  │     │     │
  │     │     ▼
  │     ├── AdapterRegistry.getAdapter(topic) → 返回 QianfangAdapter / HoudaAdapter
  │     │
  │     ├── adapter.parseStatus(topic, payload) → DeviceStatus
  │     │
  │     ├── EquipmentService.updateTemperature() / updateDeviceStatus() / reportFault()
  │     │
  │     ├── DeviceShadowService.updateReported()  ← 更新影子
  │     │
  │     └── HeartbeatService.recordHeartbeat()    ← 更新心跳时间
  │
  ├── HeartbeatScheduler (@Scheduled)
  │     │
  │     ├── 扫描 last_heartbeat_at < now - timeout
  │     │
  │     └── EquipmentService.updateDeviceStatus(deviceId, "OFFLINE")
  │
  └── DeviceShadowController
        │
        ├── 接收外部 desired 设置请求
        │
        ├── DeviceShadowService.updateDesired()
        │
        └── MqttConfig.publish(deviceId, desiredPayload)  ← 下发到设备
```

### 5.2 与 dms-production 的交互

- dms-production 通过 `EquipmentService`（已有接口）查询设备状态、下发指令
- **新增**：dms-production 可通过 `DeviceShadowService.getShadow()` 查询设备最新 reported 状态（如当前温度、实际运行模式）
- **新增**：dms-production 可通过 `DeviceShadowService.updateDesired()` 设置目标参数（如目标温度、煎药时间）

### 5.3 与 dms-common 的交互

- `DeviceAdapter` 接口定义在 dms-common（或由 kimi02 定义在 dms-equipment 的 adapter 包，其他模块不直接依赖）
- 建议在 dms-common 新增 `DeviceShadowDTO` 和 `DeviceStatusDTO` 供跨模块使用

---

## 6. 技术方案

### 6.1 MQTT 适配器路由机制

**问题**：现有 `MqttConfig.handleMessage()` 是硬编码的 `switch (messageType)`，无法支持多厂商协议差异。

**方案**：

1. **Topic 前缀区分厂商**：
   - 仟方：`qianfang/{deviceCode}/+`
   - 厚达：`houda/devices/{deviceCode}/properties/+`
   - 通用（现有）：`device/{deviceCode}/{type}` → 作为默认/兜底处理

2. **MqttConfig 改造**：
   ```java
   @Autowired
   private List<DeviceAdapter> adapters;  // Spring 自动注入所有适配器
   
   private void handleMessage(String topic, String payload) {
       for (DeviceAdapter adapter : adapters) {
           if (adapter.supports(topic)) {
               DeviceStatus status = adapter.parseStatus(topic, payload);
               applyStatus(status);  // 统一应用状态
               return;
           }
       }
       // 兜底：走原有逻辑
       legacyHandleMessage(topic, payload);
   }
   ```

3. **仟方 vs 厚达 Topic/Payload 差异处理**：

   | 维度 | 仟方 (Qianfang) | 厚达 (Houda) |
   |------|----------------|-------------|
   | Topic | `qianfang/{code}/telemetry` | `houda/devices/{code}/properties/report` |
   | Payload 示例 | `{"temp": 95, "status": 1, "fault": ""}` | `{"temperature": 95.5, "state": "heating", "errorCode": ""}` |
   | 温度字段 | `temp` (整数) | `temperature` (浮点) |
   | 状态字段 | `status` (0=idle, 1=running, 2=fault) | `state` (字符串) |
   | 故障字段 | `fault` (字符串) | `errorCode` (字符串) |

   `QianfangAdapter` 和 `HoudaAdapter` 分别处理各自的字段映射，最终输出统一的 `DeviceStatus` 对象。

### 6.2 设备影子存储方案

| 方案 | 优点 | 缺点 | 结论 |
|------|------|------|------|
| A. 纯内存 (ConcurrentHashMap) | 读写极快 | 重启丢失，多实例不一致 | ❌ 不采用 |
| B. 纯数据库 (SQLite/MySQL) | 持久化，简单 | 高频读写性能差 | ⚠️ 备选 |
| C. 内存缓存 + 数据库异步写 | 读写快 + 持久化 | 实现复杂，可能丢秒级数据 | ⚠️ 过度设计 |
| **D. 数据库 + 本地缓存 (Caffeine)** | **读写快、持久化、实现简单** | 引入新依赖 | **✅ 采用** |

**详细设计**：
- 主存储：`eq_device_shadow` 表（SQLite/MySQL）
- 本地缓存：Caffeine Cache（key=deviceId, value=DeviceShadow, expireAfterWrite=5min）
- 写策略：先写数据库，再清缓存（或更新缓存）
- 读策略：先读缓存，缓存未命中则读数据库并回填

> **理由**：设备影子读多写少（每次 MQTT 消息写一次，业务查询读多次），Caffeine 是 Spring Boot 生态标准缓存，依赖轻量。

### 6.3 心跳超时检测方案

```java
@Component
public class HeartbeatScheduler {
    
    @Scheduled(fixedRate = 60000)  // 每分钟执行一次
    public void checkHeartbeatTimeout() {
        // 查询 last_heartbeat_at 为空 或 last_heartbeat_at < now - timeout 的设备
        List<EqDevice> timeoutDevices = deviceMapper.selectTimeoutDevices();
        
        for (EqDevice device : timeoutDevices) {
            if (!"OFFLINE".equals(device.getStatus())) {
                equipmentService.updateDeviceStatus(device.getId(), "OFFLINE");
                log.warn("设备心跳超时，标记离线: deviceCode={}, lastHeartbeat={}", 
                    device.getDeviceCode(), device.getLastHeartbeatAt());
            }
        }
    }
}
```

**心跳来源**：
- 设备主动发送心跳 MQTT 消息（如 `qianfang/DEV001/heartbeat`，payload 为空或 `{"ts": 1234567890}`）
- 若设备不发送独立心跳，则以最近一次任何 MQTT 消息（status/fault/telemetry）的时间戳作为心跳时间

**超时时间**：
- 默认 180 秒（3分钟）
- 可在 `eq_device.heartbeat_timeout_sec` 按设备配置

### 6.4 与现有 MqttConfig 的集成

**现有问题**：`MqttConfig` 是 `@Configuration` 类，逻辑与配置混合。

**改造方案**：
1. 将消息处理逻辑抽取为 `MqttMessageHandler`（`@Service`）
2. `MqttConfig` 只负责连接、订阅、回调分发
3. `MqttMessageHandler` 负责：Topic 解析 → 适配器路由 → 状态更新 → 影子更新 → 心跳记录

```java
@Service
public class MqttMessageHandler {
    @Autowired private List<DeviceAdapter> adapters;
    @Autowired private EquipmentService equipmentService;
    @Autowired private DeviceShadowService shadowService;
    @Autowired private HeartbeatService heartbeatService;
    
    public void handle(String topic, String payload) {
        // 1. 路由适配器
        DeviceAdapter adapter = adapters.stream()
            .filter(a -> a.supports(topic))
            .findFirst()
            .orElse(null);
        
        if (adapter == null) {
            legacyHandle(topic, payload);
            return;
        }
        
        // 2. 解析状态
        DeviceStatus status = adapter.parseStatus(topic, payload);
        
        // 3. 更新设备状态
        if (status.getTemperature() != null) {
            equipmentService.updateTemperature(status.getDeviceId(), status.getTemperature());
        }
        if (status.getStatus() != null) {
            equipmentService.updateDeviceStatus(status.getDeviceId(), status.getStatus());
        }
        if (status.getFaultCode() != null) {
            equipmentService.reportFault(status.getDeviceId(), status.getFaultCode(), status.getFaultMessage());
        }
        
        // 4. 更新影子
        shadowService.updateReported(status.getDeviceId(), status.toReportedMap());
        
        // 5. 记录心跳
        heartbeatService.recordHeartbeat(status.getDeviceId());
    }
}
```

---

## 7. 难点与风险

| 难点 | 风险等级 | 缓解措施 |
|------|---------|---------|
| 仟方/厚达实际 Topic/Payload 格式可能与假设不符 | 高 | 适配器设计预留字段映射配置表，或提供 `Map<String, String>` 字段映射配置 |
| Caffeine 缓存引入新依赖 | 低 | Spring Boot 已内置缓存抽象，可先用 `@Cacheable` + ConcurrentHashMap 简单实现，后续替换 |
| 心跳与 MQTT 消息去重 | 中 | 同一秒内的重复消息通过设备ID+时间戳去重 |
| 与 kimi02 的 DeviceAdapter 接口冲突 | 中 | 在 reply 中标记需协调点，等 kimi02 概设完成后对齐 |

---

## 8. 待协调事项

1. **kimi02**：`DeviceAdapter` 接口的 `parseStatus` 方法签名确认（MQTT 侧需要 `String topic, String payload`，TCP 侧需要 `byte[] rawData`）
2. **kimi02**：`AdapterRegistry` 是 Spring 的 `List<DeviceAdapter>` 注入，还是独立注册中心类？
3. **架构师**：仟方/厚达的实际 MQTT Topic 和 Payload 格式是否有协议文档？当前设计基于合理假设。

---

*本概设完成后提交架构师审核，审核通过后进入详细设计。*
