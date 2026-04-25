# dms-equipment 需求与概要设计

> 模块类型：业务模块  
> 包名：`cn.org.openygt.equipment`  
> 表前缀：`eq_`  
> API 前缀：`/api/v1/eq`  
> 职责：设备全生命周期管理、IoT 接入、多厂商协议适配、温度监控与告警、设备配对关系。  
> 版本：V1.3（专家评审03后修订版）

---

## 一、模块定位

`dms-equipment` 是系统的**设备物联核心**，负责连接和管理所有煎药机、包装机、打印机等硬件设备。通过 MQTT 和 TCP 两种协议与设备通信，并向上层模块（主要是 `dms-production`）提供设备状态查询、控制、温度上报等能力。

**模块边界**：
- ✅ 负责 `eq_` 前缀表的管理
- ✅ 实现 `dms-common` 中的 `EquipmentService` SPI 接口
- ✅ 管理 MQTT/TCP 连接、消息收发
- ✅ 处理设备告警、温度曲线记录
- ❌ 不直接操作生产任务（通过 SPI 被 `dms-production` 调用）
- ❌ 不管理处方、质检、打印等业务数据

---

## 二、需求清单

### 2.1 已实现需求

| 需求编号 | 需求名称 | 优先级 | 状态 | 验收标准 |
|---------|---------|--------|------|---------|
| EQ-001 | 设备注册与信息维护 | P0 | ✅ | CRUD、编码唯一、类型区分 |
| EQ-002 | 设备状态管理 | P0 | ✅ | IDLE/RUNNING/FAULT/OFFLINE/MAINTENANCE |
| EQ-003 | MQTT 状态接收与推送 | P0 | ✅ | 订阅 `device/+/status`，下发 `device/{id}/command` |
| EQ-004 | 温度遥测与告警 | P0 | ✅ | 实时温度记录、超阈值告警写入 `eq_device_alarm` |
| EQ-005 | 设备配对关系 | P0 | ✅ | 煎药机 ↔ 包装机绑定 |
| EQ-006 | 设备自动创建 | P0 | ✅ | 首次扫码自动注册 |

### 2.2 待开发需求（当前阶段重点）

> **专家评审结论**：12天接4家设备是神话，MVP只接东华原1家，其他放阶段二。

| 需求编号 | 需求名称 | 优先级 | 状态 | 验收标准 |
|---------|---------|--------|------|---------|
| **EQ-007a** | **东华原协议调研（先文档/抓包）** | **P0** | ⏳ | **没文档不写代码。定义报文格式、字段含义、时序图** |
| EQ-007 | 设备适配器框架 | P0 | ⏳ | 统一接口 `DeviceAdapter`、注册中心 `AdapterRegistry`、运行时动态选择 |
| **EQ-008** | **东华原煎药机 TCP 二进制适配器（MVP唯一）** | **P0** | ⏳ | **MVP只接这1家，保质量** |
| EQ-009 | 三延煎药机 TCP 二进制适配器 | P1 | ⏳ | 阶段二 |
| EQ-010 | 仟方煎药机 MQTT 适配器 | P1 | ⏳ | 阶段二 |
| EQ-011 | 厚达煎药机 MQTT 适配器 | P1 | ⏳ | 阶段二 |
| EQ-012 | 包装机适配器 | P2 | ⏳ | 阶段三 |
| EQ-013 | 设备影子（Device Shadow） | P2 | ⏳ | reported/desired 双态、同步机制 |
| **EQ-014** | **心跳超时自动离线（分状态阈值）** | **P1** | ⏳ | **IDLE超时120s，RUNNING超时600s，或用温度上报代心跳** |
| EQ-015 | 设备认证 Token | P2 | ⏳ | 设备连接鉴权、Token 刷新 |
| **EQ-016** | **温度阈值三层继承** | **P1** | ⏳ | **sys_config默认 → eq_device设备级 → md_decoct_scheme方案级** |
| **EQ-017** | **并发控制：synchronized→数据库悲观锁** | **P1** | ⏳ | **SELECT FOR UPDATE 或版本号** |
| **EQ-018** | **返工设备预留（reserveDevice）** | **P0** | ⏳ | **返工不释放设备，标记预留防抢占** |

---

## 三、数据库设计

### 3.1 eq_device（设备档案表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | INTEGER | PK, AUTOINCREMENT | 主键 |
| tenant_id | VARCHAR(32) | DEFAULT 'default' | 多租户预留 |
| device_code | VARCHAR(50) | NOT NULL, UNIQUE | 设备编码（扫码/铭牌） |
| name | VARCHAR(100) | | 设备名称 |
| device_type | VARCHAR(20) | | COOKING_MACHINE / PACKING_MACHINE / PRINTER |
| status | VARCHAR(20) | DEFAULT 'OFFLINE' | IDLE / RUNNING / FAULT / OFFLINE / MAINTENANCE |
| current_temp | DECIMAL(5,2) | | 当前温度（℃） |
| fault_code | VARCHAR(50) | | 当前故障码 |
| auto_level | VARCHAR(20) | DEFAULT 'manual' | manual / semi / auto（自动化级别） |
| location | VARCHAR(100) | | 位置描述 |
| ip_address | VARCHAR(50) | | 设备 IP |
| mqtt_client_id | VARCHAR(100) | | MQTT Client ID |
| protocol_type | VARCHAR(20) | | 协议类型：MQTT / TCP_BINARY / TCP_ASCII |
| vendor | VARCHAR(50) | | 厂商：东华原 / 三延 / 仟方 / 厚达 |
| alarm_high_temp | DECIMAL(5,2) | DEFAULT 120 | 高温告警阈值 |
| alarm_low_temp | DECIMAL(5,2) | DEFAULT 50 | 低温告警阈值 |
| last_heartbeat | DATETIME | | 最后心跳时间 |
| created_at | DATETIME | | 创建时间 |
| updated_at | DATETIME | | 更新时间 |
| deleted | INTEGER | DEFAULT 0 | 逻辑删除 |

**索引**：`UNIQUE(device_code)`

### 3.2 eq_device_connection（设备配对表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | INTEGER | PK, AUTOINCREMENT | 主键 |
| tenant_id | VARCHAR(32) | DEFAULT 'default' | 多租户预留 |
| source_device_id | INTEGER | NOT NULL | 源设备 ID（煎药机） |
| target_device_id | INTEGER | NOT NULL | 目标设备 ID（包装机） |
| connection_type | VARCHAR(20) | DEFAULT 'BIND' | 配对类型 |
| created_at | DATETIME | | 创建时间 |
| deleted | INTEGER | DEFAULT 0 | 逻辑删除 |

**约束**：`source_device_id` 和 `target_device_id` 组合唯一。

### 3.3 eq_device_alarm（设备告警表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | INTEGER | PK, AUTOINCREMENT | 主键 |
| tenant_id | VARCHAR(32) | DEFAULT 'default' | 多租户预留 |
| device_id | INTEGER | NOT NULL | 设备 ID |
| alarm_type | VARCHAR(50) | | 告警类型：HIGH_TEMP / LOW_TEMP / FAULT / OFFLINE |
| alarm_level | VARCHAR(20) | | 级别：WARNING / CRITICAL |
| message | TEXT | | 告警内容 |
| is_resolved | INTEGER | DEFAULT 0 | 是否已处理 |
| resolved_at | DATETIME | | 处理时间 |
| created_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | 告警时间 |
| deleted | INTEGER | DEFAULT 0 | 逻辑删除 |

### 3.4 eq_temperature_log（温度遥测日志表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | INTEGER | PK, AUTOINCREMENT | 主键 |
| tenant_id | VARCHAR(32) | DEFAULT 'default' | 多租户预留 |
| device_id | INTEGER | NOT NULL | 设备 ID |
| temperature | DECIMAL(5,2) | | 温度值 |
| recorded_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | 记录时间 |

> **评审修订03**：`EqTemperatureLog` **不继承 `BaseEntity`**，改继承 `BaseAuditEntity`（不含 `deleted` 字段和 `@TableLogic`）。若仍继承 `BaseEntity`，MyBatis-Plus 会自动追加 `WHERE deleted = 0`，导致归档数据无法查询。  
> 温度日志是审计数据，应**物理保留+定期归档**（保留3年），符合GMP审计要求。  
> 归档策略：每月底将3个月前的数据迁移到 `eq_temperature_log_archive` 表或导出到文件。

**索引**：`INDEX(device_id, recorded_at)` — 用于温度曲线查询

### 3.5 设备影子预留表（EQ-013）

| 表名 | 说明 |
|------|------|
| eq_device_shadow | 设备影子：reported（设备上报状态）、desired（云端期望状态）、version |

---

## 四、接口设计

### 4.1 REST API

#### 设备管理

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/eq/devices` | 注册设备 |
| GET | `/api/v1/eq/devices/{id}` | 查询设备详情 |
| PUT | `/api/v1/eq/devices/{id}` | 更新设备信息（含温度阈值覆盖） |
| DELETE | `/api/v1/eq/devices/{id}` | 删除设备（逻辑删除） |
| GET | `/api/v1/eq/devices` | 分页查询（支持 status/type/vendor 过滤） |
| POST | `/api/v1/eq/devices/{id}/command` | 下发指令（启动/停止/参数设置） |
| POST | `/api/v1/eq/devices/{id}/reserve` | **预留设备（返工场景）** |
| POST | `/api/v1/eq/devices/{id}/release` | **释放预留** |

#### 设备配对

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/eq/connections` | 创建设备配对 |
| DELETE | `/api/v1/eq/connections/{id}` | 解除配对 |
| GET | `/api/v1/eq/connections` | 查询配对列表 |

#### 温度与告警

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/eq/devices/{id}/temperatures` | 查询温度曲线（支持时间范围） |
| GET | `/api/v1/eq/alarms` | 分页查询告警（支持 deviceId/type/是否已处理） |
| PUT | `/api/v1/eq/alarms/{id}/resolve` | 标记告警为已处理 |

### 4.2 MQTT Topic 设计

| Topic | 方向 | Payload 说明 |
|-------|------|-------------|
| `device/{deviceCode}/status` | 设备 → 系统 | 状态+温度+故障码 JSON |
| `device/{deviceCode}/heartbeat` | 设备 → 系统 | 心跳包（空或设备编码） |
| `device/{deviceCode}/command` | 系统 → 设备 | 指令 JSON（action + params） |
| `device/{deviceCode}/response` | 设备 → 系统 | 指令响应结果 |
| `device/{deviceCode}/fault` | 设备 → 系统 | 故障上报 |

**MQTT 消息格式示例（status）**：

```json
{
  "deviceCode": "DHY-001",
  "status": "RUNNING",
  "temperature": 105.5,
  "faultCode": null,
  "timestamp": "2026-04-25T09:00:00Z"
}
```

### 4.3 设备适配器框架（EQ-007）

```java
// 统一适配器接口
public interface DeviceAdapter {
    String getVendor();           // 厂商名称
    String getProtocolType();     // MQTT / TCP_BINARY / TCP_ASCII
    
    void connect(DeviceConfig config);
    void disconnect();
    boolean isConnected();
    
    void sendCommand(String deviceCode, Command command);
    // parseStatusMessage 已彻底移出公共接口，下沉为各适配器内部私有方法
}

// 注册中心
@Component
public class AdapterRegistry {
    private final Map<String, DeviceAdapter> adapters = new ConcurrentHashMap<>();
    
    public void register(DeviceAdapter adapter) { ... }
    public DeviceAdapter getAdapter(String vendor) { ... }
}

// MQTT 适配器基类
public abstract class AbstractMqttAdapter implements DeviceAdapter {
    // 封装 Paho MQTT 通用逻辑
}

// TCP 二进制适配器基类
public abstract class AbstractTcpBinaryAdapter implements DeviceAdapter {
    // 封装 Netty/阻塞 Socket 通用逻辑
    // 提供字节流解析钩子
}
```

**适配器实现清单**：

| 适配器类 | 厂商 | 协议 | 设备类型 | 优先级 |
|---------|------|------|---------|--------|
| `DonghuaYuanCookingAdapter` | 东华原 | TCP_BINARY | 煎药机 | P0 |
| `SanyanCookingAdapter` | 三延 | TCP_BINARY | 煎药机 | P0 |
| `QianfangMqttAdapter` | 仟方 | MQTT | 煎药机 | P0 |
| `HoudaMqttAdapter` | 厚达 | MQTT | 煎药机 | P0 |
| `DonghuaYuanPackingAdapter` | 东华原 | TCP_BINARY | 包装机 | P1 |
| `SanyanPackingAdapter` | 三延 | TCP_BINARY | 包装机 | P1 |

---

## 五、核心类结构

```
cn.org.openygt.equipment
├── adapter                    # 设备适配器框架
│   ├── DeviceAdapter.java
│   ├── AdapterRegistry.java
│   ├── mqtt/
│   │   ├── AbstractMqttAdapter.java
│   │   ├── QianfangMqttAdapter.java
│   │   └── HoudaMqttAdapter.java
│   └── tcp/
│       ├── AbstractTcpBinaryAdapter.java
│       ├── DonghuaYuanCookingAdapter.java
│       ├── SanyanCookingAdapter.java
│       ├── DonghuaYuanPackingAdapter.java
│       └── SanyanPackingAdapter.java
├── config
│   └── MqttConfig.java        # MQTT 客户端配置
├── controller
│   ├── EqDeviceController.java
│   ├── EqDeviceConnectionController.java
│   └── EqAlarmController.java
├── entity
│   ├── EqDevice.java
│   ├── EqDeviceConnection.java
│   ├── EqDeviceAlarm.java
│   └── EqTemperatureLog.java
├── mapper
│   ├── EqDeviceMapper.java
│   ├── EqDeviceConnectionMapper.java
│   ├── EqDeviceAlarmMapper.java
│   └── EqTemperatureLogMapper.java
├── service
│   ├── EqDeviceService.java / EqDeviceServiceImpl.java
│   ├── EquipmentServiceImpl.java   # 实现 dms-common 的 EquipmentService SPI
│   └── AlarmService.java / AlarmServiceImpl.java
└── dto
    ├── DeviceCreateRequest.java
    ├── DeviceCommandRequest.java
    └── TemperatureQueryRequest.java
```

---

## 六、核心流程

### 6.1 设备状态上报处理流程

```
设备发送 MQTT status 消息
    ↓
MqttConfig.messageArrived()
    ↓
解析 JSON → 提取 deviceCode, status, temperature, faultCode
    ↓
查询 eq_device（不存在则自动创建：EQ-006）
    ↓
更新设备状态、温度、最后心跳时间
    ↓
若 temperature 不为空 → 写入 eq_temperature_log
    ↓
调用 checkTemperatureAlarm() → 若超阈值写入 eq_device_alarm
    ↓
若 status 为 FAULT → 写入 eq_device_alarm
    ↓
若任务处于"待煎药"且温度上报 → 通知 production 自动推进
```

### 6.2 设备指令下发流程

```
Production / 前端调用下发指令
    ↓
EqDeviceController.command()
    ↓
根据设备 vendor + protocol_type 从 AdapterRegistry 获取适配器
    ↓
适配器封装协议 → 发送 MQTT / TCP 报文
    ↓
设备执行 → 返回 response
    ↓
更新 eq_device_shadow desired / reported
```

### 6.3 心跳超时检测流程（EQ-014，评审修订：分状态阈值）

```
@Scheduled(fixedRate = 60000)
HeartbeatCheckScheduler.run()
    ↓
查询所有状态 ≠ OFFLINE 的设备
    ↓
根据设备状态取不同阈值：
    - IDLE 状态：timeout = 120s（sys_config: device.heartbeat.idle.timeout）
    - RUNNING 状态：timeout = 600s（sys_config: device.heartbeat.running.timeout）
    - 其他状态：timeout = 300s（默认）
    ↓
若 now - last_heartbeat > timeout
    ↓
更新 status = OFFLINE
    ↓
写入 eq_device_alarm(type=OFFLINE)
```

> **评审修订03**：**温度上报时同步更新 `last_heartbeat`**。设备每次上报温度，在写入 `eq_temperature_log` 的同时，更新 `eq_device.last_heartbeat = now()`。这样心跳检测逻辑不需要改，温度上报自然延长在线判定。不需要两套独立方案。

---

## 七、开发规范

1. **适配器线程安全**：`DeviceAdapter` 的实现必须线程安全，可被多个设备并发调用。
2. **TCP 连接管理**：TCP 适配器需维护连接池，断线自动重连，重连间隔指数退避。
3. **MQTT 消息幂等**：设备可能重复上报同一状态，更新数据库时需判断时间戳，丢弃过时消息。
4. **温度采样频率**：设备上报温度频率可能很高（秒级），数据库写入建议批量或降采样，避免单条 INSERT 压垮 SQLite/**MySQL**。
5. **告警去重**：同一设备的同一类型未恢复告警，不要重复创建记录，而是更新 existing 记录。
6. **并发控制（评审强制）**：禁止继续使用 `synchronized`。设备绑定/释放使用数据库悲观锁：
   ```java
   // 示例：绑定设备时加锁
   @Transactional
   public void bindDevice(...) {
       EqDevice device = eqDeviceMapper.selectForUpdate(deviceId);
       if (!"IDLE".equals(device.getStatus())) {
           throw new IllegalStateException("设备已被占用");
       }
       device.setStatus("RUNNING");
       eqDeviceMapper.updateById(device);
   }
   ```
7. **温度阈值三层继承（评审强制）**：
   - 第一层（默认）：`sys_config` 中 `temp.alarm.high` / `temp.alarm.low`
   - 第二层（设备级）：`eq_device.alarm_high_temp` / `alarm_low_temp`，若不为空则覆盖默认
   - 第三层（方案级）：`md_decoct_scheme` 中可配置方案专属阈值，煎药时传入覆盖设备级
   - 优先级：**方案级 > 设备级 > 系统默认**
8. **多租户排除清单（评审新增）**：`TenantLineInnerInterceptor` 必须排除以下审计/遥测表：
   - `sys_log`
   - `eq_temperature_log`
   - `eq_temperature_log_archive`
   - `prod_task_status_history`
   避免归档查询和跨租户审计被租户条件拦截。
9. **温度日志归档**：`eq_temperature_log` 不逻辑删除，每月归档3个月前的数据。
8. **温度日志归档**：`eq_temperature_log` 不逻辑删除，每月归档3个月前的数据。

---

## 八、集成检查清单

- [ ] `EquipmentServiceImpl` 完整实现了 `dms-common` 的 `EquipmentService` 接口
- [ ] `eq_` 前缀业务表（`eq_device`、`eq_device_connection`、`eq_device_alarm`）继承 `BaseEntity`
- [ ] `eq_temperature_log` 继承 `BaseAuditEntity`（不含逻辑删除）
- [ ] MQTT 客户端在应用启动时自动连接，关闭时优雅断开
- [ ] 设备自动创建逻辑正确（`getOrCreateDevice`）
- [ ] 温度告警阈值可从 `sys_config` 读取
- [ ] 适配器框架支持运行时动态注册
- [ ] `mvn test` 单测通过，适配器有单元测试（可用 Mock）
