# dms-analytics 需求与概要设计

> 模块类型：业务模块（预留占位）  
> 包名：`cn.org.openygt.analytics`  
> 表前缀：`ops_`（预留）  
> API 前缀：`/api/v1/ops`  
> 职责：运营数据分析与报表，包括产能统计、设备利用率、人员绩效、损耗分析、监控大屏。  
> 版本：V1.3（专家评审03后修订版）

---

## 一、模块定位

`dms-analytics` 是系统的**数据分析和运营支撑模块**，为管理层提供煎药室运营的量化指标和可视化报表。该模块**只读**（或极少写）其他模块的数据，通过统计查询和聚合计算生成报表，不直接参与生产流程。

**模块边界**：
- ✅ 提供统计查询和报表 API
- ✅ **通过 `ProductionQueryService` / `EquipmentService` / `QualityService` SPI 获取统计数据**
- ❌ 不修改任何业务数据
- ❌ 不提供 SPI 接口（不向其他模块暴露服务）
- ❌ 不管理具体业务实体
- ❌ **禁止直接跨模块写 SQL 读表**（必须通过 SPI 获取数据）

---

## 二、需求清单

### 2.1 当前状态

当前 `dms-analytics` 为**空壳模块**，仅包含一个常量类 `AnalyticsModule`，无任何 Controller、Service、Entity、Mapper。

### 2.2 待开发需求

| 需求编号 | 需求名称 | 优先级 | 状态 | 验收标准 |
|---------|---------|--------|------|---------|
| OPS-001 | 产能统计报表 | P2 | ⏳ | 按日/周/月统计煎药剂数、袋数 |
| OPS-002 | 设备利用率分析 | P2 | ⏳ | 设备开机率、运行时长、空闲时长 |
| OPS-003 | 人员绩效统计 | P2 | ⏳ | 按操作人统计工作量、效率 |
| OPS-004 | 损耗分析 | P2 | ⏳ | 各阶段损耗量、损耗率 |
| OPS-005 | 质检合格率报表 | P2 | ⏳ | 按医院/方案/时间维度统计 |
| OPS-006 | 监控大屏数据接口 | P2 | ⏳ | 实时任务数、设备状态、告警数 |
| OPS-007 | 报表导出 | P2 | ⏳ | Excel/PDF 导出 |

---

## 三、数据来源与统计口径

### 3.1 产能统计（OPS-001）

| 指标 | 数据来源 | 计算口径 |
|------|---------|---------|
| 日煎药剂数 | `prod_task` | `COUNT(*)` 按 `created_at` 日期分组 |
| 日煎药袋数 | `prod_handover_detail` | `SUM(bag_count)` 按 `handover_time` 日期分组 |
| 日完成剂数 | `prod_task` | `COUNT(*)` 按 `complete_time` 日期分组，status = 已完成 |

### 3.2 设备利用率（OPS-002）

| 指标 | 数据来源 | 计算口径 |
|------|---------|---------|
| 设备开机率 | `eq_device` + `prod_step_log` | 运行时长 / (运行时长 + 空闲时长) |
| 平均运行时长 | `prod_step_log` | `AVG(ended_at - started_at)` 按 device_id 分组 |
| 故障次数 | `eq_device_alarm` | `COUNT(*)` 按 device_id + alarm_type = FAULT 分组 |

### 3.3 人员绩效（OPS-003）

| 指标 | 数据来源 | 计算口径 |
|------|---------|---------|
| 人均处理任务数 | `prod_task` | `COUNT(*)` 按 `operator_id` 分组 |
| 人均工时 | `prod_work_record` | `SUM(work_time)` 按 `operator_id` 分组 |
| 平均操作时长 | `prod_step_log` | `AVG(ended_at - started_at)` 按 operator_id + step_type 分组 |

### 3.4 损耗分析（OPS-004）

| 指标 | 数据来源 | 计算口径 |
|------|---------|---------|
| 损耗量 | `prod_step_log` / 预留损耗字段 | 各阶段损耗重量/体积求和 |
| 损耗率 | 同上 | 损耗量 / 总投入量 |

### 3.5 质检合格率（OPS-005）

| 指标 | 数据来源 | 计算口径 |
|------|---------|---------|
| 合格率 | `qt_inspection` | `通过次数 / 总次数` |
| 返工率 | `qt_inspection` | `返工次数 / 总次数` |
| 报废率 | `qt_inspection` | `报废次数 / 总次数` |

### 3.6 监控大屏（OPS-006）

| 指标 | 数据来源 |
|------|---------|
| 今日任务总数 | `prod_task` |
| 各状态任务数 | `prod_task` GROUP BY status |
| 在线设备数 | `eq_device` WHERE status ≠ OFFLINE |
| 当前告警数 | `eq_device_alarm` WHERE is_resolved = 0 |
| 今日完成数 | `prod_task` WHERE status = 已完成 AND complete_time = 今日 |

---

## 四、接口设计

### 4.1 产能报表

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/ops/capacity/daily` | 日产能（query: startDate, endDate, hospitalId） |
| GET | `/api/v1/ops/capacity/weekly` | 周产能 |
| GET | `/api/v1/ops/capacity/monthly` | 月产能 |

### 4.2 设备利用率

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/ops/equipment/utilization` | 设备利用率（query: deviceId, startDate, endDate） |
| GET | `/api/v1/ops/equipment/fault-stats` | 设备故障统计 |

### 4.3 人员绩效

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/ops/personnel/performance` | 人员绩效（query: operatorId, startDate, endDate） |
| GET | `/api/v1/ops/personnel/workload` | 工作量分布 |

### 4.4 质检统计

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/ops/quality/summary` | 质检汇总 |
| GET | `/api/v1/ops/quality/trend` | 质检趋势 |

### 4.5 监控大屏

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/ops/dashboard/realtime` | 实时大屏数据 |
| GET | `/api/v1/ops/dashboard/today` | 今日汇总 |

### 4.6 报表导出

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/ops/export/capacity` | 导出产能报表 |
| GET | `/api/v1/ops/export/quality` | 导出质检报表 |

---

## 五、核心类结构（规划）

```
cn.org.openygt.analytics
├── controller
│   ├── CapacityReportController.java
│   ├── EquipmentReportController.java
│   ├── PersonnelReportController.java
│   ├── QualityReportController.java
│   └── DashboardController.java
├── service
│   ├── CapacityReportService.java / CapacityReportServiceImpl.java
│   ├── EquipmentReportService.java / EquipmentReportServiceImpl.java
│   ├── PersonnelReportService.java / PersonnelReportServiceImpl.java
│   ├── QualityReportService.java / QualityReportServiceImpl.java
│   └── DashboardService.java / DashboardServiceImpl.java
├── mapper
│   └── ~~AnalyticsMapper.java~~   # ✅ 已删除（评审修订03：禁止跨模块直接读表，改为纯SPI模式）
└── dto
    ├── CapacityDailyDTO.java
    ├── EquipmentUtilizationDTO.java
    ├── PersonnelPerformanceDTO.java
    ├── QualitySummaryDTO.java
    └── DashboardRealtimeDTO.java
```

> **注意**：`dms-analytics` 不写自己的 Entity，统计查询使用 MyBatis 的 `@Select` 注解直接写 SQL 返回 DTO。

---

## 六、技术方案

### 6.1 查询策略（评审修订03：禁止直接跨模块读表）

> **致命级问题**：`dms-analytics` 直接 `SELECT` 其他模块的表（`prod_task`、`eq_device_alarm`、`qt_inspection`），违反了集成开发总计划第 2.1 节"禁止跨模块数据访问"的原则。这建立了对其他模块表结构的编译期依赖，任何表结构变更都会静默破坏报表。

**修正方案**：`dms-analytics` **禁止直接写跨模块 SQL**。统一通过 `dms-common.ProductionQueryService` SPI 获取数据：

```java
@Service
@RequiredArgsConstructor
public class CapacityReportServiceImpl implements CapacityReportService {
    private final ProductionQueryService productionQueryService;
    // private final EquipmentService equipmentService; // 如需设备数据

    @Override
    public List<CapacityDailyDTO> getDailyCapacity(LocalDate start, LocalDate end) {
        // ✅ 通过 SPI 获取数据，不直接 SELECT prod_task
        return productionQueryService.getDailyCapacity(start, end);
    }
}
```

### 6.2 视图设计（评审修订03：DDL归属数据源模块）

> **折中方案**：如果视图方式保留，**视图的 DDL 必须归属数据源模块维护**（如 `ops_daily_capacity` 的 DDL 由 `dms-production` 的 Flyway 脚本创建），`dms-analytics` 只读视图。这样表结构变更时由数据源模块同步维护视图。

**视图归属规范**：

| 视图名 | DDL归属模块 | 说明 |
|--------|------------|------|
| `ops_daily_capacity` | dms-production | 基于 `prod_task` |
| `ops_equipment_utilization` | dms-production | 基于 `prod_step_log` |
| `ops_quality_summary` | dms-quality | 基于 `qt_inspection` |

**双版本 DDL 示例**（SQLite / MySQL）：

```sql
-- SQLite 版本（开发环境）
CREATE VIEW ops_daily_capacity AS
SELECT 
    DATE(created_at) as stat_date,
    tenant_id,
    COUNT(*) as total_tasks,
    SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) as completed_tasks,
    SUM(CASE WHEN status = 'SCRAPPED' THEN 1 ELSE 0 END) as scrapped_tasks
FROM prod_task
WHERE deleted = 0
GROUP BY DATE(created_at), tenant_id;

-- MySQL 版本（生产环境）
CREATE VIEW ops_daily_capacity AS
SELECT 
    DATE(created_at) as stat_date,
    tenant_id,
    COUNT(*) as total_tasks,
    SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) as completed_tasks,
    SUM(CASE WHEN status = 'SCRAPPED' THEN 1 ELSE 0 END) as scrapped_tasks
FROM prod_task
WHERE deleted = 0
GROUP BY DATE(created_at), tenant_id;

-- 设备利用率视图（MySQL版本，注意 TIMESTAMPDIFF）
CREATE VIEW ops_equipment_utilization_mysql AS
SELECT 
    device_id,
    DATE(started_at) as stat_date,
    SUM(TIMESTAMPDIFF(MINUTE, started_at, ended_at)) as running_minutes
FROM prod_step_log
WHERE step_type IN ('DECOCT', 'WRAP') AND ended_at IS NOT NULL
GROUP BY device_id, DATE(started_at);

-- 设备利用率视图（SQLite版本，使用 (julianday(end)-julianday(start))*1440）
CREATE VIEW ops_equipment_utilization_sqlite AS
SELECT 
    device_id,
    DATE(started_at) as stat_date,
    SUM((julianday(ended_at) - julianday(started_at)) * 1440) as running_minutes
FROM prod_step_log
WHERE step_type IN ('DECOCT', 'WRAP') AND ended_at IS NOT NULL
GROUP BY device_id, DATE(started_at);
```

> **评审修订03**：`TIMESTAMPDIFF` 是 MySQL 特有函数，SQLite 不支持。视图 DDL 必须提供 SQLite 和 MySQL 两个版本，或在 Flyway 中用条件脚本区分。

### 6.3 性能考虑

1. **索引优化**：确保 `prod_task.created_at`、`eq_device_alarm.created_at`、`qt_inspection.created_at` 等时间字段有索引。
2. **缓存**：大屏实时数据可设置 5~10 秒缓存，避免频繁查询。
3. **大数据量**：若数据量增长，考虑预聚合（如每日凌晨统计昨日数据写入汇总表）。

### 6.4 未来演进

- **阶段一**：直接 SQL 聚合查询 + 视图（当前架构）
- **阶段二**：引入预计算汇总表，定时任务每日更新
- **阶段三**：若拆分为独立服务，视图可直接变为微服务的只读 API

---

## 七、开发规范

1. **只读原则**：`dms-analytics` 禁止执行 INSERT/UPDATE/DELETE 操作。
2. **禁止跨模块直接读表**：**严禁**在 `AnalyticsMapper` 中直接 `SELECT` 其他模块的表。所有数据通过 `ProductionQueryService` SPI 获取。
3. **DTO 驱动**：所有查询返回 DTO，不定义 Entity。
4. **SQL 维护**：如需写统计 SQL，仅限本模块视图或 SPI 返回后的内存聚合。
5. **时间精度**：按日统计使用 `DATE(created_at)`，按周/月使用相应函数。
6. **空值处理**：统计查询中对 NULL 值做 `COALESCE` 处理，避免报表出现空值。

---

## 八、集成检查清单

- [ ] 无任何写操作（INSERT/UPDATE/DELETE）
- [ ] 所有 API 返回 `ApiResponse<T>`
- [ ] 统计口径与需求文档一致
- [ ] 时间范围参数有默认值（如默认查最近 30 天）
- [ ] 大数据量查询有分页或限制
- [ ] `mvn test` 单测通过
