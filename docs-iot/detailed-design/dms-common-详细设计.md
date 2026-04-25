# dms-common 详细设计

> 模块：公共基础模块（openygt-dms / dms-common）  
> 版本：V1.4（对应概设 V1.4，专家评审04后修订版）  
> 日期：2026-04-25  
> 包根：`cn.org.openygt.common`  
> 输出：Jar（无独立部署单元）  

---

## 一、模块概述

### 1.1 职责定位

`dms-common` 是煎药室管理系统（OpenYGT-DMS）所有业务模块的公共依赖，**不包含业务逻辑、不提供 REST API、不操作数据库**。其核心职责：

1. **实体基类**：定义所有业务表的通用字段（`id`, `tenant_id`, `created_at`, `updated_at`, `deleted`）
2. **业务枚举**：定义全局通用的枚举值（设备状态、设备类型、任务状态、质检结果类型等）
3. **数据传输对象**：定义跨模块传递的 DTO（如质检结果、设备信息、打印任务等）
4. **异常与统一响应**：全局异常处理器、统一 API 响应包装类 `ApiResponse`
5. **SPI 接口定义**：定义模块间契约接口（`EquipmentService`、`PrintService`、`QualityService`、`ProductionQueryService`）

### 1.2 依赖关系

**Maven 依赖（入向）**：

| 依赖 | 作用 |
|------|------|
| `spring-boot-starter-web` | 提供 `@RestControllerAdvice`、`HttpStatus` 等 |
| `spring-boot-starter-validation` | 提供 `javax.validation` / `jakarta.validation` 注解（`@NotBlank` 等） |
| `mybatis-plus-boot-starter` | 提供 `@TableId`、`@TableField`、`@TableLogic` 等 |
| `lombok` | 代码生成 |

**依赖约束**：`dms-common` 禁止依赖数据库驱动、MQTT 客户端、业务模块等任何与具体业务相关的库。

**被依赖方（出向）**：

```
dms-common
    ├── dms-app          (全局异常处理、统一响应)
    ├── dms-system       (JWT DTO)
    ├── dms-masterdata   (BaseEntity)
    ├── dms-equipment    (实现 EquipmentService SPI)
    ├── dms-production   (调用 EquipmentService / PrintService / QualityService)
    ├── dms-quality      (实现 QualityService SPI，调用 ProductionQueryService)
    ├── dms-print        (实现 PrintService SPI，调用 ProductionQueryService)
    └── dms-analytics    (调用 ProductionQueryService)
```

### 1.3 代码现状 vs 目标差距摘要

当前代码与 V1.4 目标存在 **9 项差距**，其中 5 项为新增文件，4 项为现有文件修改：

| 差距项 | 现状 | 目标 | 类型 | 优先级 |
|--------|------|------|------|--------|
| BaseEntity 时间类型 | `java.time.LocalDateTime` | `java.time.LocalDateTime` | 修改 | P0 |
| BaseAuditEntity | **不存在** | 新增审计基类（无 `deleted`） | 新增 | P0 |
| InspectionResultType 枚举 | **不存在** | PASS/CONCESSION/REWORK/SCRAP | 新增 | P0 |
| EqDeviceDTO | **不存在** | 设备查询统一返回类型 | 新增 | P0 |
| PrintTaskDTO | **不存在** | 打印队列统一返回类型 | 新增 | P0 |
| ProdTaskDTO | **不存在** | 生产任务查询返回类型 | 新增 | P0 |
| CapacityDailyDTO / TaskStatusHistoryDTO | **不存在** | 统计查询传输对象 | 新增 | P0 |
| LoginRequest / TokenResponse | **不存在** | JWT 认证 DTO | 新增 | P0 |
| EquipmentService 返回值 | `Object` | `EqDeviceDTO` | 修改 | P0 |
| PrintService.getPrintQueue | `Object` | `List<PrintTaskDTO>` | 修改 | P0 |
| QualityService.inspect 参数 | `String result` | `InspectionResultType result` | 修改 | P0 |
| ProductionQueryService | **不存在** | 新增只读查询 SPI | 新增 | P0 |
| InspectionResult 时间类型 | `java.time.LocalDateTime` | `java.time.LocalDateTime` | 修改 | P0 |

---

## 二、代码现状 vs 目标差距

### 2.1 致命级差距（不修复则下游模块无法编译或运行错误）

| 编号 | 问题 | 影响 | 修复方式 |
|------|------|------|----------|
| GAP-01 | `BaseEntity.createdAt/updatedAt` 为 `Date` 类型，与 V1.4 要求的 `LocalDateTime` 不一致 | MyBatis-Plus 自动填充配置若按 `LocalDateTime` 写，会出现类型不匹配；所有子类实体时间字段混乱 | 修改 `BaseEntity.java` 字段类型 |
| GAP-02 | `BaseAuditEntity` 缺失 | `sys_log`、`eq_temperature_log`、`prod_task_status_history` 等审计/遥测表无合适基类继承；若继承 `BaseEntity` 会被逻辑删除 | 新增 `BaseAuditEntity.java` |
| GAP-03 | `InspectionResultType` 枚举缺失 | `QualityService.inspect()` 仍用中文字符串 `result` 传参，switch-case 易因空格/编码失败 | 新增 `InspectionResultType.java`，修改 `QualityService` 签名 |
| GAP-04 | `EquipmentService` 返回值仍为 `Object` | `dms-equipment` 的实现返回内部 `EqDevice` 实体，调用方需要强制转型；`dms-print` 中 `getOrCreateDevice` 返回 Object 后手动处理 | 修改接口返回 `EqDeviceDTO`，同步修改 `EquipmentServiceImpl` |
| GAP-05 | `PrintService.getPrintQueue()` 返回 `Object` | 调用方无法获得类型安全的打印队列列表 | 修改接口返回 `List<PrintTaskDTO>`，同步修改 `PrintServiceImpl` |
| GAP-06 | `ProductionQueryService` 缺失 | `dms-analytics` 可能直接跨模块 SELECT；`dms-quality`/`dms-print` 反查任务信息无标准通道 | 新增 `ProductionQueryService.java` |

### 2.2 高危差距（功能缺陷或架构债务）

| 编号 | 问题 | 影响 | 修复方式 |
|------|------|------|----------|
| GAP-07 | `EquipmentService` 缺少 `reserveDevice(Long taskId, Long deviceId)` | 返工场景设备可能被其他任务抢占 | 在接口中新增方法，`EquipmentServiceImpl` 实现 |
| GAP-08 | `InspectionResult.inspectedAt` 为 `Date` | 与 V1.4 统一时间类型冲突 | 修改字段类型为 `LocalDateTime` |
| GAP-09 | 无 JWT 认证 DTO | 医疗合规要求（个保法/GSP） | 新增 `LoginRequest.java`、`TokenResponse.java` |

---

## 三、实体类详细设计

### 3.1 类图（文字版）

```
java.io.Serializable
        ↑
  BaseEntity (abstract)            BaseAuditEntity (abstract)
  - id: Long                       - id: Long
  - tenantId: String               - tenantId: String
  - createdAt: LocalDateTime       - createdAt: LocalDateTime
  - updatedAt: LocalDateTime       - updatedAt: LocalDateTime
  - deleted: Integer (@TableLogic)  (无 deleted 字段)
        ↑
  [所有业务实体继承]
```

### 3.2 BaseEntity（含逻辑删除）

**文件**：`dms-common/src/main/java/cn/org/openygt/common/entity/BaseEntity.java`

**目标代码**：

```java
package cn.org.openygt.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 业务实体基类（含逻辑删除）。
 * <p>
 * 所有需要逻辑删除的业务表对应的实体应继承此类。
 * 字段填充由 dms-app 的 MyBatis-Plus 配置统一维护。
 *
 * @see BaseAuditEntity 用于审计/遥测表（无逻辑删除）
 */
@Data
public abstract class BaseEntity {

    /** 主键，数据库自增（SQLite: AUTOINCREMENT）。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户 ID，默认 "default"，支持多医院部署隔离。 */
    @TableField("tenant_id")
    private String tenantId = "default";

    /** 创建时间，由 MyBatis-Plus 自动填充。 */
    private LocalDateTime createdAt;

    /** 更新时间，由 MyBatis-Plus 自动填充。 */
    private LocalDateTime updatedAt;

    /** 逻辑删除标记：0=正常，1=已删除。 */
    @TableLogic
    private Integer deleted = 0;
}
```

**字段说明**：

| 字段 | 类型 | 注解 | 默认值 | 说明 |
|------|------|------|--------|------|
| `id` | `Long` | `@TableId(type = IdType.AUTO)` | — | 自增主键 |
| `tenantId` | `String` | `@TableField("tenant_id")` | `"default"` | 租户隔离 |
| `createdAt` | `LocalDateTime` | — | — | 创建时间 |
| `updatedAt` | `LocalDateTime` | — | — | 更新时间 |
| `deleted` | `Integer` | `@TableLogic` | `0` | 逻辑删除 |

**现状修改点**：
- 将 `java.time.LocalDateTime` 改为 `java.time.LocalDateTime`（`createdAt`、`updatedAt`）
- 增加 `@TableField("tenant_id")` 显式映射（现状已有）
- 增加类级 Javadoc

### 3.3 BaseAuditEntity（不含逻辑删除）

**文件**：`dms-common/src/main/java/cn/org/openygt/common/entity/BaseAuditEntity.java`（**新增**）

**目标代码**：

```java
package cn.org.openygt.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审计实体基类（不含逻辑删除）。
 * <p>
 * 适用于审计日志、遥测数据、状态历史等<strong>不允许逻辑删除</strong>的表。
 * 数据必须物理保留，以满足 GMP 审计追踪要求。
 *
 * <p>适用表示例：
 * <ul>
 *   <li>sys_log（操作日志）</li>
 *   <li>eq_temperature_log（温度遥测）</li>
 *   <li>prod_task_status_history（任务状态变更历史）</li>
 * </ul>
 */
@Data
public abstract class BaseAuditEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("tenant_id")
    private String tenantId = "default";

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // 注意：无 deleted 字段，无 @TableLogic
}
```

**设计决策**：
- 不继承 `BaseEntity`，而是平级抽象类。原因：继承会强制带入 `deleted` 字段，而审计表不需要逻辑删除。
- 字段与 `BaseEntity` 完全一致（除 `deleted`），保证 MyBatis-Plus 自动填充配置可统一处理。

---

## 四、枚举详细设计

### 4.1 枚举设计总览

| 枚举 | 文件路径 | 状态 |
|------|----------|------|
| `TaskStatus` | `.../enums/TaskStatus.java` | 已存在，无需修改 |
| `DeviceStatus` | `.../enums/DeviceStatus.java` | 已存在，无需修改 |
| `DeviceType` | `.../enums/DeviceType.java` | 已存在，无需修改 |
| `InspectionResultType` | `.../enums/InspectionResultType.java` | **新增** |

### 4.2 TaskStatus（任务状态）

**文件**：`dms-common/src/main/java/cn/org/openygt/common/enums/TaskStatus.java`

```java
package cn.org.openygt.common.enums;

/**
 * 煎药生产任务状态枚举。
 * <p>
 * <strong>此枚举是全局状态机的唯一合法状态集合</strong>，任何模块不得自行定义额外状态字符串。
 * 数据库存储使用枚举名（如 {@code WAIT_SOAK}），展示层使用 {@link #getLabel()}。
 */
public enum TaskStatus {
    WAIT_SOAK("待泡药"),
    SOAKING("泡药中"),
    WAIT_DECOCT("待煎药"),
    DECOCTING("煎药中"),
    WAIT_POUR("待出液"),
    POURING("出液中"),
    WAIT_WRAP("待包装"),
    WRAPPING("包装中"),
    WAIT_LABEL("待贴标"),
    WAIT_QC("待质检"),
    WAIT_HANDOVER("待交接"),
    COMPLETED("已完成"),
    PARTIAL_COMPLETED("已部分完成"),
    SCRAPPED("已报废");

    private final String label;

    TaskStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
```

### 4.3 DeviceStatus（设备状态）

**文件**：`dms-common/src/main/java/cn/org/openygt/common/enums/DeviceStatus.java`

```java
package cn.org.openygt.common.enums;

/**
 * 设备运行状态枚举。
 * <p>
 * 数据库存储使用枚举名（如 {@code IDLE}）。
 * 历史方法中部分使用小写字符串（{@code "idle"}, {@code "running"}），新开发建议统一使用枚举名。
 */
public enum DeviceStatus {
    IDLE("空闲"),
    RUNNING("运行中"),
    FAULT("故障"),
    OFFLINE("离线"),
    MAINTENANCE("维护中");

    private final String label;

    DeviceStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
```

### 4.4 DeviceType（设备类型）

**文件**：`dms-common/src/main/java/cn/org/openygt/common/enums/DeviceType.java`

```java
package cn.org.openygt.common.enums;

/**
 * 设备类型枚举。
 * <p>
 * 包含数值编码 {@link #getCode()}，用于与硬件协议或旧系统对接。
 */
public enum DeviceType {
    COOKING_MACHINE(1, "煎药机"),
    PACKING_MACHINE(2, "包装机"),
    PRINTER(4, "打印机");

    private final int code;
    private final String label;

    DeviceType(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public int getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }
}
```

### 4.5 InspectionResultType（质检结果类型）【新增】

**文件**：`dms-common/src/main/java/cn/org/openygt/common/enums/InspectionResultType.java`

```java
package cn.org.openygt.common.enums;

/**
 * 质检结果类型枚举。
 * <p>
 * <strong>评审修订03</strong>：替代原中文字符串传参（"通过"/"让步放行"/"返工"/"报废"），
 * 消除因空格、编码导致的 switch-case 匹配失败风险。
 *
 * <p>使用规则：
 * <ul>
 *   <li>SPI 接口参数、数据库存储、switch-case 统一使用枚举名（{@code PASS}, {@code CONCESSION}, {@code REWORK}, {@code SCRAP}）</li>
 *   <li>展示层使用 {@link #getLabel()} 获取中文标签</li>
 * </ul>
 */
public enum InspectionResultType {
    PASS("通过"),
    CONCESSION("让步放行"),
    REWORK("返工"),
    SCRAP("报废");

    private final String label;

    InspectionResultType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
```

**约束**：枚举值一旦发布不得删除，可在末尾追加新值。

---

## 五、DTO 详细设计

### 5.1 DTO 总览

| DTO | 文件路径 | 状态 | 说明 |
|-----|----------|------|------|
| `ApiResponse<T>` | `.../dto/ApiResponse.java` | 已存在，无需修改 | 统一 REST 响应 |
| `InspectionResult` | `.../dto/InspectionResult.java` | **需修改** | 质检结果（时间类型改为 LocalDateTime） |
| `EqDeviceDTO` | `.../dto/EqDeviceDTO.java` | **新增** | 设备查询统一返回类型 |
| `PrintTaskDTO` | `.../dto/PrintTaskDTO.java` | **新增** | 打印任务传输对象 |
| `ProdTaskDTO` | `.../dto/ProdTaskDTO.java` | **新增** | 生产任务查询对象 |
| `CapacityDailyDTO` | `.../dto/CapacityDailyDTO.java` | **新增** | 日产能统计对象 |
| `TaskStatusHistoryDTO` | `.../dto/TaskStatusHistoryDTO.java` | **新增** | 状态历史传输对象 |
| `LoginRequest` | `.../dto/LoginRequest.java` | **新增** | JWT 登录请求 |
| `TokenResponse` | `.../dto/TokenResponse.java` | **新增** | JWT 登录响应 |

### 5.2 ApiResponse<T>（统一响应）

**文件**：`dms-common/src/main/java/cn/org/openygt/common/dto/ApiResponse.java`

```java
package cn.org.openygt.common.dto;

import lombok.Data;

/**
 * REST API 统一响应包装类。
 *
 * @param <T> 业务数据类型
 */
@Data
public class ApiResponse<T> {

    /** HTTP 状态码或业务码，200 表示成功 */
    private int code;

    /** 提示信息 */
    private String message;

    /** 业务数据 */
    private T data;

    private ApiResponse() {}

    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> r = new ApiResponse<>();
        r.code = 200;
        r.message = "success";
        r.data = data;
        return r;
    }

    public static <T> ApiResponse<T> success() {
        return success(null);
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        ApiResponse<T> r = new ApiResponse<>();
        r.code = code;
        r.message = message;
        r.data = null;
        return r;
    }
}
```

**使用规范**：
- 所有 Controller 返回值必须是 `ApiResponse<T>`
- `data` 为 `null` 时也必须返回完整 `ApiResponse` 对象

### 5.3 InspectionResult（质检结果 DTO）【需修改】

**文件**：`dms-common/src/main/java/cn/org/openygt/common/dto/InspectionResult.java`

```java
package cn.org.openygt.common.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 质检操作结果 DTO。
 * <p>
 * 由 {@link cn.org.openygt.common.service.QualityService#inspect} 返回，
 * 跨模块传递质检记录 ID、任务下一状态及异常信息。
 */
@Data
public class InspectionResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 质检记录 ID */
    private Long inspectionId;

    /** 关联的任务 ID */
    private Long taskId;

    /** 质检结果：通过/让步放行/返工/报废 */
    private String result;

    /** 质检后任务的目标状态 */
    private String nextStatus;

    /** 操作人 ID */
    private String operatorId;

    /** 备注 */
    private String remark;

    /** 质检时间 */
    private LocalDateTime inspectedAt;

    /** 是否异常：0=正常，1=异常 */
    private Integer isException;

    /** 异常原因 */
    private String exceptionReason;
}
```

**修改点**：
- `inspectedAt` 由 `java.time.LocalDateTime` 改为 `java.time.LocalDateTime`
- 增加 `Serializable` 接口（为微服务拆分预留）
- 增加 `serialVersionUID`

### 5.4 EqDeviceDTO（设备传输 DTO）【新增】

**文件**：`dms-common/src/main/java/cn/org/openygt/common/dto/EqDeviceDTO.java`

```java
package cn.org.openygt.common.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 设备信息传输对象。
 * <p>
 * {@link cn.org.openygt.common.service.EquipmentService} 所有查询方法的统一返回类型，
 * 替代原宽松的 {@code Object} 返回值。
 */
@Data
public class EqDeviceDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 设备 ID */
    private Long id;

    /** 设备编码（唯一标识） */
    private String deviceCode;

    /** 设备名称 */
    private String name;

    /** 设备类型编码（对应 DeviceType.code） */
    private String deviceType;

    /** 当前状态（对应 DeviceStatus 枚举名） */
    private String status;

    /** 当前温度 */
    private BigDecimal currentTemp;

    /** 故障码 */
    private String faultCode;

    /** 自动化级别：manual / semi / auto */
    private String autoLevel;

    /** 安装位置 */
    private String location;

    /** 通信协议类型 */
    private String protocolType;

    /** 厂商 */
    private String vendor;

    /** 高温告警阈值 */
    private BigDecimal alarmHighTemp;

    /** 低温告警阈值 */
    private BigDecimal alarmLowTemp;

    /** 最后一次心跳时间 */
    private LocalDateTime lastHeartbeat;
}
```

### 5.5 PrintTaskDTO（打印任务传输对象）【新增】

**文件**：`dms-common/src/main/java/cn/org/openygt/common/dto/PrintTaskDTO.java`

```java
package cn.org.openygt.common.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 打印任务传输对象。
 * <p>
 * {@link cn.org.openygt.common.service.PrintService#getPrintQueue()} 的返回元素类型。
 */
@Data
public class PrintTaskDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 打印任务 ID */
    private Long id;

    /** 关联的生产任务 ID */
    private Long taskId;

    /** 打印机设备编码 */
    private String deviceCode;

    /** 操作人 ID */
    private String operatorId;

    /** 打印状态：PENDING / PRINTING / SUCCESS / FAILED / CANCELLED */
    private String status;

    /** 打印份数 */
    private Integer copies;

    /** 当前重试次数 */
    private Integer retryCount;

    /** 最大重试次数 */
    private Integer maxRetry;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
```

### 5.6 ProdTaskDTO（生产任务查询对象）【新增】

**文件**：`dms-common/src/main/java/cn/org/openygt/common/dto/ProdTaskDTO.java`

```java
package cn.org.openygt.common.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 生产任务查询传输对象。
 * <p>
 * {@link cn.org.openygt.common.service.ProductionQueryService} 的返回类型，
 * 供 dms-analytics / dms-quality / dms-print 反查任务信息使用。
 */
@Data
public class ProdTaskDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 任务 ID */
    private Long id;

    /** 处方 ID */
    private Long prescriptionId;

    /** 当前状态（TaskStatus 枚举名） */
    private String status;

    /** 煎药设备 ID */
    private Long decoctDeviceId;

    /** 包装设备 ID */
    private Long packageDeviceId;

    /** 当前操作人 ID */
    private String operatorId;

    /** 是否异常：0=正常，1=异常 */
    private Integer isException;

    /** 异常原因 */
    private String exceptionReason;

    /** 交接时间 */
    private LocalDateTime handoverTime;

    /** 完成时间 */
    private LocalDateTime completeTime;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
```

### 5.7 CapacityDailyDTO（日产能统计对象）【新增】

**文件**：`dms-common/src/main/java/cn/org/openygt/common/dto/CapacityDailyDTO.java`

```java
package cn.org.openygt.common.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 日产能统计传输对象。
 */
@Data
public class CapacityDailyDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 统计日期 */
    private LocalDate statDate;

    /** 租户 ID */
    private String tenantId;

    /** 任务总数 */
    private Long totalTasks;

    /** 已完成数 */
    private Long completedTasks;

    /** 已报废数 */
    private Long scrappedTasks;
}
```

### 5.8 TaskStatusHistoryDTO（任务状态历史对象）【新增】

**文件**：`dms-common/src/main/java/cn/org/openygt/common/dto/TaskStatusHistoryDTO.java`

```java
package cn.org.openygt.common.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 任务状态变更历史传输对象。
 */
@Data
public class TaskStatusHistoryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 历史记录 ID */
    private Long id;

    /** 任务 ID */
    private Long taskId;

    /** 变更前状态 */
    private String fromStatus;

    /** 变更后状态 */
    private String toStatus;

    /** 操作人 ID */
    private String operatorId;

    /** 操作时间 */
    private LocalDateTime operateTime;

    /** 备注 */
    private String remark;
}
```

### 5.9 LoginRequest（JWT 登录请求）【新增】

**文件**：`dms-common/src/main/java/cn/org/openygt/common/dto/LoginRequest.java`

```java
package cn.org.openygt.common.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 用户登录请求 DTO。
 * <p>
 * 医疗系统合规要求（个保法/GSP），认证为 P0 需求。
 */
@Data
public class LoginRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;
}
```

### 5.10 TokenResponse（JWT 登录响应）【新增】

**文件**：`dms-common/src/main/java/cn/org/openygt/common/dto/TokenResponse.java`

```java
package cn.org.openygt.common.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 登录成功后的令牌响应 DTO。
 */
@Data
public class TokenResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 访问令牌 */
    private String accessToken;

    /** 令牌类型，默认 Bearer */
    private String tokenType = "Bearer";

    /** 过期时间（秒） */
    private Long expiresIn;

    /** 刷新令牌 */
    private String refreshToken;
}
```

---

## 六、SPI 接口详细设计

### 6.1 SPI 设计总览

| 接口 | 文件路径 | 定义模块 | 实现模块 | 调用方 |
|------|----------|----------|----------|--------|
| `EquipmentService` | `.../service/EquipmentService.java` | dms-common | dms-equipment | dms-production, dms-print |
| `PrintService` | `.../service/PrintService.java` | dms-common | dms-print | dms-production |
| `QualityService` | `.../service/QualityService.java` | dms-common | dms-quality | dms-production |
| `ProductionQueryService` | `.../service/ProductionQueryService.java` | dms-common | dms-production | dms-analytics, dms-quality, dms-print |

**异常契约统一规则**：
- 查询方法：找不到返回 `null`（后续可演进为 `Optional<T>`）
- 操作方法：
  - 参数非法（如 `null`、格式错误、不存在的主键）→ 抛 `IllegalArgumentException`
  - 前置条件不满足（如状态转换非法、资源已被占用）→ 抛 `IllegalStateException`

### 6.2 EquipmentService（设备服务 SPI）

**文件**：`dms-common/src/main/java/cn/org/openygt/common/service/EquipmentService.java`

```java
package cn.org.openygt.common.service;

import cn.org.openygt.common.dto.EqDeviceDTO;

import java.math.BigDecimal;

/**
 * 设备物联模块对外服务接口。
 * <p>
 * 定义在 dms-common，由 dms-equipment 模块实现。
 * 其他模块（如 dms-production、dms-print）通过注入此接口进行跨模块设备操作。
 *
 * <p><strong>异常契约</strong>：
 * <ul>
 *   <li>查询类方法：设备找不到返回 {@code null}</li>
 *   <li>操作类方法：参数非法抛 {@link IllegalArgumentException}，状态冲突抛 {@link IllegalStateException}</li>
 * </ul>
 */
public interface EquipmentService {

    /**
     * 按设备编码查询设备信息。
     *
     * @param deviceCode 设备编码（非空）
     * @return 设备信息，找不到返回 {@code null}
     */
    EqDeviceDTO getDeviceByCode(String deviceCode);

    /**
     * 按设备 ID 查询设备信息。
     *
     * @param deviceId 设备 ID
     * @return 设备信息，找不到返回 {@code null}
     */
    EqDeviceDTO getDeviceById(Long deviceId);

    /**
     * 按编码查询设备，不存在时自动创建。
     *
     * @param deviceCode  设备编码（非空）
     * @param defaultType 默认设备类型编码（对应 {@link cn.org.openygt.common.enums.DeviceType#getCode()}）
     * @return 已存在或新创建的设备信息（非空）
     */
    EqDeviceDTO getOrCreateDevice(String deviceCode, int defaultType);

    /**
     * 更新设备状态。
     *
     * @param deviceId 设备 ID
     * @param status   目标状态（DeviceStatus 枚举名）
     * @throws IllegalArgumentException 设备不存在
     * @throws IllegalStateException    状态转换非法（如 RUNNING→IDLE 需先释放）
     */
    void updateDeviceStatus(Long deviceId, String status);

    /**
     * 更新设备实时温度。
     *
     * @param deviceId    设备 ID
     * @param temperature 当前温度
     * @throws IllegalArgumentException 设备不存在
     */
    void updateTemperature(Long deviceId, BigDecimal temperature);

    /**
     * 上报设备故障。
     *
     * @param deviceId  设备 ID
     * @param faultCode 故障码
     * @param message   故障描述
     * @throws IllegalArgumentException 设备不存在
     */
    void reportFault(Long deviceId, String faultCode, String message);

    /**
     * 清除设备故障，状态恢复为 IDLE。
     *
     * @param deviceId 设备 ID
     * @throws IllegalArgumentException 设备不存在
     */
    void clearFault(Long deviceId);

    /**
     * 释放设备（任务结束，状态置为 IDLE）。
     *
     * @param deviceId 设备 ID
     * @throws IllegalArgumentException 设备不存在
     * @throws IllegalStateException    设备非预留/运行中状态
     */
    void releaseDevice(Long deviceId);

    /**
     * 预留设备（返工场景，防止被其他任务抢占）。
     *
     * @param taskId   预留的任务 ID
     * @param deviceId 设备 ID
     * @throws IllegalArgumentException 设备不存在
     * @throws IllegalStateException    设备已被其他任务占用或预留
     */
    void reserveDevice(Long taskId, Long deviceId);

    /**
     * 检查温度是否超阈值并产生告警（内部逻辑，不抛异常给调用方）。
     *
     * @param deviceId    设备 ID
     * @param temperature 当前温度
     * @throws IllegalArgumentException 设备不存在
     */
    void checkTemperatureAlarm(Long deviceId, BigDecimal temperature);

    /**
     * 查询设备当前状态字符串。
     *
     * @param deviceId 设备 ID
     * @return 状态枚举名，找不到返回 {@code null}
     */
    String getDeviceStatus(Long deviceId);

    /**
     * 查询设备自动化级别。
     *
     * @param deviceId 设备 ID
     * @return 自动化级别（manual / semi / auto），找不到返回 {@code null}
     */
    String getAutoLevel(Long deviceId);

    /**
     * 查询设备编码。
     *
     * @param deviceId 设备 ID
     * @return 设备编码，找不到返回 {@code null}
     */
    String getDeviceCode(Long deviceId);

    /**
     * 按编码查询设备 ID。
     *
     * @param deviceCode 设备编码
     * @return 设备 ID，找不到返回 {@code null}
     */
    Long getDeviceId(String deviceCode);
}
```

### 6.3 PrintService（打印服务 SPI）

**文件**：`dms-common/src/main/java/cn/org/openygt/common/service/PrintService.java`

```java
package cn.org.openygt.common.service;

import cn.org.openygt.common.dto.PrintTaskDTO;

import java.util.List;

/**
 * 打印中心模块对外服务接口。
 * <p>
 * 定义在 dms-common，由 dms-print 模块实现。
 * dms-production 通过注入此接口提交打印任务。
 *
 * <p><strong>异常契约</strong>：
 * <ul>
 *   <li>{@code IllegalArgumentException}：taskId 不存在或参数非法</li>
 *   <li>{@code IllegalStateException}：业务状态冲突（如已有未完成打印记录、重试次数超限）</li>
 * </ul>
 */
public interface PrintService {

    /**
     * 提交打印任务。
     *
     * @param taskId     生产任务 ID
     * @param deviceCode 打印机设备编码
     * @param operatorId 操作人 ID
     * @throws IllegalArgumentException taskId 不存在
     * @throws IllegalStateException    该任务已有未完成的打印记录，或打印机非空闲
     */
    void submitPrintTask(Long taskId, String deviceCode, String operatorId);

    /**
     * 重试打印。
     *
     * @param taskId     生产任务 ID
     * @param deviceCode 打印机设备编码
     * @param operatorId 操作人 ID
     * @throws IllegalArgumentException taskId 不存在
     * @throws IllegalStateException    重试次数已达上限
     */
    void retryPrint(Long taskId, String deviceCode, String operatorId);

    /**
     * 获取待打印队列。
     *
     * @return 待打印任务列表（状态为 PENDING 或 FAILED 且未超限），不会返回 {@code null}（空队列返回空列表）
     */
    List<PrintTaskDTO> getPrintQueue();
}
```

### 6.4 QualityService（质检服务 SPI）

**文件**：`dms-common/src/main/java/cn/org/openygt/common/service/QualityService.java`

```java
package cn.org.openygt.common.service;

import cn.org.openygt.common.dto.InspectionResult;
import cn.org.openygt.common.enums.InspectionResultType;

/**
 * 质量追溯模块对外服务接口。
 * <p>
 * 定义在 dms-common，由 dms-quality 模块实现。
 * dms-production 通过注入此接口提交质检请求。
 *
 * <p><strong>异常契约</strong>：
 * <ul>
 *   <li>{@code IllegalArgumentException}：taskId 不存在，或 result 非法</li>
 *   <li>{@code IllegalStateException}：任务不在待质检状态</li>
 * </ul>
 *
 * <p><strong>评审修订03</strong>：{@code result} 参数统一使用 {@link InspectionResultType} 枚举，
 * 禁止使用中文字符串。
 */
public interface QualityService {

    /**
     * 对指定任务执行质检。
     *
     * @param taskId     任务 ID
     * @param result     质检结果枚举
     * @param operatorId 操作人 ID
     * @param remark     备注
     * @return 质检结果，包含生成的质检记录 ID 及任务下一状态
     * @throws IllegalArgumentException taskId 不存在或 result 为 {@code null}
     * @throws IllegalStateException    任务不在 {@code WAIT_QC} 状态
     */
    InspectionResult inspect(Long taskId, InspectionResultType result, String operatorId, String remark);

    /**
     * 查询任务最新的质检记录。
     *
     * @param taskId 任务 ID
     * @return 质检结果，找不到返回 {@code null}
     */
    InspectionResult getInspectionByTaskId(Long taskId);
}
```

### 6.5 ProductionQueryService（生产查询 SPI）【新增】

**文件**：`dms-common/src/main/java/cn/org/openygt/common/service/ProductionQueryService.java`

```java
package cn.org.openygt.common.service;

import cn.org.openygt.common.dto.CapacityDailyDTO;
import cn.org.openygt.common.dto.ProdTaskDTO;
import cn.org.openygt.common.dto.TaskStatusHistoryDTO;

import java.time.LocalDate;
import java.util.List;

/**
 * 生产任务只读查询 SPI。
 * <p>
 * <strong>评审修订03</strong>：dms-analytics 禁止直接跨模块 SELECT 其他模块的表，
 * 必须通过此 SPI 获取数据。dms-quality / dms-print 如需反查任务信息也使用此接口。
 *
 * <p>由 dms-production 模块实现，其他模块仅注入调用。
 *
 * <p><strong>异常契约</strong>：
 * <ul>
 *   <li>查询方法：找不到返回 {@code null} 或空列表</li>
 *   <li>参数非法（如日期范围倒置）抛 {@link IllegalArgumentException}</li>
 * </ul>
 */
public interface ProductionQueryService {

    /**
     * 按 ID 查询生产任务。
     *
     * @param taskId 任务 ID
     * @return 任务信息，找不到返回 {@code null}
     */
    ProdTaskDTO getTaskById(Long taskId);

    /**
     * 批量查询生产任务。
     *
     * @param taskIds 任务 ID 列表（非空，且不超过 1000 条）
     * @return 任务信息列表（顺序不保证），不会返回 {@code null}
     * @throws IllegalArgumentException taskIds 为 {@code null} 或为空
     */
    List<ProdTaskDTO> getTasksByIds(List<Long> taskIds);

    /**
     * 日产能统计。
     *
     * @param startDate 开始日期（含）
     * @param endDate   结束日期（含）
     * @return 按日期升序排列的日产能统计列表
     * @throws IllegalArgumentException 日期为 {@code null} 或 startDate 在 endDate 之后
     */
    List<CapacityDailyDTO> getDailyCapacity(LocalDate startDate, LocalDate endDate);

    /**
     * 查询指定任务的状态变更历史。
     *
     * @param taskId 任务 ID
     * @return 状态历史列表，按操作时间升序排列，不会返回 {@code null}
     */
    List<TaskStatusHistoryDTO> getTaskStatusHistory(Long taskId);
}
```

---

## 七、异常类设计

### 7.1 异常策略

`dms-common` 作为公共基础模块，以 JDK 标准异常为主，同时定义少量高频业务异常类供各模块复用：

| 异常类 | 使用场景 | HTTP 映射（由 GlobalExceptionHandler） |
|--------|----------|----------------------------------------|
| `IllegalArgumentException` | 参数非法、枚举值不匹配 | 400 Bad Request |
| `IllegalStateException` | 状态冲突、资源已被占用 | 409 Conflict |
| `UnauthorizedException` | 未登录或 Token 无效 | 401 Unauthorized |
| `ForbiddenException` | 无权限访问 | 403 Forbidden |
| `ResourceNotFoundException` | 资源不存在 | 404 Not Found |
| `Exception`（兜底） | 未预期的系统错误 | 500 Internal Server Error |

### 7.2 GlobalExceptionHandler（全局异常处理器）

**文件**：`dms-common/src/main/java/cn/org/openygt/common/exception/GlobalExceptionHandler.java`

```java
package cn.org.openygt.common.exception;

import cn.org.openygt.common.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器。
 * <p>
 * 捕获 Controller 层抛出的异常，统一包装为 {@link ApiResponse} 返回给客户端。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<?> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        return ApiResponse.error(400, msg);
    }

    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<?> handleBind(BindException e) {
        String msg = e.getAllErrors().get(0).getDefaultMessage();
        return ApiResponse.error(400, msg);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<?> handleIllegalArg(IllegalArgumentException e) {
        return ApiResponse.error(400, e.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiResponse<?> handleIllegalState(IllegalStateException e) {
        return ApiResponse.error(409, e.getMessage());
    }

    @ExceptionHandler(UnauthorizedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResponse<?> handleUnauthorized(UnauthorizedException e) {
        return ApiResponse.error(401, e.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse<?> handleForbidden(ForbiddenException e) {
        return ApiResponse.error(403, e.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<?> handleNotFound(ResourceNotFoundException e) {
        return ApiResponse.error(404, e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<?> handleGeneric(Exception e) {
        return ApiResponse.error(500, "服务器内部错误");
    }
}
```

**现状说明**：当前 `GlobalExceptionHandler.java` 已实现上述全部处理器，**无需修改**。

---

## 八、单元测试策略

### 8.1 测试范围

`dms-common` 无业务逻辑，单元测试重点：
1. DTO 的构造、Getter/Setter、序列化兼容性
2. 枚举值唯一性、标签映射正确性
3. SPI 接口的契约符合性（通过实现模块的集成测试覆盖）

### 8.2 测试目录结构

```
dms-common/src/test/java/cn/org/openygt/common/
├── dto/
│   ├── ApiResponseTest.java
│   ├── EqDeviceDTOTest.java
│   ├── InspectionResultTest.java
│   └── LoginRequestValidationTest.java
├── enums/
│   ├── DeviceStatusTest.java
│   ├── DeviceTypeTest.java
│   ├── InspectionResultTypeTest.java
│   └── TaskStatusTest.java
└── entity/
    ├── BaseEntityTest.java
    └── BaseAuditEntityTest.java
```

### 8.3 关键测试用例

#### TC-01: BaseEntity 字段类型测试

```java
@Test
public void testBaseEntityFieldTypes() {
    BaseEntity entity = new BaseEntity() {}; // 匿名子类
    entity.setCreatedAt(LocalDateTime.now());
    entity.setUpdatedAt(LocalDateTime.now());
    assertTrue(entity.getCreatedAt() instanceof LocalDateTime);
}
```

#### TC-02: 枚举值唯一性与标签映射

```java
@Test
public void testInspectionResultTypeLabels() {
    assertEquals("通过", InspectionResultType.PASS.getLabel());
    assertEquals("让步放行", InspectionResultType.CONCESSION.getLabel());
    assertEquals("返工", InspectionResultType.REWORK.getLabel());
    assertEquals("报废", InspectionResultType.SCRAP.getLabel());
}
```

#### TC-03: LoginRequest 参数校验

```java
@Test
public void testLoginRequestValidation() {
    LoginRequest req = new LoginRequest();
    req.setUsername(""); // @NotBlank 应触发
    req.setPassword("secret");
    // 使用 Validator 验证，期望约束 violations
}
```

#### TC-04: DTO 序列化兼容性

```java
@Test
public void testEqDeviceDTOSerialization() throws Exception {
    EqDeviceDTO dto = new EqDeviceDTO();
    dto.setId(1L);
    dto.setDeviceCode("EQ-001");
    
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    new ObjectOutputStream(baos).writeObject(dto);
    
    ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
    EqDeviceDTO restored = (EqDeviceDTO) ois.readObject();
    assertEquals("EQ-001", restored.getDeviceCode());
}
```

### 8.4 Mock 策略

- `dms-common` 内部无需要 Mock 的依赖。
- SPI 接口的实现测试由各自实现模块负责（如 `EquipmentServiceImplTest` 在 `dms-equipment` 中）。
- 调用方的 SPI 依赖在单元测试中使用 Mockito 创建 Mock 实例。

---

## 九、迁移路径（现状 → 目标，文件级修改清单）

### 9.1 修改现有文件（4 个）

#### 文件 1：`dms-common/src/main/java/cn/org/openygt/common/entity/BaseEntity.java`

| 行号范围 | 修改内容 |
|----------|----------|
| 9 | `import java.time.LocalDateTime;` → `import java.time.LocalDateTime;` |
| 20-22 | `private Date createdAt;` / `private Date updatedAt;` → `private LocalDateTime createdAt;` / `private LocalDateTime updatedAt;` |
| 12 | 增加类级 Javadoc（可选，建议同步） |

#### 文件 2：`dms-common/src/main/java/cn/org/openygt/common/dto/InspectionResult.java`

| 行号范围 | 修改内容 |
|----------|----------|
| 5 | `import java.time.LocalDateTime;` → `import java.time.LocalDateTime;` |
| 10-11 | `public class InspectionResult {` → `public class InspectionResult implements Serializable {` |
| 12 | 新增 `private static final long serialVersionUID = 1L;` |
| 19 | `private Date inspectedAt;` → `private LocalDateTime inspectedAt;` |

#### 文件 3：`dms-common/src/main/java/cn/org/openygt/common/service/EquipmentService.java`

| 行号范围 | 修改内容 |
|----------|----------|
| 3（新增） | `import cn.org.openygt.common.dto.EqDeviceDTO;` |
| 12-16 | 返回值 `Object` → `EqDeviceDTO`（`getDeviceByCode`、`getDeviceById`、`getOrCreateDevice`） |
| 26-37（新增） | 在 `releaseDevice` 之后、`checkTemperatureAlarm` 之前插入 `reserveDevice` 方法 |

#### 文件 4：`dms-common/src/main/java/cn/org/openygt/common/service/PrintService.java`

| 行号范围 | 修改内容 |
|----------|----------|
| 3（新增） | `import cn.org.openygt.common.dto.PrintTaskDTO;` |
| 3（新增） | `import java.util.List;` |
| 14 | `Object getPrintQueue();` → `List<PrintTaskDTO> getPrintQueue();` |

#### 文件 5：`dms-common/src/main/java/cn/org/openygt/common/service/QualityService.java`

| 行号范围 | 修改内容 |
|----------|----------|
| 3（新增） | `import cn.org.openygt.common.enums.InspectionResultType;` |
| 21 | 参数 `String result` → `InspectionResultType result` |

### 9.2 新增文件（12 个）

| 序号 | 文件路径 | 说明 |
|------|----------|------|
| 1 | `dms-common/.../entity/BaseAuditEntity.java` | 审计基类（无逻辑删除） |
| 2 | `dms-common/.../enums/InspectionResultType.java` | 质检结果类型枚举 |
| 3 | `dms-common/.../dto/EqDeviceDTO.java` | 设备传输 DTO |
| 4 | `dms-common/.../dto/PrintTaskDTO.java` | 打印任务传输 DTO |
| 5 | `dms-common/.../dto/ProdTaskDTO.java` | 生产任务查询 DTO |
| 6 | `dms-common/.../dto/CapacityDailyDTO.java` | 日产能统计 DTO |
| 7 | `dms-common/.../dto/TaskStatusHistoryDTO.java` | 状态历史 DTO |
| 8 | `dms-common/.../dto/LoginRequest.java` | JWT 登录请求 DTO |
| 9 | `dms-common/.../dto/TokenResponse.java` | JWT 登录响应 DTO |
| 10 | `dms-common/.../service/ProductionQueryService.java` | 生产查询 SPI |
| 11-12 | 测试文件若干 | 见 8.2 节测试目录 |

### 9.3 下游模块联动修改清单

修改 `dms-common` 后，必须同步修改以下模块，否则编译失败：

| 模块 | 修改文件 | 修改内容 |
|------|----------|----------|
| dms-equipment | `EquipmentServiceImpl.java` | 返回值 `EqDevice` → `EqDeviceDTO`（需新增装配逻辑）；实现 `reserveDevice` 方法 |
| dms-print | `PrintServiceImpl.java` | `getPrintQueue()` 返回 `List<PrintTaskDTO>`（需新增装配逻辑）；移除对 `dms-production` 实体类的直接依赖 |
| dms-quality | `QualityServiceImpl.java` | `inspect` 方法参数改为 `InspectionResultType`，switch-case 改用枚举名 |
| dms-production | 新增 `ProductionQueryServiceImpl.java` | 实现 `ProductionQueryService` 接口 |
| dms-production | 调用 `QualityService.inspect` 处 | 传入 `InspectionResultType` 枚举而非中文字符串 |
| dms-app | `MybatisPlusConfig.java`（如存在） | 确保自动填充处理器支持 `LocalDateTime` 类型 |

### 9.4 编译验证命令

```bash
# 1. 先编译公共模块
cd /data2/docker/decoction/openygt-dms/dms-common
mvn clean compile

# 2. 再全量编译（确认下游模块无编译错误）
cd /data2/docker/decoction/openygt-dms
mvn clean compile
```

### 9.5 迁移顺序建议

```
Step 1: dms-common 内新增文件（BaseAuditEntity、枚举、DTO、SPI）
Step 2: 修改 dms-common 现有文件（BaseEntity、EquipmentService、PrintService、QualityService、InspectionResult）
Step 3: mvn clean compile（dms-common 单独编译通过）
Step 4: dms-equipment 适配 EquipmentService 新签名 + 实现 reserveDevice
Step 5: dms-quality 适配 QualityService 新签名（InspectionResultType）
Step 6: dms-print 适配 PrintService 新签名 + 解耦 dms-production 实体
Step 7: dms-production 实现 ProductionQueryService
Step 8: mvn clean compile（全量编译通过）
```

---

## 附录：验收检查清单

- [ ] `BaseEntity.createdAt/updatedAt` 类型为 `LocalDateTime`
- [ ] `BaseAuditEntity` 已创建，无 `@TableLogic` 和 `deleted` 字段
- [ ] `InspectionResultType` 枚举包含 PASS/CONCESSION/REWORK/SCRAP
- [ ] `EquipmentService` 所有查询方法返回 `EqDeviceDTO`
- [ ] `EquipmentService` 包含 `reserveDevice(Long, Long)` 方法
- [ ] `PrintService.getPrintQueue()` 返回 `List<PrintTaskDTO>`
- [ ] `QualityService.inspect()` 参数为 `InspectionResultType`
- [ ] `ProductionQueryService` 已定义并包含 4 个方法
- [ ] 所有新增 DTO 实现 `Serializable`
- [ ] `mvn clean compile` 全量通过
