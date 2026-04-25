# dms-masterdata 需求与概要设计

> 模块类型：业务模块  
> 包名：`cn.org.openygt.masterdata`  
> 表前缀：`md_`  
> API 前缀：`/api/v1/md`  
> 职责：管理煎药室运营所需的基础主数据，包括医院/客户信息、煎药方案参数、**药品追溯编码**。  
> 版本：V1.4（专家评审04后修订版）

---

## 一、模块定位

`dms-masterdata` 是系统的**主数据管理中心**，为其他业务模块提供基础数据支撑：

- `dms-production` 创建处方时，需关联 `md_hospital` 和 `md_decoct_scheme`
- `md_decoct_scheme` 定义了不同药材的煎煮参数（压力、水量、时间等），直接影响煎药机的运行指令

**模块边界**：
- ✅ 负责 `md_` 前缀表的 CRUD
- ❌ 不参与生产状态机
- ❌ 不直接操作设备
- ❌ 不依赖其他业务模块（仅依赖 `dms-common`）

---

## 二、需求清单

### 2.1 已实现需求

| 需求编号 | 需求名称 | 优先级 | 状态 | 验收标准 |
|---------|---------|--------|------|---------|
| MD-001 | 医院信息 CRUD | P0 | ✅ | 增删改查、按名称搜索、编码唯一 |

### 2.2 待开发需求

| 需求编号 | 需求名称 | 优先级 | 状态 | 验收标准 |
|---------|---------|--------|------|---------|
| MD-002 | 煎药方案 CRUD | P1 | ⏳ | 增删改查、6 种内置方案初始化、参数校验 |
| MD-003 | 煎药方案与设备关联 | P2 | ⏳ | 方案可被设备引用、修改时校验是否被使用中 |
| MD-004 | 医院批量导入导出 | P2 | ⏳ | Excel 导入导出、模板下载 |

---

## 三、数据库设计

### 3.1 md_hospital（医院/客户表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | INTEGER | PK, AUTOINCREMENT | 主键 |
| tenant_id | VARCHAR(32) | DEFAULT 'default' | 多租户预留 |
| name | VARCHAR(100) | NOT NULL | 医院名称 |
| code | VARCHAR(50) | UNIQUE | 医院编码（内部使用） |
| contact | VARCHAR(50) | | 联系人 |
| phone | VARCHAR(20) | | 联系电话 |
| address | VARCHAR(200) | | 地址 |
| created_at | DATETIME | | 创建时间 |
| updated_at | DATETIME | | 更新时间 |
| deleted | INTEGER | DEFAULT 0 | 逻辑删除 |

**索引**：`UNIQUE(code)`

### 3.2 md_decoct_scheme（煎药方案表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | INTEGER | PK, AUTOINCREMENT | 主键 |
| tenant_id | VARCHAR(32) | DEFAULT 'default' | 多租户预留 |
| name | VARCHAR(100) | NOT NULL | 方案名称 |
| code | VARCHAR(50) | UNIQUE | 方案编码 |
| **trace_code** | **VARCHAR(100)** | | **药品追溯编码（对接国家药品追溯系统）** |
| pressure_mode | VARCHAR(20) | | 压力模式：常压 / 微压密闭 |
| water_ratio | VARCHAR(20) | | 加水比例（如 1:10） |
| soak_time | INTEGER | | 泡药时间（分钟） |
| heat_time | INTEGER | | 加热时间（分钟） |
| decoct_count | INTEGER | DEFAULT 1 | 煎煮次数 |
| pre_decoct | INTEGER | DEFAULT 0 | 是否先煎（0/1） |
| post_decoct | INTEGER | DEFAULT 0 | 是否后下（0/1） |
| pre_decoct_time | INTEGER | | 先煎时间（分钟） |
| post_decoct_time | INTEGER | | 后下时间（分钟） |
| **alarm_high_temp** | **DECIMAL(5,2)** | | **方案级高温告警阈值（覆盖设备级）** |
| **alarm_low_temp** | **DECIMAL(5,2)** | | **方案级低温告警阈值（覆盖设备级）** |
| description | TEXT | | 说明 |
| created_at | DATETIME | | 创建时间 |
| updated_at | DATETIME | | 更新时间 |
| deleted | INTEGER | DEFAULT 0 | 逻辑删除 |

**索引**：`UNIQUE(code)`

**内置方案初始化**（应用启动时若表为空则自动插入）：

| 方案名称 | 压力模式 | 特点 | alarm_high_temp | alarm_low_temp |
|---------|---------|------|-----------------|----------------|
| 常压汤药 | 常压 | 普通汤剂 | 120 | 50 |
| 常压补药 | 常压 | 补药长时煎煮 | 120 | 50 |
| 微压密闭 | 微压密闭 | 密闭煎煮 | 130 | 60 |
| 先煎 | 常压 | 部分药材先煎 | 120 | 50 |
| 后下 | 常压 | 部分药材后下 | 120 | 50 |
| 先煎后下 | 常压 | 同时具备先煎和后下 | 120 | 50 |

> **评审修订**：`md_decoct_scheme` 新增 `trace_code`（药品追溯编码，对接国家药品追溯系统）和 `alarm_high_temp`/`alarm_low_temp`（方案级温度阈值，与设备级、系统级形成三层继承）。

**温度阈值三层继承规则**（EQ-016）：

```
系统默认（sys_config）
    ↓ 被覆盖
设备级（eq_device.alarm_high_temp / alarm_low_temp）
    ↓ 被覆盖
方案级（md_decoct_scheme.alarm_high_temp / alarm_low_temp）
    
优先级：方案级 > 设备级 > 系统默认
```

---

## 四、接口设计

### 4.1 REST API

#### 医院管理

| 方法 | 路径 | 说明 | 请求/响应 |
|------|------|------|----------|
| POST | `/api/v1/md/hospitals` | 创建医院 | `HospitalCreateRequest` → `ApiResponse<Hospital>` |
| GET | `/api/v1/md/hospitals/{id}` | 查询医院详情 | path id → `ApiResponse<Hospital>` |
| PUT | `/api/v1/md/hospitals/{id}` | 更新医院 | `HospitalUpdateRequest` → `ApiResponse<Hospital>` |
| DELETE | `/api/v1/md/hospitals/{id}` | 删除医院 | path id → `ApiResponse<Void>` |
| GET | `/api/v1/md/hospitals` | 分页查询 | query: name(模糊), page, size → `ApiResponse<IPage<Hospital>>` |

#### 煎药方案管理

| 方法 | 路径 | 说明 | 请求/响应 |
|------|------|------|----------|
| POST | `/api/v1/md/schemes` | 创建方案 | `SchemeCreateRequest` → `ApiResponse<DecoctScheme>` |
| GET | `/api/v1/md/schemes/{id}` | 查询方案详情 | path id → `ApiResponse<DecoctScheme>` |
| PUT | `/api/v1/md/schemes/{id}` | 更新方案 | `SchemeUpdateRequest` → `ApiResponse<DecoctScheme>` |
| DELETE | `/api/v1/md/schemes/{id}` | 删除方案 | path id → `ApiResponse<Void>` |
| GET | `/api/v1/md/schemes` | 分页查询 | query: name(模糊), page, size → `ApiResponse<IPage<DecoctScheme>>` |

### 4.2 请求/响应 DTO

```java
// HospitalCreateRequest
@Data
public class HospitalCreateRequest {
    @NotBlank private String name;
    private String code;
    private String contact;
    private String phone;
    private String address;
}

// SchemeCreateRequest
@Data
public class SchemeCreateRequest {
    @NotBlank private String name;
    private String code;
    private String traceCode;           // 药品追溯编码（对接国家药品追溯系统）
    private String pressureMode;
    private String waterRatio;
    private Integer soakTime;
    private Integer heatTime;
    private Integer decoctCount;
    private Integer preDecoct;
    private Integer postDecoct;
    private Integer preDecoctTime;
    private Integer postDecoctTime;
    private BigDecimal alarmHighTemp;   // 方案级高温告警阈值（覆盖设备级）
    private BigDecimal alarmLowTemp;    // 方案级低温告警阈值（覆盖设备级）
    private String description;
}
```

---

## 五、核心类结构

```
cn.org.openygt.masterdata
├── controller
│   ├── HospitalController.java
│   └── DecoctSchemeController.java
├── entity
│   ├── Hospital.java
│   └── DecoctScheme.java
├── mapper
│   ├── HospitalMapper.java
│   └── DecoctSchemeMapper.java
├── service
│   ├── HospitalService.java
│   ├── HospitalServiceImpl.java
│   ├── DecoctSchemeService.java
│   └── DecoctSchemeServiceImpl.java
└── dto
    ├── HospitalCreateRequest.java
    ├── HospitalUpdateRequest.java
    ├── SchemeCreateRequest.java
    └── SchemeUpdateRequest.java
```

---

## 六、开发规范

1. **仅操作 `md_` 前缀表**，禁止访问其他模块表。
2. **编码唯一性**：`code` 字段全局唯一，创建和更新时需校验重复。
3. **内置方案保护**：6 种内置方案的 `code` 以 `DEFAULT_` 开头，禁止删除，允许修改参数。
4. **事务**：单表操作无需显式 `@Transactional`，多表操作需加注解。
5. **删除**：必须使用逻辑删除（`BaseEntity` 已集成），禁止物理删除。

---

## 七、集成检查清单

- [ ] `Hospital` 和 `DecoctScheme` 实体继承 `BaseEntity`
- [ ] `code` 字段有唯一性校验
- [ ] 内置方案初始化逻辑放在 `CommandLineRunner` 或 `ApplicationRunner` 中
- [ ] 所有 API 返回 `ApiResponse<T>`
- [ ] `mvn test` 单测通过
