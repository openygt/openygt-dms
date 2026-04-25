# dms-print 详细设计

> 模块：煎药室管理系统 — 打印中心模块  
> 包名：`cn.org.openygt.print`  
> 表前缀：`prt_`  
> API 前缀：`/api/v1/prt`  
> 版本：V1.4 详细设计  
> 关联文档：`dms-print-需求与概设.md`、`dms-common-详细设计.md`  

---

## 一、模块概述

### 1.1 职责定位

`dms-print` 是 openygt-dms 的**打印中心模块**，负责煎药标签的打印任务全生命周期管理：

1. **打印任务队列管理**：接收 `dms-production` 或其他模块通过 SPI 提交的打印请求，维护 `prt_task` 队列。
2. **打印执行与记录**：驱动打印机执行标签打印，记录每次执行结果到 `prt_record`。
3. **失败重试闭环**：对打印失败的任务进行自动/手动重试，确保重试次数不超过上限，超过后进入终态 `FAILED`。
4. **队列查询**：提供打印队列（`PENDING` / `FAILED`）查询能力，供前端或调度器使用。

### 1.2 依赖关系

```
dms-print
├── Maven 依赖
│   ├── dms-common（SPI 接口 + DTO + 枚举 + BaseEntity）
│   └── ~~dms-production~~（V1.4 必须彻底移除，历史债务）
├── SPI 调用（注入 dms-common 接口，由其他模块实现）
│   ├── EquipmentService — 打印机设备状态查询、占用/释放
│   └── ProductionQueryService — 只读查询生产任务信息（替代直接 SELECT prod_task）
└── 被依赖
    └── dms-production → PrintService（提交打印任务、重试、查询队列）
```

### 1.3 核心组件

| 组件 | 职责 |
|------|------|
| `PrintServiceImpl` | 实现 `dms-common.PrintService` SPI，核心逻辑入口 |
| `PrintController` | REST API 暴露（submit / retry / queue） |
| `PrintTaskMapper` / `PrintRecordMapper` | 本模块表数据访问 |
| `PrintTask` / `PrintRecord` | 本模块实体（继承 `BaseEntity`） |
| `PrintAsyncExecutor` | 异步打印执行器（`@Async` 或 Spring Event） |

---

## 二、代码现状 vs 目标差距

### 2.1 差距总表

| # | 差距项 | 现状 | 目标 | 优先级 | 关联需求 |
|---|--------|------|------|--------|----------|
| G1 | 跨模块直接引用 | `PrintServiceImpl` 直接 `import dms-production` 的 `TaskMapper`、`TaskStatusHistoryMapper`、`Task` 实体 | **彻底移除**，改为注入 `ProductionQueryService` SPI | P0 | 模块解耦 |
| G2 | `getPrintQueue()` 返回类型 | `Object`（实际返回 `List<PrintTask>`） | `List<PrintTaskDTO>`（定义在 `dms-common.dto`） | P0 | PRT-002 |
| G3 | 提交幂等性 | 无幂等校验，重复提交会创建多条 `prt_task` | 同一 `taskId` 重复提交返回已有任务，不创建重复记录 | P0 | PRT-001 |
| G4 | 重试上限闭环 | `retryPrint()` 未校验 `max_retry` 上限，可无限重试 | 校验 `retry_count < max_retry`，超过则终态 `FAILED` | P0 | PRT-003 |
| G5 | `prt_record` 字段缺失 | 无 `operator_id` 字段 | 新增 `operator_id`，记录触发打印/重试的操作人 | P0 | 审计追溯 |
| G6 | 打印同步阻塞 | `doPrint()` 同步模拟，立即改 `prod_task` 状态为 `PRINTED` | 异步打印：`@Async` 或 Spring Event，不阻塞调用方 | P0 | PRT-001 |
| G7 | 直接修改 `prod_task` | `doPrint()` 中直接 `taskMapper.updateById(task)` 修改 `printStatus` / `printTime` / `printDeviceId` | **禁止操作**，打印完成后通过回调或 SPI 反向通知（可选，当前由 production 轮询或自行维护） | P0 | 模块边界 |
| G8 | 状态历史侵入 | `recordHistory()` 直接写入 `prod_task_status_history` | **禁止操作**，状态历史由 `dms-production` 自行维护 | P0 | 模块边界 |
| G9 | `prt_task` 缺 `max_retry` | 实体无 `maxRetry` 字段 | 新增 `max_retry` 字段（默认 3） | P0 | PRT-003 |
| G10 | 时间字段类型 | `java.time.LocalDateTime` | `java.time.LocalDateTime`（跟随 V1.4 全局规范） | P1 | 数据一致性 |

### 2.2 致命级差距详解（含关键代码片段）

#### G1 — 跨模块 Mapper/Entity 直接引用（最严重架构违规）

**现状代码**（`PrintServiceImpl.java` 当前真实源码）：

```java
package cn.org.openygt.print.service;

import cn.org.openygt.production.entity.Task;               // ❌ 违规
import cn.org.openygt.production.entity.TaskStatusHistory;  // ❌ 违规
import cn.org.openygt.production.mapper.TaskMapper;         // ❌ 违规
import cn.org.openygt.production.mapper.TaskStatusHistoryMapper; // ❌ 违规

@Slf4j
@Service
@RequiredArgsConstructor
public class PrintServiceImpl implements PrintService {

    private final TaskMapper taskMapper;                    // ❌ 违规
    private final TaskStatusHistoryMapper historyMapper;    // ❌ 违规
    private final PrintTaskMapper printTaskMapper;
    private final PrintRecordMapper printRecordMapper;
    private final EquipmentService equipmentService;

    // ...
}
```

**危害**：
- `dms-print` 的 `pom.xml` 中声明了对 `dms-production` 的 Maven 依赖，形成循环依赖风险。
- 任何 `prod_task` 表结构变更都会直接影响 `dms-print` 的编译和运行。
- 违反了"模块只操作自己表前缀的表"的核心架构约束。

**修复方式**：
1. 从 `pom.xml` 中移除 `dms-production` 依赖。
2. 删除所有 `import cn.org.openygt.production.*` 语句。
3. 注入 `ProductionQueryService` SPI 获取任务信息（只读）。
4. 删除 `recordHistory()` 方法及 `TaskStatusHistoryMapper` 引用。
5. 删除所有直接 `taskMapper.updateById(task)` 调用。

#### G2 — `getPrintQueue()` 返回 `Object`

**现状**：
```java
@Override
public Object getPrintQueue() {  // ❌ 返回 Object，调用方无类型安全
    LambdaQueryWrapper<PrintTask> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(PrintTask::getStatus, "PENDING")
           .or().eq(PrintTask::getStatus, "FAILED");
    wrapper.orderByDesc(PrintTask::getCreatedAt);
    return printTaskMapper.selectList(wrapper);
}
```

**目标**：返回 `List<PrintTaskDTO>`，DTO 定义在 `dms-common` 中。

#### G3 — 无幂等校验

**现状**：`submitPrintTask()` 每次调用都 `new PrintTask()` 并 `insert`，重复提交产生多条记录。

**目标**：先按 `taskId` 查询，若已有未完成（非终态）记录，直接返回已有记录。

#### G4 — 重试未校验上限

**现状**：`retryPrint()` 中仅 `retryCount + 1`，无任何上限判断，可无限重试。

```java
lastPrintTask.setRetryCount((lastPrintTask.getRetryCount() != null ? lastPrintTask.getRetryCount() : 0) + 1);
// ❌ 未判断 max_retry
```

**目标**：校验 `retry_count < max_retry`，超过则状态固定为 `FAILED`，不再允许重试。

#### G6/G7 — 同步打印 + 直接修改 `prod_task`

**现状**：`doPrint()` 方法内同步完成所有操作，且直接修改 `Task.printStatus`、`Task.printTime`、`Task.printDeviceId`：

```java
private void doPrint(Task task, Long deviceId, String deviceCode, String operatorId) {
    // ...
    task.setPrintStatus("PRINTING");
    taskMapper.updateById(task);        // ❌ 直接修改 prod_task
    // 执行打印（同步模拟）
    task.setPrintStatus("PRINTED");
    task.setPrintTime(LocalDateTime.now());
    taskMapper.updateById(task);        // ❌ 直接修改 prod_task
    // ...
}
```

**目标**：
- `submitPrintTask()` / `retryPrint()` 仅完成参数校验和 `prt_task` 记录创建/更新，立即返回。
- 真正的打印动作由异步执行器完成，不阻塞调用方。
- 不再操作 `prod_task` 表，打印结果通过回调或 SPI 通知 `dms-production`（或由 production 自行轮询 `prt_task` 状态）。

---

## 三、数据库详细设计（DDL + 字段说明）

> 以下 DDL 提供 **SQLite** 和 **MySQL** 双版本。所有脚本支持幂等执行（`IF NOT EXISTS`）。

### 3.1 prt_task（打印任务表）

**字段说明**：

| 字段 | 类型 | 约束 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGINT | PK, AUTO_INCREMENT | — | 主键 |
| tenant_id | VARCHAR(32) | | 'default' | 多租户预留 |
| task_id | BIGINT | NOT NULL | — | 关联的生产任务 ID（逻辑关联 prod_task.id） |
| device_code | VARCHAR(50) | | — | 目标打印机设备编码 |
| operator_id | VARCHAR(50) | | — | 提交/重试操作人 ID |
| status | VARCHAR(20) | NOT NULL | 'PENDING' | PENDING / PRINTING / SUCCESS / FAILED / CANCELLED |
| copies | INT | | 1 | 打印份数 |
| retry_count | INT | | 0 | 已重试次数 |
| max_retry | INT | | 3 | 最大重试次数 |
| created_at | DATETIME | | CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | | CURRENT_TIMESTAMP | 更新时间 |
| deleted | INT | | 0 | 逻辑删除（0=未删除，1=已删除） |

**索引**：
- `INDEX idx_task_id(task_id)` — 幂等查询、按任务查打印记录
- `INDEX idx_status(status)` — 队列查询
- `INDEX idx_created_at(created_at)` — 队列排序

**SQLite DDL**：
```sql
CREATE TABLE IF NOT EXISTS prt_task (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    tenant_id VARCHAR(32) DEFAULT 'default',
    task_id INTEGER NOT NULL,
    device_code VARCHAR(50),
    operator_id VARCHAR(50),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    copies INTEGER DEFAULT 1,
    retry_count INTEGER DEFAULT 0,
    max_retry INTEGER DEFAULT 3,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_prt_task_task_id ON prt_task(task_id);
CREATE INDEX IF NOT EXISTS idx_prt_task_status ON prt_task(status);
CREATE INDEX IF NOT EXISTS idx_prt_task_created_at ON prt_task(created_at);
```

**MySQL DDL**：
```sql
CREATE TABLE IF NOT EXISTS prt_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(32) DEFAULT 'default',
    task_id BIGINT NOT NULL,
    device_code VARCHAR(50),
    operator_id VARCHAR(50),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    copies INT DEFAULT 1,
    retry_count INT DEFAULT 0,
    max_retry INT DEFAULT 3,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_task_id(task_id),
    INDEX idx_status(status),
    INDEX idx_created_at(created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 3.2 prt_record（打印执行记录表）

> **评审修订03**：新增 `operator_id` 字段，记录触发本次打印/重试的操作人，满足审计追溯要求。

**字段说明**：

| 字段 | 类型 | 约束 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGINT | PK, AUTO_INCREMENT | — | 主键 |
| tenant_id | VARCHAR(32) | | 'default' | 多租户预留 |
| print_task_id | BIGINT | NOT NULL, FK → prt_task(id) | — | 打印任务 ID |
| result | VARCHAR(20) | | — | SUCCESS / FAILED |
| error_message | TEXT | | — | 失败原因（成功时为空） |
| printer_code | VARCHAR(50) | | — | 实际执行打印的打印机编码 |
| operator_id | VARCHAR(50) | | — | 触发本次打印/重试的操作人 ID |
| printed_at | DATETIME | | CURRENT_TIMESTAMP | 打印时间 |
| deleted | INT | | 0 | 逻辑删除 |

**索引**：
- `INDEX idx_print_task_id(print_task_id)` — 按打印任务查记录

**SQLite DDL**：
```sql
CREATE TABLE IF NOT EXISTS prt_record (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    tenant_id VARCHAR(32) DEFAULT 'default',
    print_task_id INTEGER NOT NULL,
    result VARCHAR(20),
    error_message TEXT,
    printer_code VARCHAR(50),
    operator_id VARCHAR(50),
    printed_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_prt_record_print_task_id ON prt_record(print_task_id);
```

**MySQL DDL**：
```sql
CREATE TABLE IF NOT EXISTS prt_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(32) DEFAULT 'default',
    print_task_id BIGINT NOT NULL,
    result VARCHAR(20),
    error_message TEXT,
    printer_code VARCHAR(50),
    operator_id VARCHAR(50),
    printed_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_print_task_id(print_task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 3.3 数据库迁移脚本（prt_record 增加 operator_id）

对于已部署环境，需执行以下增量迁移：

**SQLite**：
```sql
-- SQLite 不支持直接 ADD COLUMN 带默认值（部分版本支持）
-- 若列已存在则跳过
PRAGMA foreign_keys=OFF;
ALTER TABLE prt_record ADD COLUMN operator_id VARCHAR(50);
PRAGMA foreign_keys=ON;
```

**MySQL**：
```sql
-- 幂等：仅当列不存在时才添加
SET @dbname = DATABASE();
SET @tablename = 'prt_record';
SET @columnname = 'operator_id';
SET @preparedStatement = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = @columnname) = 0,
    CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN operator_id VARCHAR(50)'),
    'SELECT 1'
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;
```

---

## 四、实体类详细设计

### 4.1 类图（文字版）

```
BaseEntity (from dms-common)
  - id: Long
  - tenantId: String
  - createdAt: LocalDateTime
  - updatedAt: LocalDateTime
  - deleted: Integer (@TableLogic)
        ↑
  ┌─────┴─────┐
  │           │
PrintTask   PrintRecord
```

### 4.2 PrintTask（打印任务实体）

**文件**：`dms-print/src/main/java/cn/org/openygt/print/entity/PrintTask.java`

**目标代码**：

```java
package cn.org.openygt.print.entity;

import cn.org.openygt.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 打印任务实体。
 * <p>
 * 对应表 {@code prt_task}，记录每一个需要打印的标签任务。
 *
 * <p>状态流转：
 * <pre>
 *   PENDING → PRINTING → SUCCESS
 *                      → FAILED（retry_count < max_retry 可重试）
 *   PENDING → CANCELLED（人工取消，PRT-008）
 * </pre>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("prt_task")
public class PrintTask extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 关联的生产任务 ID（逻辑关联 prod_task.id，无外键约束） */
    private Long taskId;

    /** 目标打印机设备编码 */
    private String deviceCode;

    /** 提交/重试操作人 ID */
    private String operatorId;

    /** 打印状态：PENDING / PRINTING / SUCCESS / FAILED / CANCELLED */
    private String status;

    /** 打印份数，默认 1 */
    private Integer copies;

    /** 已重试次数，默认 0 */
    private Integer retryCount;

    /** 最大重试次数，默认 3 */
    private Integer maxRetry;
}
```

**与现状差异**：
- 新增继承 `BaseEntity`（原无继承）。
- 新增 `maxRetry` 字段（原缺失）。
- 时间字段改为 `LocalDateTime`（由 `BaseEntity` 统一管理，原 `Date`）。

### 4.3 PrintRecord（打印执行记录实体）

**文件**：`dms-print/src/main/java/cn/org/openygt/print/entity/PrintRecord.java`

**目标代码**：

```java
package cn.org.openygt.print.entity;

import cn.org.openygt.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 打印执行记录实体。
 * <p>
 * 对应表 {@code prt_record}，记录每一次打印尝试的结果（成功或失败）。
 * <p>
 * 注意：每条记录代表一次打印尝试，一个 {@link PrintTask} 可对应多条 PrintRecord。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("prt_record")
public class PrintRecord extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 关联的打印任务 ID */
    private Long printTaskId;

    /** 打印结果：SUCCESS / FAILED */
    private String result;

    /** 失败原因（成功时为空） */
    private String errorMessage;

    /** 实际执行打印的打印机编码 */
    private String printerCode;

    /** 触发本次打印/重试的操作人 ID（评审修订03 新增） */
    private String operatorId;

    /** 打印时间（成功或失败的时间点） */
    private LocalDateTime printedAt;
}
```

**与现状差异**：
- 新增继承 `BaseEntity`。
- 新增 `operatorId` 字段（评审修订03）。
- 新增 `printerCode` 字段（记录实际执行的打印机）。
- `printedAt` 改为 `LocalDateTime`。

### 4.4 Mapper 接口

**PrintTaskMapper**：
```java
package cn.org.openygt.print.mapper;

import cn.org.openygt.print.entity.PrintTask;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PrintTaskMapper extends BaseMapper<PrintTask> {
}
```

**PrintRecordMapper**：
```java
package cn.org.openygt.print.mapper;

import cn.org.openygt.print.entity.PrintRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PrintRecordMapper extends BaseMapper<PrintRecord> {
}
```

---

## 五、SPI 实现设计（PrintServiceImpl 完整代码）

### 5.1 目标依赖结构

**`dms-print/pom.xml` 修改**：

```xml
<dependencies>
    <dependency>
        <groupId>cn.org.openygt</groupId>
        <artifactId>dms-common</artifactId>
    </dependency>
    <!-- ❌ 移除 dms-production 依赖 -->
    <!--
    <dependency>
        <groupId>cn.org.openygt</groupId>
        <artifactId>dms-production</artifactId>
    </dependency>
    -->
</dependencies>
```

**`PrintServiceImpl` 注入列表**：

| 注入项 | 来源 | 用途 |
|--------|------|------|
| `PrintTaskMapper` | 本模块 | 打印任务 CRUD |
| `PrintRecordMapper` | 本模块 | 打印记录 CRUD |
| `EquipmentService` | `dms-common` SPI，`dms-equipment` 实现 | 打印机状态查询、占用、释放 |
| `ProductionQueryService` | `dms-common` SPI，`dms-production` 实现 | 只读查询生产任务信息 |

### 5.2 PrintServiceImpl 完整目标代码

**文件**：`dms-print/src/main/java/cn/org/openygt/print/service/PrintServiceImpl.java`

```java
package cn.org.openygt.print.service;

import cn.org.openygt.common.dto.PrintTaskDTO;
import cn.org.openygt.common.dto.ProdTaskDTO;
import cn.org.openygt.common.service.EquipmentService;
import cn.org.openygt.common.service.PrintService;
import cn.org.openygt.common.service.ProductionQueryService;
import cn.org.openygt.print.entity.PrintRecord;
import cn.org.openygt.print.entity.PrintTask;
import cn.org.openygt.print.mapper.PrintRecordMapper;
import cn.org.openygt.print.mapper.PrintTaskMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 打印服务实现。
 * <p>
 * 实现 {@link PrintService} SPI，由 dms-production 注入调用。
 * <p>
 * <strong>架构约束</strong>：
 * <ul>
 *   <li>禁止引入 dms-production 的任何 Mapper / Entity</li>
 *   <li>禁止直接修改 prod_task 表</li>
 *   <li>禁止直接写入 prod_task_status_history 表</li>
 *   <li>任务信息通过 {@link ProductionQueryService} 只读查询</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PrintServiceImpl implements PrintService {

    /** 默认最大重试次数 */
    private static final int DEFAULT_MAX_RETRY = 3;

    private final PrintTaskMapper printTaskMapper;
    private final PrintRecordMapper printRecordMapper;
    private final EquipmentService equipmentService;
    private final ProductionQueryService productionQueryService;
    private final ApplicationEventPublisher eventPublisher;

    // ==================== 公开 SPI 方法 ====================

    @Override
    @Transactional
    public void submitPrintTask(Long taskId, String deviceCode, String operatorId) {
        // 1. 通过 SPI 校验任务存在性（只读，不操作 prod_task）
        ProdTaskDTO task = getTaskOrThrow(taskId);

        // 2. 幂等性检查：同一 taskId 是否已有未完成的打印任务
        PrintTask existing = findActiveByTaskId(taskId);
        if (existing != null) {
            log.info("打印任务幂等返回: taskId={}, existingPrtTaskId={}", taskId, existing.getId());
            return; // 直接返回，不创建重复记录
        }

        // 3. 校验打印机状态
        validatePrinter(deviceCode);

        // 4. 创建打印任务（状态 PENDING）
        PrintTask printTask = new PrintTask();
        printTask.setTaskId(taskId);
        printTask.setDeviceCode(deviceCode);
        printTask.setOperatorId(operatorId);
        printTask.setStatus("PENDING");
        printTask.setCopies(1); // 默认 1 份，后续可从 ProductionQueryService 扩展获取
        printTask.setRetryCount(0);
        printTask.setMaxRetry(DEFAULT_MAX_RETRY);
        printTaskMapper.insert(printTask);

        log.info("打印任务已创建: prtTaskId={}, taskId={}, deviceCode={}, operatorId={}",
                printTask.getId(), taskId, deviceCode, operatorId);

        // 5. 发布异步打印事件（不阻塞调用方）
        eventPublisher.publishEvent(new PrintTaskSubmittedEvent(this, printTask.getId(), deviceCode, operatorId));
    }

    @Override
    @Transactional
    public void retryPrint(Long taskId, String deviceCode, String operatorId) {
        // 1. 通过 SPI 校验任务存在性
        getTaskOrThrow(taskId);

        // 2. 查询该任务最新的打印记录
        PrintTask printTask = findLatestByTaskId(taskId);
        if (printTask == null) {
            throw new IllegalArgumentException("该任务无打印记录，无法重试");
        }

        // 3. 重试闭环：校验是否已达 max_retry 上限
        int currentRetry = printTask.getRetryCount() != null ? printTask.getRetryCount() : 0;
        int maxRetry = printTask.getMaxRetry() != null ? printTask.getMaxRetry() : DEFAULT_MAX_RETRY;

        if (currentRetry >= maxRetry) {
            // 超过上限，固定为 FAILED 终态
            printTask.setStatus("FAILED");
            printTaskMapper.updateById(printTask);

            // 记录终态日志
            recordPrintResult(printTask.getId(), null, "FAILED",
                    "重试次数已达上限(" + maxRetry + ")，进入终态 FAILED", operatorId);

            throw new IllegalStateException("重试次数已达上限(" + maxRetry + ")，无法继续重试");
        }

        // 4. 更新重试计数并回退为 PENDING
        printTask.setRetryCount(currentRetry + 1);
        printTask.setStatus("PENDING");
        printTask.setDeviceCode(deviceCode);
        printTask.setOperatorId(operatorId);
        printTaskMapper.updateById(printTask);

        // 5. 记录重试发起
        recordPrintResult(printTask.getId(), null, "RETRY",
                "第 " + (currentRetry + 1) + " 次重试发起", operatorId);

        log.info("打印任务重试: prtTaskId={}, taskId={}, retryCount={}/{}",
                printTask.getId(), taskId, currentRetry + 1, maxRetry);

        // 6. 发布异步打印事件
        eventPublisher.publishEvent(new PrintTaskSubmittedEvent(this, printTask.getId(), deviceCode, operatorId));
    }

    @Override
    public List<PrintTaskDTO> getPrintQueue() {
        LambdaQueryWrapper<PrintTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PrintTask::getStatus, "PENDING")
               .or().eq(PrintTask::getStatus, "FAILED");
        wrapper.orderByDesc(PrintTask::getCreatedAt);

        List<PrintTask> tasks = printTaskMapper.selectList(wrapper);
        return tasks.stream().map(this::toDTO).collect(Collectors.toList());
    }

    // ==================== 异步打印执行 ====================

    /**
     * 异步执行打印。
     * <p>
     * 由 Spring Event 触发，独立事务，失败可回滚打印记录但不影响主流程。
     *
     * @param event 打印任务提交事件
     */
    @Async("printExecutor")
    @Transactional
    public void handlePrintTaskEvent(PrintTaskSubmittedEvent event) {
        Long prtTaskId = event.getPrtTaskId();
        String deviceCode = event.getDeviceCode();
        String operatorId = event.getOperatorId();

        PrintTask printTask = printTaskMapper.selectById(prtTaskId);
        if (printTask == null || !"PENDING".equals(printTask.getStatus())) {
            log.warn("异步打印跳过: prtTaskId={} 不存在或状态非 PENDING", prtTaskId);
            return;
        }

        // 1. 占用打印机
        Long deviceId = equipmentService.getDeviceId(deviceCode);
        if (deviceId == null) {
            failPrint(printTask, deviceCode, operatorId, "打印机不存在: " + deviceCode);
            return;
        }

        try {
            equipmentService.updateDeviceStatus(deviceId, "RUNNING");

            // 2. 更新打印任务状态为 PRINTING
            printTask.setStatus("PRINTING");
            printTaskMapper.updateById(printTask);

            // 3. 执行真实打印（模拟，阶段二接入真实打印驱动）
            boolean printSuccess = executePrint(printTask, deviceCode);

            if (printSuccess) {
                // 4a. 打印成功
                printTask.setStatus("SUCCESS");
                printTaskMapper.updateById(printTask);

                recordPrintResult(prtTaskId, deviceCode, "SUCCESS", null, operatorId);
                log.info("异步打印成功: prtTaskId={}, deviceCode={}", prtTaskId, deviceCode);
            } else {
                // 4b. 打印失败（由 executePrint 内部抛异常或返回 false）
                throw new RuntimeException("打印驱动返回失败");
            }

        } catch (Exception e) {
            log.error("异步打印异常: prtTaskId={}, deviceCode={}", prtTaskId, deviceCode, e);
            failPrint(printTask, deviceCode, operatorId, e.getMessage());
        } finally {
            // 5. 无论成败，释放打印机
            try {
                equipmentService.releaseDevice(deviceId);
            } catch (Exception ex) {
                log.error("释放打印机异常: deviceId={}", deviceId, ex);
            }
        }
    }

    // ==================== 私有辅助方法 ====================

    private ProdTaskDTO getTaskOrThrow(Long taskId) {
        ProdTaskDTO task = productionQueryService.getTaskById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("生产任务不存在: taskId=" + taskId);
        }
        return task;
    }

    private PrintTask findActiveByTaskId(Long taskId) {
        LambdaQueryWrapper<PrintTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PrintTask::getTaskId, taskId)
               .in(PrintTask::getStatus, List.of("PENDING", "PRINTING"))
               .orderByDesc(PrintTask::getCreatedAt)
               .last("LIMIT 1");
        return printTaskMapper.selectOne(wrapper);
    }

    private PrintTask findLatestByTaskId(Long taskId) {
        LambdaQueryWrapper<PrintTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PrintTask::getTaskId, taskId)
               .orderByDesc(PrintTask::getCreatedAt)
               .last("LIMIT 1");
        return printTaskMapper.selectOne(wrapper);
    }

    private void validatePrinter(String deviceCode) {
        Long deviceId = equipmentService.getDeviceId(deviceCode);
        if (deviceId == null) {
            throw new IllegalStateException("打印机不存在: " + deviceCode);
        }
        String deviceStatus = equipmentService.getDeviceStatus(deviceId);
        if (!"IDLE".equalsIgnoreCase(deviceStatus)) {
            throw new IllegalStateException("打印机不是空闲状态: " + deviceCode + " status=" + deviceStatus);
        }
    }

    private void failPrint(PrintTask printTask, String deviceCode, String operatorId, String errorMessage) {
        // 状态回退为 FAILED（等待下次重试调度或人工干预）
        printTask.setStatus("FAILED");
        printTaskMapper.updateById(printTask);

        recordPrintResult(printTask.getId(), deviceCode, "FAILED", errorMessage, operatorId);
        log.warn("打印失败: prtTaskId={}, error={}", printTask.getId(), errorMessage);
    }

    private void recordPrintResult(Long printTaskId, String printerCode, String result, String errorMessage, String operatorId) {
        PrintRecord record = new PrintRecord();
        record.setPrintTaskId(printTaskId);
        record.setPrinterCode(printerCode);
        record.setResult(result);
        record.setErrorMessage(errorMessage);
        record.setOperatorId(operatorId);
        record.setPrintedAt(LocalDateTime.now());
        printRecordMapper.insert(record);
    }

    private boolean executePrint(PrintTask printTask, String deviceCode) {
        // TODO: 阶段二接入真实打印驱动（串口/USB/网络打印机）
        // 当前为模拟：随机模拟成功/失败，用于测试重试闭环
        // return printerDriver.print(labelContent, deviceCode);
        return true; // 默认模拟成功
    }

    private PrintTaskDTO toDTO(PrintTask task) {
        PrintTaskDTO dto = new PrintTaskDTO();
        dto.setId(task.getId());
        dto.setTaskId(task.getTaskId());
        dto.setDeviceCode(task.getDeviceCode());
        dto.setOperatorId(task.getOperatorId());
        dto.setStatus(task.getStatus());
        dto.setCopies(task.getCopies());
        dto.setRetryCount(task.getRetryCount());
        dto.setMaxRetry(task.getMaxRetry());
        dto.setCreatedAt(task.getCreatedAt());
        return dto;
    }
}
```

### 5.3 打印任务提交事件

**文件**：`dms-print/src/main/java/cn/org/openygt/print/event/PrintTaskSubmittedEvent.java`

```java
package cn.org.openygt.print.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 打印任务提交事件。
 * <p>
 * 由 {@link cn.org.openygt.print.service.PrintServiceImpl#submitPrintTask}
 * 或 {@code retryPrint} 发布后，由异步监听器执行真实打印。
 */
@Getter
public class PrintTaskSubmittedEvent extends ApplicationEvent {

    private final Long prtTaskId;
    private final String deviceCode;
    private final String operatorId;

    public PrintTaskSubmittedEvent(Object source, Long prtTaskId, String deviceCode, String operatorId) {
        super(source);
        this.prtTaskId = prtTaskId;
        this.deviceCode = deviceCode;
        this.operatorId = operatorId;
    }
}
```

### 5.4 事件监听器

**文件**：`dms-print/src/main/java/cn/org/openygt/print/event/PrintTaskEventListener.java`

```java
package cn.org.openygt.print.event;

import cn.org.openygt.print.service.PrintServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PrintTaskEventListener {

    private final PrintServiceImpl printServiceImpl;

    // 注：当前迭代采用同步执行，尚未引入 Spring Event 机制。
    // 阶段二如需异步化，应使用 @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    // 确保事务提交后才触发异步打印，避免读取到未提交数据。
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPrintTaskSubmitted(PrintTaskSubmittedEvent event) {
        printServiceImpl.handlePrintTaskEvent(event);
    }
}
```

### 5.5 线程池配置

**文件**：`dms-print/src/main/java/cn/org/openygt/print/config/PrintAsyncConfig.java`

```java
package cn.org.openygt.print.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class PrintAsyncConfig {

    @Bean("printExecutor")
    public Executor printExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("print-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
```

---

## 六、REST API 详细设计

### 6.1 接口清单

| 方法 | 路径 | 说明 | 请求参数 | 响应 |
|------|------|------|----------|------|
| POST | `/api/v1/prt/tasks/{taskId}/submit` | 提交打印任务 | `deviceCode`, `operatorId` | `ApiResponse<Void>` |
| POST | `/api/v1/prt/tasks/{taskId}/retry` | 重试打印任务 | `deviceCode`, `operatorId` | `ApiResponse<Void>` |
| GET | `/api/v1/prt/queue` | 查询打印队列 | — | `ApiResponse<List<PrintTaskDTO>>` |

### 6.2 PrintController 目标代码

**文件**：`dms-print/src/main/java/cn/org/openygt/print/controller/PrintController.java`

```java
package cn.org.openygt.print.controller;

import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.common.dto.PrintTaskDTO;
import cn.org.openygt.common.service.PrintService;
import cn.org.openygt.print.PrintModule;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(PrintModule.API_PREFIX)
@RequiredArgsConstructor
public class PrintController {

    private final PrintService printService;

    @PostMapping("/tasks/{taskId}/submit")
    public ApiResponse<Void> submitPrintTask(
            @PathVariable Long taskId,
            @RequestParam String deviceCode,
            @RequestParam String operatorId) {
        printService.submitPrintTask(taskId, deviceCode, operatorId);
        return ApiResponse.success();
    }

    @PostMapping("/tasks/{taskId}/retry")
    public ApiResponse<Void> retryPrint(
            @PathVariable Long taskId,
            @RequestParam String deviceCode,
            @RequestParam String operatorId) {
        printService.retryPrint(taskId, deviceCode, operatorId);
        return ApiResponse.success();
    }

    @GetMapping("/queue")
    public ApiResponse<List<PrintTaskDTO>> getPrintQueue() {
        return ApiResponse.success(printService.getPrintQueue());
    }
}
```

**与现状差异**：
- `getPrintQueue()` 返回类型从 `ApiResponse<Object>` 收紧为 `ApiResponse<List<PrintTaskDTO>>`。

### 6.3 异常映射

由 `dms-common` 的 `GlobalExceptionHandler` 统一处理：

| 异常类型 | HTTP 状态码 | 场景 |
|----------|-------------|------|
| `IllegalArgumentException` | 400 Bad Request | taskId 不存在、参数非法 |
| `IllegalStateException` | 409 Conflict | 打印机非空闲、重试次数已达上限 |
| `Exception` | 500 Internal Server Error | 未预期的系统错误 |

---

## 七、打印队列与重试机制详细设计（时序图文字版）

### 7.1 打印任务提交时序

```
dms-production (调用方)          dms-print (PrintServiceImpl)          异步执行器          EquipmentService
        |                                |                                   |                   |
        |  submitPrintTask(taskId, dev, op)                               |                   |
        |------------------------------->|                                   |                   |
        |                                |  1. ProductionQueryService.getTaskById(taskId)    |
        |                                |  2. findActiveByTaskId(taskId)  [幂等检查]          |
        |                                |  3. validatePrinter(dev)          |                   |
        |                                |  4. INSERT prt_task (PENDING)     |                   |
        |                                |  5. publish PrintTaskSubmittedEvent               |
        |                                |---------------------------------->|                   |
        |      ApiResponse.success()     |                                   |                   |
        |<-------------------------------|                                   |                   |
        |                                |                                   |  6. handlePrintTaskEvent
        |                                |                                   |  7. updateDeviceStatus(dev, RUNNING)
        |                                |                                   |------------------->|
        |                                |                                   |  8. UPDATE prt_task PRINTING
        |                                |                                   |  9. executePrint()
        |                                |                                   |  10. UPDATE prt_task SUCCESS
        |                                |                                   |  11. INSERT prt_record SUCCESS
        |                                |                                   |  12. releaseDevice(dev)
        |                                |                                   |------------------->|
```

**要点**：
- 步骤 1-5 在调用方线程同步完成，立即返回。
- 步骤 6-12 在异步线程完成，调用方无感知。
- 幂等检查在同步阶段完成，确保重复提交不会创建多条 `prt_task`。

### 7.2 打印失败与重试闭环时序

```
异步执行器                        prt_task                    prt_record
    |                                |                           |
    |  executePrint() 失败           |                           |
    |--------------------------------|                           |
    |  UPDATE status=FAILED          |                           |
    |  INSERT prt_record (FAILED)    |                           |
    |----------------------------------------------------------->|
    |                                |                           |
    |  [定时调度器 / 手动 retryPrint] |                           |
    |--------------------------------|                           |
    |  SELECT retry_count, max_retry |                           |
    |  校验: retry_count < max_retry?                           |
    |  YES: retry_count++, status=PENDING                       |
    |  INSERT prt_record (RETRY)     |                           |
    |----------------------------------------------------------->|
    |  重新发布 PrintTaskSubmittedEvent                         |
    |                                |                           |
    |  [异步执行器再次执行]            |                           |
    |  ...                           |                           |
    |  若再次失败:                   |                           |
    |  UPDATE status=FAILED          |                           |
    |  INSERT prt_record (FAILED)    |                           |
    |----------------------------------------------------------->|
    |                                |                           |
    |  [再次 retryPrint]             |                           |
    |  校验: retry_count >= max_retry                           |
    |  NO: 抛 IllegalStateException  |                           |
    |  UPDATE status=FAILED (终态)   |                           |
    |  INSERT prt_record (FAILED, "已达上限")                   |
    |----------------------------------------------------------->|
```

**重试闭环检查清单**：
- [x] 失败任务状态正确回退到 `PENDING`（`retryPrint` 中设置）。
- [x] `retry_count` 每次重试正确递增。
- [x] 超过 `max_retry` 后状态固定为 `FAILED`，不再自动重试，再次调用 `retryPrint` 抛异常。
- [x] 每次重试（无论成败）都写入 `prt_record`。
- [x] 异步执行器有异常捕获（`try-finally`），单任务失败不影响其他任务，打印机一定释放。

---

## 八、异步打印策略

### 8.1 策略选择：Spring Event + @Async

**为什么不使用 `@Async` 直接注解 SPI 方法？**
- `@Transactional` 和 `@Async` 若同时注解在同一方法上，`@Async` 会创建新线程，导致事务上下文丢失。
- 解决方案：**SPI 方法只做事务性数据操作，当前迭代采用同步执行**。
  阶段二如需异步化，应通过 `ApplicationEventPublisher` 发布事件，由 `@TransactionalEventListener(phase = AFTER_COMMIT)` + `@Async` 执行异步逻辑，确保事务提交后才触发打印。

**为什么不使用 MQ（如 RabbitMQ / Kafka）？**
- 当前系统规模下，单机线程池已足够。
- 减少外部依赖，降低部署复杂度。
- 若后续需分布式部署，可将 `ApplicationEvent` 替换为 MQ 消息，监听逻辑基本不变。

### 8.2 线程池参数

| 参数 | 值 | 说明 |
|------|-----|------|
| corePoolSize | 2 | 最少保持 2 个打印线程（煎药室通常只有 1-2 台打印机） |
| maxPoolSize | 5 | 高峰期最多 5 个并发打印任务 |
| queueCapacity | 100 | 队列缓冲，避免瞬时高峰丢任务 |
| rejectionPolicy | CallerRunsPolicy | 队列满时由调用线程（Spring 调度线程）自己执行，确保不丢任务 |

### 8.3 异步异常处理

- `handlePrintTaskEvent` 内部有完整的 `try-catch-finally`，异常不会外泄到 Spring 事件框架。
- 若 `executePrint` 抛异常，`catch` 块中：
  1. `prt_task.status` 更新为 `FAILED`。
  2. `prt_record` 写入失败记录。
  3. `finally` 块确保打印机释放。
- 异常日志通过 `log.error` 输出，后续可接入告警（PRT-009 阶段二）。

---

## 九、异常处理策略

### 9.1 异常分层

| 层级 | 异常类 | 使用场景 | 处理方 |
|------|--------|----------|--------|
| 参数校验 | `IllegalArgumentException` | taskId 不存在、printer 不存在 | Controller → 400 |
| 状态冲突 | `IllegalStateException` | 打印机非空闲、重试次数超限、已有未完成打印 | Controller → 409 |
| 打印失败 | `RuntimeException`（内部捕获） | 驱动异常、IO 异常 | 异步执行器内部捕获，记 `prt_record` |
| 系统错误 | `Exception` | 未预期错误 | GlobalExceptionHandler → 500 |

### 9.2 关键异常场景

**场景 1：幂等提交**
```
调用方 A: submitPrintTask(1001, "PRINTER_01", "OP_01")
→ 创建 prt_task #1 (PENDING)

调用方 B: submitPrintTask(1001, "PRINTER_01", "OP_02")
→ findActiveByTaskId(1001) 返回 prt_task #1
→ 直接返回，不创建新记录，不抛异常
```

**场景 2：重试次数超限**
```
prt_task #1: retry_count=3, max_retry=3, status=FAILED
调用方: retryPrint(1001, "PRINTER_01", "OP_01")
→ 校验: 3 >= 3 → 抛 IllegalStateException("重试次数已达上限(3)，无法继续重试")
→ prt_task #1 保持 FAILED（终态）
→ 写入 prt_record: result=FAILED, errorMessage="重试次数已达上限(3)，进入终态 FAILED"
```

**场景 3：异步打印中打印机离线**
```
异步执行器: handlePrintTaskEvent(prtTaskId=1)
→ validatePrinter 通过（提交时校验的）
→ updateDeviceStatus → 可能抛异常（打印机此时离线）
→ catch 块: failPrint(..., "打印机离线") 
→ prt_task.status = FAILED
→ prt_record: result=FAILED, errorMessage="打印机离线"
→ finally: releaseDevice（若 deviceId 有效则释放）
```

---

## 十、单元测试策略

### 10.1 测试分层

| 测试类型 | 框架 | 覆盖目标 | 覆盖率要求 |
|----------|------|----------|------------|
| 单元测试 | JUnit 5 + Mockito | PrintServiceImpl 各分支 | ≥ 80% |
| 集成测试 | Spring Boot Test + H2 | Controller + Service + Mapper 完整链路 | 核心流程 |

### 10.2 核心测试用例

**`PrintServiceImplTest`**（使用 Mockito，不依赖 Spring 上下文）：

```java
@ExtendWith(MockitoExtension.class)
class PrintServiceImplTest {

    @Mock PrintTaskMapper printTaskMapper;
    @Mock PrintRecordMapper printRecordMapper;
    @Mock EquipmentService equipmentService;
    @Mock ProductionQueryService productionQueryService;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks
    PrintServiceImpl printServiceImpl;

    // === submitPrintTask ===

    @Test
    void submitPrintTask_shouldCreateNewTask_whenNoActiveTaskExists() {
        when(productionQueryService.getTaskById(1L)).thenReturn(new ProdTaskDTO());
        when(printTaskMapper.selectOne(any())).thenReturn(null);
        when(equipmentService.getDeviceId("PRINTER_01")).thenReturn(10L);
        when(equipmentService.getDeviceStatus(10L)).thenReturn("IDLE");

        printServiceImpl.submitPrintTask(1L, "PRINTER_01", "OP_01");

        verify(printTaskMapper).insert(any(PrintTask.class));
        verify(eventPublisher).publishEvent(any(PrintTaskSubmittedEvent.class));
    }

    @Test
    void submitPrintTask_shouldBeIdempotent_whenActiveTaskExists() {
        when(productionQueryService.getTaskById(1L)).thenReturn(new ProdTaskDTO());
        PrintTask existing = new PrintTask();
        existing.setId(100L);
        when(printTaskMapper.selectOne(any())).thenReturn(existing);

        printServiceImpl.submitPrintTask(1L, "PRINTER_01", "OP_01");

        verify(printTaskMapper, never()).insert(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void submitPrintTask_shouldThrow_whenPrinterNotIdle() {
        when(productionQueryService.getTaskById(1L)).thenReturn(new ProdTaskDTO());
        when(printTaskMapper.selectOne(any())).thenReturn(null);
        when(equipmentService.getDeviceId("PRINTER_01")).thenReturn(10L);
        when(equipmentService.getDeviceStatus(10L)).thenReturn("RUNNING");

        assertThrows(IllegalStateException.class,
            () -> printServiceImpl.submitPrintTask(1L, "PRINTER_01", "OP_01"));
    }

    // === retryPrint ===

    @Test
    void retryPrint_shouldIncrementRetryAndSetPending_whenUnderMaxRetry() {
        when(productionQueryService.getTaskById(1L)).thenReturn(new ProdTaskDTO());
        PrintTask task = new PrintTask();
        task.setId(100L);
        task.setRetryCount(1);
        task.setMaxRetry(3);
        task.setStatus("FAILED");
        when(printTaskMapper.selectOne(any())).thenReturn(task);

        printServiceImpl.retryPrint(1L, "PRINTER_01", "OP_01");

        assertEquals(2, task.getRetryCount());
        assertEquals("PENDING", task.getStatus());
        verify(printTaskMapper).updateById(task);
        verify(printRecordMapper).insert(any(PrintRecord.class));
        verify(eventPublisher).publishEvent(any(PrintTaskSubmittedEvent.class));
    }

    @Test
    void retryPrint_shouldThrowAndSetFinalFailed_whenMaxRetryReached() {
        when(productionQueryService.getTaskById(1L)).thenReturn(new ProdTaskDTO());
        PrintTask task = new PrintTask();
        task.setId(100L);
        task.setRetryCount(3);
        task.setMaxRetry(3);
        task.setStatus("FAILED");
        when(printTaskMapper.selectOne(any())).thenReturn(task);

        assertThrows(IllegalStateException.class,
            () -> printServiceImpl.retryPrint(1L, "PRINTER_01", "OP_01"));

        assertEquals("FAILED", task.getStatus());
        verify(printTaskMapper).updateById(task);
        verify(printRecordMapper).insert(any(PrintRecord.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    // === getPrintQueue ===

    @Test
    void getPrintQueue_shouldReturnDtoList() {
        PrintTask t1 = new PrintTask();
        t1.setId(1L);
        t1.setStatus("PENDING");
        when(printTaskMapper.selectList(any())).thenReturn(List.of(t1));

        List<PrintTaskDTO> result = printServiceImpl.getPrintQueue();

        assertEquals(1, result.size());
        assertEquals("PENDING", result.get(0).getStatus());
    }
}
```

### 10.3 集成测试要点

- 使用 `@SpringBootTest` + H2 内存数据库，验证 `prt_task` / `prt_record` 真实落库。
- 验证 `@Async` 事件发布和监听的完整链路（可使用 `CountDownLatch` 等待异步完成）。
- 验证异常场景下的事务回滚行为（`prt_task` 状态应正确回退）。

---

## 十一、迁移路径

### 11.1 迁移步骤（按顺序执行）

#### Step 1：准备 dms-common 依赖（前置条件）

确保 `dms-common` 已完成以下变更（参见 `dms-common-详细设计.md`）：
- `PrintTaskDTO` 已新增。
- `ProductionQueryService` SPI 已定义。
- `PrintService.getPrintQueue()` 签名已改为 `List<PrintTaskDTO>`。
- `BaseEntity` 已支持 `LocalDateTime`。

#### Step 2：修改 `dms-print/pom.xml`

```xml
<dependencies>
    <dependency>
        <groupId>cn.org.openygt</groupId>
        <artifactId>dms-common</artifactId>
    </dependency>
    <!-- 彻底移除以下依赖 -->
    <!--
    <dependency>
        <groupId>cn.org.openygt</groupId>
        <artifactId>dms-production</artifactId>
    </dependency>
    -->
</dependencies>
```

**编译影响**：移除依赖后，`PrintServiceImpl.java` 会立即编译失败（因为还引用着 `TaskMapper` 等），这是预期行为，驱动下一步重构。

#### Step 3：删除所有 dms-production 的直接引用

在 `PrintServiceImpl.java` 中：
1. 删除以下 `import`：
   ```java
   import cn.org.openygt.production.entity.Task;
   import cn.org.openygt.production.entity.TaskStatusHistory;
   import cn.org.openygt.production.mapper.TaskMapper;
   import cn.org.openygt.production.mapper.TaskStatusHistoryMapper;
   ```
2. 删除字段注入：
   ```java
   private final TaskMapper taskMapper;                    // 删除
   private final TaskStatusHistoryMapper historyMapper;    // 删除
   ```
3. 新增注入：
   ```java
   private final ProductionQueryService productionQueryService;
   private final ApplicationEventPublisher eventPublisher;
   ```

#### Step 4：替换 `getTaskOrThrow()` 实现

**现状**：
```java
private Task getTaskOrThrow(Long taskId) {
    Task task = taskMapper.selectById(taskId);
    if (task == null) throw new IllegalArgumentException("任务不存在");
    return task;
}
```

**目标**：
```java
private ProdTaskDTO getTaskOrThrow(Long taskId) {
    ProdTaskDTO task = productionQueryService.getTaskById(taskId);
    if (task == null) throw new IllegalArgumentException("生产任务不存在: taskId=" + taskId);
    return task;
}
```

#### Step 5：删除 `recordHistory()` 方法

`dms-print` 不再负责写入 `prod_task_status_history`，该方法整段删除。

`dms-production` 如需记录打印相关的状态历史，应在调用 `PrintService` 前后自行维护。

#### Step 6：删除所有直接修改 `prod_task` 的代码

在 `doPrint()` 现状代码中，删除以下操作：
```java
// 全部删除
// task.setPrintDeviceId(deviceId);
// task.setPrintStatus("PRINTING");
// taskMapper.updateById(task);
// task.setPrintStatus("PRINTED");
// task.setPrintTime(LocalDateTime.now());
// taskMapper.updateById(task);
```

若 `dms-production` 需要感知打印完成，可选方案：
- **方案 A（推荐）**：`dms-production` 自行轮询 `ProductionQueryService` 扩展方法查询 `prt_task` 状态。
- **方案 B**：`dms-print` 在打印完成后发布 `PrintTaskCompletedEvent`，`dms-production` 订阅（需新增事件契约，当前迭代可选）。

#### Step 7：实体类改造

- `PrintTask.java`：继承 `BaseEntity`，新增 `maxRetry` 字段，删除独立 `createdAt/updatedAt/deleted`。
- `PrintRecord.java`：继承 `BaseEntity`，新增 `operatorId`、`printerCode` 字段，`printedAt` 改为 `LocalDateTime`。

#### Step 8：数据库迁移

1. 执行 `prt_record` 增加 `operator_id` 的增量 DDL（见第三章）。
2. 执行 `prt_task` 增加 `max_retry` 的增量 DDL（若已有数据需设置默认值 3）。

**SQLite 增量脚本**：
```sql
ALTER TABLE prt_task ADD COLUMN max_retry INTEGER DEFAULT 3;
UPDATE prt_task SET max_retry = 3 WHERE max_retry IS NULL;

ALTER TABLE prt_record ADD COLUMN operator_id VARCHAR(50);
ALTER TABLE prt_record ADD COLUMN printer_code VARCHAR(50);
```

#### Step 9：引入异步机制

1. 新增 `PrintAsyncConfig.java`（`@EnableAsync` + `printExecutor` Bean）。
2. 新增 `PrintTaskSubmittedEvent.java`。
3. 新增 `PrintTaskEventListener.java`。
4. 重构 `PrintServiceImpl`：SPI 方法只做校验+入库+发事件，真实打印逻辑移至 `handlePrintTaskEvent`。

#### Step 10：编译与测试

```bash
# 1. 先编译 dms-common（确保 SPI 和 DTO 已更新）
cd /data2/docker/decoction/openygt-dms && mvn clean install -pl dms-common

# 2. 编译 dms-print（此时应无 dms-production 依赖）
mvn clean compile -pl dms-print

# 3. 运行单元测试
mvn test -pl dms-print

# 4. 全量编译验证（确保移除 dms-production 依赖后，其他模块未受影响）
mvn clean compile
```

### 11.2 迁移检查清单

- [ ] `dms-print/pom.xml` 中已移除 `dms-production` 依赖。
- [ ] `PrintServiceImpl.java` 中无任何 `import cn.org.openygt.production.*` 语句。
- [ ] `PrintServiceImpl.java` 中无 `TaskMapper`、`TaskStatusHistoryMapper` 注入。
- [ ] `PrintServiceImpl.java` 中无直接修改 `prod_task` 或写入 `prod_task_status_history` 的代码。
- [ ] `getPrintQueue()` 返回类型为 `List<PrintTaskDTO>`。
- [ ] `submitPrintTask()` 具有幂等性（重复提交不创建新记录）。
- [ ] `retryPrint()` 校验 `max_retry` 上限，超过则终态 `FAILED` 并抛异常。
- [ ] `prt_record` 表已增加 `operator_id` 字段。
- [ ] `prt_task` 表已增加 `max_retry` 字段。
- [ ] 异步打印机制已生效（`@Async` + Spring Event）。
- [ ] 单元测试覆盖率 ≥ 80%。
- [ ] `mvn test -pl dms-print` 全部通过。

---

## 附录：文件变更清单

| 文件路径 | 变更类型 | 说明 |
|----------|----------|------|
| `dms-print/pom.xml` | 修改 | 移除 `dms-production` 依赖 |
| `dms-print/src/.../entity/PrintTask.java` | 修改 | 继承 `BaseEntity`，新增 `maxRetry` |
| `dms-print/src/.../entity/PrintRecord.java` | 修改 | 继承 `BaseEntity`，新增 `operatorId`、`printerCode` |
| `dms-print/src/.../service/PrintServiceImpl.java` | **重写** | 清理跨模块引用，实现幂等、重试闭环、异步 |
| `dms-print/src/.../controller/PrintController.java` | 修改 | `getPrintQueue` 返回类型收紧 |
| `dms-print/src/.../config/PrintAsyncConfig.java` | **新增** | `@EnableAsync` + `printExecutor` |
| `dms-print/src/.../event/PrintTaskSubmittedEvent.java` | **新增** | 异步打印事件 |
| `dms-print/src/.../event/PrintTaskEventListener.java` | **新增** | 事件监听器 |
| `dms-print/src/.../service/PrintServiceImplTest.java` | **新增** | 单元测试 |
