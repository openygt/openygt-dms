# kimi04 概要设计 - 煎药方案 + 系统配置 + 操作日志

> 负责人: kimi04
> 对应需求: MD-002, SYS-002, SYS-003
> 日期: 2026-04-25

---

## 1. 需求范围

### 1.1 煎药方案管理 (MD-002)
- 煎药方案 CRUD：名称、类型、煎药次数、压力、加水量、加热时间等
- 与处方关联：`prod_prescription.scheme_id` 外键已存在
- API 前缀: `/api/v1/md/schemes`

### 1.2 系统配置管理 (SYS-002)
- 系统配置项 CRUD：key-value 形式，支持按 key 查询
- 缓存策略：读多写少，需内存缓存避免频繁查库
- API 前缀: `/api/v1/sys/configs`

### 1.3 操作日志 (SYS-003)
- 操作日志记录：用户、动作、模块、详情、IP
- 查询支持：按模块、用户、时间范围过滤
- 对外服务：提供 `SysLogService.log()` 供其他模块调用
- API 前缀: `/api/v1/sys/logs`

---

## 2. 模块设计

### 2.1 新增/修改的包和类

```
dms-masterdata/
├── entity/
│   └── DecoctScheme.java          # 煎药方案实体
├── mapper/
│   └── DecoctSchemeMapper.java    # MyBatis-Plus BaseMapper
├── service/
│   ├── DecoctSchemeService.java
│   └── impl/
│       └── DecoctSchemeServiceImpl.java
└── controller/
    └── DecoctSchemeController.java  # /api/v1/md/schemes

dms-system/
├── entity/
│   ├── SysConfig.java             # 系统配置实体
│   └── SysLog.java                # 操作日志实体
├── mapper/
│   ├── SysConfigMapper.java
│   └── SysLogMapper.java
├── service/
│   ├── SysConfigService.java
│   ├── SysLogService.java
│   └── impl/
│       ├── SysConfigServiceImpl.java
│       └── SysLogServiceImpl.java
├── controller/
│   ├── SysConfigController.java   # /api/v1/sys/configs
│   └── SysLogController.java      # /api/v1/sys/logs
└── aspect/
    └── OperationLogAspect.java    # AOP 自动记录 Controller 操作
```

### 2.2 与现有代码的关系

- **复用现有模式**：完全遵循 Hospital / SysUser 的代码风格（BaseEntity、构造器注入、ApiResponse 封装）
- **无破坏性变更**：仅新增表对应的 Entity/Mapper/Service/Controller，不改现有代码
- **包名对齐**：按 openygt-dms 规范使用 `cn.org.openygt.*` 包名（注：当前 decoction 项目为 `com.decoction`，需在迁移时统一）

---

## 3. 接口设计

### 3.1 煎药方案 `/api/v1/md/schemes`

| 方法 | 路径 | 说明 | 请求体 |
|------|------|------|--------|
| POST | / | 创建方案 | `{"name":"常压汤药","schemeType":0,"decoctTimes":0,"pressure":1,"upperWater":0.0,"heatingTime":30,"preHeatingTime":null,"postHeatingTime":null,"description":"常规"}` |
| PUT | /{id} | 更新方案 | 同上 + id |
| GET | /{id} | 查询详情 | - |
| GET | / | 列表（分页） | `?page=1&size=20` |
| DELETE | /{id} | 删除 | - |

### 3.2 系统配置 `/api/v1/sys/configs`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | / | 创建配置项 |
| PUT | /{id} | 更新配置项 |
| GET | /{id} | 按ID查询 |
| GET | /key/{configKey} | **按key查询（支持缓存）** |
| GET | / | 列表（分页+keyword） |
| DELETE | /{id} | 删除 |

**配置项示例**：
```json
{"configKey":"soak.default_duration","configValue":"30","description":"默认泡药时长(分钟)"}
```

### 3.3 操作日志 `/api/v1/sys/logs`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | / | 查询列表（分页） |
| GET | / | 过滤查询 | `?module=production&userId=OP001&startTime=2026-04-01&endTime=2026-04-30` |

**对外 API（供其他模块调用）**：
```java
// SysLogService.java
void log(String userId, String action, String module, String detail, String ipAddress);
```

---

## 4. 数据库设计

### 4.1 煎药方案 `md_decoct_scheme`（已存在，仅需补全 Entity）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | INTEGER PK AI | 主键 |
| name | VARCHAR(100) | 方案名称 |
| scheme_type | INTEGER | 0=常规, 1=先煎, 2=后下, 3=先煎后下 |
| decoct_times | INTEGER | 煎药次数 |
| pressure | INTEGER | 0=微压, 1=常压 |
| upper_water | DECIMAL(6,1) | 加水量(ml) |
| heating_time | INTEGER | 加热时长(分钟) |
| pre_heating_time | INTEGER | 先煎时长 |
| post_heating_time | INTEGER | 后下时长 |
| description | VARCHAR(500) | 描述 |
| tenant_id | VARCHAR(32) | 多租户（V4添加） |
| deleted | INTEGER | 软删 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

### 4.2 系统配置 `sys_config`（已存在）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | INTEGER PK AI | 主键 |
| config_key | VARCHAR(100) UNIQUE | 配置键 |
| config_value | TEXT | 配置值 |
| description | VARCHAR(200) | 说明 |
| tenant_id | VARCHAR(32) | 多租户 |
| deleted | INTEGER | 软删 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

### 4.3 操作日志 `sys_log`（已存在）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | INTEGER PK AI | 主键 |
| user_id | VARCHAR(50) | 操作人 |
| action | VARCHAR(100) | 动作（如 CREATE_TASK） |
| module | VARCHAR(50) | 模块（如 production/quality） |
| detail | TEXT | 详情（JSON或文本） |
| ip_address | VARCHAR(50) | IP地址 |
| tenant_id | VARCHAR(32) | 多租户 |
| created_at | DATETIME | 创建时间（无 updated_at/deleted，审计日志物理留存） |

---

## 5. 与其他模块的交互

### 5.1 dms-production 调用关系

```
dms-production (TaskService/PrescriptionService)
    │
    ├── 读取 ──▶ DecoctSchemeService.getById(schemeId)
    │            任务创建时根据处方关联的 scheme_id 读取煎药参数
    │
    └── 调用 ──▶ SysLogService.log(userId, "CREATE_TASK", "production", detail, ip)
                 记录生产操作日志
```

### 5.2 被调用方

| 调用方 | 调用内容 | 触发时机 |
|--------|---------|----------|
| dms-production | `DecoctSchemeService.getById()` | 创建任务时读取煎药参数 |
| dms-production | `SysLogService.log()` | 状态变更、任务创建等 |
| dms-quality | `SysLogService.log()` | 质检操作记录 |
| dms-equipment | `SysLogService.log()` | 设备故障、绑定等 |
| dms-print | `SysLogService.log()` | 打印任务提交 |

---

## 6. 技术方案

### 6.1 煎药方案字段设计

参考现有 `md_decoct_scheme` 表结构，Entity 直接映射所有字段：

```java
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("md_decoct_scheme")
public class DecoctScheme extends BaseEntity {
    private String name;
    private Integer schemeType;      // 0=常规 1=先煎 2=后下 3=先煎后下
    private Integer decoctTimes;     // 煎药次数
    private Integer pressure;        // 0=微压 1=常压
    private BigDecimal upperWater;   // 加水量
    private Integer heatingTime;     // 加热时长(分钟)
    private Integer preHeatingTime;  // 先煎时长
    private Integer postHeatingTime; // 后下时长
    private String description;
}
```

### 6.2 系统配置缓存策略

**决策：使用 Spring Cache + 本地 ConcurrentHashMap**

理由：
- 配置项读多写少（99%读取），缓存命中率高
- 数据量小（预计 < 100条），本地缓存足够
- 避免引入 Redis 依赖，保持单体应用简单

实现：
```java
@Cacheable(value = "sysConfig", key = "#tenantId + ':' + #configKey")
public String getConfigValue(String tenantId, String configKey) { ... }

@CacheEvict(value = "sysConfig", key = "#tenantId + ':' + #configKey")
public SysConfig update(Long id, SysConfig config) { ... }
```

### 6.3 操作日志写入方式

**决策：双轨制 = AOP 自动记录 + 手动调用**

| 方式 | 适用场景 | 实现 |
|------|---------|------|
| AOP 切面 | Controller 层 REST 请求 | `@LogOperation(module="production", action="CREATE_TASK")` 注解 + Aspect 拦截 |
| 手动调用 | Service 层复杂业务 | `sysLogService.log(userId, action, module, detail, ip)` |

**异步写入**：
```java
@Async("sysLogExecutor")
public void log(String userId, String action, String module, String detail, String ipAddress) {
    sysLogMapper.insert(log);
}
```

理由：
- 日志写入不应阻塞业务线程（尤其是高频操作如温度上报）
- `@Async` 配合 `ThreadPoolTaskExecutor`（核心线程2，队列100）即可
- AOP 减少重复代码，手动调用覆盖切面无法捕获的场景

**线程池配置**：
```java
@Bean("sysLogExecutor")
public Executor sysLogExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(5);
    executor.setQueueCapacity(100);
    executor.setThreadNamePrefix("syslog-");
    return executor;
}
```

### 6.4 与 dms-production 的关联设计

当前 `prod_prescription` 表已有 `scheme_id` 外键，但无显式 FOREIGN KEY 约束（SQLite 限制）。

**关联方式**：
- 处方创建时，前端选择煎药方案，保存 `scheme_id`
- 任务创建时（由处方自动建任务），TaskService 读取 `Prescription.schemeId`，关联到 `DecoctScheme`
- **可选增强**：在 `prod_task` 表增加 `scheme_id` 字段，任务级直接关联（无需每次查处方）

---

## 7. 难点和风险

| 风险 | 等级 | 应对 |
|------|------|------|
| sys_log 表数据量膨胀 | 中 | 不加软删字段，未来通过定时归档/分表处理 |
| @Async 日志丢失 | 低 | 队列满时降级为同步写入，或记录到本地文件 |
| 包名迁移冲突 | 中 | 当前代码为 `com.decoction`，新代码按 `cn.org.openygt` 编写，迁移时统一处理 |
| 缓存一致性 | 低 | @CacheEvict 在 update/delete 时清除，极小概率不一致可接受 |

---

## 8. 开发顺序建议

1. **第一步**：DecoctScheme（无依赖，最简单）
2. **第二步**：SysConfig（引入 @Cacheable）
3. **第三步**：SysLog + OperationLogAspect（引入 @Async + AOP）
4. **第四步**：在 dms-production 中注入 SysLogService，补充关键操作日志
