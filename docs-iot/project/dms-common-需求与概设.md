# dms-common 需求与概要设计

> 模块类型：公共基础模块（无独立部署单元）  
> 包名：`cn.org.openygt.common`  
> 职责：提供跨模块共享的实体基类、枚举、DTO、异常处理、以及模块间 SPI 接口定义。  
> 版本：V1.3（专家评审03后修订版）

---

## 一、模块定位

`dms-common` 是**所有业务模块的公共依赖**，本身不包含业务逻辑，不提供 REST API，不操作数据库。其核心职责：

1. **实体基类**：定义所有业务表的通用字段（`id`, `tenant_id`, `created_at`, `updated_at`, `deleted`）
2. **业务枚举**：定义全局通用的枚举值（设备状态、设备类型、任务状态等）
3. **数据传输对象**：定义跨模块传递的 DTO（如质检结果）
4. **异常与统一响应**：全局异常处理器、统一 API 响应包装类
5. **SPI 接口定义**：定义 `EquipmentService`、`PrintService`、`QualityService` 三个模块间契约接口

---

## 二、需求清单

| 需求编号 | 需求名称 | 优先级 | 状态 | 说明 |
|---------|---------|--------|------|------|
| COM-001 | 实体基类 BaseEntity | P0 | ✅ | 所有业务表继承 |
| COM-002 | 统一响应 ApiResponse | P0 | ✅ | REST API 标准响应包装 |
| COM-003 | 全局异常处理器 | P0 | ✅ | `@RestControllerAdvice` |
| COM-004 | 任务状态枚举 TaskStatus | P0 | ✅ | 12 步 + 2 个终态 |
| COM-005 | 设备状态枚举 DeviceStatus | P0 | ✅ | idle/running/fault/offline/maintenance |
| COM-006 | 设备类型枚举 DeviceType | P0 | ✅ | 煎药机/包装机/打印机 |
| COM-007 | 质检结果 DTO InspectionResult | P0 | ✅ | 跨模块传递质检结果 |
| COM-008 | EquipmentService SPI | P0 | ✅ | 设备模块对外服务契约 |
| COM-009 | PrintService SPI | P0 | ✅ | 打印模块对外服务契约 |
| COM-010 | QualityService SPI | P0 | ✅ | 质检模块对外服务契约 |
| COM-011 | 新增设备自动化级别枚举 | P1 | ⏳ | manual / semi / auto |
| COM-012 | 新增打印状态枚举 | P1 | ⏳ | PENDING / PRINTING / SUCCESS / FAILED |
| COM-013 | **EquipmentService返回值收紧为EqDeviceDTO** | **P0** | ⏳ | 替代Object，明确类型 |
| COM-014 | **新增设备预留接口reserveDevice** | **P0** | ⏳ | 返工场景防设备抢占 |
| COM-015 | **JWT认证DTO（TokenResponse/LoginRequest）** | **P0** | ⏳ | 医疗系统合规要求 |
| COM-016 | **ProductionQueryService SPI** | **P0** | ⏳ | 只读查询SPI，供analytics/quality/print反查 |
| COM-017 | **ProdTaskDTO** | **P0** | ⏳ | ProductionQueryService返回对象 |
| COM-018 | **CapacityDailyDTO / TaskStatusHistoryDTO** | **P0** | ⏳ | 统计查询传输对象 |

---

## 三、核心设计

### 3.1 BaseEntity（实体基类 — 含逻辑删除）

```java
package cn.org.openygt.common.entity;

@Data
public abstract class BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("tenant_id")
    private String tenantId = "default";

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted = 0;
}
```

**约束**：
- 业务 Entity（需要逻辑删除的表）继承 `BaseEntity`
- `id` 使用数据库自增（SQLite：`AUTOINCREMENT`）
- `deleted` 为逻辑删除标记，`0=正常`, `1=已删除`
- `createdAt` / `updatedAt` 由 `dms-app` 的 MyBatis-Plus 自动填充配置维护，类型为 `LocalDateTime`

### 3.1a BaseAuditEntity（审计实体基类 — 不含逻辑删除）【新增】

> **评审修订03**：审计/遥测表（`sys_log`、`eq_temperature_log`、`prod_task_status_history`）不应逻辑删除，物理保留符合GMP审计要求。

```java
package cn.org.openygt.common.entity;

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

**适用表**：
- `sys_log`（操作日志）
- `eq_temperature_log`（温度遥测）
- `eq_temperature_log_archive`（温度归档）
- `prod_task_status_history`（状态变更历史）

### 3.2 业务枚举

#### InspectionResultType（质检结果类型）【新增】

> **评审修订03**：质检结果原用 "通过"/"让步放行"/"返工"/"报废" 中文字符串做 switch-case，容易因空格、编码导致匹配失败。

```java
public enum InspectionResultType {
    PASS("通过"),
    CONCESSION("让步放行"),
    REWORK("返工"),
    SCRAP("报废");

    private final String label;
    InspectionResultType(String label) { this.label = label; }
    public String getLabel() { return label; }
}
```

**使用规则**：
- SPI 接口参数、数据库存储、switch-case 统一使用枚举名（`PASS`, `CONCESSION`, `REWORK`, `SCRAP`）
- 展示层使用 `getLabel()` 获取中文标签

#### TaskStatus（生产任务状态）

```java
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
}
```

> **重要**：此枚举是全局状态机的唯一合法状态集合，任何模块不得自行定义额外状态字符串。

#### DeviceStatus（设备状态）

```java
public enum DeviceStatus {
    IDLE("空闲"),
    RUNNING("运行中"),
    FAULT("故障"),
    OFFLINE("离线"),
    MAINTENANCE("维护中");
}
```

> 数据库存储使用枚举名（如 `IDLE`），但 `EquipmentService` 的部分历史方法使用小写字符串（`"idle"`, `"running"`），新开发建议统一使用枚举名。

#### DeviceType（设备类型）

```java
public enum DeviceType {
    COOKING_MACHINE(1, "煎药机"),
    PACKING_MACHINE(2, "包装机"),
    PRINTER(4, "打印机");
}
```

### 3.3 ApiResponse（统一响应）

```java
@Data
public class ApiResponse<T> {
    private int code;       // HTTP 状态码或业务码，200 表示成功
    private String message; // 提示信息
    private T data;         // 业务数据

    public static <T> ApiResponse<T> ok(T data) { ... }
    public static <T> ApiResponse<T> error(int code, String message) { ... }
}
```

**使用规范**：
- 所有 Controller 的返回值必须是 `ApiResponse<T>` 或其子类
- 成功时 `code = 200`，失败时 `code ≠ 200`
- `data` 为 `null` 时也必须返回完整 `ApiResponse` 对象，不要直接返回 `null`

### 3.4 GlobalExceptionHandler（全局异常处理）

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResponse<Void> handleIllegalArgument(...) { ... }

    @ExceptionHandler(IllegalStateException.class)
    public ApiResponse<Void> handleIllegalState(...) { ... }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(...) { ... }
}
```

### 3.5 SPI 接口定义

#### EquipmentService

> **评审修订03**：补充异常契约 Javadoc；返回值已从 `Object` 收紧为 `EqDeviceDTO`。

```java
public interface EquipmentService {

    /**
     * 按编码查询设备。
     * @return 设备信息，找不到返回 null
     */
    EqDeviceDTO getDeviceByCode(String deviceCode);

    /**
     * 按 ID 查询设备。
     * @return 设备信息，找不到返回 null
     */
    EqDeviceDTO getDeviceById(Long deviceId);

    /**
     * 自动创建设备（首次扫码）。
     * @return 创建后的设备信息
     */
    EqDeviceDTO getOrCreateDevice(String deviceCode, int defaultType);

    /**
     * 更新设备状态。
     * @throws IllegalArgumentException 设备不存在
     * @throws IllegalStateException 状态转换非法（如 RUNNING→IDLE 需先释放）
     */
    void updateDeviceStatus(Long deviceId, String status);

    /**
     * 更新设备实时温度。
     * @throws IllegalArgumentException 设备不存在
     */
    void updateTemperature(Long deviceId, BigDecimal temperature);

    /**
     * 上报设备故障。
     * @throws IllegalArgumentException 设备不存在
     */
    void reportFault(Long deviceId, String faultCode, String message);

    /**
     * 清除故障。
     * @throws IllegalArgumentException 设备不存在
     */
    void clearFault(Long deviceId);

    /**
     * 释放设备（任务结束，状态置 idle）。
     * @throws IllegalArgumentException 设备不存在
     * @throws IllegalStateException 设备非预留/运行中状态
     */
    void releaseDevice(Long deviceId);

    /**
     * 预留设备（返工场景，防止被其他任务抢占）。
     * @param taskId 预留的任务 ID
     * @param deviceId 设备 ID
     * @throws IllegalArgumentException 设备不存在
     * @throws IllegalStateException 设备已被其他任务占用或预留
     */
    void reserveDevice(Long taskId, Long deviceId);

    /**
     * 检查温度是否超阈值并产生告警。
     * @throws IllegalArgumentException 设备不存在
     */
    void checkTemperatureAlarm(Long deviceId, BigDecimal temperature);

    String getDeviceStatus(Long deviceId);
    String getAutoLevel(Long deviceId);
    String getDeviceCode(Long deviceId);
    Long getDeviceId(String deviceCode);
}
```

> **异常契约规则**：
> - 查询方法：找不到返回 `null`（后续可演进为 `Optional<T>`）
> - 操作方法：前置条件不满足时抛 `IllegalStateException`，参数非法时抛 `IllegalArgumentException`

#### PrintService

> **评审修订03**：`getPrintQueue()` 返回值从 `Object` 收紧为 `List<PrintTaskDTO>`。

```java
public interface PrintService {

    /**
     * 提交打印任务。
     * @throws IllegalArgumentException taskId 不存在
     * @throws IllegalStateException 该任务已有未完成的打印记录
     */
    void submitPrintTask(Long taskId, String deviceCode, String operatorId);

    /**
     * 重试打印。
     * @throws IllegalArgumentException taskId 不存在
     * @throws IllegalStateException 重试次数已达上限
     */
    void retryPrint(Long taskId, String deviceCode, String operatorId);

    /**
     * 获取待打印队列。
     * @return 待打印任务列表（状态为 PENDING 或 FAILED 且未超限）
     */
    List<PrintTaskDTO> getPrintQueue();
}
```

#### QualityService

```java
public interface QualityService {

    /**
     * 执行质检。
     * @param result 质检结果枚举名：PASS / CONCESSION / REWORK / SCRAP
     * @throws IllegalArgumentException taskId 不存在 或 result 非法
     * @throws IllegalStateException 任务不在待质检状态
     */
    InspectionResult inspect(Long taskId, InspectionResultType result, String operatorId, String remark);

    /**
     * 查询任务最新质检记录。
     * @return 质检结果，找不到返回 null
     */
    InspectionResult getInspectionByTaskId(Long taskId);
}
```

> **评审修订03**：`result` 参数统一使用 `InspectionResultType` 枚举名（`PASS`, `CONCESSION`, `REWORK`, `SCRAP`），禁止使用中文字符串。

### 3.6 InspectionResult（质检结果 DTO）

```java
@Data
public class InspectionResult {
    private Long inspectionId;      // 质检记录 ID
    private Long taskId;            // 任务 ID
    private String result;          // 通过/让步放行/返工/报废
    private String nextStatus;      // 质检后任务的目标状态
    private String operatorId;      // 操作人
    private String remark;          // 备注
    private LocalDateTime inspectedAt; // 质检时间
    private Integer isException;    // 是否异常（0=正常, 1=异常）
    private String exceptionReason; // 异常原因
}
```

### 3.7 EqDeviceDTO（设备传输 DTO）【新增】

```java
@Data
public class EqDeviceDTO implements Serializable {
    private Long id;
    private String deviceCode;
    private String name;
    private String deviceType;
    private String status;
    private BigDecimal currentTemp;
    private String faultCode;
    private String autoLevel;
    private String location;
    private String protocolType;
    private String vendor;
    private BigDecimal alarmHighTemp;
    private BigDecimal alarmLowTemp;
    private LocalDateTime lastHeartbeat;
}
```

> **用途**：`EquipmentService` 所有查询方法的统一返回类型，替代宽松的 `Object`。

### 3.8 JWT 认证 DTO（新增）

```java
@Data
public class LoginRequest {
    @NotBlank private String username;
    @NotBlank private String password;
}

@Data
public class TokenResponse {
    private String accessToken;
    private String tokenType = "Bearer";
    private Long expiresIn;
    private String refreshToken;
}
```

> **合规要求**：医疗系统无认证违反《个保法》《GSP》，认证必须从阶段二提升为P0。

### 3.9 PrintTaskDTO（打印任务传输对象）【新增】

> **评审修订03**：替代 `PrintService.getPrintQueue()` 原 `Object` 返回类型。

```java
@Data
public class PrintTaskDTO implements Serializable {
    private Long id;
    private Long taskId;
    private String deviceCode;
    private String operatorId;
    private String status;        // PENDING / PRINTING / SUCCESS / FAILED / CANCELLED
    private Integer copies;
    private Integer retryCount;
    private Integer maxRetry;
    private LocalDateTime createdAt;
}
```

### 3.10 ProdTaskDTO（生产任务查询对象）【新增】

> **评审修订03**：为 `ProductionQueryService` SPI 提供只读数据传输。

```java
@Data
public class ProdTaskDTO implements Serializable {
    private Long id;
    private Long prescriptionId;
    private String status;
    private Long decoctDeviceId;
    private Long packageDeviceId;
    private String operatorId;
    private Integer isException;
    private String exceptionReason;
    private LocalDateTime handoverTime;
    private LocalDateTime completeTime;
    private LocalDateTime createdAt;
}
```

### 3.11 CapacityDailyDTO / TaskStatusHistoryDTO（统计对象）【新增】

```java
@Data
public class CapacityDailyDTO implements Serializable {
    private LocalDate statDate;
    private String tenantId;
    private Long totalTasks;
    private Long completedTasks;
    private Long scrappedTasks;
}

@Data
public class TaskStatusHistoryDTO implements Serializable {
    private Long id;
    private Long taskId;
    private String fromStatus;
    private String toStatus;
    private String operatorId;
    private LocalDateTime operateTime;
    private String remark;
}
```

### 3.12 ProductionQueryService（生产查询 SPI）【新增】

> **评审修订03**：dms-analytics 禁止直接跨模块 SELECT 其他模块的表，必须通过此 SPI 获取数据。dms-quality/dms-print 如需反查任务信息也使用此接口。

```java
public interface ProductionQueryService {

    /**
     * 按 ID 查询生产任务。
     * @return 任务信息，找不到返回 null
     */
    ProdTaskDTO getTaskById(Long taskId);

    /**
     * 批量查询生产任务。
     */
    List<ProdTaskDTO> getTasksByIds(List<Long> taskIds);

    /**
     * 日产能统计。
     */
    List<CapacityDailyDTO> getDailyCapacity(LocalDate startDate, LocalDate endDate);

    /**
     * 查询任务状态历史。
     */
    List<TaskStatusHistoryDTO> getTaskStatusHistory(Long taskId);
}
```

---

## 四、Maven 配置

```xml
<artifactId>dms-common</artifactId>
<dependencies>
    <!-- MyBatis-Plus -->
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>mybatis-plus-boot-starter</artifactId>
    </dependency>
    <!-- Lombok -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
    </dependency>
</dependencies>
```

> `dms-common` **禁止**依赖 `spring-boot-starter-web` 以外的 Spring Boot 模块，尤其禁止依赖数据库驱动、MQTT 客户端等业务相关库。

---

## 五、开发规范

1. **禁止在 dms-common 中写业务逻辑**：只放定义（接口、枚举、基类、DTO），不放实现。
2. **SPI 接口变更需架构师审批**：`EquipmentService`、`PrintService`、`QualityService` 的任何方法签名变更都会影响多个模块，必须同步更新所有实现方和调用方。
3. **枚举值一旦发布不得删除**：可在末尾追加新值，但禁止修改或删除已有值（数据库中已存储）。
4. **DTO 必须可序列化**：所有 DTO 需实现 `Serializable`（虽然当前单 JVM 不严格需要，但为将来微服务拆分预留）。

---

## 六、集成检查清单

提交变更到 `dms-common` 前，检查：

- [ ] 新增/修改的 SPI 接口是否有对应的实现模块同步更新？
- [ ] 枚举变更是否影响数据库现有数据？
- [ ] 是否引入了不该有的业务依赖（如数据库驱动、MQTT 客户端）？
- [ ] 是否保持 Java 1.8 兼容？
- [ ] `mvn clean compile` 是否通过？
