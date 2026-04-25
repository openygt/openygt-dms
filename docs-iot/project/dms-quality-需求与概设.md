# dms-quality 需求与概要设计

> 模块类型：业务模块  
> 包名：`cn.org.openygt.quality`  
> 表前缀：`qt_`  
> API 前缀：`/api/v1/qt`  
> 职责：质量管理，包括质检执行、质检记录查询、质检追溯报表、**GMP合规映射**。  
> 版本：V1.3（专家评审03后修订版）

---

## 一、模块定位

`dms-quality` 是系统的**质量追溯模块**，在生产流程的"待质检"环节被 `dms-production` 调用，完成质检判定。同时提供质检记录的查询和统计能力，支持质量问题的追溯分析。

**模块边界**：
- ✅ 负责 `qt_` 前缀表的管理
- ✅ 实现 `dms-common` 中的 `QualityService` SPI 接口
- ✅ 提供质检记录查询 API
- ❌ 不直接修改 `prod_task` 的状态（由 `dms-production` 调用 SPI 后自行更新）
- ❌ 不管理处方、设备、打印等业务数据

**重要历史债务**：当前代码中 `dms-quality` 内部独立定义了一个 `Task` 实体，映射表名为 `task`，但 V4 迁移后实际表名为 `prod_task`。此问题需在迭代中修正，**禁止直接定义其他模块的实体**。

---

## 二、需求清单

### 2.1 已实现需求

| 需求编号 | 需求名称 | 优先级 | 状态 | 验收标准 |
|---------|---------|--------|------|---------|
| QT-001 | 质检执行 | P0 | ✅ | 4 种结果：通过/让步放行/返工/报废 |
| QT-002 | 质检记录查询 | P0 | ✅ | 按 taskId 查询最新质检记录 |

### 2.2 待开发需求

| 需求编号 | 需求名称 | 优先级 | 状态 | 验收标准 |
|---------|---------|--------|------|---------|
| **QT-003** | **质检追溯报表** | **P2** | ⏳ | 按时间/医院/方案统计合格率、返工率、报废率 |
| **QT-007** | **GMP合规映射表** | **P2** | ⏳ | **质检项对标《药品生产质量管理规范》条款** |
| QT-004 | 质检异常趋势分析 | P2 | ⏳ | 按周/月统计异常趋势 |
| QT-005 | 质检照片附件 | P2 | ⏳ | 支持上传质检现场照片 |
| QT-006 | 质检标准库 | P2 | ⏳ | 可配置质检项目和标准 |

---

## 三、数据库设计

### 3.1 qt_inspection（质检记录表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | INTEGER | PK, AUTOINCREMENT | 主键 |
| tenant_id | VARCHAR(32) | DEFAULT 'default' | 多租户预留 |
| task_id | INTEGER | NOT NULL | 任务 ID（逻辑关联 prod_task） |
| result | VARCHAR(20) | NOT NULL | 通过 / 让步放行 / 返工 / 报废 |
| operator_id | VARCHAR(50) | | 质检操作人 |
| remark | TEXT | | 备注/原因 |
| is_exception | INTEGER | DEFAULT 0 | 是否异常（0/1） |
| exception_reason | VARCHAR(200) | | 异常原因 |
| created_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | 质检时间 |
| updated_at | DATETIME | | 更新时间 |
| deleted | INTEGER | DEFAULT 0 | 逻辑删除 |

**索引**：`INDEX(task_id, created_at)`

> **注意**：`qt_inspection` 通过 `task_id` 逻辑关联 `prod_task`，**禁止添加外键约束**（跨模块表），应用层保证数据一致性。

### 3.2 预留表

| 表名 | 说明 | 阶段 |
|------|------|------|
| qt_inspection_attachment | 质检附件（照片） | 阶段二 |
| qt_standard | 质检标准库 | 阶段二 |
| qt_standard_item | 质检标准项 | 阶段二 |
| **qt_gmp_clause** | **GMP条款映射表** | **阶段二** |
| **qt_gmp_mapping** | **质检项与GMP条款关联表** | **阶段二** |

#### GMP合规映射表设计（QT-007）

**qt_gmp_clause（GMP条款表）**：

| 字段 | 类型 | 说明 |
|------|------|------|
| id | INTEGER PK | 主键 |
| clause_no | VARCHAR(50) | 条款编号（如"第七章第二百二十条"） |
| clause_title | VARCHAR(200) | 条款标题 |
| clause_content | TEXT | 条款内容 |
| category | VARCHAR(50) | 类别：质量控制/设备管理/生产管理 |

**qt_gmp_mapping（质检项与GMP条款关联表）**：

| 字段 | 类型 | 说明 |
|------|------|------|
| id | INTEGER PK | 主键 |
| inspection_item | VARCHAR(100) | 质检项名称 |
| gmp_clause_id | INTEGER FK | 关联的GMP条款ID |
| mapping_description | TEXT | 映射说明 |

---

## 四、接口设计

### 4.1 SPI 接口（由 dms-production 调用）

> 定义位置：`dms-common.QualityService`

```java
public interface QualityService {
    InspectionResult inspect(Long taskId, String result, String operatorId, String remark);
    InspectionResult getInspectionByTaskId(Long taskId);
}
```

#### inspect 方法实现要求

1. **参数校验**：`result` 必须是 `InspectionResultType` 枚举名之一：`PASS`、`CONCESSION`、`REWORK`、`SCRAP`。禁止接受中文字符串。
2. **创建记录**：向 `qt_inspection` 插入一条新记录
3. **构造返回值**：
   - `inspectionId`：新生成的记录 ID
   - `taskId`：入参 taskId
   - `result`：入参 result（枚举名）
   - `nextStatus`：根据 result 映射目标状态（PASS/CONCESSION→待交接，REWORK→待煎药，SCRAP→已报废）
   - `operatorId`、`remark`、`inspectedAt`
   - `isException`：PASS=0，其他=1
   - `exceptionReason`：result ≠ PASS 时填充 remark
4. **不修改 prod_task**：返回 `InspectionResult` 后，由 `dms-production` 根据 `nextStatus` 更新任务状态

> **评审修订03**：`result` 参数和 switch-case 统一使用 `InspectionResultType` 枚举名，避免中文字符串匹配失败问题。

### 4.2 REST API

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/qt/inspect` | 执行质检（前端直接调用入口，内部同样调用 Service） |
| GET | `/api/v1/qt/inspection/{taskId}` | 查询任务最新质检记录 |
| GET | `/api/v1/qt/inspections` | 分页查询质检记录（支持 taskId/result/日期范围过滤） |

### 4.3 报表接口（QT-003，阶段二）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/qt/reports/summary` | 质检汇总统计（合格率/返工率/报废率） |
| GET | `/api/v1/qt/reports/trend` | 质检趋势（按日/周/月） |
| GET | `/api/v1/qt/reports/by-hospital` | 按医院统计质检结果 |
| GET | `/api/v1/qt/reports/by-scheme` | 按煎药方案统计质检结果 |

---

## 五、核心类结构

```
cn.org.openygt.quality
├── controller
│   └── QualityController.java
├── entity
│   ├── Inspection.java
│   └── Task.java          # ⚠️ 历史债务：应删除，改为使用 SPI 或 DTO
├── mapper
│   ├── InspectionMapper.java
│   └── TaskMapper.java    # ⚠️ 历史债务：应删除
├── service
│   ├── QualityServiceImpl.java   # 实现 dms-common.QualityService SPI
│   └── InspectionQueryService.java
└── dto
    └── InspectionQueryRequest.java
```

### 历史债务修正计划

| 问题 | 影响 | 修正方案 |
|------|------|---------|
| `Task.java` 实体映射 `task` 表 | 表名错误，数据不一致 | 删除 `Task.java` 和 `TaskMapper.java`，质检如需任务信息通过 `TaskService` SPI 或 REST API 获取 |
| `dms-quality` 内部直接引用 `prod_task` | 跨模块数据访问违规 | 统一改为逻辑关联（仅存储 task_id），不定义实体 |

---

## 六、核心流程

### 6.1 质检执行流程

```
dms-production 调用 QualityService.inspect(taskId, result, operatorId, remark)
    ↓
参数校验（result 合法性）
    ↓
创建 Inspection 实体
    ↓
inspectionMapper.insert(inspection)
    ↓
构造 InspectionResult
    ↓
返回给 dms-production
    ↓
dms-production 根据 InspectionResult.nextStatus 更新 prod_task.status
```

### 6.2 质检记录查询流程

```
GET /api/v1/qt/inspection/{taskId}
    ↓
按 task_id 查询 qt_inspection，取最新一条
    ↓
返回 ApiResponse<InspectionResult>
```

---

## 七、开发规范

1. **禁止直接操作 prod_task**：`dms-quality` 只能读写 `qt_` 前缀表，任务状态更新必须由 `dms-production` 完成。
2. **SPI 实现幂等**：同一 taskId 的多次质检应生成多条记录（历史追溯需求），但返回的最新结果应基于最新记录。
3. **结果枚举值严格匹配**：统一使用 `InspectionResultType` 枚举名（`PASS`, `CONCESSION`, `REWORK`, `SCRAP`），禁止使用中文字符串。
4. **报表查询性能**：质检报表如需关联任务信息，**禁止直接写联表 SQL**，必须通过 `ProductionQueryService` SPI 获取任务数据。
5. **数据加密（P2）**：质检备注中可能包含敏感信息，需遵循 `dms-system` 的 AES 加密策略。

---

## 八、集成检查清单

- [ ] `QualityServiceImpl` 完整实现了 `dms-common.QualityService` 接口
- [ ] 所有 `qt_` 表实体继承 `BaseEntity`
- [ ] 未直接引用 `dms-production` 的 Mapper 或 Entity（历史债务已清理）
- [ ] `inspect` 方法返回的 `InspectionResult.nextStatus` 与 `TaskStatus` 枚举匹配
- [ ] 质检记录查询支持分页和过滤
- [ ] `mvn test` 单测通过
