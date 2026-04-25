# dms-quality 质量追溯模块详细设计

> 模块：dms-quality（质量追溯模块）  
> 版本：V1.4（专家评审04后修订版）  
> 文档状态：待开发（当前代码存在历史债务，需按本设计执行重构）  
> 适用范围：`cn.org.openygt.quality` 包及 `dms-common` 中 `QualityService` SPI 实现  
> 依赖：`dms-common`（SPI 接口、`InspectionResultType`、`ProductionQueryService`、`ProdTaskDTO`）  

---

## 一、模块概述

`dms-quality` 是煎药室管理系统（openygt-dms）的质量追溯模块，负责在生产流程的"待质检"环节提供质检判定服务，并持久化质检记录。模块边界如下：

- **负责范围**：`qt_` 前缀表的管理（`qt_inspection`、`qt_gmp_clause`、`qt_gmp_mapping` 等），实现 `dms-common.QualityService` SPI。
- **禁止行为**：
  - ❌ 不直接修改 `prod_task` 的状态（由 `dms-production` 调用 SPI 后自行更新）。
  - ❌ 不管理处方、设备、打印等业务数据。
  - ❌ 不写跨模块联表 SQL（如 `JOIN prod_task`），反查任务信息必须通过 `ProductionQueryService` SPI。

**架构位置**：
```
dms-production ──► QualityService.inspect() ──► dms-quality
                     (创建 qt_inspection 记录)
                     
dms-quality ──► ProductionQueryService.getTaskById() ──► dms-production
                     (只读反查任务状态)
```

---

## 二、代码现状 vs 目标差距

### 2.1 现状清单（必须如实反映）

| 文件 | 现状说明 |
|------|----------|
| `QualityServiceImpl.java` | 已实现 `inspect()` 和 `getInspectionByTaskId()`，逻辑基本可用 |
| `Task.java` + `TaskMapper.java` | **致命问题**：`dms-quality` 内部重复定义了 `Task` 实体，注解 `@TableName("task")`，但 V4 迁移后实际表名为 `prod_task`，**启动即报错**（表不存在） |
| `QualityServiceImpl.inspect()` | 使用 `String result` 中文字符串做 `switch-case`（"通过"/"让步放行"/"返工"/"报废"），存在空格/编码风险 |
| `QualityServiceImpl.inspect()` | 当前代码通过 `taskMapper.updateById(task)` **试图修改 task 表**（因表名错误实际未生效到 `prod_task`） |
| `qt_inspection` 表 | 当前缺少 `is_exception`、`exception_reason` 字段（与需求概设不符） |
| `Inspection.java` | 缺少 `isException`、`exceptionReason` 字段映射 |

### 2.2 核心差距分析

#### GAP-1：Task 实体致命缺陷（P0）

**问题**：`dms-quality` 内定义了 `cn.org.openygt.quality.entity.Task`，映射表 `task`：

```java
@Data
@TableName("task")
public class Task { ... }
```

V4 迁移脚本 `V4__module_split.sql` 已将 `task` 表重命名为 `prod_task`：

```sql
ALTER TABLE task RENAME TO prod_task;
```

因此启动时 MyBatis-Plus 会尝试访问不存在的 `task` 表，导致：**应用启动失败**。

**根因**：跨模块数据访问违规。`prod_task` 属于 `dms-production` 模块，不应在 `dms-quality` 中定义实体。

**修正方案**：
1. **删除** `Task.java` 和 `TaskMapper.java`。
2. 质检如需任务信息（如校验当前状态是否为"待质检"），改为注入 `ProductionQueryService` SPI，调用 `getTaskById(taskId)` 获取 `ProdTaskDTO`。
3. `dms-quality` 只保留 `qt_inspection` 的读写权限。

#### GAP-2：中文字符串 switch-case（P0）

**问题**：当前代码：

```java
switch (result) {
    case "通过": ...
    case "让步放行": ...
    case "返工": ...
    case "报废": ...
}
```

风险：前端传参若包含全角空格、编码差异（如 UTF-8 BOM）或大小写不一致，会导致 `default` 分支命中，抛出非法参数异常。

**修正方案**：统一使用 `InspectionResultType` 枚举（`PASS`/`CONCESSION`/`REWORK`/`SCRAP`），彻底消除字符串匹配风险。

#### GAP-3：模块边界混淆（P1）

**问题**：当前 `QualityServiceImpl` 在 `inspect()` 内部修改 `task` 表的状态、异常标记、操作人。根据模块边界设计，`prod_task` 的状态变更应由 `dms-production` 负责。

**修正方案**：`QualityServiceImpl` 仅执行以下操作：
1. 参数校验。
2. 向 `qt_inspection` 插入质检记录。
3. 构造并返回 `InspectionResult`（含 `nextStatus`、`isException` 等字段）。
4. **不执行任何 `UPDATE prod_task` 操作**。

---

## 三、数据库详细设计

### 3.1 qt_inspection（质检记录表）

#### DDL

```sql
-- 当前表结构（V4 已创建）
-- 需补充 is_exception / exception_reason 字段

CREATE TABLE IF NOT EXISTS qt_inspection (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    tenant_id VARCHAR(32) DEFAULT 'default',
    task_id INTEGER NOT NULL,
    result VARCHAR(20) NOT NULL,              -- PASS / CONCESSION / REWORK / SCRAP
    operator_id VARCHAR(50),
    remark TEXT,
    is_exception INTEGER DEFAULT 0,           -- 0=正常, 1=异常（CONCESSION/REWORK/SCRAP 均置 1）
    exception_reason VARCHAR(200),            -- 异常原因（非 PASS 时填充 remark）
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

-- 若从旧版本迁移，执行 ALTER：
-- ALTER TABLE qt_inspection ADD COLUMN is_exception INTEGER DEFAULT 0;
-- ALTER TABLE qt_inspection ADD COLUMN exception_reason VARCHAR(200);
```

#### 字段说明

| 字段名 | 类型 | 可空 | 默认值 | 说明 |
|--------|------|------|--------|------|
| `id` | INTEGER | NO | AUTOINCREMENT | 主键 |
| `tenant_id` | VARCHAR(32) | NO | 'default' | 多租户预留 |
| `task_id` | INTEGER | NO | — | 关联 `prod_task.id`，**逻辑关联，无外键约束** |
| `result` | VARCHAR(20) | NO | — | 枚举名：`PASS`/`CONCESSION`/`REWORK`/`SCRAP` |
| `operator_id` | VARCHAR(50) | YES | NULL | 质检操作人 ID |
| `remark` | TEXT | YES | NULL | 备注/原因 |
| `is_exception` | INTEGER | NO | 0 | 是否异常：0=正常，1=异常 |
| `exception_reason` | VARCHAR(200) | YES | NULL | 异常原因摘要 |
| `created_at` | DATETIME | NO | CURRENT_TIMESTAMP | 创建时间（即质检时间） |
| `updated_at` | DATETIME | NO | CURRENT_TIMESTAMP | 更新时间 |
| `deleted` | INTEGER | NO | 0 | 逻辑删除（0=未删除，1=已删除） |

#### 索引设计

```sql
-- 按任务查询最新质检记录（最频繁）
CREATE INDEX idx_qt_inspection_task_created 
    ON qt_inspection(task_id, created_at DESC);

-- 按结果统计（报表场景）
CREATE INDEX idx_qt_inspection_result_created 
    ON qt_inspection(result, created_at);

-- 按租户隔离（多租户场景）
CREATE INDEX idx_qt_inspection_tenant 
    ON qt_inspection(tenant_id);
```

**约束说明**：
- `task_id` 与 `prod_task.id` 为**逻辑关联**，禁止添加 `FOREIGN KEY`（跨模块表，避免强耦合）。
- `result` 列存储枚举名（大写英文），非中文标签，便于索引和统计。

### 3.2 qt_gmp_clause（GMP 条款表）【预留】

> **需求编号**：QT-007  
> **阶段**：阶段二（V2.0）  
> **用途**：存储《药品生产质量管理规范》条款，供质检项对标。

```sql
CREATE TABLE IF NOT EXISTS qt_gmp_clause (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    tenant_id VARCHAR(32) DEFAULT 'default',
    clause_no VARCHAR(50) NOT NULL,           -- 条款编号，如"第七章第二百二十条"
    clause_title VARCHAR(200) NOT NULL,       -- 条款标题
    clause_content TEXT,                      -- 条款完整内容
    category VARCHAR(50),                     -- 类别：质量控制/设备管理/生产管理/物料管理
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

CREATE INDEX idx_qmp_clause_no ON qt_gmp_clause(clause_no);
CREATE INDEX idx_qmp_clause_category ON qt_gmp_clause(category);
```

### 3.3 qt_gmp_mapping（质检项与 GMP 条款关联表）【预留】

> **阶段**：阶段二（V2.0）  
> **用途**：建立质检项与 GMP 条款的多对多映射。

```sql
CREATE TABLE IF NOT EXISTS qt_gmp_mapping (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    tenant_id VARCHAR(32) DEFAULT 'default',
    inspection_item VARCHAR(100) NOT NULL,    -- 质检项名称，如"煎药液色泽检查"
    gmp_clause_id INTEGER NOT NULL,           -- 关联 qt_gmp_clause.id
    mapping_description TEXT,                 -- 映射说明：为何该项对应该条款
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

CREATE INDEX idx_qmp_mapping_item ON qt_gmp_mapping(inspection_item);
CREATE INDEX idx_qmp_mapping_clause ON qt_gmp_mapping(gmp_clause_id);
```

---

## 四、实体类详细设计

### 4.1 实体类总览

| 实体 | 文件路径 | 状态 | 说明 |
|------|----------|------|------|
| `Inspection` | `.../quality/entity/Inspection.java` | **修改** | 补充 `isException`、`exceptionReason` 字段 |
| `Task` | `.../quality/entity/Task.java` | **删除** | 跨模块实体，映射错误表名 |
| `GmpClause` | `.../quality/entity/GmpClause.java` | **预留新增** | 阶段二实现 |
| `GmpMapping` | `.../quality/entity/GmpMapping.java` | **预留新增** | 阶段二实现 |

### 4.2 Inspection.java（修改后）

**文件**：`dms-quality/src/main/java/cn/org/openygt/quality/entity/Inspection.java`

```java
package cn.org.openygt.quality.entity;

import cn.org.openygt.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 质检记录实体，对应表 {@code qt_inspection}。
 *
 * <p>评审修订：补充 {@code isException}、{@code exceptionReason} 字段，
 * 与 {@link cn.org.openygt.common.enums.InspectionResultType} 配套使用。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("qt_inspection")
public class Inspection extends BaseEntity {

    private Long taskId;

    /** 质检结果枚举名：PASS / CONCESSION / REWORK / SCRAP */
    private String result;

    private String operatorId;

    private String remark;

    /** 是否异常：0=正常，1=异常 */
    private Integer isException;

    /** 异常原因摘要 */
    private String exceptionReason;
}
```

### 4.3 Task.java / TaskMapper.java（删除说明）

**删除文件**：
1. `dms-quality/src/main/java/cn/org/openygt/quality/entity/Task.java`
2. `dms-quality/src/main/java/cn/org/openygt/quality/mapper/TaskMapper.java`

**删除原因**：
- `Task` 实体属于 `dms-production` 模块，映射表为 `prod_task`。
- `dms-quality` 如需获取任务信息，应通过 `ProductionQueryService` SPI 调用 `getTaskById(Long)` 获取 `ProdTaskDTO`。
- 保留该实体违反模块边界，且表名错误导致启动失败。

**替代方式**：

```java
// 旧方式（删除）
Task task = taskMapper.selectById(taskId);

// 新方式
ProdTaskDTO task = productionQueryService.getTaskById(taskId);
```

---

## 五、SPI 实现设计

### 5.1 接口定义（dms-common 权威来源）

**文件**：`dms-common/src/main/java/cn/org/openygt/common/service/QualityService.java`

```java
package cn.org.openygt.common.service;

import cn.org.openygt.common.dto.InspectionResult;
import cn.org.openygt.common.enums.InspectionResultType;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 质量追溯模块对外服务接口。
 * 定义在 dms-common，由 dms-quality 实现。
 * dms-production 通过注入此接口提交质检请求。
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
     * @throws IllegalArgumentException taskId 不存在或 result 为 null
     * @throws IllegalStateException    任务不在待质检状态
     */
    InspectionResult inspect(Long taskId, InspectionResultType result, String operatorId, String remark);

    /**
     * 查询任务最新的质检记录。
     *
     * @param taskId 任务 ID
     * @return 质检结果，找不到返回 null
     */
    InspectionResult getInspectionByTaskId(Long taskId);

    /**
     * 质检汇总统计（评审03扩展）。
     *
     * @param startDate 开始日期（含）
     * @param endDate   结束日期（含）
     * @return 合格率/返工率/报废率汇总
     */
    InspectionSummaryDTO getInspectionSummary(Date startDate, Date endDate);

    /**
     * 质检趋势分析（评审03扩展）。
     *
     * @param groupBy 分组维度：DAY / WEEK / MONTH
     * @return 趋势数据列表
     */
    List<InspectionTrendDTO> getInspectionTrend(String groupBy);
}
```

### 5.2 QualityServiceImpl 完整实现代码

**文件**：`dms-quality/src/main/java/cn/org/openygt/quality/service/QualityServiceImpl.java`

```java
package cn.org.openygt.quality.service;

import cn.org.openygt.common.dto.InspectionResult;
import cn.org.openygt.common.dto.ProdTaskDTO;
import cn.org.openygt.common.enums.InspectionResultType;
import cn.org.openygt.common.enums.TaskStatus;
import cn.org.openygt.common.service.ProductionQueryService;
import cn.org.openygt.common.service.QualityService;
import cn.org.openygt.quality.entity.Inspection;
import cn.org.openygt.quality.mapper.InspectionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * QualityService SPI 实现。
 *
 * <p><strong>关键变更（V1.4）</strong>：
 * <ul>
 *   <li>删除 TaskMapper / Task 实体依赖，改为 ProductionQueryService SPI 获取任务信息</li>
 *   <li>result 参数改为 InspectionResultType 枚举，消除中文字符串 switch-case</li>
 *   <li>不再修改 prod_task，仅创建 qt_inspection 记录并返回 nextStatus</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QualityServiceImpl implements QualityService {

    private final ProductionQueryService productionQueryService;
    private final InspectionMapper inspectionMapper;

    @Override
    @Transactional
    public InspectionResult inspect(Long taskId, InspectionResultType result, String operatorId, String remark) {
        if (taskId == null || result == null) {
            throw new IllegalArgumentException("taskId 和 result 不能为空");
        }

        // 通过 SPI 获取任务信息（只读），校验状态
        ProdTaskDTO task = productionQueryService.getTaskById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在: taskId=" + taskId);
        }
        // 注：当前版本 prod_task.status 存储中文状态值，使用 getLabel() 比对。
        // V6 迁移后将统一改为枚举名存储，此处需同步改为 name() 比对。
        if (!TaskStatus.WAIT_QC.getLabel().equals(task.getStatus())) {
            throw new IllegalStateException("任务不在待质检状态，当前状态: " + task.getStatus());
        }

        // 枚举驱动状态映射
        // 注：nextStatus 当前返回中文标签（与 prod_task.status 存储格式一致）。
        // V6 迁移后统一改为枚举名，届时同步改为 TaskStatus.XXX.name()。
        String nextStatus;
        Integer isException = 0;
        String exceptionReason = null;

        switch (result) {
            case PASS:
                nextStatus = TaskStatus.WAIT_HANDOVER.getLabel();
                break;
            case CONCESSION:
                nextStatus = TaskStatus.WAIT_HANDOVER.getLabel();
                isException = 1;
                exceptionReason = remark;
                break;
            case REWORK:
                nextStatus = TaskStatus.WAIT_DECOCT.getLabel();
                isException = 1;
                exceptionReason = remark;
                break;
            case SCRAP:
                nextStatus = TaskStatus.SCRAPPED.getLabel();
                isException = 1;
                exceptionReason = remark;
                break;
            default:
                // 理论上不会到达，防御性编程
                throw new IllegalArgumentException("未知的质检结果枚举: " + result);
        }

        // 写入质检记录（本模块唯一职责）
        Inspection inspection = new Inspection();
        inspection.setTaskId(taskId);
        inspection.setResult(result.name());
        inspection.setOperatorId(operatorId);
        inspection.setRemark(remark);
        inspection.setIsException(isException);
        inspection.setExceptionReason(exceptionReason);
        inspectionMapper.insert(inspection);

        // 构造返回结果
        InspectionResult ir = new InspectionResult();
        ir.setInspectionId(inspection.getId());
        ir.setTaskId(taskId);
        ir.setResult(result.name());
        ir.setNextStatus(nextStatus);
        ir.setOperatorId(operatorId);
        ir.setRemark(remark);
        ir.setInspectedAt(LocalDateTime.now());
        ir.setIsException(isException);
        ir.setExceptionReason(exceptionReason);

        log.info("质检完成: taskId={}, result={}, nextStatus={}, operatorId={}",
                taskId, result, nextStatus, operatorId);
        return ir;
    }

    @Override
    public InspectionResult getInspectionByTaskId(Long taskId) {
        Inspection inspection = inspectionMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Inspection>()
                        .eq(Inspection::getTaskId, taskId)
                        .orderByDesc(Inspection::getCreatedAt)
                        .last("LIMIT 1")
        );
        if (inspection == null) {
            return null;
        }
        InspectionResult ir = new InspectionResult();
        ir.setInspectionId(inspection.getId());
        ir.setTaskId(inspection.getTaskId());
        ir.setResult(inspection.getResult());
        ir.setOperatorId(inspection.getOperatorId());
        ir.setRemark(inspection.getRemark());
        ir.setInspectedAt(inspection.getCreatedAt());
        ir.setIsException(inspection.getIsException());
        ir.setExceptionReason(inspection.getExceptionReason());
        return ir;
    }

    @Override
    public InspectionSummaryDTO getInspectionSummary(Date startDate, Date endDate) {
        // TODO: 阶段二实现（QT-003）
        throw new UnsupportedOperationException("质检汇总报表功能尚未实现");
    }

    @Override
    public List<InspectionTrendDTO> getInspectionTrend(String groupBy) {
        // TODO: 阶段二实现（QT-003）
        throw new UnsupportedOperationException("质检趋势分析功能尚未实现");
    }
}
```

### 5.3 关键设计决策

| 决策点 | 方案 | 理由 |
|--------|------|------|
| 不修改 `prod_task` | `QualityServiceImpl` 仅 INSERT `qt_inspection` | 模块边界清晰，`dms-production` 负责任务生命周期 |
| 状态校验通过 SPI | `productionQueryService.getTaskById(taskId)` | 只读反查，解耦表结构 |
| 枚举存储格式 | `result` 字段存枚举名，`nextStatus` 当前存中文标签 | `result` 已改为枚举；`nextStatus` 因下游 `prod_task.status` 仍存中文，暂保持中文。V6 统一迁移为枚举名 |
| `isException` 逻辑 | PASS=0，其余=1 | 与 `dms-production` 的 `Task.isException` 语义保持一致 |
| `nextStatus` 返回 | `InspectionResult.nextStatus` 当前为中文标签 | 保持与 `TaskStatus.getLabel()` 及 `prod_task.status` 存储格式一致。V6 统一改为枚举名 |

---

## 六、REST API 详细设计

### 6.1 API 总览

**前缀**：`/api/v1/qt`

| 方法 | 路径 | 说明 | 实现状态 |
|------|------|------|----------|
| POST | `/inspect` | 执行质检 | ✅ 已有，需适配枚举参数 |
| GET | `/inspection/{taskId}` | 查询任务最新质检记录 | ✅ 已有 |
| GET | `/inspections` | 分页查询质检记录 | ⏳ 阶段二 |
| GET | `/reports/summary` | 质检汇总统计 | ⏳ 阶段二（QT-003） |
| GET | `/reports/trend` | 质检趋势 | ⏳ 阶段二（QT-003） |
| GET | `/reports/by-hospital` | 按医院统计 | ⏳ 阶段二（QT-003） |
| GET | `/reports/by-scheme` | 按煎药方案统计 | ⏳ 阶段二（QT-003） |

### 6.2 执行质检（POST /api/v1/qt/inspect）

**修改点**：`result` 参数由中文字符串改为枚举名。

**请求**：

```http
POST /api/v1/qt/inspect?taskId=123&result=PASS&operatorId=QC001&remark= HTTP/1.1
```

**参数说明**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `taskId` | Long | 是 | 任务 ID |
| `result` | String | 是 | 枚举名：`PASS` / `CONCESSION` / `REWORK` / `SCRAP` |
| `operatorId` | String | 否 | 操作人 ID |
| `remark` | String | 否 | 备注/异常原因 |

**参数校验**：
- `result` 必须能成功 `InspectionResultType.valueOf(result)`，否则返回 400 Bad Request。

**成功响应**（200 OK）：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "inspectionId": 45,
    "taskId": 123,
    "result": "PASS",
    "nextStatus": "待交接",
    "operatorId": "QC001",
    "remark": null,
    "inspectedAt": "2026-04-25T09:06:03.000+00:00",
    "isException": 0,
    "exceptionReason": null
  }
}
```

**错误响应**：
- `400 Bad Request`：`taskId` 不存在，`result` 非法枚举值。
- `409 Conflict`：任务不在"待质检"状态。

### 6.3 查询最新质检记录（GET /api/v1/qt/inspection/{taskId}）

**请求**：

```http
GET /api/v1/qt/inspection/123 HTTP/1.1
```

**成功响应**（200 OK）：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "inspectionId": 45,
    "taskId": 123,
    "result": "PASS",
    "nextStatus": null,
    "operatorId": "QC001",
    "remark": null,
    "inspectedAt": "2026-04-25T09:06:03.000+00:00",
    "isException": 0,
    "exceptionReason": null
  }
}
```

**无记录响应**：`data` 为 `null`。

### 6.4 分页查询质检记录（GET /api/v1/qt/inspections）【阶段二】

**请求**：

```http
GET /api/v1/qt/inspections?page=1&size=20&taskId=123&result=PASS&startDate=2026-04-01&endDate=2026-04-25 HTTP/1.1
```

---

## 七、质检报表设计

### 7.1 设计原则

> **评审修订03核心约束**：质检报表如需关联任务信息（如医院名称、煎药方案），**禁止直接写跨模块 SQL**（如 `JOIN prod_task`、`JOIN md_hospital`），必须通过 `ProductionQueryService` SPI 获取任务数据，在内存中聚合。

**正确方式**：
```
qt_inspection (本模块) ──► 获取 taskId 列表
                                    │
                                    ▼
               ProductionQueryService.getTasksByIds(taskIds)
                                    │
                                    ▼
                              ProdTaskDTO 列表（含 prescriptionId）
                                    │
                                    ▼
                         内存聚合（按 status / hospital / scheme 分组统计）
```

**错误方式**：
```sql
-- ❌ 禁止在 QualityMapper 中直接写此类 SQL
SELECT i.*, t.status, p.hospital_name
FROM qt_inspection i
JOIN prod_task t ON i.task_id = t.id
JOIN prod_prescription p ON t.prescription_id = p.id
```

### 7.2 质检汇总统计（getInspectionSummary）

**数据来源**：
1. 从 `qt_inspection` 查询时间范围内的所有记录。
2. 提取 `taskId` 列表，调用 `productionQueryService.getTasksByIds()` 批量获取任务信息。
3. 在 `QualityServiceImpl` 或专用的 `InspectionReportService` 中内存聚合。

**统计维度**：

| 指标 | 计算方式 |
|------|----------|
| 总质检数 | `COUNT(*)` |
| 合格数 | `result = 'PASS'` 的数量 |
| 让步放行数 | `result = 'CONCESSION'` 的数量 |
| 返工数 | `result = 'REWORK'` 的数量 |
| 报废数 | `result = 'SCRAP'` 的数量 |
| 合格率 | `PASS / 总数 * 100%` |
| 返工率 | `REWORK / 总数 * 100%` |
| 报废率 | `SCRAP / 总数 * 100%` |

**DTO 定义（建议置于 dms-common 或 dms-quality）**：

```java
@Data
public class InspectionSummaryDTO {
    private Long totalCount;
    private Long passCount;
    private Long concessionCount;
    private Long reworkCount;
    private Long scrapCount;
    private BigDecimal passRate;
    private BigDecimal reworkRate;
    private BigDecimal scrapRate;
    private Date startDate;
    private Date endDate;
}
```

### 7.3 质检趋势分析（getInspectionTrend）

**分组维度**：
- `DAY`：按日分组（`YYYY-MM-DD`）
- `WEEK`：按周分组（`YYYY-Wxx`）
- `MONTH`：按月分组（`YYYY-MM`）

**实现策略**：
1. 从 `qt_inspection` 按时间范围查询。
2. 在内存中按 `created_at` 的日期部分分组。
3. 每组内分别统计 PASS / CONCESSION / REWORK / SCRAP 的数量。

```java
@Data
public class InspectionTrendDTO {
    private String period;           // 如 "2026-04-25" 或 "2026-W17"
    private Long totalCount;
    private Long passCount;
    private Long concessionCount;
    private Long reworkCount;
    private Long scrapCount;
    private BigDecimal passRate;
}
```

### 7.4 按医院/方案统计

**实现策略**：
1. 从 `qt_inspection` 获取记录及 `taskId`。
2. 通过 `ProductionQueryService.getTasksByIds()` 获取 `ProdTaskDTO` 列表（含 `prescriptionId`、`schemeId`）。
3. 若需医院名称，再通过 `prescriptionId` 调用 `dms-masterdata` 提供的 SPI（或扩展 `ProductionQueryService` 返回）。
4. 在内存中按医院/方案维度分组聚合。

---

## 八、GMP 合规映射预留设计

### 8.1 设计目标

支持将质检项与《药品生产质量管理规范》（GMP）条款进行映射，满足监管审计要求。

### 8.2 预留实体类

**GmpClause.java**（阶段二实现）：

```java
package cn.org.openygt.quality.entity;

import cn.org.openygt.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("qt_gmp_clause")
public class GmpClause extends BaseEntity {
    private String clauseNo;        // 条款编号
    private String clauseTitle;     // 条款标题
    private String clauseContent;   // 条款内容
    private String category;        // 类别
}
```

**GmpMapping.java**（阶段二实现）：

```java
package cn.org.openygt.quality.entity;

import cn.org.openygt.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("qt_gmp_mapping")
public class GmpMapping extends BaseEntity {
    private String inspectionItem;      // 质检项名称
    private Long gmpClauseId;           // 关联 GMP 条款 ID
    private String mappingDescription;  // 映射说明
}
```

### 8.3 预留 Mapper

```java
package cn.org.openygt.quality.mapper;

import cn.org.openygt.quality.entity.GmpClause;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

public interface GmpClauseMapper extends BaseMapper<GmpClause> {
}
```

```java
package cn.org.openygt.quality.mapper;

import cn.org.openygt.quality.entity.GmpMapping;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

public interface GmpMappingMapper extends BaseMapper<GmpMapping> {
}
```

### 8.4 预留 API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/qt/gmp-clauses` | 查询 GMP 条款列表 |
| POST | `/api/v1/qt/gmp-clauses` | 新增 GMP 条款 |
| GET | `/api/v1/qt/gmp-mappings` | 查询质检项与 GMP 映射关系 |
| POST | `/api/v1/qt/gmp-mappings` | 新增映射关系 |

---

## 九、异常处理策略

### 9.1 异常体系

`dms-quality` 作为业务模块，**不定义新的业务异常类**，统一使用 JDK 标准异常，由 `dms-common.GlobalExceptionHandler` 统一映射为 HTTP 响应。

| 异常类 | 使用场景 | HTTP 状态码 |
|--------|----------|-------------|
| `IllegalArgumentException` | 参数非法、任务不存在、枚举值无法解析 | 400 Bad Request |
| `IllegalStateException` | 任务状态冲突（非待质检） | 409 Conflict |
| `UnsupportedOperationException` | 阶段二功能尚未实现 | 501 Not Implemented |

### 9.2 异常映射示例

```java
// 参数校验
taskId == null || result == null
    → IllegalArgumentException("taskId 和 result 不能为空") → 400

// 任务不存在
productionQueryService.getTaskById(taskId) == null
    → IllegalArgumentException("任务不存在: taskId=" + taskId) → 400

// 状态冲突
!TaskStatus.WAIT_QC.getLabel().equals(task.getStatus())
    → IllegalStateException("任务不在待质检状态，当前状态: " + task.getStatus()) → 409

// 非法枚举值（Controller 层参数绑定失败）
InspectionResultType.valueOf("通过")  // 中文无法匹配
    → IllegalArgumentException("No enum constant ...") → 400
```

### 9.3 Controller 层枚举绑定

为便于 REST 层接收枚举字符串，Spring 会自动尝试 `InspectionResultType.valueOf(result)`。若前端传入非法值，Spring 默认抛出 `MethodArgumentTypeMismatchException`，需确保 `GlobalExceptionHandler` 将其映射为 400。

建议 Controller 参数类型保持 `String`，在 Service 层显式转换，以便给出更友好的错误信息：

```java
// QualityController
@PostMapping("/inspect")
public ApiResponse<InspectionResult> inspect(
        @RequestParam @NotNull Long taskId,
        @RequestParam @NotBlank String result,   // 先以 String 接收
        @RequestParam(required = false) String operatorId,
        @RequestParam(required = false) String remark) {
    // 显式转换，捕获异常并包装
    InspectionResultType resultType;
    try {
        resultType = InspectionResultType.valueOf(result.toUpperCase());
    } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException("非法的质检结果枚举值: " + result + "，可选值: PASS, CONCESSION, REWORK, SCRAP");
    }
    return ApiResponse.success(qualityService.inspect(taskId, resultType, operatorId, remark));
}
```

---

## 十、单元测试策略

### 10.1 测试目录结构

```
dms-quality/src/test/java/cn/org/openygt/quality
├── service
│   └── QualityServiceImplTest.java
├── mapper
│   └── InspectionMapperTest.java
└── controller
    └── QualityControllerTest.java
```

### 10.2 核心测试用例

#### QualityServiceImplTest

| 用例 | 输入 | 期望结果 | 说明 |
|------|------|----------|------|
| `inspect_pass` | taskId=1, result=PASS, operatorId=QC001 | 返回 nextStatus="待交接", isException=0 | 正常通过 |
| `inspect_concession` | taskId=1, result=CONCESSION, remark="颜色偏差" | 返回 nextStatus="待交接", isException=1, exceptionReason="颜色偏差" | 让步放行 |
| `inspect_rework` | taskId=1, result=REWORK, remark="浓度不足" | 返回 nextStatus="待煎药", isException=1 | 返工 |
| `inspect_scrap` | taskId=1, result=SCRAP, remark="污染" | 返回 nextStatus="已报废", isException=1 | 报废 |
| `inspect_taskNotFound` | taskId=9999, result=PASS | 抛出 IllegalArgumentException | SPI 返回 null |
| `inspect_statusMismatch` | taskId=1（状态=已完成）, result=PASS | 抛出 IllegalStateException | 状态校验 |
| `inspect_nullResult` | taskId=1, result=null | 抛出 IllegalArgumentException | 参数校验 |
| `getInspectionByTaskId_exists` | taskId=1 | 返回最新一条记录 | 查询 |
| `getInspectionByTaskId_notExists` | taskId=9999 | 返回 null | 无记录 |

#### 测试依赖处理

`ProductionQueryService` 使用 Mockito 模拟：

```java
@ExtendWith(MockitoExtension.class)
class QualityServiceImplTest {

    @Mock
    private ProductionQueryService productionQueryService;

    @Mock
    private InspectionMapper inspectionMapper;

    @InjectMocks
    private QualityServiceImpl qualityService;

    @Test
    void inspect_pass() {
        // 模拟 SPI 返回任务
        ProdTaskDTO task = new ProdTaskDTO();
        task.setId(1L);
        task.setStatus("待质检");
        when(productionQueryService.getTaskById(1L)).thenReturn(task);

        // 执行
        InspectionResult result = qualityService.inspect(1L, InspectionResultType.PASS, "QC001", null);

        // 验证
        assertEquals("待交接", result.getNextStatus());
        assertEquals(0, result.getIsException());
        verify(inspectionMapper).insert(any(Inspection.class));
    }
}
```

### 10.3 覆盖率要求

- `QualityServiceImpl`：行覆盖率 ≥ 85%，分支覆盖率 ≥ 80%。
- `QualityController`：至少覆盖 200/400/409 三种 HTTP 状态码路径。

---

## 十一、迁移路径

### 11.1 迁移顺序（必须严格按序执行）

```
Step 1: 修改 dms-common（新增 InspectionResultType、ProductionQueryService、ProdTaskDTO）
        → mvn clean compile（dms-common 通过）

Step 2: 修改 dms-production（实现 ProductionQueryServiceImpl）
        → mvn clean compile（dms-production 通过）

Step 3: 修改 dms-quality（本模块重构）
        ├── 3.1 删除 Task.java / TaskMapper.java
        ├── 3.2 修改 Inspection.java（补充 isException / exceptionReason）
        ├── 3.3 修改 QualityServiceImpl.java（改为 SPI 调用 + 枚举）
        ├── 3.4 修改 QualityController.java（适配枚举参数）
        └── 3.5 数据库 ALTER TABLE（补充字段）
        → mvn clean compile（dms-quality 通过）

Step 4: 全量编译验证
        → cd /data2/docker/decoction/openygt-dms && mvn clean compile

Step 5: 运行单测
        → mvn test -pl dms-quality
```

### 11.2 文件变更清单

#### 删除文件（2 个）

| # | 文件路径 | 删除原因 |
|---|----------|----------|
| 1 | `dms-quality/.../entity/Task.java` | 跨模块实体，映射错误表名 `task`（应为 `prod_task`） |
| 2 | `dms-quality/.../mapper/TaskMapper.java` | 随 Task.java 一并删除 |

#### 修改文件（4 个）

| # | 文件路径 | 修改内容 |
|---|----------|----------|
| 1 | `dms-quality/.../entity/Inspection.java` | 新增 `isException`、`exceptionReason` 字段 |
| 2 | `dms-quality/.../service/QualityServiceImpl.java` | ① 删除 `TaskMapper` 依赖，注入 `ProductionQueryService`；② `result` 参数改为 `InspectionResultType`；③ switch-case 改为枚举；④ 删除 `taskMapper.updateById`；⑤ 新增 `getInspectionSummary` / `getInspectionTrend` 占位 |
| 3 | `dms-quality/.../controller/QualityController.java` | `result` 参数接收逻辑：支持 `String` → `InspectionResultType.valueOf()` 转换，并给出友好错误提示 |
| 4 | `dms-common/.../service/QualityService.java` | 新增 `getInspectionSummary`、`getInspectionTrend` 方法声明（如尚未添加） |

#### 新增文件（阶段二，5 个）

| # | 文件路径 | 说明 |
|---|----------|------|
| 1 | `dms-quality/.../entity/GmpClause.java` | GMP 条款实体 |
| 2 | `dms-quality/.../entity/GmpMapping.java` | GMP 映射实体 |
| 3 | `dms-quality/.../mapper/GmpClauseMapper.java` | GMP 条款 Mapper |
| 4 | `dms-quality/.../mapper/GmpMappingMapper.java` | GMP 映射 Mapper |
| 5 | `dms-quality/.../dto/InspectionSummaryDTO.java` | 汇总统计 DTO（或置于 dms-common） |
| 6 | `dms-quality/.../dto/InspectionTrendDTO.java` | 趋势分析 DTO（或置于 dms-common） |

#### 数据库变更（1 个 ALTER）

```sql
-- 在应用启动前执行（Flyway 新增 V5__quality_enhancement.sql）
ALTER TABLE qt_inspection ADD COLUMN is_exception INTEGER DEFAULT 0;
ALTER TABLE qt_inspection ADD COLUMN exception_reason VARCHAR(200);
CREATE INDEX idx_qt_inspection_task_created ON qt_inspection(task_id, created_at DESC);
CREATE INDEX idx_qt_inspection_result_created ON qt_inspection(result, created_at);
```

### 11.3 编译验证命令

```bash
# 1. 公共模块编译
cd /data2/docker/decoction/openygt-dms/dms-common
mvn clean compile

# 2. 生产模块编译（需先确保 ProductionQueryServiceImpl 已实现）
cd /data2/docker/decoction/openygt-dms/dms-production
mvn clean compile

# 3. 质量模块编译
cd /data2/docker/decoction/openygt-dms/dms-quality
mvn clean compile

# 4. 全量编译
cd /data2/docker/decoction/openygt-dms
mvn clean compile

# 5. 质量模块单测
mvn test -pl dms-quality
```

### 11.4 回滚预案

若迁移失败，回滚策略如下：
1. **代码回滚**：`git checkout -- dms-quality/src/main/java/.../entity/Task.java` 等已删除文件。
2. **数据库回滚**：`ALTER TABLE qt_inspection DROP COLUMN is_exception; DROP COLUMN exception_reason;`
3. **接口兼容**：若 `dms-production` 尚未适配 `InspectionResultType` 枚举，可在 `QualityServiceImpl` 中临时提供兼容逻辑（同时接受枚举名和中文标签，但需标记 `@Deprecated`）。

---

## 附录：验收检查清单

- [ ] `Task.java` 和 `TaskMapper.java` 已彻底删除，无残留引用。
- [ ] `QualityServiceImpl` 仅依赖 `ProductionQueryService` 和 `InspectionMapper`，无 `TaskMapper` 依赖。
- [ ] `inspect()` 方法参数为 `InspectionResultType` 枚举，switch-case 使用枚举常量。
- [ ] `inspect()` 不执行任何 `UPDATE prod_task` 操作，仅 INSERT `qt_inspection`。
- [ ] `qt_inspection` 表已添加 `is_exception` 和 `exception_reason` 字段。
- [ ] `Inspection.java` 实体已补充 `isException` 和 `exceptionReason` 字段。
- [ ] `QualityController` 对非法 `result` 参数返回 400，并附带可读错误信息。
- [ ] `dms-quality` 模块 `mvn test` 单测通过，行覆盖率 ≥ 80%。
- [ ] 全量 `mvn clean compile` 通过，无编译错误。
- [ ] `qt_gmp_clause` / `qt_gmp_mapping` DDL 已预留（阶段二激活）。
