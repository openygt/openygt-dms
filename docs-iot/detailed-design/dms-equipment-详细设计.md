# dms-equipment 模块详细设计文档（V1.4）

> 文档版本：V1.4  
> 模块：煎药室管理系统 — 设备物联模块（dms-equipment）  
> 适用范围：设备管理、MQTT 通信、温度监控、设备适配器框架  

---

## 一、模块概述

### 1.1 职责边界

`dms-equipment` 模块是煎药室管理系统的设备物联核心，负责：

1. **设备生命周期管理**：煎药机、包装机、清洗机等 IoT 设备的注册、查询、修改、删除；
2. **设备状态监控**：实时接收设备状态、温度、故障码，维护设备在线/离线状态；
3. **温度告警**：基于三层继承的温度阈值策略，触发超温/低温告警；
4. **设备并发控制**：煎药任务排程时的设备占用与释放，采用数据库悲观锁；
5. **多协议适配**：通过设备适配器框架统一接入 MQTT（JSON）与 TCP 二进制（东华原等厂商）协议。

### 1.2 模块位置

```
openygt-dms/
├── dms-equipment/                # 本模块
│   ├── src/main/java/com/openygt/dms/equipment/
│   │   ├── controller/           # EqDeviceController
│   │   ├── service/
│   │   │   ├── EqDeviceService.java
│   │   │   ├── impl/
│   │   │   │   ├── EqDeviceServiceImpl.java
│   │   │   │   └── EquipmentServiceImpl.java   # SPI 实现
│   │   ├── adapter/              # 【新增】设备适配器框架
│   │   │   ├── DeviceAdapter.java
│   │   │   ├── AdapterRegistry.java
│   │   │   ├── AbstractMqttAdapter.java
│   │   │   ├── AbstractTcpBinaryAdapter.java
│   │   │   └── vendor/
│   │   │       └── DonghuayuanTcpAdapter.java  # MVP 唯一实现
│   │   ├── mqtt/                 # MQTT 配置与消息处理
│   │   │   └── MqttConfig.java
│   │   ├── entity/               # MyBatis-Plus 实体
│   │   ├── dto/                  # 数据传输对象
│   │   ├── mapper/               # MapStruct 映射器
│   │   ├── repository/           # MyBatis-Plus Mapper
│   │   └── enums/                # 状态、类型、告警等级枚举
│   └── pom.xml
```

### 1.3 依赖关系

| 依赖模块 | 用途 |
|---------|------|
| `dms-common` | 基础实体（BaseEntity、BaseAuditEntity）、工具类、统一异常 |
| `dms-masterdata` | 调用 `MdDecoctSchemeService` 获取方案级温度阈值 |
| `dms-system` | 读取 `sys_config` 获取系统默认温度阈值 |
| `spring-boot-starter-integration-mqtt` | MQTT 消息收发 |
| `netty`（新增） | TCP 二进制协议接入（东华原） |

---

## 二、代码现状 vs 目标差距

### 2.1 差距总览表

| # | 功能点 | 现状 | 目标（V1.4） | 优先级 |
|---|--------|------|-------------|--------|
| 1 | 设备适配器框架 | **完全未实现** | DeviceAdapter + AdapterRegistry + 两类基类 + 东华原实现 | P0 |
| 2 | MQTT JSON 解析 | `case "status"` 直接 `new BigDecimal(payload.trim())` | 解析 JSON 提取 deviceCode/status/temperature/faultCode | P0 |
| 3 | 心跳超时离线 | **未实现** | 分状态超时策略：IDLE 120s / RUNNING 600s | P0 |
| 4 | 温度阈值继承 | **未实现** | sys_config 默认 → eq_device 设备级 → md_decoct_scheme 方案级 | P1 |
| 5 | EquipmentService 返回值 | 返回 `Object` | 返回 `EqDeviceDTO`，实现 `reserveDevice`/`releaseDevice` | P1 |
| 6 | 并发控制 | `synchronized` 方法锁 | 数据库悲观锁 `SELECT FOR UPDATE` | P1 |
| 7 | EqTemperatureLog 基类 | 继承 `BaseEntity`（含 deleted） | 继承 `BaseAuditEntity`（无 deleted，含 createdBy/createdAt/updatedBy/updatedAt） | P1 |
| 8 | EqDeviceController | 已实现基础 CRUD | 补充温度阈值查询接口、设备占用/释放接口 | P2 |

### 2.2 关键问题分析

#### 问题 A：MQTT 消息解析错误

**现状代码（MqttConfig.java）：**
```java
// 当前实现 —— 错误！payload 是 JSON，不是纯数字
case "status":
    BigDecimal temperature = new BigDecimal(payload.trim());
    eqDeviceService.updateTemperature(deviceCode, temperature);
    break;
```

**问题**：实际 payload 为 `{"deviceCode":"D001","status":"RUNNING","temperature":98.5,"faultCode":"E01"}`，直接转 BigDecimal 会抛 `NumberFormatException`。

#### 问题 B：设备适配器框架缺失

当前所有协议逻辑硬编码在 `MqttConfig` 中，无法扩展 TCP 二进制协议。东华原煎药机使用私有 TCP 二进制协议，必须新增适配器框架才能接入。

#### 问题 C：心跳与温度耦合

现状无心跳机制，设备离线无法感知。需建立心跳超时检测，且温度上报应视为有效心跳。

---

## 三、数据库详细设计

### 3.1 DDL

```sql
-- ============================================================
-- 设备主表
-- ============================================================
CREATE TABLE eq_device (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id       BIGINT NOT NULL DEFAULT 0,
    device_code     VARCHAR(64) NOT NULL,
    name            VARCHAR(128) NOT NULL,
    device_type     VARCHAR(32) NOT NULL COMMENT 'DECOCT_MACHINE/PACKING_MACHINE/WASHING_MACHINE',
    status          VARCHAR(16) NOT NULL DEFAULT 'OFFLINE' COMMENT 'OFFLINE/IDLE/RUNNING/FAULT/MAINTENANCE',
    current_temp    DECIMAL(5,2),
    fault_code      VARCHAR(16),
    auto_level      VARCHAR(16) DEFAULT 'MANUAL' COMMENT 'MANUAL/AUTO/SEMI',
    location        VARCHAR(256),
    ip_address      VARCHAR(64),
    mqtt_client_id  VARCHAR(128),
    protocol_type   VARCHAR(32) NOT NULL DEFAULT 'MQTT' COMMENT 'MQTT/TCP_BINARY',
    vendor          VARCHAR(64) DEFAULT 'GENERIC',
    alarm_high_temp DECIMAL(5,2),
    alarm_low_temp  DECIMAL(5,2),
    last_heartbeat  DATETIME,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_eq_device_code (tenant_id, device_code),
    KEY idx_eq_device_tenant (tenant_id),
    KEY idx_eq_device_status (tenant_id, status),
    KEY idx_eq_device_type (tenant_id, device_type),
    KEY idx_eq_device_heartbeat (last_heartbeat)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备主表';

-- ============================================================
-- 设备关联表（煎药机 ↔ 包装机关联等）
-- ============================================================
CREATE TABLE eq_device_connection (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id       BIGINT NOT NULL DEFAULT 0,
    source_device_id BIGINT NOT NULL COMMENT '源设备ID',
    target_device_id BIGINT NOT NULL COMMENT '目标设备ID',
    connection_type VARCHAR(32) NOT NULL DEFAULT 'AUTO_FEED' COMMENT 'AUTO_FEED/FEEDBACK',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_eq_connection (tenant_id, source_device_id, target_device_id, connection_type),
    KEY idx_eq_conn_source (tenant_id, source_device_id),
    KEY idx_eq_conn_target (tenant_id, target_device_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备关联表';

-- ============================================================
-- 设备告警表
-- ============================================================
CREATE TABLE eq_device_alarm (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id       BIGINT NOT NULL DEFAULT 0,
    device_id       BIGINT NOT NULL,
    alarm_type      VARCHAR(32) NOT NULL COMMENT 'HIGH_TEMP/LOW_TEMP/FAULT/OFFLINE',
    alarm_level     VARCHAR(16) NOT NULL DEFAULT 'WARNING' COMMENT 'INFO/WARNING/CRITICAL',
    message         VARCHAR(512) NOT NULL,
    is_resolved     TINYINT NOT NULL DEFAULT 0,
    resolved_at     DATETIME,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT NOT NULL DEFAULT 0,
    KEY idx_eq_alarm_device (tenant_id, device_id),
    KEY idx_eq_alarm_type (tenant_id, alarm_type),
    KEY idx_eq_alarm_resolved (tenant_id, is_resolved, created_at),
    KEY idx_eq_alarm_created (tenant_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备告警表';

-- ============================================================
-- 温度记录表（物理删除，继承 BaseAuditEntity）
-- ============================================================
CREATE TABLE eq_temperature_log (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id       BIGINT NOT NULL DEFAULT 0,
    device_id       BIGINT NOT NULL,
    temperature     DECIMAL(5,2) NOT NULL,
    recorded_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_eq_temp_device (tenant_id, device_id),
    KEY idx_eq_temp_recorded (tenant_id, recorded_at),
    KEY idx_eq_temp_device_recorded (tenant_id, device_id, recorded_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='温度记录表';
```

### 3.2 字段说明

#### eq_device

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | BIGINT | 是 | 主键，自增 |
| tenant_id | BIGINT | 是 | 租户ID，默认0（单租户时） |
| device_code | VARCHAR(64) | 是 | 设备编码，租户内唯一 |
| name | VARCHAR(128) | 是 | 设备名称 |
| device_type | VARCHAR(32) | 是 | 设备类型枚举 |
| status | VARCHAR(16) | 是 | 设备状态：OFFLINE/IDLE/RUNNING/FAULT/MAINTENANCE |
| current_temp | DECIMAL(5,2) | 否 | 当前温度（℃） |
| fault_code | VARCHAR(16) | 否 | 设备故障码 |
| auto_level | VARCHAR(16) | 否 | 自动化等级 |
| location | VARCHAR(256) | 否 | 安装位置 |
| ip_address | VARCHAR(64) | 否 | TCP 设备IP地址 |
| mqtt_client_id | VARCHAR(128) | 否 | MQTT Client ID |
| protocol_type | VARCHAR(32) | 是 | 协议类型：MQTT / TCP_BINARY |
| vendor | VARCHAR(64) | 否 | 厂商名称 |
| alarm_high_temp | DECIMAL(5,2) | 否 | 设备级高温阈值（覆盖系统默认） |
| alarm_low_temp | DECIMAL(5,2) | 否 | 设备级低温阈值（覆盖系统默认） |
| last_heartbeat | DATETIME | 否 | 最后心跳时间 |
| created_at / updated_at / deleted | — | 是 | 标准软删除字段 |

#### eq_temperature_log（无 deleted 字段）

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | BIGINT | 是 | 主键 |
| tenant_id | BIGINT | 是 | 租户ID |
| device_id | BIGINT | 是 | 关联设备ID |
| temperature | DECIMAL(5,2) | 是 | 记录温度 |
| recorded_at | DATETIME | 是 | 温度记录时间（设备上报时间） |
| created_at / updated_at | — | 是 | BaseAuditEntity 审计字段 |

### 3.3 索引设计说明

1. `uk_eq_device_code`：租户+设备编码联合唯一，确保设备编码不重复。
2. `idx_eq_device_status`：按状态筛选在线/空闲设备时命中索引，用于任务排程。
3. `idx_eq_device_heartbeat`：心跳检测定时任务扫描使用。
4. `idx_eq_temp_device_recorded`：查询某设备某时间段温度曲线时覆盖索引。

---

## 四、实体类详细设计

### 4.1 包结构

```
cn.org.openygt.equipment.entity
├── EqDevice.java            # 设备主实体（继承 BaseEntity）
├── EqDeviceConnection.java  # 设备关联实体（继承 BaseEntity）
├── EqDeviceAlarm.java       # 告警实体（继承 BaseEntity）
└── EqTemperatureLog.java    # 温度记录实体（继承 BaseAuditEntity）【修改】
```

### 4.2 EqDevice

```java
package cn.org.openygt.equipment.entity;

import cn.org.openygt.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "eq_device",
       indexes = {
           @Index(name = "idx_eq_device_tenant", columnList = "tenantId"),
           @Index(name = "idx_eq_device_status", columnList = "tenantId,status"),
           @Index(name = "idx_eq_device_type", columnList = "tenantId,deviceType"),
           @Index(name = "idx_eq_device_heartbeat", columnList = "lastHeartbeat")
       },
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_eq_device_code", columnNames = {"tenantId", "deviceCode"})
       })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EqDevice extends BaseEntity {

    @Column(name = "device_code", nullable = false, length = 64)
    private String deviceCode;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", nullable = false, length = 32)
    private DeviceType deviceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private DeviceStatus status;

    @Column(name = "current_temp", precision = 5, scale = 2)
    private BigDecimal currentTemp;

    @Column(name = "fault_code", length = 16)
    private String faultCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "auto_level", length = 16)
    private AutoLevel autoLevel;

    @Column(name = "location", length = 256)
    private String location;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "mqtt_client_id", length = 128)
    private String mqttClientId;

    @Enumerated(EnumType.STRING)
    @Column(name = "protocol_type", nullable = false, length = 32)
    private ProtocolType protocolType;

    @Column(name = "vendor", length = 64)
    private String vendor;

    @Column(name = "alarm_high_temp", precision = 5, scale = 2)
    private BigDecimal alarmHighTemp;

    @Column(name = "alarm_low_temp", precision = 5, scale = 2)
    private BigDecimal alarmLowTemp;

    @Column(name = "last_heartbeat")
    private LocalDateTime lastHeartbeat;
}
```

### 4.3 EqTemperatureLog（关键修改点）

```java
package cn.org.openygt.equipment.entity;

import cn.org.openygt.common.entity.BaseAuditEntity;  // 【修改】从 BaseEntity 改为 BaseAuditEntity
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "eq_temperature_log",
       indexes = {
           @Index(name = "idx_eq_temp_device", columnList = "tenantId,deviceId"),
           @Index(name = "idx_eq_temp_recorded", columnList = "tenantId,recordedAt"),
           @Index(name = "idx_eq_temp_device_recorded", columnList = "tenantId,deviceId,recordedAt")
       })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EqTemperatureLog extends BaseAuditEntity {

    @Column(name = "device_id", nullable = false)
    private Long deviceId;

    @Column(name = "temperature", nullable = false, precision = 5, scale = 2)
    private BigDecimal temperature;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;
}
```

> **注意**：`BaseAuditEntity` 不含 `deleted` 字段，因此 `eq_temperature_log` 表无软删除。历史数据通过定时归档清理。

### 4.4 枚举定义

```java
// DeviceStatus.java
public enum DeviceStatus {
    OFFLINE,     // 离线
    IDLE,        // 在线空闲
    RUNNING,     // 运行中
    FAULT,       // 告警中
    MAINTENANCE  // 维护中
}

// DeviceType.java
public enum DeviceType {
    DECOCT_MACHINE,    // 煎药机
    PACKING_MACHINE,   // 包装机
    WASHING_MACHINE    // 清洗机
}

// ProtocolType.java
public enum ProtocolType {
    MQTT,         // 标准 MQTT JSON
    TCP_BINARY    // TCP 二进制（厂商私有协议）
}

// AlarmType.java
public enum AlarmType {
    HIGH_TEMP,   // 超温
    LOW_TEMP,    // 低温
    FAULT,       // 设备故障
    OFFLINE      // 离线
}

// AlarmLevel.java
public enum AlarmLevel {
    INFO,      // 提示
    WARNING,   // 警告
    CRITICAL   // 严重
}

// AutoLevel.java
public enum AutoLevel {
    MANUAL,   // 手动
    AUTO,     // 全自动
    SEMI      // 半自动
}
```

### 4.5 DTO 设计

```java
// EqDeviceDTO.java —— 设备详情/列表返回
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class EqDeviceDTO {
    private Long id;
    private String deviceCode;
    private String name;
    private String deviceType;
    private String status;
    private BigDecimal currentTemp;
    private String faultCode;
    private String location;
    private String protocolType;
    private String vendor;
    private BigDecimal alarmHighTemp;   // 实际生效的高温阈值（继承后）
    private BigDecimal alarmLowTemp;    // 实际生效的低温阈值（继承后）
    private LocalDateTime lastHeartbeat;
    private LocalDateTime createdAt;
}

// DeviceStatusPayload.java —— MQTT JSON 解析用
@Data @NoArgsConstructor @AllArgsConstructor
public class DeviceStatusPayload {
    private String deviceCode;
    private String status;        // "IDLE" / "RUNNING" / "FAULT"
    private BigDecimal temperature;
    private String faultCode;     // 可能为 null
}

// DeviceReserveRequest.java —— 占用设备请求
@Data @NoArgsConstructor @AllArgsConstructor
public class DeviceReserveRequest {
    @NotBlank private String deviceCode;
    @NotNull private Long taskId;      // 关联煎药任务
}

// TemperatureThresholdDTO.java —— 查询生效阈值返回
@Data @Builder
public class TemperatureThresholdDTO {
    private String deviceCode;
    private BigDecimal highTemp;
    private BigDecimal lowTemp;
    private String source;  // "SYSTEM" / "DEVICE" / "SCHEME"
}
```


---

## 五、REST API 详细设计

### 5.1 接口清单

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/api/v1/eq/devices` | 分页查询设备列表 | equipment:read |
| GET | `/api/v1/eq/devices/{id}` | 查询设备详情 | equipment:read |
| POST | `/api/v1/eq/devices` | 创建设备 | equipment:write |
| PUT | `/api/v1/eq/devices/{id}` | 修改设备 | equipment:write |
| DELETE | `/api/v1/eq/devices/{id}` | 删除设备（软删） | equipment:write |
| POST | `/api/v1/eq/devices/{id}/reserve` | 占用设备 | equipment:operate |
| POST | `/api/v1/eq/devices/{id}/release` | 释放设备 | equipment:operate |
| GET | `/api/v1/eq/devices/{id}/threshold` | 查询生效温度阈值 | equipment:read |
| GET | `/api/v1/eq/devices/{id}/temperature-logs` | 查询温度曲线 | equipment:read |
| GET | `/api/v1/eq/alarms` | 分页查询告警列表 | equipment:read |
| POST | `/api/v1/eq/alarms/{id}/resolve` | 手动解除告警 | equipment:operate |

### 5.2 关键接口详细设计

#### 5.2.1 占用设备 —— POST /api/v1/eq/devices/{id}/reserve

```java
@PostMapping("/{id}/reserve")
public ApiResponse<EqDeviceDTO> reserveDevice(
        @PathVariable Long id,
        @RequestBody @Valid DeviceReserveRequest request) {
    EqDeviceDTO device = equipmentService.reserveDevice(id, request.getTaskId());
    return Result.success(device);
}
```

**业务规则**：
1. 校验设备存在且未删除；
2. 设备当前状态必须为 `IDLE`（空闲），否则抛 `DeviceBusyException`；
3. 使用 `SELECT FOR UPDATE` 悲观锁锁定设备行；
4. 状态变更为 `RUNNING`，记录任务ID（可扩展字段）；
5. 返回更新后的 `EqDeviceDTO`。

**响应示例（成功）**：
```json
{
  "code": 200,
  "data": {
    "id": 1,
    "deviceCode": "D001",
    "name": "煎药机-01",
    "status": "RUNNING",
    "currentTemp": 98.5,
    "lastHeartbeat": "2026-04-25T08:30:00"
  }
}
```

**响应示例（设备被占用）**：
```json
{
  "code": 409,
  "message": "设备当前非空闲状态，无法占用：RUNNING"
}
```

#### 5.2.2 释放设备 —— POST /api/v1/eq/devices/{id}/release

```java
@PostMapping("/{id}/release")
public ApiResponse<EqDeviceDTO> releaseDevice(
        @PathVariable Long id,
        @RequestParam(required = false) Long taskId) {
    EqDeviceDTO device = equipmentService.releaseDevice(id, taskId);
    return Result.success(device);
}
```

**业务规则**：
1. 校验设备存在；
2. 使用 `SELECT FOR UPDATE` 悲观锁锁定设备行；
3. 状态变更为 `IDLE`；
4. `taskId` 用于校验是否为同一任务释放（可选，如传入则校验匹配）。

#### 5.2.3 查询生效温度阈值 —— GET /api/v1/eq/devices/{id}/threshold

```java
@GetMapping("/{id}/threshold")
public ApiResponse<TemperatureThresholdDTO> getEffectiveThreshold(@PathVariable Long id) {
    return Result.success(equipmentService.getEffectiveThreshold(id));
}
```

**返回值示例**：
```json
{
  "code": 200,
  "data": {
    "deviceCode": "D001",
    "highTemp": 105.0,
    "lowTemp": 85.0,
    "source": "SCHEME"
  }
}
```

### 5.3 Controller 完整代码

```java
package cn.org.openygt.equipment.controller;

import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.equipment.dto.*;
import cn.org.openygt.equipment.service.EquipmentService;
import cn.org.openygt.equipment.service.EqDeviceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/eq/devices")
@RequiredArgsConstructor
public class EqDeviceController {

    private final EqDeviceService eqDeviceService;
    private final EquipmentService equipmentService;

    // ... 基础 CRUD 已存在，略 ...

    @PostMapping("/{id}/reserve")
    public ApiResponse<EqDeviceDTO> reserveDevice(@PathVariable Long id,
                                             @RequestBody @Valid DeviceReserveRequest request) {
        return Result.success(equipmentService.reserveDevice(id, request.getTaskId()));
    }

    @PostMapping("/{id}/release")
    public ApiResponse<EqDeviceDTO> releaseDevice(@PathVariable Long id,
                                             @RequestParam(required = false) Long taskId) {
        return Result.success(equipmentService.releaseDevice(id, taskId));
    }

    @GetMapping("/{id}/threshold")
    public ApiResponse<TemperatureThresholdDTO> getEffectiveThreshold(@PathVariable Long id) {
        return Result.success(equipmentService.getEffectiveThreshold(id));
    }

    @GetMapping("/{id}/temperature-logs")
    public ApiResponse<PageApiResponse<TemperatureLogDTO>> getTemperatureLogs(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            PageParam pageParam) {
        return Result.success(eqDeviceService.getTemperatureLogs(id, start, end, pageParam));
    }
}
```

---

## 六、SPI 实现设计（EquipmentServiceImpl）

### 6.1 接口定义

```java
package cn.org.openygt.equipment.service;

public interface EquipmentService {
    EqDeviceDTO getDeviceById(Long id);
    EqDeviceDTO reserveDevice(Long deviceId, Long taskId);
    EqDeviceDTO releaseDevice(Long deviceId, Long taskId);
    TemperatureThresholdDTO getEffectiveThreshold(Long deviceId);
}
```

### 6.2 EquipmentServiceImpl 完整实现

```java
package cn.org.openygt.equipment.service.impl;

import cn.org.openygt.common.exception.IllegalStateException;
import cn.org.openygt.equipment.dto.EqDeviceDTO;
import cn.org.openygt.equipment.dto.TemperatureThresholdDTO;
import cn.org.openygt.equipment.entity.EqDevice;
import cn.org.openygt.equipment.enums.DeviceStatus;
import cn.org.openygt.equipment.mapper.EqDeviceMapper;
import cn.org.openygt.equipment.mapper.EqDeviceMapper;
import cn.org.openygt.equipment.service.EquipmentService;
import cn.org.openygt.masterdata.service.MdDecoctSchemeService;
import cn.org.openygt.system.service.SysConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class EquipmentServiceImpl implements EquipmentService {

    private final EqDeviceMapper eqDeviceMapper;
    private final EqDeviceMapper eqDeviceMapper;
    private final SysConfigService sysConfigService;
    private final MdDecoctSchemeService mdDecoctSchemeService;

    // ========== 常量：系统默认阈值配置键 ==========
    private static final String CFG_DEFAULT_HIGH_TEMP = "equipment.default.alarmHighTemp";
    private static final String CFG_DEFAULT_LOW_TEMP  = "equipment.default.alarmLowTemp";
    private static final BigDecimal DEFAULT_HIGH_TEMP = new BigDecimal("110.0");
    private static final BigDecimal DEFAULT_LOW_TEMP  = new BigDecimal("80.0");

    @Override
    public EqDeviceDTO getDeviceById(Long id) {
        EqDevice device = eqDeviceMapper.selectById(id)
            .orElseThrow(() -> new IllegalStateException("设备不存在: " + id));
        return eqDeviceMapper.toDto(device);
    }

    /**
     * 占用设备 —— 悲观锁保证并发安全
     */
    @Override
    @Transactional
    public EqDeviceDTO reserveDevice(Long deviceId, Long taskId) {
        // 1. 使用 SELECT FOR UPDATE 加锁
        EqDevice device = eqDeviceMapper.selectForUpdate(deviceId)
            .orElseThrow(() -> new IllegalStateException("设备不存在: " + deviceId));

        // 2. 状态校验
        if (device.getStatus() != DeviceStatus.IDLE) {
            throw new IllegalStateException("设备当前非空闲状态，无法占用：" + device.getStatus());
        }

        // 3. 更新状态
        device.setStatus(DeviceStatus.RUNNING);
        // device.setCurrentTaskId(taskId);  // 如有任务ID字段则记录
        eqDeviceMapper.insert(device);

        log.info("设备已占用: deviceId={}, taskId={}", deviceId, taskId);
        return eqDeviceMapper.toDto(device);
    }

    /**
     * 释放设备 —— 悲观锁保证并发安全
     */
    @Override
    @Transactional
    public EqDeviceDTO releaseDevice(Long deviceId, Long taskId) {
        EqDevice device = eqDeviceMapper.selectForUpdate(deviceId)
            .orElseThrow(() -> new IllegalStateException("设备不存在: " + deviceId));

        // 可选：校验是否为同一任务释放
        // if (taskId != null && !taskId.equals(device.getCurrentTaskId())) {
        //     throw new IllegalStateException("任务ID不匹配，无法释放设备");
        // }

        device.setStatus(DeviceStatus.IDLE);
        // device.setCurrentTaskId(null);
        eqDeviceMapper.insert(device);

        log.info("设备已释放: deviceId={}, taskId={}", deviceId, taskId);
        return eqDeviceMapper.toDto(device);
    }

    /**
     * 查询生效温度阈值 —— 三层继承策略
     */
    @Override
    public TemperatureThresholdDTO getEffectiveThreshold(Long deviceId) {
        EqDevice device = eqDeviceMapper.findById(deviceId)
            .orElseThrow(() -> new IllegalStateException("设备不存在: " + deviceId));

        BigDecimal highTemp = device.getAlarmHighTemp();
        BigDecimal lowTemp = device.getAlarmLowTemp();
        String source = "DEVICE";

        // 1. 设备级未配置，取系统默认
        if (highTemp == null || lowTemp == null) {
            highTemp = sysConfigService.getDecimalValue(CFG_DEFAULT_HIGH_TEMP, DEFAULT_HIGH_TEMP);
            lowTemp = sysConfigService.getDecimalValue(CFG_DEFAULT_LOW_TEMP, DEFAULT_LOW_TEMP);
            source = "SYSTEM";
        }

        // 2. 【扩展点】如有运行中的方案，且方案配置了阈值，则覆盖
        // TODO: 集成排程模块，根据 deviceId 查询当前运行方案
        // MdDecoctScheme scheme = mdDecoctSchemeService.getRunningSchemeByDevice(deviceId);
        // if (scheme != null && scheme.getAlarmHighTemp() != null) {
        //     highTemp = scheme.getAlarmHighTemp();
        //     lowTemp = scheme.getAlarmLowTemp();
        //     source = "SCHEME";
        // }

        return TemperatureThresholdDTO.builder()
            .deviceCode(device.getDeviceCode())
            .highTemp(highTemp)
            .lowTemp(lowTemp)
            .source(source)
            .build();
    }
}
```

### 6.3 Mapper 层扩展（悲观锁）（悲观锁）

```java
package cn.org.openygt.equipment.mapper;

import cn.org.openygt.equipment.entity.EqDevice;
import org.springframework.data.jpa.repository.*;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.Optional;

@Mapper
public interface EqDeviceMapper extends BaseMapper<EqDevice> {

    Optional<EqDevice> findByDeviceCodeAndDeletedFalse(String deviceCode);

    // ========== 【新增】悲观锁查询 ==========
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM EqDevice d WHERE d.id = :id AND d.deleted = false")
    Optional<EqDevice> selectForUpdate(Long id);

    // 心跳检测：查询所有未删除设备
    @Query("SELECT d FROM EqDevice d WHERE d.deleted = false")
    java.util.List<EqDevice> findAllActive();
}
```

### 6.4 异常处理设计

| 异常场景 | 异常类 | HTTP 状态码 | 错误消息 |
|---------|--------|------------|---------|
| 设备不存在 | `IllegalStateException` | 404 | 设备不存在: {id} |
| 设备非空闲 | `IllegalStateException` | 409 | 设备当前非空闲状态，无法占用: {status} |
| 任务ID不匹配 | `IllegalStateException` | 409 | 任务ID不匹配，无法释放设备 |
| 乐观锁冲突（备用） | `OptimisticLockException` | 409 | 数据已被修改，请重试 |

---

## 七、MQTT 通信详细设计

### 7.1 Topic 设计

Topic 统一采用 `/openygt/{tenantId}/{deviceCode}/{messageType}` 格式。

| Topic 模式 | 方向 | 说明 |
|-----------|------|------|
| `/openygt/+/+/status` | 设备 → 平台 | 状态/温度/故障上报（订阅） |
| `/openygt/+/+/heartbeat` | 设备 → 平台 | 纯心跳上报（订阅） |
| `/openygt/+/+/command` | 平台 → 设备 | 下发控制指令（发布） |

**通配订阅**：平台订阅 `/openygt/+/+/+` 接收所有设备消息，在 `MqttConfig` 中统一处理。

### 7.2 JSON 报文格式

#### 7.2.1 状态上报（status）

```json
{
  "deviceCode": "D001",
  "status": "RUNNING",
  "temperature": 98.5,
  "faultCode": null
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| deviceCode | String | 是 | 设备编码，对应 eq_device.device_code |
| status | String | 是 | IDLE / RUNNING / FAULT / MAINTENANCE |
| temperature | Number | 否 | 当前温度（℃），保留1位小数 |
| faultCode | String | 否 | 故障码，无故障为 null 或省略 |

#### 7.2.2 心跳上报（heartbeat）

```json
{
  "deviceCode": "D001",
  "timestamp": "2026-04-25T08:30:00+08:00"
}
```

#### 7.2.3 控制指令下发（command）

```json
{
  "command": "START",
  "params": {
    "temperature": 100,
    "duration": 1800
  }
}
```

### 7.3 消息处理流程

```
设备发送 MQTT 消息
    │
    ▼
MqttInboundMessageHandler (Spring Integration)
    │
    ▼
MqttConfig.handleMessage(String topic, String payload)
    │
    ├── 解析 topic 提取 deviceCode, messageType
    │
    ├── switch(messageType)
    │       │
    │       ├── "status" ──► 解析 JSON DeviceStatusPayload
    │       │                    │
    │       │                    ├── 更新设备状态、温度、故障码
    │       │                    ├── 同步更新 last_heartbeat = now()
    │       │                    ├── 写入 eq_temperature_log
    │       │                    └── 调用 TemperatureAlarmService.checkAlarm()
    │       │
    │       ├── "heartbeat" ──► 仅更新 last_heartbeat = now()
    │       │
    │       └── default ──► 日志.warn("未知消息类型")
    │
    └── 异常处理：解析失败记录日志，不抛异常避免断开连接
```

### 7.4 MqttConfig 完整实现

```java
package cn.org.openygt.equipment.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import cn.org.openygt.equipment.dto.DeviceStatusPayload;
import cn.org.openygt.equipment.service.EqDeviceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class MqttConfig {

    private final EqDeviceService eqDeviceService;
    private final ObjectMapper objectMapper;

    @Value("${mqtt.broker-url:tcp://localhost:1883}")
    private String brokerUrl;

    @Value("${mqtt.client-id:openygt-dms-server}")
    private String clientId;

    @Value("${mqtt.topic-prefix:/openygt}")
    private String topicPrefix;

    @Bean
    public DefaultMqttPahoClientFactory mqttClientFactory() {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        MqttConnectOptions options = new MqttConnectOptions();
        options.setServerURIs(new String[]{brokerUrl});
        options.setAutomaticReconnect(true);
        options.setCleanSession(false);  // 持久会话，避免离线消息丢失
        options.setConnectionTimeout(10);
        options.setKeepAliveInterval(60);
        factory.setConnectionOptions(options);
        return factory;
    }

    @Bean
    public MessageChannel mqttInputChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageProducer mqttInbound(DefaultMqttPahoClientFactory factory) {
        MqttPahoMessageDrivenChannelAdapter adapter =
            new MqttPahoMessageDrivenChannelAdapter(clientId + "-in", factory,
                topicPrefix + "/+/+/status",
                topicPrefix + "/+/+/heartbeat");
        adapter.setCompletionTimeout(5000);
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setQos(1);
        adapter.setOutputChannel(mqttInputChannel());
        return adapter;
    }

    @Bean
    @ServiceActivator(inputChannel = "mqttInputChannel")
    public MessageHandler mqttMessageHandler() {
        return message -> {
            String topic = message.getHeaders().get("mqtt_receivedTopic", String.class);
            String payload = message.getPayload().toString();
            handleMessage(topic, payload);
        };
    }

    // ========== 【重写】消息处理核心 ==========
    public void handleMessage(String topic, String payload) {
        log.debug("MQTT received: topic={}, payload={}", topic, payload);

        try {
            // 解析 topic: /openygt/{tenantId}/{deviceCode}/{messageType}
            String[] parts = topic.split("/");
            if (parts.length < 4) {
                log.warn("非法 topic 格式: {}", topic);
                return;
            }
            String deviceCode = parts[parts.length - 2];
            String messageType = parts[parts.length - 1];

            switch (messageType) {
                case "status":
                    handleStatusMessage(deviceCode, payload);
                    break;
                case "heartbeat":
                    handleHeartbeatMessage(deviceCode, payload);
                    break;
                default:
                    log.warn("未知消息类型: {}, topic={}", messageType, topic);
            }
        } catch (Exception e) {
            // 必须捕获所有异常，避免 Spring Integration 通道异常中断
            log.error("MQTT 消息处理异常, topic={}, payload={}", topic, payload, e);
        }
    }

    private void handleStatusMessage(String deviceCode, String payload) {
        try {
            DeviceStatusPayload statusPayload = objectMapper.readValue(payload, DeviceStatusPayload.class);

            // 校验 deviceCode 一致性
            if (!deviceCode.equals(statusPayload.getDeviceCode())) {
                log.warn("Topic 中的 deviceCode 与 payload 不一致: topic={}, payload={}",
                    deviceCode, statusPayload.getDeviceCode());
            }

            // 委托 Service 处理状态更新、温度记录、告警检测
            eqDeviceService.handleDeviceStatusReport(statusPayload);

        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            log.error("状态消息 JSON 解析失败: deviceCode={}, payload={}", deviceCode, payload, e);
        }
    }

    private void handleHeartbeatMessage(String deviceCode, String payload) {
        // 纯心跳只需更新最后心跳时间
        eqDeviceService.updateHeartbeat(deviceCode);
    }

    // ========== 发布通道 ==========
    @Bean
    public MessageChannel mqttOutputChannel() {
        return new DirectChannel();
    }

    @Bean
    @ServiceActivator(inputChannel = "mqttOutputChannel")
    public MessageHandler mqttOutbound(DefaultMqttPahoClientFactory factory) {
        MqttPahoMessageHandler handler =
            new MqttPahoMessageHandler(clientId + "-out", factory);
        handler.setAsync(true);
        handler.setDefaultQos(1);
        return handler;
    }
}
```

### 7.5 心跳机制设计

#### 7.5.1 心跳超时策略

| 设备状态 | 超时阈值 | 说明 |
|---------|---------|------|
| IDLE（空闲） | 120 秒 | 空闲时更敏感，快速发现离线 |
| RUNNING（运行） | 600 秒 | 运行中允许更长超时，避免误报 |
| FAULT（告警） | 120 秒 | 告警状态同空闲敏感级别 |
| MAINTENANCE（维护） | 不检测 | 维护中不触发离线 |

#### 7.5.2 心跳检测定时任务

```java
package cn.org.openygt.equipment.scheduler;

import cn.org.openygt.equipment.entity.EqDevice;
import cn.org.openygt.equipment.enums.DeviceStatus;
import cn.org.openygt.equipment.mapper.EqDeviceMapper;
import cn.org.openygt.equipment.service.EqDeviceAlarmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class HeartbeatCheckScheduler {

    private final EqDeviceMapper eqDeviceMapper;
    private final EqDeviceAlarmService alarmService;

    // IDLE/FAULT 超时秒数
    private static final long IDLE_TIMEOUT_SECONDS = 120;
    // RUNNING 超时秒数
    private static final long RUNNING_TIMEOUT_SECONDS = 600;

    /**
     * 每 30 秒执行一次心跳检测
     */
    @Scheduled(fixedRate = 30000)
    @Transactional
    public void checkHeartbeatTimeout() {
        LocalDateTime now = LocalDateTime.now();
        List<EqDevice> devices = eqDeviceMapper.findAllActive();

        for (EqDevice device : devices) {
            if (device.getStatus() == DeviceStatus.OFFLINE
                || device.getStatus() == DeviceStatus.MAINTENANCE) {
                continue;
            }

            LocalDateTime lastHb = device.getLastHeartbeat();
            if (lastHb == null) {
                // 从未上报过心跳，视为离线
                markOffline(device, now);
                continue;
            }

            long timeout = getTimeoutSeconds(device.getStatus());
            if (lastHb.plusSeconds(timeout).isBefore(now)) {
                markOffline(device, now);
            }
        }
    }

    private long getTimeoutSeconds(DeviceStatus status) {
        return switch (status) {
            case IDLE, FAULT -> IDLE_TIMEOUT_SECONDS;
            case RUNNING -> RUNNING_TIMEOUT_SECONDS;
            default -> IDLE_TIMEOUT_SECONDS;
        };
    }

    private void markOffline(EqDevice device, LocalDateTime now) {
        log.warn("设备心跳超时离线: deviceCode={}, status={}, lastHeartbeat={}",
            device.getDeviceCode(), device.getStatus(), device.getLastHeartbeat());
        device.setStatus(DeviceStatus.OFFLINE);
        device.setCurrentTemp(null);
        eqDeviceMapper.insert(device);

        // 触发离线告警
        alarmService.createAlarm(device, AlarmType.OFFLINE, AlarmLevel.CRITICAL,
            "设备心跳超时，自动标记为离线");
    }
}
```

### 7.6 EqDeviceService.handleDeviceStatusReport 实现

```java
@Override
@Transactional
public void handleDeviceStatusReport(DeviceStatusPayload payload) {
    EqDevice device = eqDeviceMapper.findByDeviceCodeAndDeletedFalse(payload.getDeviceCode())
        .orElseThrow(() -> new IllegalStateException("未知设备: " + payload.getDeviceCode()));

    // 1. 更新状态（FAULT 状态优先于 RUNNING/IDLE）
    if (payload.getFaultCode() != null && !payload.getFaultCode().isBlank()) {
        device.setStatus(DeviceStatus.FAULT);
        device.setFaultCode(payload.getFaultCode());
    } else if (payload.getStatus() != null) {
        device.setStatus(DeviceStatus.valueOf(payload.getStatus()));
        device.setFaultCode(null);
    }

    // 2. 更新温度
    if (payload.getTemperature() != null) {
        device.setCurrentTemp(payload.getTemperature());
        // 写入温度日志
        EqTemperatureLog logEntry = EqTemperatureLog.builder()
            .deviceId(device.getId())
            .temperature(payload.getTemperature())
            .recordedAt(LocalDateTime.now())
            .build();
        temperatureLogMapper.save(logEntry);
    }

    // 3. 心跳时间同步更新（温度上报视为心跳）
    device.setLastHeartbeat(LocalDateTime.now());

    eqDeviceMapper.insert(device);

    // 4. 温度告警检测
    if (payload.getTemperature() != null) {
        alarmService.checkTemperatureAlarm(device, payload.getTemperature());
    }
}

@Override
@Transactional
public void updateHeartbeat(String deviceCode) {
    EqDevice device = eqDeviceMapper.findByDeviceCodeAndDeletedFalse(deviceCode)
        .orElse(null);
    if (device == null) {
        log.warn("心跳设备不存在: {}", deviceCode);
        return;
    }
    // 如果设备处于 OFFLINE，恢复为 IDLE
    if (device.getStatus() == DeviceStatus.OFFLINE) {
        device.setStatus(DeviceStatus.IDLE);
        log.info("设备恢复在线: {}", deviceCode);
    }
    device.setLastHeartbeat(LocalDateTime.now());
    eqDeviceMapper.insert(device);
}
```

---

## 八、设备适配器框架详细设计

### 8.1 设计目标

1. **统一接入**：无论设备使用 MQTT JSON 还是 TCP 二进制协议，上层业务代码无感知；
2. **插件扩展**：新增厂商只需实现 `DeviceAdapter` 接口并注册到 `AdapterRegistry`；
3. **MVP 聚焦**：V1.4 仅实现东华原 TCP 二进制适配器，框架预留 MQTT 扩展能力。

### 8.2 类图

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           AdapterRegistry                               │
│  - adapters: Map<String, DeviceAdapter>                                 │
│  + register(protocolType, vendor, adapter): void                        │
│  + getAdapter(protocolType, vendor): Optional<DeviceAdapter>            │
│  + getAllAdapters(): Collection<DeviceAdapter>                          │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
          ┌─────────────────────────┼─────────────────────────┐
          │                         │                         │
          ▼                         ▼                         ▼
┌──────────────────┐    ┌────────────────────┐    ┌────────────────────┐
│  DeviceAdapter   │◄───│ AbstractMqttAdapter│    │AbstractTcpBinary   │
│  (interface)     │    │  (abstract)        │    │Adapter (abstract)  │
│                  │    │                    │    │                    │
│ + connect()      │    │ + onMessage(topic, │    │ + startServer()    │
│ + disconnect()   │    │   payload)         │    │ + stopServer()     │
│ + sendCommand()  │    │ + sendCommand()    │    │ + onDataReceived() │
│ + getProtocol()  │    │                    │    │ + sendCommand()    │
│ + getVendor()    │    │                    │    │                    │
└──────────────────┘    └────────────────────┘    └────────────────────┘
                                                             │
                                                             ▼
                                              ┌────────────────────────────┐
                                              │  DonghuayuanTcpAdapter     │
                                              │  (东华原 TCP 二进制适配器)   │
                                              │                            │
                                              │ - nettyServer: ServerBootstrap│
                                              │ - port: int                │
                                              │                            │
                                              │ + decodeFrame(ByteBuf):    │
                                              │   DonghuayuanFrame         │
                                              │ + encodeCommand(cmd):      │
                                              │   ByteBuf                  │
                                              └────────────────────────────┘
```

### 8.3 核心接口：DeviceAdapter

```java
package cn.org.openygt.equipment.adapter;

import cn.org.openygt.equipment.dto.DeviceCommandDTO;

/**
 * 设备适配器接口 —— 所有协议适配器必须实现
 */
public interface DeviceAdapter {

    /**
     * 建立与设备的连接（或启动服务端监听）
     */
    void connect();

    /**
     * 断开连接（或停止服务端）
     */
    void disconnect();

    /**
     * 向指定设备发送控制指令
     */
    void sendCommand(String deviceCode, DeviceCommandDTO command);

    /**
     * 获取支持的协议类型
     */
    String getProtocolType();

    /**
     * 获取厂商标识
     */
    String getVendor();

    /**
     * 初始化注册到 AdapterRegistry（由 Spring @PostConstruct 自动调用）
     */
    default void register(AdapterRegistry registry) {
        registry.register(getProtocolType(), getVendor(), this);
    }
}
```

### 8.4 适配器注册中心：AdapterRegistry

```java
package cn.org.openygt.equipment.adapter;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 设备适配器注册中心 —— 线程安全，支持运行时动态注册
 */
@Component
public class AdapterRegistry {

    // Key: protocolType + ":" + vendor, e.g., "TCP_BINARY:donghuayuan"
    private final Map<String, DeviceAdapter> adapters = new ConcurrentHashMap<>();

    public void register(String protocolType, String vendor, DeviceAdapter adapter) {
        String key = buildKey(protocolType, vendor);
        adapters.put(key, adapter);
    }

    public Optional<DeviceAdapter> getAdapter(String protocolType, String vendor) {
        return Optional.ofNullable(adapters.get(buildKey(protocolType, vendor)));
    }

    public Optional<DeviceAdapter> getAdapterByDevice(String protocolType, String vendor) {
        return getAdapter(protocolType, vendor);
    }

    public Collection<DeviceAdapter> getAllAdapters() {
        return Collections.unmodifiableCollection(adapters.values());
    }

    private String buildKey(String protocolType, String vendor) {
        return protocolType.toUpperCase() + ":" + vendor.toLowerCase();
    }
}
```

### 8.5 MQTT 抽象基类：AbstractMqttAdapter

```java
package cn.org.openygt.equipment.adapter;

import cn.org.openygt.equipment.dto.DeviceCommandDTO;
import lombok.extern.slf4j.Slf4j;

/**
 * MQTT 协议适配器抽象基类 —— 当前由 MqttConfig 直接处理，
 * 保留此类作为未来多厂商 MQTT 扩展的基类。
 */
@Slf4j
public abstract class AbstractMqttAdapter implements DeviceAdapter {

    @Override
    public void connect() {
        // MQTT 连接由 Spring Integration MqttPahoMessageDrivenChannelAdapter 统一管理
        log.info("MQTT adapter connected: vendor={}", getVendor());
    }

    @Override
    public void disconnect() {
        log.info("MQTT adapter disconnected: vendor={}", getVendor());
    }

    @Override
    public void sendCommand(String deviceCode, DeviceCommandDTO command) {
        // 默认实现：发布到 /openygt/{tenantId}/{deviceCode}/command
        // 子类可覆盖以支持厂商自定义 Topic 格式
        doSendCommand(deviceCode, command);
    }

    protected abstract void doSendCommand(String deviceCode, DeviceCommandDTO command);

    @Override
    public String getProtocolType() {
        return "MQTT";
    }

    /**
     * 处理设备上报消息 —— 由 MqttConfig 路由调用
     */
    public abstract void onMessage(String topic, String payload);
}
```

### 8.6 TCP 二进制抽象基类：AbstractTcpBinaryAdapter

```java
package cn.org.openygt.equipment.adapter;

import cn.org.openygt.equipment.dto.DeviceCommandDTO;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

/**
 * TCP 二进制协议适配器抽象基类 —— 基于 Netty 实现服务端监听
 */
@Slf4j
public abstract class AbstractTcpBinaryAdapter implements DeviceAdapter {

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    @Override
    public String getProtocolType() {
        return "TCP_BINARY";
    }

    /**
     * 子类提供监听端口
     */
    protected abstract int getPort();

    /**
     * 子类提供 ChannelInitializer，包含编解码器和业务 Handler
     */
    protected abstract ChannelInitializer<SocketChannel> getChannelInitializer();

    @PostConstruct
    @Override
    public void connect() {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(getChannelInitializer())
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true);

            ChannelFuture future = bootstrap.bind(getPort()).sync();
            serverChannel = future.channel();
            log.info("TCP binary adapter started: vendor={}, port={}", getVendor(), getPort());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("TCP adapter start interrupted", e);
        }
    }

    @PreDestroy
    @Override
    public void disconnect() {
        if (serverChannel != null) {
            serverChannel.close();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        log.info("TCP binary adapter stopped: vendor={}", getVendor());
    }

    @Override
    public void sendCommand(String deviceCode, DeviceCommandDTO command) {
        // 通过 ChannelGroup 或 deviceCode→Channel 映射找到对应连接发送
        Channel channel = findChannelByDeviceCode(deviceCode);
        if (channel == null || !channel.isActive()) {
            throw new IllegalStateException("设备未连接: " + deviceCode);
        }
        byte[] frame = encodeCommand(command);
        channel.writeAndFlush(frame);
    }

    /**
     * 子类实现：根据 deviceCode 查找对应 Netty Channel
     */
    protected abstract Channel findChannelByDeviceCode(String deviceCode);

    /**
     * 子类实现：将指令编码为字节帧
     */
    protected abstract byte[] encodeCommand(DeviceCommandDTO command);

    /**
     * 子类调用：收到原始字节数据后的统一入口
     */
    protected abstract void onDataReceived(String deviceCode, byte[] data);
}
```

### 8.7 东华原 TCP 二进制适配器（MVP 唯一实现）

#### 8.7.1 东华原协议帧格式（假设）

```
帧头(2B)  +  命令字(1B)  +  设备地址(1B)  +  数据长度(2B)  +  数据(NB)  +  CRC16(2B)
  0xAA55       0x01          0x01           大端uint16      变长         Modbus-CRC
```

> **说明**：以上为假设格式，实际对接时需根据东华原提供的《通信协议文档》调整编解码器。

#### 8.7.2 东华原适配器完整代码

```java
package cn.org.openygt.equipment.adapter.vendor;

import cn.org.openygt.equipment.adapter.AbstractTcpBinaryAdapter;
import cn.org.openygt.equipment.adapter.AdapterRegistry;
import cn.org.openygt.equipment.dto.DeviceCommandDTO;
import cn.org.openygt.equipment.service.EqDeviceService;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class DonghuayuanTcpAdapter extends AbstractTcpBinaryAdapter {

    private final AdapterRegistry adapterRegistry;
    private final EqDeviceService eqDeviceService;

    // deviceCode -> Channel 映射（通过设备地址或注册包建立映射）
    private final ConcurrentMap<String, Channel> deviceChannelMap = new ConcurrentHashMap<>();

    // 监听端口（可配置到 sys_config）
    private static final int LISTEN_PORT = 9001;

    @PostConstruct
    public void init() {
        register(adapterRegistry);
    }

    @Override
    protected int getPort() {
        return LISTEN_PORT;
    }

    @Override
    public String getVendor() {
        return "donghuayuan";
    }

    @Override
    protected ChannelInitializer<SocketChannel> getChannelInitializer() {
        return new ChannelInitializer<>() {
            @Override
            protected void initChannel(SocketChannel ch) {
                ChannelPipeline pipeline = ch.pipeline();
                pipeline.addLast(new DonghuayuanFrameDecoder());
                pipeline.addLast(new DonghuayuanHandler());
            }
        };
    }

    @Override
    protected Channel findChannelByDeviceCode(String deviceCode) {
        return deviceChannelMap.get(deviceCode);
    }

    @Override
    protected byte[] encodeCommand(DeviceCommandDTO command) {
        // 示例：编码启动指令
        ByteBuf buf = Unpooled.buffer();
        buf.writeShort(0xAA55);          // 帧头
        buf.writeByte(0x01);             // 命令字：启动
        buf.writeByte(0x01);             // 设备地址（需映射）
        byte[] data = command.toString().getBytes(StandardCharsets.UTF_8);
        buf.writeShort(data.length);     // 数据长度
        buf.writeBytes(data);            // 数据
        buf.writeShort(calcCrc(data));   // CRC16
        byte[] frame = new byte[buf.readableBytes()];
        buf.readBytes(frame);
        buf.release();
        return frame;
    }

    @Override
    protected void onDataReceived(String deviceCode, byte[] data) {
        // 解析二进制数据为业务语义，委托 EqDeviceService 处理
        // 此处假设 data 已解码为结构化对象
        log.debug("东华原数据接收: deviceCode={}, len={}", deviceCode, data.length);

        // TODO: 根据实际协议解析温度、状态、故障码
        // 解析完成后调用 eqDeviceService.handleDeviceStatusReport(payload);
    }

    private short calcCrc(byte[] data) {
        // TODO: 实现 Modbus CRC16 计算
        return 0;
    }

    // ========== Netty Handler 内部类 ==========

    /**
     * 东华原帧解码器 —— 处理粘包/拆包
     */
    private class DonghuayuanFrameDecoder extends ByteToMessageDecoder {

        private static final int MIN_FRAME_LEN = 7;  // 头2 + 命令1 + 地址1 + 长度2

        @Override
        protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
            if (in.readableBytes() < MIN_FRAME_LEN) {
                return;  // 数据不足，等待下一次读取
            }

            in.markReaderIndex();
            short header = in.readShort();
            if (header != (short) 0xAA55) {
                log.warn("非法帧头: 0x{}", Integer.toHexString(header & 0xFFFF));
                in.resetReaderIndex();
                in.skipBytes(1);  // 丢弃1字节继续同步
                return;
            }

            in.readByte();  // 命令字
            byte address = in.readByte();
            int dataLen = in.readUnsignedShort();

            if (in.readableBytes() < dataLen + 2) {  // 数据 + CRC
                in.resetReaderIndex();  // 数据不足，回滚等待
                return;
            }

            ByteBuf frame = in.readSlice(dataLen + 2).retain();
            out.add(frame);
        }
    }

    /**
     * 东华原业务处理器
     */
    private class DonghuayuanHandler extends SimpleChannelInboundHandler<ByteBuf> {

        // TODO: 通过设备注册包或地址映射建立 channel → deviceCode 关系
        private String deviceCode;

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            log.info("东华原设备连接建立: {}", ctx.channel().remoteAddress());
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
            // TODO: 解析 msg 提取 deviceCode、温度、状态
            // 临时示例：假设第一个数据包包含 deviceCode
            if (deviceCode == null) {
                deviceCode = "DHY_" + ctx.channel().remoteAddress().toString();
                deviceChannelMap.put(deviceCode, ctx.channel());
            }

            byte[] data = new byte[msg.readableBytes()];
            msg.readBytes(data);
            onDataReceived(deviceCode, data);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            log.info("东华原设备连接断开: {}", ctx.channel().remoteAddress());
            if (deviceCode != null) {
                deviceChannelMap.remove(deviceCode);
                // 可选：标记设备离线
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            log.error("东华原连接异常: {}", ctx.channel().remoteAddress(), cause);
            ctx.close();
        }
    }
}
```

### 8.8 适配器扩展指南

新增一家厂商（如"中药科技"MQTT协议）的步骤：

1. 创建 `ZhongyaokejiMqttAdapter extends AbstractMqttAdapter`；
2. 实现 `getVendor()` 返回 `"zhongyaokeji"`；
3. 实现 `onMessage(String topic, String payload)` 解析厂商自定义 JSON；
4. 实现 `doSendCommand()` 处理厂商自定义指令格式；
5. 类上加 `@Component`，在 `init()` 方法中调用 `register(adapterRegistry)`；
6. 创建设备时，`protocol_type='MQTT'`, `vendor='zhongyaokeji'`，`AdapterRegistry` 自动路由。

---

## 九、温度告警与阈值继承策略

### 9.1 三层继承模型

```
┌─────────────────────────────────────────┐
│  第一层：系统默认（sys_config）            │
│  equipment.default.alarmHighTemp = 110.0 │
│  equipment.default.alarmLowTemp  = 80.0  │
└─────────────────────────────────────────┘
                    │
                    ▼  设备级未配置时继承
┌─────────────────────────────────────────┐
│  第二层：设备级（eq_device）               │
│  alarm_high_temp = 105.0                 │
│  alarm_low_temp  = 85.0                  │
└─────────────────────────────────────────┘
                    │
                    ▼  方案级运行时覆盖
┌─────────────────────────────────────────┐
│  第三层：方案级（md_decoct_scheme）       │
│  alarm_high_temp = 102.0                 │
│  alarm_low_temp  = 88.0                  │
└─────────────────────────────────────────┘
```

**继承规则**：
- 如果方案正在运行（设备当前任务关联了方案），且方案配置了阈值，**方案级生效**；
- 否则如果设备级配置了阈值（非 null），**设备级生效**；
- 否则使用 **系统默认**。

### 9.2 告警服务实现

```java
package cn.org.openygt.equipment.service.impl;

import cn.org.openygt.equipment.entity.EqDevice;
import cn.org.openygt.equipment.entity.EqDeviceAlarm;
import cn.org.openygt.equipment.enums.*;
import cn.org.openygt.equipment.mapper.EqDeviceAlarmMapper;
import cn.org.openygt.equipment.service.EquipmentService;
import cn.org.openygt.equipment.service.EqDeviceAlarmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EqDeviceAlarmServiceImpl implements EqDeviceAlarmService {

    private final EqDeviceAlarmMapper alarmMapper;
    private final EquipmentService equipmentService;

    // 告警抑制间隔：同一设备同类告警 5 分钟内不重复触发
    private static final long FAULT_COOLDOWN_MINUTES = 5;

    @Override
    @Transactional
    public void checkTemperatureAlarm(EqDevice device, BigDecimal currentTemp) {
        var threshold = equipmentService.getEffectiveThreshold(device.getId());
        BigDecimal high = threshold.getHighTemp();
        BigDecimal low = threshold.getLowTemp();

        if (high != null && currentTemp.compareTo(high) > 0) {
            triggerAlarm(device, AlarmType.HIGH_TEMP, AlarmLevel.CRITICAL,
                String.format("设备超温: 当前 %.1f℃ > 阈值 %.1f℃", currentTemp, high));
        } else if (low != null && currentTemp.compareTo(low) < 0) {
            triggerAlarm(device, AlarmType.LOW_TEMP, AlarmLevel.WARNING,
                String.format("设备低温: 当前 %.1f℃ < 阈值 %.1f℃", currentTemp, low));
        } else {
            // 温度恢复正常，自动解除同类未恢复告警
            resolveActiveAlarm(device.getId(), AlarmType.HIGH_TEMP);
            resolveActiveAlarm(device.getId(), AlarmType.LOW_TEMP);
        }
    }

    @Override
    @Transactional
    public void createAlarm(EqDevice device, AlarmType type, AlarmLevel level, String message) {
        triggerAlarm(device, type, level, message);
    }

    private void triggerAlarm(EqDevice device, AlarmType type, AlarmLevel level, String message) {
        // 检查冷却期
        Optional<EqDeviceAlarm> latest = alarmMapper
            .findTopByDeviceIdAndAlarmTypeOrderByCreatedAtDesc(device.getId(), type);

        if (latest.isPresent()) {
            LocalDateTime cooldownEnd = latest.get().getCreatedAt().plusMinutes(FAULT_COOLDOWN_MINUTES);
            if (latest.get().getIsResolved() == 0 && cooldownEnd.isAfter(LocalDateTime.now())) {
                log.debug("告警冷却期内，跳过: deviceCode={}, type={}", device.getDeviceCode(), type);
                return;
            }
        }

        EqDeviceAlarm alarm = EqDeviceAlarm.builder()
            .deviceId(device.getId())
            .alarmType(type)
            .alarmLevel(level)
            .message(message)
            .isResolved(0)
            .build();
        alarmMapper.save(alarm);

        log.warn("设备告警触发: deviceCode={}, type={}, level={}, msg={}",
            device.getDeviceCode(), type, level, message);
    }

    private void resolveActiveAlarm(Long deviceId, AlarmType type) {
        alarmMapper.findActiveByDeviceIdAndType(deviceId, type)
            .forEach(alarm -> {
                alarm.setIsResolved(1);
                alarm.setResolvedAt(LocalDateTime.now());
                alarmMapper.save(alarm);
                log.info("告警自动解除: deviceId={}, type={}", deviceId, type);
            });
    }
}
```

### 9.3 告警 Mapper 扩展

```java
@Mapper
public interface EqDeviceAlarmMapper extends BaseMapper<EqDeviceAlarm> {

    Optional<EqDeviceAlarm> findTopByDeviceIdAndAlarmTypeOrderByCreatedAtDesc(
        Long deviceId, AlarmType alarmType);

    @Query("SELECT a FROM EqDeviceAlarm a WHERE a.deviceId = :deviceId " +
           "AND a.alarmType = :type AND a.isResolved = 0 AND a.deleted = 0")
    List<EqDeviceAlarm> findActiveByDeviceIdAndType(@Param("deviceId") Long deviceId,
                                                     @Param("type") AlarmType type);
}
```

---

## 十、并发与事务策略（悲观锁）

### 10.1 为什么不用 synchronized

现状使用 `synchronized` 方法锁存在以下问题：
1. **单 JVM 有效**：集群部署时无法跨节点互斥；
2. **粒度太粗**：锁整个方法，影响吞吐量；
3. **无持久化**：宕机后锁状态丢失，无法恢复设备占用关系。

### 10.2 悲观锁实现

采用 MyBatis-Plus `@Select("SELECT * FROM eq_device WHERE id = #{id} FOR UPDATE")`（即 `SELECT ... FOR UPDATE`）。

**加锁流程**：
```
事务开始
    │
    ▼
SELECT * FROM eq_device WHERE id = ? AND deleted = 0 FOR UPDATE
    │
    ▼
校验状态（IDLE?）
    │
    ▼
UPDATE status = 'RUNNING'
    │
    ▼
事务提交（锁释放）
```

### 10.3 使用规范

```java
// 正确：事务内调用加锁查询
@Transactional
public EqDeviceDTO reserveDevice(Long deviceId, Long taskId) {
    EqDevice device = eqDeviceMapper.selectForUpdate(deviceId)
        .orElseThrow(() -> new IllegalStateException("设备不存在"));
    // ... 业务逻辑
}

// 错误：锁在事务外获取，不生效
public EqDeviceDTO wrongReserve(Long deviceId) {
    EqDevice device = eqDeviceMapper.selectForUpdate(deviceId).orElseThrow(...);
    // 此处事务已结束，锁已释放！
    updateDevice(device);  // 无锁保护，并发不安全
}
```

### 10.4 锁超时与死锁预防

在 `application.yml` 中配置：

```yaml
spring:
  jpa:
    properties:
      hibernate:
        lock:
          timeout: 3000  # 锁等待超时 3 秒，超时抛 LockTimeoutException
```

**异常处理**：
```java
try {
    return reserveDevice(deviceId, taskId);
} catch (LockTimeoutException e) {
    throw new IllegalStateException("设备操作繁忙，请稍后重试");
} catch (PessimisticLockException e) {
    throw new IllegalStateException("设备数据被锁定，请稍后重试");
}
```

---

## 十一、单元测试策略

### 11.1 测试分层

| 层级 | 范围 | 工具 | 覆盖率目标 |
|------|------|------|-----------|
| 单元测试 | Service / Adapter / Util | JUnit 5 + Mockito | ≥ 80% |
| 集成测试 | Mapper + SQLite 数据库 | @MybatisPlusTest | 关键查询 |
| 协议测试 | MQTT / TCP 帧编解码 | 自定义 ByteBuf 测试 | 全部分支 |

### 11.2 关键测试用例

#### 11.2.1 悲观锁并发测试

```java
@SpringBootTest
class EquipmentServiceConcurrencyTest {

    @Autowired EquipmentService equipmentService;
    @Autowired EqDeviceMapper repository;

    @BeforeEach
    void setup() {
        EqDevice device = EqDevice.builder()
            .deviceCode("D_TEST")
            .name("测试机")
            .status(DeviceStatus.IDLE)
            .build();
        repository.insert(device);
    }

    @Test
    void testReserveDevice_concurrentAccess() throws InterruptedException {
        Long deviceId = repository.findByDeviceCodeAndDeletedFalse("D_TEST").get().getId();
        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final long taskId = i;
            new Thread(() -> {
                try {
                    equipmentService.reserveDevice(deviceId, taskId);
                    successCount.incrementAndGet();
                } catch (IllegalStateException e) {
                    if (e.getMessage().contains("非空闲")) {
                        conflictCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        latch.await(10, TimeUnit.SECONDS);
        assertEquals(1, successCount.get(), "仅应有一个线程成功占用");
        assertEquals(threadCount - 1, conflictCount.get(), "其余应冲突失败");
    }
}
```

#### 11.2.2 MQTT JSON 解析测试

```java
@ExtendWith(MockitoExtension.class)
class MqttConfigTest {

    @Mock EqDeviceService eqDeviceService;
    @InjectMocks MqttConfig mqttConfig;
    ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testHandleMessage_statusJsonParsed() {
        String topic = "/openygt/0/D001/status";
        String payload = "{\"deviceCode\":\"D001\",\"status\":\"RUNNING\",\"temperature\":98.5,\"faultCode\":null}";

        mqttConfig.handleMessage(topic, payload);

        ArgumentCaptor<DeviceStatusPayload> captor = ArgumentCaptor.forClass(DeviceStatusPayload.class);
        verify(eqDeviceService).handleDeviceStatusReport(captor.capture());

        assertEquals("D001", captor.getValue().getDeviceCode());
        assertEquals(new BigDecimal("98.5"), captor.getValue().getTemperature());
    }

    @Test
    void testHandleMessage_malformedPayload_noException() {
        // 确保异常被捕获，不会抛出
        assertDoesNotThrow(() -> mqttConfig.handleMessage("/openygt/0/D001/status", "{invalid"));
    }
}
```

#### 11.2.3 温度阈值继承测试

```java
@ExtendWith(MockitoExtension.class)
class EquipmentServiceThresholdTest {

    @Mock EqDeviceMapper deviceMapper;
    @Mock SysConfigService sysConfigService;
    @InjectMocks EquipmentServiceImpl service;

    @Test
    void testGetEffectiveThreshold_deviceLevelWins() {
        EqDevice device = EqDevice.builder()
            .id(1L).deviceCode("D001")
            .alarmHighTemp(new BigDecimal("105.0"))
            .alarmLowTemp(new BigDecimal("85.0"))
            .build();
        deviceMapper.selectById(1L);(device));

        TemperatureThresholdDTO result = service.getEffectiveThreshold(1L);

        assertEquals(new BigDecimal("105.0"), result.getHighTemp());
        assertEquals("DEVICE", result.getSource());
    }

    @Test
    void testGetEffectiveThreshold_fallbackToSystem() {
        EqDevice device = EqDevice.builder()
            .id(1L).deviceCode("D001")
            .alarmHighTemp(null)
            .alarmLowTemp(null)
            .build();
        deviceMapper.selectById(1L);(device));
        when(sysConfigService.getDecimalValue(any(), any())).thenReturn(new BigDecimal("110.0"));

        TemperatureThresholdDTO result = service.getEffectiveThreshold(1L);

        assertEquals(new BigDecimal("110.0"), result.getHighTemp());
        assertEquals("SYSTEM", result.getSource());
    }
}
```

---

## 十二、迁移路径

### 12.1 数据库迁移（Flyway）

创建文件 `V1.4__equipment_iot_enhancement.sql`：

```sql
-- 1. 修改 eq_device 表：新增协议相关字段
ALTER TABLE eq_device
    ADD COLUMN protocol_type VARCHAR(32) NOT NULL DEFAULT 'MQTT' COMMENT 'MQTT/TCP_BINARY',
    ADD COLUMN vendor VARCHAR(64) DEFAULT 'GENERIC',
    ADD COLUMN alarm_high_temp DECIMAL(5,2),
    ADD COLUMN alarm_low_temp DECIMAL(5,2),
    ADD COLUMN last_heartbeat DATETIME,
    ADD INDEX idx_eq_device_heartbeat (last_heartbeat);

-- 2. 新建 eq_device_connection 表
CREATE TABLE IF NOT EXISTS eq_device_connection (...);  -- 见第三章DDL

-- 3. 新建 eq_device_alarm 表
CREATE TABLE IF NOT EXISTS eq_device_alarm (...);  -- 见第三章DDL

-- 4. 重建 eq_temperature_log（注意：如已有数据需先迁移）
-- 方案A：如有少量数据，可导出后重建
-- 方案B：如无数据，直接 DROP 重建
DROP TABLE IF EXISTS eq_temperature_log;
CREATE TABLE eq_temperature_log (...);  -- 见第三章DDL，无 deleted 字段
```

### 12.2 代码迁移步骤

| 步骤 | 内容 | 影响范围 |
|------|------|---------|
| 1 | 新增枚举类 `ProtocolType`、`AlarmType`、`AlarmLevel` | `equipment.enums` |
| 2 | 修改 `EqTemperatureLog` 继承 `BaseAuditEntity` | `equipment.entity` |
| 3 | 新增 `EqDeviceAlarm`、`EqDeviceConnection` 实体 | `equipment.entity` |
| 4 | 新增 Mapper：`EqDeviceAlarmMapper`、`EqTemperatureLogMapper` | `equipment.mapper` |
| 5 | 扩展 `EqDeviceMapper` 加 `selectForUpdate` | `equipment.mapper` |
| 6 | 重写 `MqttConfig.handleMessage` 支持 JSON 解析 | `equipment.mqtt` |
| 7 | 新增 `EqDeviceAlarmService` + `Impl` | `equipment.service` |
| 8 | 修改 `EquipmentServiceImpl`：返回值改 `EqDeviceDTO`，实现 `reserveDevice`/`releaseDevice` | `equipment.service.impl` |
| 9 | 删除 `synchronized` 关键字，改用 `selectForUpdate` | `equipment.service.impl` |
| 10 | 新增适配器框架包 `equipment.adapter` 及东华原实现 | `equipment.adapter` |
| 11 | 新增心跳检测定时任务 `HeartbeatCheckScheduler` | `equipment.scheduler` |
| 12 | 补充 Controller 接口：`reserve`、`release`、`threshold` | `equipment.controller` |

### 12.3 回滚预案

若上线后出现问题：
1. **数据库**：Flyway 已执行的历史不可直接 `DELETE`，需手动准备回滚脚本 `U1.4__...`（如需）；
2. **代码**：Git 回滚到上一个 tag，重新打包部署；
3. **MQTT 兼容性**：JSON 解析失败后当前实现会 catch 异常并记录日志，不影响其他设备；
4. **TCP 服务端口**：东华原适配器启动失败不影响 MQTT 设备，二者独立运行。

### 12.4 验证清单

- [ ] 创建设备后，MQTT 状态上报正确解析 JSON 并更新温度
- [ ] 温度上报同时更新 `last_heartbeat`
- [ ] 纯心跳报文正确更新 `last_heartbeat`，不改变温度
- [ ] 停止心跳 120 秒后，IDLE 设备自动标记 OFFLINE
- [ ] 停止心跳 600 秒后，RUNNING 设备自动标记 OFFLINE
- [ ] 两个并发请求同时占用同一设备，仅一个成功，另一个返回 409
- [ ] 温度超过阈值后 5 分钟内不重复告警
- [ ] 温度恢复正常后告警自动解除
- [ ] 东华原 TCP 设备连接后可在 `deviceChannelMap` 中查到映射
- [ ] `eq_temperature_log` 表无 `deleted` 字段，可正常写入

---

> 本文档为 dms-equipment V1.4 详细设计，开发人员应以此为准进行编码实现。
