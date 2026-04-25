# dms-analytics 详细设计

> 模块：煎药室管理系统 — 运营分析模块（预留占位）
> 包名：`cn.org.openygt.analytics`
> 表前缀：`ops_`（预留，本模块不建实体表）
> API 前缀：`/api/v1/ops`
> 版本：V1.4 详细设计
> 关联文档：`dms-analytics-需求与概设.md`

---

## 一、模块概述

### 1.1 职责定位

`dms-analytics` 是 openygt-dms 的**运营分析与报表支撑模块**，为煎药室管理层提供产能、设备、人员、损耗、质检等多维度量化指标。该模块在 V1.4 阶段为**纯只读聚合层**，不持有任何业务实体，不直接操作数据库，仅通过 SPI 消费其他模块的统计数据并进行内存级聚合计算。

### 1.2 设计原则（专家评审04结论）

| 原则 | 说明 |
|------|------|
| **纯 SPI 模式** | 禁止定义 Entity 和 Mapper，禁止直接跨模块写 SQL |
| **只读** | 本模块不执行任何 INSERT / UPDATE / DELETE |
| **DTO 驱动** | 所有出入参均为 DTO，无 Entity 暴露 |
| **DDL 归属数据源模块** | 若存在统计视图，其 DDL 由数据源模块（dms-production / dms-equipment / dms-quality）维护 |
| **内存聚合** | Service 层对 SPI 返回的原始数据做分组、汇总、比率计算 |

### 1.3 依赖关系

```
dms-analytics
├── Maven 依赖
│   ├── dms-common（SPI 接口 + ApiResponse + 工具类）
│   └── dms-system（JWT 过滤器、用户上下文）
├── SPI 注入（只消费，不暴露）
│   ├── ProductionQueryService — 产能、任务、工序、人员数据
│   ├── EquipmentService — 设备状态、利用率、故障统计
│   └── QualityService — 质检结果、合格率汇总
└── 被依赖
    └── 无（本模块不向其他模块暴露 SPI）
```

### 1.4 核心组件（目标态）

| 组件 | 职责 |
|------|------|
| `CapacityReportController` / `CapacityReportService` | 产能统计（日 / 周 / 月） |
| `EquipmentReportController` / `EquipmentReportService` | 设备利用率、故障统计 |
| `PersonnelReportController` / `PersonnelReportService` | 人员绩效、工作量分布 |
| `LossReportController` / `LossReportService` | 损耗量、损耗率分析 |
| `QualityReportController` / `QualityReportService` | 质检合格率、返工率、报废率 |
| `DashboardController` / `DashboardService` | 监控大屏实时数据聚合 |

---

## 二、代码现状 vs 目标差距

### 2.1 差距总表

| # | 差距项 | 现状 | 目标（V1.4） | 优先级 | 关联需求 |
|---|--------|------|-------------|--------|----------|
| A1 | 模块结构 | 仅 `AnalyticsModule.java` 常量类 | 完整 Controller + Service + DTO 层 | P2 | 全部 |
| A2 | SPI 消费接口 | 未注入任何 SPI | 注入 `ProductionQueryService` / `EquipmentService` / `QualityService` | P2 | 全部 |
| A3 | 产能报表 API | 缺失 | `GET /api/v1/ops/capacity/{period}` | P2 | OPS-001 |
| A4 | 设备利用率 API | 缺失 | `GET /api/v1/ops/equipment/utilization` | P2 | OPS-002 |
| A5 | 人员绩效 API | 缺失 | `GET /api/v1/ops/personnel/performance` | P2 | OPS-003 |
| A6 | 损耗分析 API | 缺失 | `GET /api/v1/ops/loss/analysis` | P2 | OPS-004 |
| A7 | 质检合格率 API | 缺失 | `GET /api/v1/ops/quality/summary` | P2 | OPS-005 |
| A8 | 监控大屏 API | 缺失 | `GET /api/v1/ops/dashboard/realtime` | P2 | OPS-006 |
| A9 | 缓存 | 缺失 | 大屏数据 5~10 秒本地缓存 | P2 | OPS-006 |
| A10 | 单测覆盖 | 缺失 | Service 层 Mock SPI 单测 ≥ 80% | P2 | — |

### 2.2 现状代码片段

```java
// dms-analytics/src/main/java/cn/org/openygt/analytics/AnalyticsModule.java
public final class AnalyticsModule {
    private AnalyticsModule() {}
    public static final String TABLE_PREFIX = "ops_";
    public static final String API_PREFIX = "/api/v1/ops";
}
```

> 当前无任何 Controller、Service、Entity、Mapper、DTO。

---

## 三、模块边界与架构约束（纯SPI模式的设计原则）

### 3.1 禁止清单

本模块严格遵守以下禁止项（评审修订03 / 评审修订04）：

| 禁止项 | 理由 | 违规后果 |
|--------|------|----------|
| ❌ 定义 JPA Entity | 无自己的业务表 | 引入不必要的维护负担 |
| ❌ 定义 MyBatis Mapper | 不直接读写数据库 | 破坏纯 SPI 架构 |
| ❌ 直接 `SELECT` 其他模块表 | 编译期依赖他人表结构 | 表结构变更静默破坏报表 |
| ❌ 直接 `JOIN` 跨模块表 | 同上，且事务边界混乱 | 模块间耦合 |
| ❌ 暴露 SPI 接口 | 本模块只消费不生产 | 反向依赖 |
| ❌ 写操作（INSERT/UPDATE/DELETE） | 只读聚合层 | 数据安全风险 |

### 3.2 允许清单

| 允许项 | 场景 | 示例 |
|--------|------|------|
| ✅ 消费 dms-common SPI | 获取统计数据 | `productionQueryService.getDailyCapacity(start, end)` |
| ✅ 内存聚合（Stream / 循环） | DTO 分组汇总 | `list.stream().collect(groupingBy(...))` |
| ✅ 定义 DTO | API 出参 | `CapacityDailyDTO`、`DashboardRealtimeDTO` |
| ✅ 定义常量 / 枚举 | 本模块业务语义 | `AnalyticsModule`、`ReportPeriodEnum` |
| ✅ 本地缓存 | 大屏实时数据 | `@Cacheable(value = "dashboard", key = "'realtime'")` |
| ✅ 调用 dms-system 配置 | 报表阈值、参数 | `sys_config` 读取（通过现有 System SPI） |

### 3.3 SPI 数据流

```
┌─────────────────┐     SPI      ┌──────────────────────┐
│  dms-production │ ───────────> │                      │
│  (任务/工序数据) │              │   dms-analytics      │
└─────────────────┘              │   (内存聚合/报表)     │
┌─────────────────┐     SPI      │                      │
│  dms-equipment  │ ───────────> │   Controller →       │
│  (设备/故障数据) │              │   Service (纯内存)   │
└─────────────────┘              │   → DTO → API        │
┌─────────────────┐     SPI      │                      │
│  dms-quality    │ ───────────> │                      │
│  (质检结果数据)  │              └──────────────────────┘
└─────────────────┘
```

### 3.4 SPI 接口扩展约定

当前 `dms-common` 中 `EquipmentService` / `QualityService` 以命令型接口为主（设备占用、提交质检），缺少批量统计查询方法。V1.4 需在 `dms-common` 中**新增或扩展**以下查询方法（由数据源模块实现）：

| SPI 方法（建议签名） | 归属接口 | 实现模块 |
|---------------------|---------|---------|
| `List<DailyCapacityDTO> getDailyCapacity(LocalDate start, LocalDate end)` | `ProductionQueryService` | dms-production |
| `List<TaskDTO> getTasksByIds(List<Long> taskIds)` | `ProductionQueryService` | dms-production |
| `List<TaskStatusHistoryDTO> getTaskStatusHistory(Long taskId)` | `ProductionQueryService` | dms-production |
| `List<DeviceUtilizationDTO> getDeviceUtilization(Long deviceId, LocalDate start, LocalDate end)` | `EquipmentService` | dms-equipment |
| `List<FaultStatsDTO> getFaultStats(LocalDate start, LocalDate end)` | `EquipmentService` | dms-equipment |
| `Long getOnlineDeviceCount()` | `EquipmentService` | dms-equipment |
| `InspectionSummaryDTO getInspectionSummary(LocalDate start, LocalDate end)` | `QualityService` | dms-quality |

> **注意**：上述 SPI 方法为 V1.4 规划新增。若某些方法短期内未在 dms-common 定义，dms-analytics 应通过已有的原子方法组合获取（如先查 taskIds 再循环查详情），避免直接读表。

---

## 四、DTO 详细设计（所有报表返回的DTO）

所有 DTO 使用 `lombok.Data` 注解，字段命名遵循下划线转驼峰（与前端约定一致）。数值统计字段使用 `BigDecimal` 避免浮点误差，计数字段使用 `Long`。

### 4.1 产能统计 DTO

```java
package cn.org.openygt.analytics.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 日产能统计项
 */
@Data
public class CapacityDailyDTO {
    /** 统计日期 */
    private LocalDate statDate;
    /** 煎药任务总数 */
    private Long totalTasks;
    /** 已完成剂数 */
    private Long completedTasks;
    /** 煎药总袋数（从交接明细汇总） */
    private Long totalBags;
    /** 报废剂数 */
    private Long scrappedTasks;
    /** 完成率 = completedTasks / totalTasks */
    private BigDecimal completionRate;
}
```

```java
package cn.org.openygt.analytics.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 周/月产能聚合项（按周或月汇总）
 */
@Data
public class CapacityPeriodDTO {
    /** 周期标识，如 "2024-W03" 或 "2024-04" */
    private String period;
    /** 周期开始日期 */
    private String periodStart;
    /** 周期结束日期 */
    private String periodEnd;
    /** 任务总数 */
    private Long totalTasks;
    /** 已完成数 */
    private Long completedTasks;
    /** 总袋数 */
    private Long totalBags;
    /** 平均日完成数 */
    private BigDecimal avgDailyCompleted;
}
```

### 4.2 设备利用率 DTO

```java
package cn.org.openygt.analytics.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 单设备单日利用率
 */
@Data
public class DeviceUtilizationDTO {
    /** 设备ID */
    private Long deviceId;
    /** 设备编码 */
    private String deviceCode;
    /** 设备名称 */
    private String deviceName;
    /** 统计日期 */
    private LocalDate statDate;
    /** 运行时长（分钟） */
    private Long runningMinutes;
    /** 理论可用时长（分钟），默认 1440（24h）或按班次 */
    private Long availableMinutes;
    /** 利用率 = runningMinutes / availableMinutes */
    private BigDecimal utilizationRate;
}
```

```java
package cn.org.openygt.analytics.dto;

import lombok.Data;
import java.time.LocalDate;

/**
 * 设备故障统计项
 */
@Data
public class FaultStatsDTO {
    /** 设备ID */
    private Long deviceId;
    /** 设备编码 */
    private String deviceCode;
    /** 故障码 */
    private String faultCode;
    /** 故障描述 */
    private String faultMessage;
    /** 发生次数 */
    private Long occurrenceCount;
    /** 首次发生时间 */
    private LocalDate firstOccurrence;
    /** 末次发生时间 */
    private LocalDate lastOccurrence;
    /** 是否已解决 */
    private Boolean resolved;
}
```

### 4.3 人员绩效 DTO

```java
package cn.org.openygt.analytics.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 人员绩效统计项
 */
@Data
public class PersonnelPerformanceDTO {
    /** 操作人ID */
    private String operatorId;
    /** 操作人姓名 */
    private String operatorName;
    /** 统计日期 */
    private LocalDate statDate;
    /** 处理任务数 */
    private Long taskCount;
    /** 处理袋数 */
    private Long bagCount;
    /** 总工时（分钟，从状态历史推导） */
    private Long totalWorkMinutes;
    /** 平均单任务耗时（分钟） */
    private BigDecimal avgTaskDuration;
    /** 平均工时利用率（与班次时长对比） */
    private BigDecimal workTimeUtilization;
}
```

### 4.4 损耗分析 DTO

```java
package cn.org.openygt.analytics.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 损耗分析项（按工序/药品维度）
 */
@Data
public class LossAnalysisDTO {
    /** 维度类型：SOAK / DECOCT / POUR / WRAP / 药品 */
    private String dimensionType;
    /** 维度名称 */
    private String dimensionName;
    /** 理论投入量（克/剂） */
    private BigDecimal theoreticalInput;
    /** 实际产出量（克/剂） */
    private BigDecimal actualOutput;
    /** 损耗量 */
    private BigDecimal lossAmount;
    /** 损耗率 = lossAmount / theoreticalInput */
    private BigDecimal lossRate;
}
```

### 4.5 质检合格率 DTO

```java
package cn.org.openygt.analytics.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 质检汇总统计
 */
@Data
public class QualitySummaryDTO {
    /** 统计日期 */
    private LocalDate statDate;
    /** 质检总数 */
    private Long totalInspected;
    /** 合格数（含让步放行） */
    private Long passedCount;
    /** 返工数 */
    private Long reworkCount;
    /** 报废数 */
    private Long scrappedCount;
    /** 合格率 */
    private BigDecimal passRate;
    /** 返工率 */
    private BigDecimal reworkRate;
    /** 报废率 */
    private BigDecimal scrapRate;
}
```

### 4.6 监控大屏 DTO

```java
package cn.org.openygt.analytics.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

/**
 * 监控大屏实时数据
 */
@Data
public class DashboardRealtimeDTO {
    /** 今日任务总数 */
    private Long todayTotalTasks;
    /** 今日已完成数 */
    private Long todayCompletedTasks;
    /** 今日进行中数 */
    private Long todayInProgressTasks;
    /** 今日待处理数 */
    private Long todayPendingTasks;
    /** 在线设备数 */
    private Long onlineDeviceCount;
    /** 离线设备数 */
    private Long offlineDeviceCount;
    /** 当前告警/故障数（未解决） */
    private Long activeAlarmCount;
    /** 今日质检批次 */
    private Long todayInspectionCount;
    /** 今日合格率 */
    private BigDecimal todayPassRate;
    /** 各状态任务分布 */
    private List<TaskStatusDistributionDTO> taskStatusDistribution;
    /** 设备类型在线分布 */
    private List<DeviceTypeDistributionDTO> deviceTypeDistribution;
}
```

```java
package cn.org.openygt.analytics.dto;

import lombok.Data;

@Data
public class TaskStatusDistributionDTO {
    private String status;
    private String statusName;
    private Long count;
}
```

```java
package cn.org.openygt.analytics.dto;

import lombok.Data;

@Data
public class DeviceTypeDistributionDTO {
    private String deviceType;
    private String typeName;
    private Long onlineCount;
    private Long totalCount;
}
```

---

## 五、REST API 详细设计（产能/设备/人员/损耗/质检/大屏）

统一返回 `ApiResponse<T>` 或 `ApiResponse<List<T>>`。时间参数默认最近 30 天。

### 5.1 产能报表

| 方法 | 路径 | 参数 | 返回 |
|------|------|------|------|
| GET | `/api/v1/ops/capacity/daily` | `startDate` (可选, 默认 30 天前), `endDate` (可选, 默认今天), `hospitalId` (可选) | `ApiResponse<List<CapacityDailyDTO>>` |
| GET | `/api/v1/ops/capacity/weekly` | `startDate`, `endDate`, `hospitalId` (可选) | `ApiResponse<List<CapacityPeriodDTO>>` |
| GET | `/api/v1/ops/capacity/monthly` | `startDate`, `endDate`, `hospitalId` (可选) | `ApiResponse<List<CapacityPeriodDTO>>` |

**Controller 示例**：

```java
@RestController
@RequestMapping(AnalyticsModule.API_PREFIX + "/capacity")
@RequiredArgsConstructor
public class CapacityReportController {

    private final CapacityReportService capacityReportService;

    @GetMapping("/daily")
    public ApiResponse<List<CapacityDailyDTO>> daily(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long hospitalId) {
        if (startDate == null) startDate = LocalDate.now().minusDays(30);
        if (endDate == null) endDate = LocalDate.now();
        return ApiResponse.success(capacityReportService.getDailyCapacity(startDate, endDate, hospitalId));
    }
}
```

### 5.2 设备利用率

| 方法 | 路径 | 参数 | 返回 |
|------|------|------|------|
| GET | `/api/v1/ops/equipment/utilization` | `deviceId` (可选), `startDate`, `endDate` | `ApiResponse<List<DeviceUtilizationDTO>>` |
| GET | `/api/v1/ops/equipment/fault-stats` | `deviceId` (可选), `startDate`, `endDate`, `resolved` (可选) | `ApiResponse<List<FaultStatsDTO>>` |

### 5.3 人员绩效

| 方法 | 路径 | 参数 | 返回 |
|------|------|------|------|
| GET | `/api/v1/ops/personnel/performance` | `operatorId` (可选), `startDate`, `endDate` | `ApiResponse<List<PersonnelPerformanceDTO>>` |
| GET | `/api/v1/ops/personnel/workload` | `startDate`, `endDate` | `ApiResponse<List<PersonnelPerformanceDTO>>`（按人汇总） |

### 5.4 损耗分析

| 方法 | 路径 | 参数 | 返回 |
|------|------|------|------|
| GET | `/api/v1/ops/loss/analysis` | `startDate`, `endDate`, `dimension` (可选: STEP/DRUG) | `ApiResponse<List<LossAnalysisDTO>>` |

### 5.5 质检统计

| 方法 | 路径 | 参数 | 返回 |
|------|------|------|------|
| GET | `/api/v1/ops/quality/summary` | `startDate`, `endDate`, `hospitalId` (可选) | `ApiResponse<List<QualitySummaryDTO>>` |
| GET | `/api/v1/ops/quality/trend` | `startDate`, `endDate`, `granularity` (day/week/month) | `ApiResponse<List<QualitySummaryDTO>>` |

### 5.6 监控大屏

| 方法 | 路径 | 参数 | 返回 | 缓存 |
|------|------|------|------|------|
| GET | `/api/v1/ops/dashboard/realtime` | 无 | `ApiResponse<DashboardRealtimeDTO>` | 5~10 秒 |
| GET | `/api/v1/ops/dashboard/today` | 无 | `ApiResponse<DashboardRealtimeDTO>` | 5~10 秒 |

---

## 六、Service 层详细设计（内存聚合策略、大数据量处理）

### 6.1 包结构与类职责

```
cn.org.openygt.analytics.service
├── CapacityReportService.java / CapacityReportServiceImpl.java
├── EquipmentReportService.java / EquipmentReportServiceImpl.java
├── PersonnelReportService.java / PersonnelReportServiceImpl.java
├── LossReportService.java / LossReportServiceImpl.java
├── QualityReportService.java / QualityReportServiceImpl.java
└── DashboardService.java / DashboardServiceImpl.java
```

### 6.2 内存聚合策略

Service 层接收 SPI 返回的原始 DTO 列表后，使用 Java Stream API 进行内存聚合。**禁止在 Service 中拼接 SQL 或调用 Mapper**。

#### 示例：日产能聚合

```java
@Service
@RequiredArgsConstructor
public class CapacityReportServiceImpl implements CapacityReportService {

    private final ProductionQueryService productionQueryService;

    @Override
    public List<CapacityDailyDTO> getDailyCapacity(LocalDate start, LocalDate end, Long hospitalId) {
        // 1. 通过 SPI 获取原始数据
        List<DailyCapacityRawDTO> rawList = productionQueryService.getDailyCapacity(start, end);

        // 2. 内存聚合：按日期分组
        Map<LocalDate, List<DailyCapacityRawDTO>> grouped = rawList.stream()
            .filter(r -> hospitalId == null || hospitalId.equals(r.getHospitalId()))
            .collect(Collectors.groupingBy(DailyCapacityRawDTO::getStatDate));

        // 3. 计算派生指标
        List<CapacityDailyDTO> result = new ArrayList<>();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            List<DailyCapacityRawDTO> dayList = grouped.getOrDefault(date, Collections.emptyList());
            long total = dayList.size();
            long completed = dayList.stream().filter(r -> "COMPLETED".equals(r.getStatus())).count();
            long scrapped = dayList.stream().filter(r -> "SCRAPPED".equals(r.getStatus())).count();

            CapacityDailyDTO dto = new CapacityDailyDTO();
            dto.setStatDate(date);
            dto.setTotalTasks(total);
            dto.setCompletedTasks(completed);
            dto.setScrappedTasks(scrapped);
            dto.setCompletionRate(total == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(completed)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP));
            result.add(dto);
        }
        return result;
    }
}
```

#### 示例：大屏数据聚合

```java
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final ProductionQueryService productionQueryService;
    private final EquipmentService equipmentService;
    private final QualityService qualityService;

    @Override
    @Cacheable(value = "dashboard", key = "'realtime'")
    public DashboardRealtimeDTO getRealtime() {
        LocalDate today = LocalDate.now();
        DashboardRealtimeDTO dto = new DashboardRealtimeDTO();

        // 产能数据
        List<DailyCapacityRawDTO> todayTasks = productionQueryService.getDailyCapacity(today, today);
        dto.setTodayTotalTasks((long) todayTasks.size());
        dto.setTodayCompletedTasks(todayTasks.stream().filter(t -> "COMPLETED".equals(t.getStatus())).count());
        dto.setTodayInProgressTasks(todayTasks.stream().filter(t -> "IN_PROGRESS".equals(t.getStatus())).count());
        dto.setTodayPendingTasks(todayTasks.stream().filter(t -> "PENDING".equals(t.getStatus())).count());

        // 设备数据
        dto.setOnlineDeviceCount(equipmentService.getOnlineDeviceCount());

        // 故障数据（未解决）
        List<FaultStatsDTO> faults = equipmentService.getFaultStats(today, today);
        dto.setActiveAlarmCount(faults.stream().filter(f -> !Boolean.TRUE.equals(f.getResolved())).count());

        // 质检数据
        InspectionSummaryDTO summary = qualityService.getInspectionSummary(today, today);
        if (summary != null) {
            dto.setTodayInspectionCount(summary.getTotalInspected());
            dto.setTodayPassRate(summary.getPassRate());
        }

        return dto;
    }
}
```

### 6.3 大数据量处理策略

由于本模块纯内存聚合，大数据量场景需做以下保护：

| 场景 | 策略 | 说明 |
|------|------|------|
| 时间范围过大 | 参数校验 + 默认值 | 单次查询最大 365 天，超范围抛异常或截断 |
| SPI 返回数据量大 | SPI 侧分页 + 本模块流式聚合 | 要求 SPI 提供分页参数 `page`, `size` |
| 周/月聚合 | 先查日明细再二次聚合 | 不直接查跨月大跨度明细 |
| 人员绩效多维度 | 按需查询，不加无关字段 | SPI 方法提供字段筛选参数 |

**时间范围限制示例**：

```java
private void validateDateRange(LocalDate start, LocalDate end) {
    if (start == null || end == null) throw new IllegalArgumentException("日期范围不能为空");
    if (start.isAfter(end)) throw new IllegalArgumentException("开始日期不能晚于结束日期");
    if (ChronoUnit.DAYS.between(start, end) > 365) {
        throw new IllegalArgumentException("单次查询时间范围不能超过 365 天");
    }
}
```

### 6.4 空值与除零处理

所有比率计算必须处理除零和空值：

```java
// 统一工具方法
private BigDecimal safeRate(Long numerator, Long denominator) {
    if (denominator == null || denominator == 0) return BigDecimal.ZERO;
    return BigDecimal.valueOf(numerator)
        .multiply(BigDecimal.valueOf(100))
        .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
}
```

---

## 七、视图设计（DDL归属说明，由数据源模块维护）

### 7.1 视图归属原则

`dms-analytics` **不创建、不维护任何视图 DDL**。如果数据源模块选择以数据库视图方式暴露统计数据，则视图的 `CREATE VIEW` 语句必须放在**数据源模块自身的 Flyway 迁移脚本**中。

### 7.2 视图归属表

| 视图名 | 数据源表 | DDL 归属模块 | 说明 |
|--------|---------|-------------|------|
| `ops_daily_capacity` | `prod_task` | **dms-production** | 日任务数、完成数、报废数 |
| `ops_equipment_utilization` | `prod_step_log` + `eq_device` | **dms-production** / **dms-equipment** | 设备运行时长（以工序日志为主，归 production） |
| `ops_equipment_fault_stats` | `eq_device_alarm` | **dms-equipment** | 设备故障统计 |
| `ops_quality_summary` | `qt_inspection` | **dms-quality** | 质检合格/返工/报废统计 |

### 7.3 双版本 DDL 示例（由数据源模块维护）

以下 DDL **仅供数据源模块参考**，不作为 `dms-analytics` 的交付物：

```sql
-- =============================================
-- 归属：dms-production / db/migration/Vxxx__ops_views.sql
-- =============================================

-- SQLite 版本（开发环境）
CREATE VIEW IF NOT EXISTS ops_daily_capacity AS
SELECT
    DATE(created_at) as stat_date,
    tenant_id,
    hospital_id,
    COUNT(*) as total_tasks,
    SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) as completed_tasks,
    SUM(CASE WHEN status = 'SCRAPPED' THEN 1 ELSE 0 END) as scrapped_tasks
FROM prod_task
WHERE deleted = 0
GROUP BY DATE(created_at), tenant_id, hospital_id;

-- MySQL 版本（生产环境）
CREATE VIEW IF NOT EXISTS ops_daily_capacity AS
SELECT
    DATE(created_at) as stat_date,
    tenant_id,
    hospital_id,
    COUNT(*) as total_tasks,
    SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) as completed_tasks,
    SUM(CASE WHEN status = 'SCRAPPED' THEN 1 ELSE 0 END) as scrapped_tasks
FROM prod_task
WHERE deleted = 0
GROUP BY DATE(created_at), tenant_id, hospital_id;
```

> **重要**：`dms-analytics` 的 Service 代码中**不得出现任何视图名或表名字面量**。所有数据访问通过 SPI 接口完成。

---

## 八、缓存策略（大屏数据5~10秒缓存）

### 8.1 缓存范围

仅对监控大屏 API 启用短期缓存，其他报表 API 不缓存（确保数据实时性）。

| API | 缓存键 | TTL | 说明 |
|-----|--------|-----|------|
| GET `/api/v1/ops/dashboard/realtime` | `"dashboard:realtime"` | 5~10 秒 | 并发高时避免反复查 SPI |
| GET `/api/v1/ops/dashboard/today` | `"dashboard:today"` | 5~10 秒 | 与 realtime 共享同一数据源 |

### 8.2 实现方式

使用 Spring Cache + Caffeine（本地缓存，单节点足够）：

```java
@Configuration
public class AnalyticsCacheConfig {

    @Bean("analyticsCacheManager")
    public CacheManager analyticsCacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager("dashboard");
        manager.setCaffeine(Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(10))
            .maximumSize(100));
        return manager;
    }
}
```

Service 层使用：

```java
@Cacheable(value = "dashboard", key = "'realtime'", cacheManager = "analyticsCacheManager")
public DashboardRealtimeDTO getRealtime() { ... }
```

### 8.3 缓存刷新策略

- 被动过期：Caffeine 10 秒自动过期，下次请求重新计算。
- 无主动刷新：大屏数据不要求强一致性，10 秒延迟可接受。
- 多节点部署时：如后续拆分为独立服务，可替换为 Redis，TTL 保持 5~10 秒。

---

## 九、单元测试策略（Mock SPI接口）

### 9.1 测试原则

- **不启动 Spring 上下文**：Service 层测试使用纯 Mockito，不依赖 `@SpringBootTest`。
- **Mock 全部 SPI**：`ProductionQueryService`、`EquipmentService`、`QualityService` 全部 `@Mock`。
- **断言聚合结果**：验证内存计算（分组、汇总、比率）正确性。
- **覆盖率目标**：Service 层行覆盖 ≥ 80%，分支覆盖 ≥ 70%。

### 9.2 测试目录结构

```
src/test/java/cn/org/openygt/analytics/service
├── CapacityReportServiceTest.java
├── DashboardServiceTest.java
├── EquipmentReportServiceTest.java
├── PersonnelReportServiceTest.java
├── LossReportServiceTest.java
└── QualityReportServiceTest.java
```

### 9.3 测试示例

```java
@ExtendWith(MockitoExtension.class)
class CapacityReportServiceTest {

    @Mock
    private ProductionQueryService productionQueryService;

    @InjectMocks
    private CapacityReportServiceImpl capacityReportService;

    @Test
    void getDailyCapacity_shouldAggregateCorrectly() {
        // Given
        LocalDate start = LocalDate.of(2024, 4, 1);
        LocalDate end = LocalDate.of(2024, 4, 2);
        List<DailyCapacityRawDTO> raw = Arrays.asList(
            createRaw(start, "COMPLETED", 1L),
            createRaw(start, "COMPLETED", 1L),
            createRaw(start, "SCRAPPED", 1L),
            createRaw(end, "PENDING", 1L)
        );
        when(productionQueryService.getDailyCapacity(start, end)).thenReturn(raw);

        // When
        List<CapacityDailyDTO> result = capacityReportService.getDailyCapacity(start, end, null);

        // Then
        assertEquals(2, result.size());
        CapacityDailyDTO day1 = result.get(0);
        assertEquals(3L, day1.getTotalTasks());
        assertEquals(2L, day1.getCompletedTasks());
        assertEquals(0, day1.getCompletionRate().compareTo(new BigDecimal("66.67")));
    }

    @Test
    void getDailyCapacity_shouldReturnZeroForEmptyRange() {
        LocalDate start = LocalDate.of(2024, 4, 1);
        LocalDate end = LocalDate.of(2024, 4, 1);
        when(productionQueryService.getDailyCapacity(start, end)).thenReturn(Collections.emptyList());

        List<CapacityDailyDTO> result = capacityReportService.getDailyCapacity(start, end, null);

        assertEquals(1, result.size());
        assertEquals(0L, result.get(0).getTotalTasks());
        assertEquals(BigDecimal.ZERO, result.get(0).getCompletionRate());
    }
}
```

### 9.4 边界 case 清单

| 场景 | 预期行为 |
|------|----------|
| SPI 返回空列表 | 返回全零 DTO，不抛异常 |
| 除零（总数为 0） | 比率返回 `0.00` |
| 时间范围逆序 | 抛 `IllegalArgumentException` |
| 时间范围超过 365 天 | 抛 `IllegalArgumentException` |
| SPI 返回含 null 字段 | `COALESCE` 由 SPI 实现侧保证，本模块做二次空值兜底 |

---

## 十、迁移路径

### 10.1 阶段一：SPI 接口补齐（前置依赖）

1. 在 `dms-common` 中定义 `ProductionQueryService` 接口（如尚未定义）。
2. 在 `dms-common` 中扩展 `EquipmentService` / `QualityService` 的查询方法签名。
3. 在 `dms-production` / `dms-equipment` / `dms-quality` 中实现上述 SPI 方法。

> **关键约束**：SPI 方法的返回值必须是**内聚的 DTO**，不得返回 `Map<String, Object>` 或裸 `Object`，以保证类型安全。

### 10.2 阶段二：dms-analytics 核心开发

1. 创建 `dto` 包及全部 DTO 类（第4章）。
2. 创建 `service` 包及接口 + 实现，注入 SPI，实现内存聚合。
3. 创建 `controller` 包，暴露 REST API（第5章）。
4. 配置 Caffeine 缓存（第8章）。
5. 编写 Mockito 单测（第9章）。

### 10.3 阶段三：视图 DDL 移交（如采用视图方案）

1. 若数据源模块决定以视图方式优化性能，将 `ops_*` 视图的 DDL 迁移至对应模块的 Flyway 脚本。
2. `dms-analytics` 代码中**不出现任何视图名**。

### 10.4 阶段四：集成验证

- [ ] 所有 API 返回 `ApiResponse<T>` 且 HTTP 200
- [ ] Swagger / Knife4j 可正常展示 analytics 接口
- [ ] `mvn test -pl dms-analytics` 单测通过且覆盖率 ≥ 80%
- [ ] 无跨模块 SQL（静态扫描 `ops_` / `prod_` / `eq_` / `qt_` 表名在 analytics 模块的出现次数应为 0）
- [ ] 大屏接口压测 QPS 达标（借助 10 秒缓存）

---

## 附录：参考文档

| 文档 | 路径 |
|------|------|
| 需求与概设 | `docs-iot/project/dms-analytics-需求与概设.md` |
| 集成开发总计划（模块边界） | `docs-iot/project/集成开发总计划.md` |
| 架构设计说明书 | `docs-iot/architecture/架构设计说明书.md` |
| dms-production 详细设计 | `docs-iot/detailed-design/dms-production-详细设计.md` |
| dms-equipment 详细设计 | `docs-iot/detailed-design/dms-equipment-详细设计.md` |
| dms-quality 详细设计 | `docs-iot/detailed-design/dms-quality-详细设计.md` |
