# dms-production 详细设计

> 模块：煎药室管理系统 — 生产执行模块  
> 包名：`cn.org.openygt.production`  
> 表前缀：`prod_`  
> API 前缀：`/api/v1/prod`  
> 版本：V1.4 详细设计  
> 关联文档：`dms-production-需求与概设.md`  

---

## 一、模块概述

### 1.1 职责定位

`dms-production` 是 openygt-dms 的**核心业务模块**，负责煎药全流程生产执行。从处方接收开始，驱动任务在 12 步状态机中流转，记录每一步的工序日志、状态历史、工时，并与其他模块协同完成设备绑定、打印、质检等操作。

### 1.2 依赖关系

```
dms-production
├── Maven 依赖
│   ├── dms-common（SPI 接口 + 枚举 + BaseEntity）
│   └── dms-system（JWT 过滤器、sys_config 配置查询）
├── SPI 调用（注入 dms-common 接口，dms-equipment/print/quality 实现）
│   ├── EquipmentService — 设备绑定/释放/温度/预留
│   ├── PrintService     — 打印任务提交/重试
│   └── QualityService   — 质检委托
└── 被依赖
    ├── dms-quality → ProductionQueryService（查询任务信息）
    ├── dms-print   → ProductionQueryService（查询任务信息）
    └── dms-analytics → ProductionQueryService（报表数据源）
```

### 1.3 核心组件

| 组件 | 职责 |
|------|------|
| `TaskService` / `TaskServiceImpl` | 状态机核心，所有状态变更的唯一入口 |
| `PrescriptionService` | 处方接收、任务生成 |
| `TaskController` / `PrescriptionController` | REST API 暴露 |
| `SoakTimeoutScheduler` | 泡药超时扫描（从 sys_config 读阈值） |
| `TaskTimeoutScheduler` | 全状态超时告警扫描（PROD-017） |
| `ProductionQueryServiceImpl` | 实现 dms-common 的 ProductionQueryService SPI |

---

## 二、代码现状 vs 目标差距

### 2.1 差距总表

| # | 差距项 | 现状 | 目标 | 优先级 | 关联需求 |
|---|--------|------|------|--------|----------|
| G1 | 并发控制 | `synchronized`（`startDecoct`/`startWrap`/`bindDevice`） | 数据库悲观锁 `SELECT FOR UPDATE` | P0 | PROD-019 |
| G2 | 质检结果类型 | 中文字符串 `switch-case`（"通过"/"让步放行"/"返工"/"报废"） | `InspectionResultType` 枚举 | P0 | PROD-018 |
| G3 | 返工设备处理 | 直接释放设备（未调用预留） | `EquipmentService.reserveDevice(taskId, deviceId)` | P0 | PROD-018 |
| G4 | 任务表字段冗余 | `soakStartTime`/`soakEndTime`/`decoctStartTime`/`decoctEndTime`/`pourStartTime`/`pourEndTime`/`wrapStartTime`/`wrapEndTime` 等 8 个字段 | 全部移除，仅保留 `stage_start_time` + `complete_time` | P1 | PROD-016 |
| G5 | 状态历史实体基类 | `TaskStatusHistory` 不继承任何基类 | 继承 `BaseAuditEntity`（无 `@TableLogic`） | P1 | 审计规范 |
| G6 | 超时配置来源 | `SoakTimeoutScheduler` 硬编码 / 直接读 `task.soakDuration` | 从 `sys_config` 读取 `timeout.{STATUS}.minutes` | P1 | PROD-017 |
| G7 | 全状态超时告警 | 仅泡药有超时扫描 | 新增 `TaskTimeoutScheduler`，覆盖所有非终态，超时不自动跳，仅告警 | P1 | PROD-017 |
| G8 | ProductionQueryService SPI | 未在 `dms-common` 定义 | 在 `dms-common` 定义接口，`dms-production` 提供实现 | P1 | 跨模块解耦 |
| G9 | 工序日志 device_id 类型 | `String`（存设备编码） | `Long`（存设备 ID，与 `EquipmentService` 一致） | P1 | 数据一致性 |
| G10 | 暂停机制 | `pauseStep`/`resumeStep` 仅更新 `step_log`，未联动 `task` 状态 | 保持现状（暂停是工序级，不触发任务状态变更） | P2 | — |

### 2.2 关键代码片段（现状）

**G1 — synchronized 问题**：
```java
@Override
@Transactional
public synchronized Task startDecoct(Long taskId, String deviceCode, String operatorId) { ... }
// 以及 startWrap、bindDevice 同样问题
```

**G2 — 中文 switch-case 问题**：
```java
private void doQualityInspect(Task task, String result, String operatorId, String remark) {
    switch (result) {
        case "通过": ...
        case "让步放行": ...
        case "返工": ...
        case "报废": ...
    }
}
```

**G3 — 返工未预留设备**：
```java
case "返工":
    transition(task, "待煎药", operatorId, "质检返工...");
    // ❌ 缺少 EquipmentService.reserveDevice(taskId, decoctDeviceId)
    break;
```

**G4 — 冗余字段**：
```java
public class Task {
    private Date soakStartTime;   // 待删除
    private Date soakEndTime;     // 待删除
    private Date decoctStartTime; // 待删除
    private Date decoctEndTime;   // 待删除
    private Date pourStartTime;   // 待删除
    private Date pourEndTime;     // 待删除
    private Date wrapStartTime;   // 待删除
    private Date wrapEndTime;     // 待删除
    // ✅ 保留 stage_start_time + complete_time
}
```

---

## 三、数据库详细设计（DDL + 字段说明 + 索引 + 外键）

> 以下 DDL 提供 **SQLite** 和 **MySQL** 双版本。所有脚本支持幂等执行（`IF NOT EXISTS`）。

### 3.1 prod_prescription（处方/医嘱表）

**字段说明**：

| 字段 | 类型 | 约束 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGINT | PK, AUTO_INCREMENT | — | 主键 |
| tenant_id | VARCHAR(32) | | 'default' | 多租户预留 |
| hospital_id | BIGINT | FK → md_hospital | — | 医院 ID |
| scheme_id | BIGINT | FK → md_decoct_scheme | — | 煎药方案 ID |
| prescription_no | VARCHAR(50) | UNIQUE, NOT NULL | — | 处方编号 |
| patient_name | VARCHAR(50) | | — | 患者姓名 |
| patient_phone | VARCHAR(20) | | — | 患者电话 |
| total_dose | INT | | — | 总剂数 |
| status | VARCHAR(20) | | 'PENDING' | PENDING/PROCESSING/COMPLETED/CANCELLED |
| created_at | DATETIME | | CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | | CURRENT_TIMESTAMP | 更新时间 |
| deleted | INT | | 0 | 逻辑删除（0=未删除，1=已删除） |

**SQLite DDL**：
```sql
CREATE TABLE IF NOT EXISTS prod_prescription (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    tenant_id VARCHAR(32) DEFAULT 'default',
    hospital_id INTEGER,
    scheme_id INTEGER,
    prescription_no VARCHAR(50) NOT NULL UNIQUE,
    patient_name VARCHAR(50),
    patient_phone VARCHAR(20),
    total_dose INTEGER,
    status VARCHAR(20) DEFAULT 'PENDING',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);
```

**MySQL DDL**：
```sql
CREATE TABLE IF NOT EXISTS prod_prescription (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id VARCHAR(32) DEFAULT 'default',
    hospital_id BIGINT,
    scheme_id BIGINT,
    prescription_no VARCHAR(50) NOT NULL UNIQUE,
    patient_name VARCHAR(50),
    patient_phone VARCHAR(20),
    total_dose INT,
    status VARCHAR(20) DEFAULT 'PENDING',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_hospital_id (hospital_id),
    INDEX idx_prescription_no (prescription_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

### 3.2 prod_task（生产任务表）— 状态机核心

**字段说明**：

| 字段 | 类型 | 约束 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGINT | PK, AUTO_INCREMENT | — | 主键 |
| tenant_id | VARCHAR(32) | | 'default' | 多租户预留 |
| prescription_id | BIGINT | FK → prod_prescription, NOT NULL | — | 处方 ID |
| status | VARCHAR(20) | NOT NULL | — | 当前状态（TaskStatus 枚举名） |
| decoct_device_id | BIGINT | FK → eq_device | — | 煎药机 ID |
| package_device_id | BIGINT | FK → eq_device | — | 包装机 ID |
| current_temp | DECIMAL(5,2) | | — | 当前温度（°C） |
| current_stage_duration | INT | | 0 | 当前阶段已耗时（分钟） |
| operator_id | VARCHAR(50) | | — | 当前操作人 |
| print_status | VARCHAR(20) | | 'PENDING' | PENDING/PRINTING/SUCCESS/FAILED |
| is_exception | INT | | 0 | 是否异常（0=正常，1=异常） |
| exception_reason | VARCHAR(200) | | — | 异常原因 |
| handover_type | VARCHAR(50) | | — | 交接方式 |
| handover_user | VARCHAR(50) | | — | 交接人 |
| handover_time | DATETIME | | — | 交接时间 |
| stage_start_time | DATETIME | | — | **当前阶段开始时间（通用）** |
| complete_time | DATETIME | | — | 完成时间（终态） |
| created_at | DATETIME | | CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | | CURRENT_TIMESTAMP | 更新时间 |
| deleted | INT | | 0 | 逻辑删除 |

> **评审修订**：删除 `soak_start_time`/`soak_end_time`/`decoct_start_time`/`decoct_end_time`/`pour_start_time`/`pour_end_time`/`wrap_start_time`/`wrap_end_time` 共 8 个冗余字段。历史步骤时间由 `prod_step_log` 推导。

**SQLite DDL**：
```sql
CREATE TABLE IF NOT EXISTS prod_task (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    tenant_id VARCHAR(32) DEFAULT 'default',
    prescription_id INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL,
    decoct_device_id INTEGER,
    package_device_id INTEGER,
    current_temp DECIMAL(5,2),
    current_stage_duration INTEGER DEFAULT 0,
    operator_id VARCHAR(50),
    print_status VARCHAR(20) DEFAULT 'PENDING',
    is_exception INTEGER DEFAULT 0,
    exception_reason VARCHAR(200),
    handover_type VARCHAR(50),
    handover_user VARCHAR(50),
    handover_time DATETIME,
    stage_start_time DATETIME,
    complete_time DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);
```

**MySQL DDL**：
```sql
CREATE TABLE IF NOT EXISTS prod_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id VARCHAR(32) DEFAULT 'default',
    prescription_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    decoct_device_id BIGINT,
    package_device_id BIGINT,
    current_temp DECIMAL(5,2),
    current_stage_duration INT DEFAULT 0,
    operator_id VARCHAR(50),
    print_status VARCHAR(20) DEFAULT 'PENDING',
    is_exception INT DEFAULT 0,
    exception_reason VARCHAR(200),
    handover_type VARCHAR(50),
    handover_user VARCHAR(50),
    handover_time DATETIME,
    stage_start_time DATETIME,
    complete_time DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_status (status),
    INDEX idx_decoct_device_id (decoct_device_id),
    INDEX idx_prescription_id (prescription_id),
    INDEX idx_complete_time (complete_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

### 3.3 prod_task_status_history（状态变更历史表）

**字段说明**：

| 字段 | 类型 | 约束 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGINT | PK, AUTO_INCREMENT | — | 主键 |
| tenant_id | VARCHAR(32) | | 'default' | 多租户预留 |
| task_id | BIGINT | NOT NULL | — | 任务 ID |
| from_status | VARCHAR(20) | | — | 变更前状态 |
| to_status | VARCHAR(20) | | — | 变更后状态 |
| operator_id | VARCHAR(50) | | — | 操作人 |
| operate_time | DATETIME | | CURRENT_TIMESTAMP | 操作时间 |
| remark | TEXT | | — | 备注 |

> **评审修订**：审计数据表**不含** `deleted` 字段（无逻辑删除）。实体继承 `BaseAuditEntity`。

**SQLite DDL**：
```sql
CREATE TABLE IF NOT EXISTS prod_task_status_history (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    tenant_id VARCHAR(32) DEFAULT 'default',
    task_id INTEGER NOT NULL,
    from_status VARCHAR(20),
    to_status VARCHAR(20),
    operator_id VARCHAR(50),
    operate_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    remark TEXT
);
```

**MySQL DDL**：
```sql
CREATE TABLE IF NOT EXISTS prod_task_status_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id VARCHAR(32) DEFAULT 'default',
    task_id BIGINT NOT NULL,
    from_status VARCHAR(20),
    to_status VARCHAR(20),
    operator_id VARCHAR(50),
    operate_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    remark TEXT,
    INDEX idx_task_id_operate_time (task_id, operate_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

### 3.4 prod_step_log（工序记录表）

**字段说明**：

| 字段 | 类型 | 约束 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGINT | PK, AUTO_INCREMENT | — | 主键 |
| tenant_id | VARCHAR(32) | | 'default' | 多租户预留 |
| task_id | BIGINT | NOT NULL | — | 任务 ID |
| step_type | VARCHAR(20) | | — | SOAK/DECOCT/POUR/WRAP/LABEL/INSPECT/HANDOVER |
| device_id | BIGINT | | — | **使用的设备 ID（Long）** |
| operator_id | VARCHAR(50) | | — | 操作人 |
| started_at | DATETIME | | — | 开始时间 |
| ended_at | DATETIME | | — | 结束时间 |
| result | VARCHAR(20) | | '正常' | 正常/异常/暂停/通过/让步放行/返工/报废 |
| abort_reason | VARCHAR(200) | | — | 中断/异常原因 |
| is_paused | INT | | 0 | 是否暂停（0=否，1=是） |
| pause_reason | VARCHAR(200) | | — | 暂停原因 |
| pause_duration | INT | | 0 | 暂停时长（分钟） |
| parent_id | BIGINT | | — | 父记录 ID（返工关联） |
| created_at | DATETIME | | CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | | CURRENT_TIMESTAMP | 更新时间 |
| deleted | INT | | 0 | 逻辑删除 |

> **评审修订03**：`device_id` 统一为 `BIGINT`（存设备 ID），与 `prod_task.decoct_device_id` 和 `EquipmentService` 参数类型一致。

**SQLite DDL**：
```sql
CREATE TABLE IF NOT EXISTS prod_step_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    tenant_id VARCHAR(32) DEFAULT 'default',
    task_id INTEGER NOT NULL,
    step_type VARCHAR(20),
    device_id INTEGER,
    operator_id VARCHAR(50),
    started_at DATETIME,
    ended_at DATETIME,
    result VARCHAR(20) DEFAULT '正常',
    abort_reason VARCHAR(200),
    is_paused INTEGER DEFAULT 0,
    pause_reason VARCHAR(200),
    pause_duration INTEGER DEFAULT 0,
    parent_id INTEGER,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);
```

**MySQL DDL**：
```sql
CREATE TABLE IF NOT EXISTS prod_step_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id VARCHAR(32) DEFAULT 'default',
    task_id BIGINT NOT NULL,
    step_type VARCHAR(20),
    device_id BIGINT,
    operator_id VARCHAR(50),
    started_at DATETIME,
    ended_at DATETIME,
    result VARCHAR(20) DEFAULT '正常',
    abort_reason VARCHAR(200),
    is_paused INT DEFAULT 0,
    pause_reason VARCHAR(200),
    pause_duration INT DEFAULT 0,
    parent_id BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_task_id_step_type_started_at (task_id, step_type, started_at),
    INDEX idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

### 3.5 prod_work_record（工时记录表）

**字段说明**：

| 字段 | 类型 | 约束 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGINT | PK, AUTO_INCREMENT | — | 主键 |
| tenant_id | VARCHAR(32) | | 'default' | 多租户预留 |
| task_id | BIGINT | NOT NULL | — | 任务 ID |
| operator_id | VARCHAR(50) | | — | 操作人 ID |
| operator_name | VARCHAR(50) | | — | 操作人姓名 |
| action | VARCHAR(20) | | — | SOAK/DECOCT/POUR/WRAP/LABEL/INSPECT/HANDOVER |
| work_time | INT | | — | 工时（分钟） |
| created_at | DATETIME | | CURRENT_TIMESTAMP | 创建时间 |
| deleted | INT | | 0 | 逻辑删除 |

**SQLite DDL**：
```sql
CREATE TABLE IF NOT EXISTS prod_work_record (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    tenant_id VARCHAR(32) DEFAULT 'default',
    task_id INTEGER NOT NULL,
    operator_id VARCHAR(50),
    operator_name VARCHAR(50),
    action VARCHAR(20),
    work_time INTEGER,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);
```

**MySQL DDL**：
```sql
CREATE TABLE IF NOT EXISTS prod_work_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id VARCHAR(32) DEFAULT 'default',
    task_id BIGINT NOT NULL,
    operator_id VARCHAR(50),
    operator_name VARCHAR(50),
    action VARCHAR(20),
    work_time INT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_task_id (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

### 3.6 prod_handover_detail（交接明细表）

**字段说明**：

| 字段 | 类型 | 约束 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGINT | PK, AUTO_INCREMENT | — | 主键 |
| tenant_id | VARCHAR(32) | | 'default' | 多租户预留 |
| task_id | BIGINT | NOT NULL | — | 任务 ID |
| bag_count | INT | | — | 本次交接袋数 |
| handover_type | VARCHAR(50) | | — | 交接方式（自取/快递/配送） |
| handover_user | VARCHAR(50) | | — | 交接人 |
| handover_time | DATETIME | | — | 交接时间 |
| remark | TEXT | | — | 备注 |
| created_at | DATETIME | | CURRENT_TIMESTAMP | 创建时间 |
| deleted | INT | | 0 | 逻辑删除 |

**SQLite DDL**：
```sql
CREATE TABLE IF NOT EXISTS prod_handover_detail (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    tenant_id VARCHAR(32) DEFAULT 'default',
    task_id INTEGER NOT NULL,
    bag_count INTEGER,
    handover_type VARCHAR(50),
    handover_user VARCHAR(50),
    handover_time DATETIME,
    remark TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);
```

**MySQL DDL**：
```sql
CREATE TABLE IF NOT EXISTS prod_handover_detail (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id VARCHAR(32) DEFAULT 'default',
    task_id BIGINT NOT NULL,
    bag_count INT,
    handover_type VARCHAR(50),
    handover_user VARCHAR(50),
    handover_time DATETIME,
    remark TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_task_id_handover_time (task_id, handover_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

### 3.7 外键关系图

```
prod_prescription ||--o{ prod_task : "1:N"
prod_task ||--o{ prod_task_status_history : "1:N"
prod_task ||--o{ prod_step_log : "1:N"
prod_task ||--o{ prod_work_record : "1:N"
prod_task ||--o{ prod_handover_detail : "1:N"
prod_prescription }o--|| md_hospital : "FK"
prod_prescription }o--|| md_decoct_scheme : "FK"
prod_task }o--|| eq_device : "decoct_device_id FK"
prod_task }o--|| eq_device : "package_device_id FK"
prod_step_log }o--|| prod_step_log : "parent_id 自关联"
```

---

## 四、实体类详细设计

### 4.1 继承体系

```
cn.org.openygt.common.entity.BaseEntity
├── id: Long
├── tenantId: String
├── createdAt: Date
├── updatedAt: Date
└── deleted: Integer (@TableLogic)
    ├── Prescription
    ├── Task
    ├── StepLog
    ├── WorkRecord
    └── HandoverDetail

cn.org.openygt.common.entity.BaseAuditEntity
├── id: Long
├── tenantId: String
├── createdAt: Date
└── updatedAt: Date
    └── TaskStatusHistory  (无 @TableLogic)
```

### 4.2 Prescription.java

```java
package cn.org.openygt.production.entity;

import cn.org.openygt.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("prod_prescription")
public class Prescription extends BaseEntity {
    private Long hospitalId;
    private Long schemeId;
    private String prescriptionNo;
    private String patientName;
    private String patientPhone;
    private Integer totalDose;
    private String status; // PENDING / PROCESSING / COMPLETED / CANCELLED
}
```

### 4.3 Task.java

```java
package cn.org.openygt.production.entity;

import cn.org.openygt.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("prod_task")
public class Task extends BaseEntity {
    private Long prescriptionId;
    private String status;              // TaskStatus 枚举名
    private Long decoctDeviceId;
    private Long packageDeviceId;
    private BigDecimal currentTemp;
    private Integer currentStageDuration;
    private String operatorId;
    private String printStatus;
    private Integer isException;
    private String exceptionReason;
    private String handoverType;
    private String handoverUser;
    private Date handoverTime;
    private Date stageStartTime;        // 当前阶段开始时间（通用）
    private Date completeTime;          // 完成时间（终态）
}
```

### 4.4 TaskStatusHistory.java

```java
package cn.org.openygt.production.entity;

import cn.org.openygt.common.entity.BaseAuditEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("prod_task_status_history")
public class TaskStatusHistory extends BaseAuditEntity {
    private Long taskId;
    private String fromStatus;
    private String toStatus;
    private String operatorId;
    private Date operateTime;
    private String remark;
}
```

### 4.5 StepLog.java

```java
package cn.org.openygt.production.entity;

import cn.org.openygt.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("prod_step_log")
public class StepLog extends BaseEntity {
    private Long taskId;
    private String stepType;    // SOAK / DECOCT / POUR / WRAP / LABEL / INSPECT / HANDOVER
    private Long deviceId;      // ✅ Long 类型，统一存设备 ID
    private String operatorId;
    private Date startedAt;
    private Date endedAt;
    private String result;
    private String abortReason;
    private Integer isPaused;
    private String pauseReason;
    private Integer pauseDuration;
    private Long parentId;      // 返工关联
}
```

### 4.6 WorkRecord.java

```java
package cn.org.openygt.production.entity;

import cn.org.openygt.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("prod_work_record")
public class WorkRecord extends BaseEntity {
    private Long taskId;
    private String operatorId;
    private String operatorName;
    private String action;      // SOAK / DECOCT / POUR / WRAP / LABEL / INSPECT / HANDOVER
    private Integer workTime;   // 分钟
}
```

### 4.7 HandoverDetail.java

```java
package cn.org.openygt.production.entity;

import cn.org.openygt.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("prod_handover_detail")
public class HandoverDetail extends BaseEntity {
    private Long taskId;
    private Integer bagCount;
    private String handoverType;
    private String handoverUser;
    private Date handoverTime;
    private String remark;
}
```

---

## 五、REST API 详细设计

### 5.1 通用约定

- 请求/响应格式：`application/json`
- 统一响应体：`ApiResponse<T>`（定义在 dms-common）
- 幂等性：状态机端点天然幂等（同状态重复调用抛 `IllegalStateException`）
- 认证：请求头携带 `Authorization: Bearer {JWT}`（dms-system 过滤器处理）

### 5.2 处方管理端点

| 方法 | 路径 | 说明 | RequestDTO | Response |
|------|------|------|------------|----------|
| POST | `/api/v1/prod/prescriptions` | 创建处方 | `PrescriptionCreateRequest` | `ApiResponse<Prescription>` |
| GET | `/api/v1/prod/prescriptions/{id}` | 查询处方 | — | `ApiResponse<Prescription>` |
| GET | `/api/v1/prod/prescriptions` | 分页查询 | `hospitalId`, `patientType`, `page`, `size` | `ApiResponse<IPage<Prescription>>` |

**PrescriptionCreateRequest**：
```java
@Data
public class PrescriptionCreateRequest {
    @NotNull private Long hospitalId;
    @NotNull private Long schemeId;
    @NotBlank private String prescriptionNo;
    @NotBlank private String patientName;
    private String patientPhone;
    @NotNull @Min(1) private Integer totalDose;
}
```

### 5.3 任务状态机端点

| 方法 | 路径 | 说明 | RequestDTO | 异常码 |
|------|------|------|------------|--------|
| POST | `/api/v1/prod/tasks/{id}/soak/start` | 开始泡药 | `StageRequest` | 409 状态不符 |
| POST | `/api/v1/prod/tasks/{id}/soak/end` | 结束泡药 | `StageRequest` | 409 状态不符 |
| POST | `/api/v1/prod/tasks/{id}/decoct/start` | 开始煎药 | `DecoctStartRequest` | 409 状态不符 / 409 设备占用 |
| POST | `/api/v1/prod/tasks/{id}/decoct/end` | 结束煎药 | `StageRequest` | 409 状态不符 |
| POST | `/api/v1/prod/tasks/{id}/pour/start` | 开始出液 | `StageRequest` | 409 状态不符 |
| POST | `/api/v1/prod/tasks/{id}/pour/end` | 结束出液 | `StageRequest` | 409 状态不符 |
| POST | `/api/v1/prod/tasks/{id}/wrap/start` | 开始包装 | `DecoctStartRequest` | 409 状态不符 / 409 设备占用 |
| POST | `/api/v1/prod/tasks/{id}/wrap/end` | 结束包装 | `StageRequest` | 409 状态不符 |
| POST | `/api/v1/prod/tasks/{id}/label/confirm` | 确认贴标 | `StageRequest` | 409 状态不符 |
| POST | `/api/v1/prod/tasks/{id}/quality` | 质检 | `QualityInspectRequest` | 409 状态不符 / 400 非法结果 |
| POST | `/api/v1/prod/tasks/{id}/handover` | 交接 | `HandoverRequest` | 409 状态不符 |

**StageRequest**：
```java
@Data
public class StageRequest {
    @NotBlank private String operatorId;
}
```

**DecoctStartRequest**（复用现有，设备绑定场景通用）：
```java
@Data
public class DecoctStartRequest {
    @NotBlank private String deviceCode;
    @NotBlank private String operatorId;
}
```

**QualityInspectRequest**（✅ 使用枚举，禁止中文）：
```java
@Data
public class QualityInspectRequest {
    @NotNull private InspectionResultType result;  // 枚举 PASS/CONCESSION/REWORK/SCRAP
    @NotBlank private String operatorId;
    private String remark;
}
```

**HandoverRequest**：
```java
@Data
public class HandoverRequest {
    @NotNull @Min(1) private Integer bagCount;
    @NotBlank private String handoverType;
    @NotBlank private String handoverUser;
    private String remark;
    @NotNull private Boolean isFinal;
}
```

### 5.4 任务查询与管理端点

| 方法 | 路径 | 说明 | 参数/RequestDTO | Response |
|------|------|------|-----------------|----------|
| GET | `/api/v1/prod/tasks` | 分页查询 | `status`, `deviceId`, `page`, `size` | `ApiResponse<IPage<Task>>` |
| GET | `/api/v1/prod/tasks/{id}` | 详情 | — | `ApiResponse<Task>` |
| POST | `/api/v1/prod/tasks/{id}/bind` | 绑定设备 | `DeviceBindRequest` | `ApiResponse<Task>` |
| POST | `/api/v1/prod/tasks/{id}/force` | 强制状态 | `ForceRequest` | `ApiResponse<Task>` |
| GET | `/api/v1/prod/tasks/{id}/steps` | 工序记录 | — | `ApiResponse<List<StepLog>>` |
| GET | `/api/v1/prod/tasks/{id}/history` | 状态历史 | — | `ApiResponse<List<TaskStatusHistory>>` |
| GET | `/api/v1/prod/tasks/{id}/handover-details` | 交接明细 | — | `ApiResponse<List<HandoverDetail>>` |
| GET | `/api/v1/prod/tasks/print-queue` | 打印队列 | `printStatus` | `ApiResponse<List<Task>>` |
| POST | `/api/v1/prod/tasks/{id}/print` | 提交打印 | `DecoctStartRequest` | `ApiResponse<Task>` |
| POST | `/api/v1/prod/tasks/{id}/print/retry` | 重试打印 | `DecoctStartRequest` | `ApiResponse<Task>` |
| POST | `/api/v1/prod/tasks/clear` | 清空所有 | — | `ApiResponse<Integer>` |

**DeviceBindRequest**：
```java
@Data
public class DeviceBindRequest {
    @NotBlank private String deviceCode;
}
```

**ForceRequest**：
```java
@Data
public class ForceRequest {
    @NotBlank private String targetStatus;   // TaskStatus 枚举名
    @NotBlank private String operatorId;
    private String deviceCode;               // 强制到煎药中/包装中时需提供
    private String remark;
}
```

### 5.5 工序暂停/恢复端点

| 方法 | 路径 | 说明 | RequestDTO | Response |
|------|------|------|------------|----------|
| POST | `/api/v1/prod/steps/{id}/pause` | 暂停工序 | `PauseRequest` | `ApiResponse<StepLog>` |
| POST | `/api/v1/prod/steps/{id}/resume` | 恢复工序 | — | `ApiResponse<StepLog>` |

**PauseRequest**：
```java
@Data
public class PauseRequest {
    private String reason;
}
```

---

## 六、状态机详细设计

### 6.1 状态枚举（TaskStatus）

```java
package cn.org.openygt.common.enums;

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
    TaskStatus(String label) { this.label = label; }
    public String getLabel() { return label; }
}
```

### 6.2 完整状态转换矩阵

| 当前状态 | 操作/事件 | 目标状态 | 触发条件 | 是否写 step_log | 是否写 work_record | 设备动作 |
|----------|-----------|----------|----------|-----------------|-------------------|----------|
| `WAIT_SOAK` | `startSoak` | `SOAKING` | 操作人确认开始泡药 | ✅ SOAK | ✅ | — |
| `SOAKING` | `endSoak` | `WAIT_DECOCT` | 操作人确认 / 泡药超时自动推进 | ✅ SOAK(关闭) | ✅ | — |
| `WAIT_DECOCT` | `startDecoct` | `DECOCTING` | 绑定煎药机 + 操作人确认 | ✅ DECOCT | ✅ | 设备 → RUNNING |
| `WAIT_DECOCT` | `updateTemperature` | `DECOCTING` | 温度上报自动推进 | ✅ DECOCT | ❌ | 设备 → RUNNING |
| `DECOCTING` | `endDecoct` | `WAIT_POUR` | 煎药完成（手动/auto自动） | ✅ DECOCT(关闭) | ✅ | 设备 → IDLE（释放） |
| `WAIT_POUR` | `startPour` | `POURING` | 操作人确认（auto级自动） | ✅ POUR | ❌ | — |
| `POURING` | `endPour` | `WAIT_WRAP` | 出液完成（手动/auto自动） | ✅ POUR(关闭) | ✅ | — |
| `WAIT_WRAP` | `startWrap` | `WRAPPING` | 绑定包装机 | ✅ WRAP | ✅ | 包装机 → RUNNING |
| `WRAPPING` | `endWrap` | `WAIT_LABEL` | 包装完成 | ✅ WRAP(关闭) | ✅ | 包装机 → IDLE（释放） |
| `WAIT_LABEL` | `confirmLabel` | `WAIT_QC` | 贴标完成 | ✅ LABEL | ❌ | — |
| `WAIT_QC` | `qualityInspect` → `PASS` | `WAIT_HANDOVER` | 质检通过 | ✅ INSPECT | ❌ | 释放煎药机 |
| `WAIT_QC` | `qualityInspect` → `CONCESSION` | `WAIT_HANDOVER` | 让步放行，标记异常 | ✅ INSPECT | ❌ | 释放煎药机 |
| `WAIT_QC` | `qualityInspect` → `REWORK` | `WAIT_DECOCT` | 返工，回退待煎药 | ✅ INSPECT | ❌ | **预留设备**（不释放） |
| `WAIT_QC` | `qualityInspect` → `SCRAP` | `SCRAPPED` | 报废，终止 | ✅ INSPECT | ❌ | 释放煎药机 |
| `WAIT_HANDOVER` | `handover(isFinal=false)` | `PARTIAL_COMPLETED` | 部分交接 | ✅ HANDOVER | ❌ | — |
| `WAIT_HANDOVER` | `handover(isFinal=true)` | `COMPLETED` | 最终交接 | ✅ HANDOVER | ❌ | — |
| `PARTIAL_COMPLETED` | `handover(isFinal=true)` | `COMPLETED` | 从部分完成最终交接 | ✅ HANDOVER | ❌ | — |
| **任意非终态** | `forceStatus` | **任意** | 管理员强制跳转 | ❌ | ❌ | 按需绑定/释放 |

> **终态定义**：`COMPLETED` / `PARTIAL_COMPLETED` / `SCRAPPED`。终态任务不接受任何状态机操作（`forceStatus` 除外）。

### 6.3 自动流转规则

#### 规则 A：泡药超时自动推进
```
触发器：SoakTimeoutScheduler 每分钟执行
条件：status = SOAKING AND now - stage_start_time > sys_config[timeout.SOAKING.minutes]
动作：调用 taskService.endSoak(taskId, "SYSTEM")
说明：仅泡药支持超时自动推进，其他状态超时只告警不自动跳
```

#### 规则 B：温度上报自动推进
```
触发器：设备 MQTT 温度上报 → EquipmentService.updateTemperature → TaskService.updateTemperature
条件：status = WAIT_DECOCT
动作：status → DECOCTING（不写 work_record，因为无明确操作人）
```

#### 规则 C：auto 级设备自动级联
```
触发器：endDecoct 完成后
条件：equipmentService.getAutoLevel(decoctDeviceId) == "auto"
动作：自动调用 startPour(taskId, operatorId)

触发器：endPour 完成后
条件：equipmentService.getAutoLevel(decoctDeviceId) == "auto"
动作：自动调用 startWrap(taskId, deviceCode, operatorId)
```

### 6.4 全状态超时配置与告警（PROD-017）

**sys_config 配置项**：

| config_key | 默认值 | 说明 |
|------------|--------|------|
| `timeout.WAIT_SOAK.minutes` | 1440 | 待泡药超时时长（24h） |
| `timeout.SOAKING.minutes` | 30 | 泡药中超时时长 |
| `timeout.WAIT_DECOCT.minutes` | 60 | 待煎药超时时长 |
| `timeout.DECOCTING.minutes` | 120 | 煎药中超时时长 |
| `timeout.WAIT_POUR.minutes` | 30 | 待出液超时时长 |
| `timeout.POURING.minutes` | 20 | 出液中超时时长 |
| `timeout.WAIT_WRAP.minutes` | 60 | 待包装超时时长 |
| `timeout.WRAPPING.minutes` | 30 | 包装中超时时长 |
| `timeout.WAIT_LABEL.minutes` | 30 | 待贴标超时时长 |
| `timeout.WAIT_QC.minutes` | 60 | 待质检超时时长 |
| `timeout.WAIT_HANDOVER.minutes` | 1440 | 待交接超时时长（24h） |

**TaskTimeoutScheduler 算法**：
```java
@Scheduled(fixedRate = 60000)
public void checkAllTimeout() {
    // 1. 查询所有非终态任务
    List<Task> tasks = taskMapper.selectNonTerminalTasks();
    
    for (Task task : tasks) {
        if (task.getStageStartTime() == null) continue;
        
        // 2. 从 sys_config 读取对应状态的超时阈值
        Integer threshold = configService.getInt(
            "timeout." + task.getStatus() + ".minutes", 
            getDefaultTimeout(task.getStatus())
        );
        
        long elapsedMin = Duration.between(
            task.getStageStartTime().toInstant(), 
            Instant.now()
        ).toMinutes();
        
        // 3. 超时则告警，不自动跳转
        if (elapsedMin > threshold) {
            alarmService.raiseTaskTimeoutAlarm(task.getId(), task.getStatus(), elapsedMin, threshold);
            
            // 4. 写入 history 标记超时告警（可选，用于审计追溯）
            historyMapper.insert(new TaskStatusHistory(
                task.getId(),
                task.getStatus(),
                task.getStatus(),
                "SYSTEM",
                "【超时告警】状态=" + task.getStatus() + ", 已持续" + elapsedMin + "分钟, 阈值=" + threshold
            ));
        }
    }
}
```

---

## 七、核心流程详细设计（时序图文字版）

### 7.1 startDecoct（开始煎药）— 含悲观锁

```
Actor: 操作人
Controller: TaskController
Service: TaskServiceImpl
EquipmentSPI: EquipmentService (dms-common, dms-equipment实现)
TaskMapper: TaskMapper (MyBatis-Plus)
StepLogMapper: StepLogMapper
HistoryMapper: TaskStatusHistoryMapper
WorkMapper: WorkRecordMapper
DeviceMapper: EqDeviceMapper (dms-equipment)
DB: SQLite/MySQL

操作人 → Controller: POST /tasks/{id}/decoct/start {deviceCode, operatorId}
Controller → Service: startDecoct(taskId, deviceCode, operatorId)

Service → TaskMapper: selectById(taskId)
TaskMapper → DB: SELECT * FROM prod_task WHERE id = ?
DB → TaskMapper: Task(row)
TaskMapper → Service: Task(status=WAIT_DECOCT)

alt status != WAIT_DECOCT
    Service → Controller: throw IllegalStateException("任务状态不正确...")
    Controller → 操作人: 409 状态不符
end

Service → EquipmentSPI: getDeviceId(deviceCode)
EquipmentSPI → DB: SELECT id FROM eq_device WHERE code = ?
DB → EquipmentSPI: deviceId (or null)
EquipmentSPI → Service: deviceId

alt deviceId == null
    Service → EquipmentSPI: getOrCreateDevice(deviceCode, DECOCTING_TYPE=1)
    EquipmentSPI → DB: INSERT INTO eq_device ...
    EquipmentSPI → Service: deviceId
end

Service → DeviceMapper: selectForUpdate(deviceId)   <-- 悲观锁
DeviceMapper → DB: SELECT * FROM eq_device WHERE id = ? FOR UPDATE
DB → DeviceMapper: EqDevice(status=IDLE)
DeviceMapper → Service: EqDevice

alt status != IDLE
    Service → Controller: throw IllegalStateException("设备已被占用")
    Controller → 操作人: 409 设备占用
end

Service → DeviceMapper: updateStatus(deviceId, RUNNING)
DeviceMapper → DB: UPDATE eq_device SET status = 'RUNNING' WHERE id = ?

Service → Service: transition(task, DECOCTING, operatorId, remark)
Service → HistoryMapper: INSERT INTO prod_task_status_history ...
Service → StepLogMapper: INSERT INTO prod_step_log (stepType=DECOCT, deviceId=?, ...)
Service → WorkMapper: INSERT INTO prod_work_record (action=DECOCT, workTime=0, ...)

Service → Task: setDecoctDeviceId(deviceId)
Service → Task: setStageStartTime(now)
Service → Task: setCurrentStageDuration(0)
Service → TaskMapper: updateById(task)
TaskMapper → DB: UPDATE prod_task SET ... WHERE id = ?

Service → Controller: Task(updated)
Controller → 操作人: ApiResponse.success(Task)
```

### 7.2 qualityInspect（质检分支）— 返工设备预留

```
Actor: 质检员
Controller: TaskController
Service: TaskServiceImpl
EquipmentSPI: EquipmentService
DB: SQLite/MySQL

质检员 → Controller: POST /tasks/{id}/quality {result=REWORK, operatorId, remark}
Controller → Service: qualityInspect(taskId, REWORK, operatorId, remark)

Service → DB: SELECT * FROM prod_task WHERE id = ?
DB → Service: Task(status=WAIT_QC, decoctDeviceId=1001)

alt status != WAIT_QC
    Service → Controller: throw IllegalStateException
end

alt result == PASS
    Service → Service: transition(task, WAIT_HANDOVER, ...)
    Service → EquipmentSPI: releaseDevice(decoctDeviceId)
    
alt result == CONCESSION
    Service → Service: transition(task, WAIT_HANDOVER, ...)
    Service → Task: setIsException(1), setExceptionReason(remark)
    Service → EquipmentSPI: releaseDevice(decoctDeviceId)
    
alt result == REWORK
    Service → Service: transition(task, WAIT_DECOCT, ...)
    Service → EquipmentSPI: reserveDevice(taskId, decoctDeviceId)   <-- 关键！
    Note over EquipmentSPI: 设备状态 → RESERVED，绑定到当前任务
    Service → StepLogMapper: INSERT (stepType=INSPECT, result=返工)
    Service → StepLogMapper: INSERT (stepType=DECOCT, parentId=上一条, 新煎药记录)
    
alt result == SCRAP
    Service → Service: transition(task, SCRAPPED, ...)
    Service → Task: setIsException(1), setExceptionReason(remark)
    Service → EquipmentSPI: releaseDevice(decoctDeviceId)
end

Service → DB: UPDATE prod_task SET ...
Service → DB: INSERT INTO prod_task_status_history ...
Service → DB: INSERT INTO prod_step_log ...

Service → Controller: Task(updated)
Controller → 质检员: ApiResponse.success(Task)
```

### 7.3 handover（交接）

```
Actor: 交接员
Controller: TaskController
Service: TaskServiceImpl
DB: SQLite/MySQL

交接员 → Controller: POST /tasks/{id}/handover {bagCount=5, handoverType=快递, handoverUser=张三, isFinal=false}
Controller → Service: handover(taskId, 5, "快递", "张三", null, false)

Service → DB: SELECT * FROM prod_task WHERE id = ?
DB → Service: Task(status=WAIT_HANDOVER)

alt status not in [WAIT_HANDOVER, PARTIAL_COMPLETED, COMPLETED]
    Service → Controller: throw IllegalStateException
end

Service → DB: INSERT INTO prod_handover_detail (taskId, bagCount, handoverType, handoverUser, handoverTime, remark)
Service → Task: setHandoverType("快递"), setHandoverUser("张三"), setHandoverTime(now)

alt isFinal == true
    Service → Service: transition(task, COMPLETED, ...)
    Service → Task: setCompleteTime(now)
    Service → DB: INSERT INTO prod_step_log (stepType=HANDOVER, result=正常)
    
else
    Service → Service: transition(task, PARTIAL_COMPLETED, ...)
    Service → DB: INSERT INTO prod_step_log (stepType=HANDOVER, result=正常)
end

Service → DB: UPDATE prod_task SET ...
Service → DB: INSERT INTO prod_task_status_history ...

Service → Controller: Task(updated)
Controller → 交接员: ApiResponse.success(Task)
```

### 7.4 forceStatus（强制状态变更）

```
Actor: 管理员
Controller: TaskController
Service: TaskServiceImpl
EquipmentSPI: EquipmentService
DB: SQLite/MySQL

管理员 → Controller: POST /tasks/{id}/force {targetStatus=DECOCTING, operatorId, deviceCode=D001, remark=设备故障恢复}
Controller → Service: forceStatus(taskId, "DECOCTING", operatorId, "D001", "设备故障恢复")

Service → DB: SELECT * FROM prod_task WHERE id = ?
DB → Service: Task(status=WAIT_DECOCT)

alt targetStatus == DECOCTING && deviceCode != null
    Service → EquipmentSPI: getOrCreateDevice(deviceCode, 1)
    Service → EquipmentSPI: updateDeviceStatus(deviceId, RUNNING)
    Service → Task: setDecoctDeviceId(deviceId)
    Service → Task: setStageStartTime(now)
    
alt targetStatus == WRAPPING && deviceCode != null
    Service → EquipmentSPI: getOrCreateDevice(deviceCode, 2)
    Service → EquipmentSPI: updateDeviceStatus(deviceId, RUNNING)
    Service → Task: setPackageDeviceId(deviceId)
    Service → Task: setStageStartTime(now)
end

Service → Task: setStatus(targetStatus), setOperatorId(operatorId)
Service → DB: UPDATE prod_task SET ...
Service → DB: INSERT INTO prod_task_status_history (fromStatus, toStatus, operatorId, remark="【强制操作】...")

Note right of Service: 强制操作不写 step_log / work_record，
                       仅在 history 中标记【强制操作】

Service → Controller: Task(updated)
Controller → 管理员: ApiResponse.success(Task)
```

### 7.5 updateTemperature（温度上报自动推进）

```
Actor: MQTT Broker
Adapter: MqttMessageAdapter (dms-equipment)
EquipmentService: EquipmentServiceImpl
Service: TaskServiceImpl
DB: SQLite/MySQL

MQTT Broker → Adapter: topic: /device/D001/temp payload: {"temp":98.5}
Adapter → EquipmentService: updateTemperature("D001", 98.5)
EquipmentService → DB: UPDATE eq_device SET current_temp = 98.5 WHERE code = 'D001'

EquipmentService → Service: updateTemperature("D001", 98.5)
Service → DB: SELECT * FROM prod_task WHERE decoct_device_id = ? ORDER BY created_at DESC LIMIT 1
DB → Service: Task(status=WAIT_DECOCT, decoctDeviceId=1001)

alt task != null && status == WAIT_DECOCT
    Service → Task: setCurrentTemp(98.5)
    Service → Task: setStatus("DECOCTING")
    Service → Task: setStageStartTime(now)
    Service → DB: UPDATE prod_task SET status='DECOCTING', current_temp=98.5, stage_start_time=now ...
    Service → DB: INSERT INTO prod_task_status_history (fromStatus='待煎药', toStatus='煎药中', operatorId=null, remark='温度上报自动推进')
    Service → DB: INSERT INTO prod_step_log (stepType='DECOCT', deviceId=1001, startedAt=now, ...)
end

Service → EquipmentService: checkTemperatureAlarm(1001, 98.5)
EquipmentService → DB: 触发温度告警（如超阈值）
```

---

## 八、ProductionQueryService 实现设计

### 8.1 SPI 接口定义（dms-common）

> 新增文件：`dms-common/src/main/java/cn/org/openygt/common/service/ProductionQueryService.java`

```java
package cn.org.openygt.common.service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 生产执行模块查询 SPI。
 * 定义在 dms-common，由 dms-production 实现。
 * 供 dms-quality、dms-print、dms-analytics 查询任务信息，禁止跨模块直接访问 prod_task 表。
 */
public interface ProductionQueryService {

    /**
     * 根据任务 ID 查询任务基本信息。
     */
    TaskSummaryDTO getTaskById(Long taskId);

    /**
     * 根据处方编号查询关联的任务列表。
     */
    List<TaskSummaryDTO> getTasksByPrescriptionNo(String prescriptionNo);

    /**
     * 查询任务当前状态。
     */
    String getTaskStatus(Long taskId);

    /**
     * 查询任务的工序记录。
     */
    List<StepLogDTO> getStepLogs(Long taskId);

    /**
     * 查询任务的状态变更历史。
     */
    List<StatusHistoryDTO> getStatusHistory(Long taskId);

    /**
     * 查询待打印任务列表（供 dms-print 调用）。
     */
    List<TaskSummaryDTO> getPendingPrintTasks();

    /**
     * 查询设备当前绑定的任务（供 dms-equipment 调用）。
     */
    TaskSummaryDTO getTaskByDeviceId(Long deviceId);

    /**
     * 查询任务交接明细。
     */
    List<HandoverDetailDTO> getHandoverDetails(Long taskId);

    // ==================== DTO 定义 ====================

    @lombok.Data
    class TaskSummaryDTO {
        private Long id;
        private Long prescriptionId;
        private String prescriptionNo;
        private String patientName;
        private String status;
        private Long decoctDeviceId;
        private Long packageDeviceId;
        private BigDecimal currentTemp;
        private String operatorId;
        private Integer isException;
        private String exceptionReason;
        private Date stageStartTime;
        private Date completeTime;
        private Date createdAt;
    }

    @lombok.Data
    class StepLogDTO {
        private Long id;
        private String stepType;
        private Long deviceId;
        private String operatorId;
        private Date startedAt;
        private Date endedAt;
        private String result;
        private String abortReason;
        private Integer isPaused;
        private Integer pauseDuration;
        private Long parentId;
    }

    @lombok.Data
    class StatusHistoryDTO {
        private Long id;
        private String fromStatus;
        private String toStatus;
        private String operatorId;
        private Date operateTime;
        private String remark;
    }

    @lombok.Data
    class HandoverDetailDTO {
        private Long id;
        private Integer bagCount;
        private String handoverType;
        private String handoverUser;
        private Date handoverTime;
        private String remark;
    }
}
```

### 8.2 实现类（dms-production）

> 新增文件：`dms-production/src/main/java/cn/org/openygt/production/service/impl/ProductionQueryServiceImpl.java`

```java
package cn.org.openygt.production.service.impl;

import cn.org.openygt.common.service.ProductionQueryService;
import cn.org.openygt.production.entity.*;
import cn.org.openygt.production.mapper.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductionQueryServiceImpl implements ProductionQueryService {

    private final TaskMapper taskMapper;
    private final PrescriptionMapper prescriptionMapper;
    private final StepLogMapper stepLogMapper;
    private final TaskStatusHistoryMapper historyMapper;
    private final HandoverDetailMapper handoverDetailMapper;

    @Override
    public TaskSummaryDTO getTaskById(Long taskId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) return null;
        return convertToSummary(task);
    }

    @Override
    public List<TaskSummaryDTO> getTasksByPrescriptionNo(String prescriptionNo) {
        Prescription p = prescriptionMapper.selectOne(
            new LambdaQueryWrapper<Prescription>()
                .eq(Prescription::getPrescriptionNo, prescriptionNo)
        );
        if (p == null) return List.of();
        List<Task> tasks = taskMapper.selectList(
            new LambdaQueryWrapper<Task>()
                .eq(Task::getPrescriptionId, p.getId())
        );
        return tasks.stream().map(this::convertToSummary).collect(Collectors.toList());
    }

    @Override
    public String getTaskStatus(Long taskId) {
        Task task = taskMapper.selectById(taskId);
        return task == null ? null : task.getStatus();
    }

    @Override
    public List<StepLogDTO> getStepLogs(Long taskId) {
        List<StepLog> logs = stepLogMapper.selectList(
            new LambdaQueryWrapper<StepLog>()
                .eq(StepLog::getTaskId, taskId)
                .orderByAsc(StepLog::getStartedAt)
        );
        return logs.stream().map(this::convertStepLog).collect(Collectors.toList());
    }

    @Override
    public List<StatusHistoryDTO> getStatusHistory(Long taskId) {
        List<TaskStatusHistory> list = historyMapper.selectList(
            new LambdaQueryWrapper<TaskStatusHistory>()
                .eq(TaskStatusHistory::getTaskId, taskId)
                .orderByDesc(TaskStatusHistory::getOperateTime)
        );
        return list.stream().map(this::convertHistory).collect(Collectors.toList());
    }

    @Override
    public List<TaskSummaryDTO> getPendingPrintTasks() {
        List<Task> tasks = taskMapper.selectList(
            new LambdaQueryWrapper<Task>()
                .eq(Task::getStatus, "待贴标")
                .and(w -> w.eq(Task::getPrintStatus, "PENDING").or().eq(Task::getPrintStatus, "FAILED"))
                .orderByDesc(Task::getCompleteTime)
        );
        return tasks.stream().map(this::convertToSummary).collect(Collectors.toList());
    }

    @Override
    public TaskSummaryDTO getTaskByDeviceId(Long deviceId) {
        Task task = taskMapper.selectOne(
            new LambdaQueryWrapper<Task>()
                .eq(Task::getDecoctDeviceId, deviceId)
                .or().eq(Task::getPackageDeviceId, deviceId)
                .orderByDesc(Task::getCreatedAt)
                .last("LIMIT 1")
        );
        return task == null ? null : convertToSummary(task);
    }

    @Override
    public List<HandoverDetailDTO> getHandoverDetails(Long taskId) {
        List<HandoverDetail> list = handoverDetailMapper.selectList(
            new LambdaQueryWrapper<HandoverDetail>()
                .eq(HandoverDetail::getTaskId, taskId)
                .orderByDesc(HandoverDetail::getHandoverTime)
        );
        return list.stream().map(this::convertHandover).collect(Collectors.toList());
    }

    // ==================== 转换方法 ====================

    private TaskSummaryDTO convertToSummary(Task task) {
        TaskSummaryDTO dto = new TaskSummaryDTO();
        dto.setId(task.getId());
        dto.setPrescriptionId(task.getPrescriptionId());
        dto.setStatus(task.getStatus());
        dto.setDecoctDeviceId(task.getDecoctDeviceId());
        dto.setPackageDeviceId(task.getPackageDeviceId());
        dto.setCurrentTemp(task.getCurrentTemp());
        dto.setOperatorId(task.getOperatorId());
        dto.setIsException(task.getIsException());
        dto.setExceptionReason(task.getExceptionReason());
        dto.setStageStartTime(task.getStageStartTime());
        dto.setCompleteTime(task.getCompleteTime());
        dto.setCreatedAt(task.getCreatedAt());
        
        // 补处方编号和患者姓名
        Prescription p = prescriptionMapper.selectById(task.getPrescriptionId());
        if (p != null) {
            dto.setPrescriptionNo(p.getPrescriptionNo());
            dto.setPatientName(p.getPatientName());
        }
        return dto;
    }

    private StepLogDTO convertStepLog(StepLog log) {
        StepLogDTO dto = new StepLogDTO();
        dto.setId(log.getId());
        dto.setStepType(log.getStepType());
        dto.setDeviceId(log.getDeviceId());
        dto.setOperatorId(log.getOperatorId());
        dto.setStartedAt(log.getStartedAt());
        dto.setEndedAt(log.getEndedAt());
        dto.setResult(log.getResult());
        dto.setAbortReason(log.getAbortReason());
        dto.setIsPaused(log.getIsPaused());
        dto.setPauseDuration(log.getPauseDuration());
        dto.setParentId(log.getParentId());
        return dto;
    }

    private StatusHistoryDTO convertHistory(TaskStatusHistory h) {
        StatusHistoryDTO dto = new StatusHistoryDTO();
        dto.setId(h.getId());
        dto.setFromStatus(h.getFromStatus());
        dto.setToStatus(h.getToStatus());
        dto.setOperatorId(h.getOperatorId());
        dto.setOperateTime(h.getOperateTime());
        dto.setRemark(h.getRemark());
        return dto;
    }

    private HandoverDetailDTO convertHandover(HandoverDetail d) {
        HandoverDetailDTO dto = new HandoverDetailDTO();
        dto.setId(d.getId());
        dto.setBagCount(d.getBagCount());
        dto.setHandoverType(d.getHandoverType());
        dto.setHandoverUser(d.getHandoverUser());
        dto.setHandoverTime(d.getHandoverTime());
        dto.setRemark(d.getRemark());
        return dto;
    }
}
```

---

## 九、并发与事务策略

### 9.1 事务边界（@Transactional）

| 方法 | 事务级别 | 说明 |
|------|----------|------|
| `startSoak` / `endSoak` | `@Transactional` | task + history + step_log + work_record 原子写 |
| `startDecoct` / `startWrap` | `@Transactional` | 含设备状态更新，需跨表原子性 |
| `endDecoct` / `endPour` / `endWrap` | `@Transactional` | task + 设备释放 + step_log 关闭 |
| `qualityInspect` | `@Transactional` | task + 设备动作 + history + step_log |
| `handover` | `@Transactional` | task + handover_detail + history + step_log |
| `forceStatus` | `@Transactional` | task + 设备绑定/释放 + history |
| `bindDevice` | `@Transactional` | task + 设备状态 |
| `updateTemperature` | `@Transactional` | task + 设备温度 + history |
| `printLabel` / `retryPrint` | `@Transactional` | 委托 PrintService |

### 9.2 悲观锁策略（替代 synchronized）

**问题**：`synchronized` 仅在单 JVM 内有效，无法支撑集群部署。

**方案**：使用数据库悲观锁 `SELECT ... FOR UPDATE`，通过 MyBatis-Plus 自定义 SQL 实现。

#### 9.2.1 TaskMapper 悲观锁方法

```java
package cn.org.openygt.production.mapper;

import cn.org.openygt.production.entity.Task;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface TaskMapper extends BaseMapper<Task> {

    /**
     * 悲观锁查询任务。
     * 在事务内调用，锁定任务行，防止并发状态冲突。
     */
    @Select("SELECT * FROM prod_task WHERE id = #{taskId} AND deleted = 0 FOR UPDATE")
    Task selectByIdForUpdate(@Param("taskId") Long taskId);

    /**
     * 查询所有非终态任务（供超时调度器使用）。
     */
    @Select("SELECT * FROM prod_task WHERE status NOT IN ('已完成', '已部分完成', '已报废') AND deleted = 0")
    List<Task> selectNonTerminalTasks();
}
```

#### 9.2.2 设备悲观锁（EqDeviceMapper 在 dms-equipment）

> 需 dms-equipment 提供：

```java
package cn.org.openygt.equipment.mapper;

import cn.org.openygt.equipment.entity.EqDevice;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface EqDeviceMapper extends BaseMapper<EqDevice> {

    @Select("SELECT * FROM eq_device WHERE id = #{deviceId} FOR UPDATE")
    EqDevice selectForUpdate(@Param("deviceId") Long deviceId);

    @Update("UPDATE eq_device SET status = #{status} WHERE id = #{deviceId}")
    int updateStatus(@Param("deviceId") Long deviceId, @Param("status") String status);
}
```

#### 9.2.3 startDecoct 修订后伪代码（无 synchronized）

```java
@Override
@Transactional
public Task startDecoct(Long taskId, String deviceCode, String operatorId) {
    // 1. 悲观锁查询任务（替代 synchronized）
    Task task = taskMapper.selectByIdForUpdate(taskId);
    if (task == null) throw new IllegalArgumentException("任务不存在");
    assertStatus(task, TaskStatus.WAIT_DECOCT);

    // 2. 获取或创建设备
    Long deviceId = equipmentService.getDeviceId(deviceCode);
    if (deviceId == null) {
        equipmentService.getOrCreateDevice(deviceCode, DeviceType.DECOCTING.getCode());
        deviceId = equipmentService.getDeviceId(deviceCode);
    }

    // 3. 悲观锁查询设备（在 dms-equipment 的事务内）
    // EquipmentService 的实现内部应使用 selectForUpdate
    String deviceStatus = equipmentService.getDeviceStatusWithLock(deviceId);
    if (!DeviceStatus.IDLE.name().equalsIgnoreCase(deviceStatus)) {
        throw new IllegalStateException("设备不是空闲状态，无法绑定");
    }

    // 4. 更新设备状态
    equipmentService.updateDeviceStatus(deviceId, DeviceStatus.RUNNING.name());

    // 5. 状态流转
    transition(task, TaskStatus.DECOCTING, operatorId, "开始煎药，绑定设备: " + deviceCode);
    task.setDecoctDeviceId(deviceId);
    task.setStageStartTime(new Date());
    task.setCurrentStageDuration(0);
    taskMapper.updateById(task);

    // 6. 记录辅助数据
    recordWork(taskId, operatorId, null, "DECOCT", 0);
    createStepLog(taskId, "DECOCT", deviceId, operatorId, null); // deviceId 为 Long

    return task;
}
```

### 9.3 并发场景分析

| 场景 | 风险 | 控制手段 |
|------|------|----------|
| 两个操作员同时点击"开始煎药" | 任务状态被覆盖 | `selectByIdForUpdate` 串行化 |
| 两个任务同时绑定同一台设备 | 设备被重复占用 | `eq_device FOR UPDATE` 串行化 + 状态校验 |
| 泡药超时自动推进 + 人工结束泡药 | 重复 endSoak | 事务内状态校验，`assertStatus` 抛异常 |
| 返工预留期间其他任务绑定设备 | 预留设备被抢占 | `reserveDevice` 将设备置为 RESERVED |

---

## 十、异常处理策略

### 10.1 异常类层次

```
java.lang.RuntimeException
├── IllegalArgumentException      — 参数非法（任务不存在、枚举值非法）
├── IllegalStateException         — 状态不符（当前状态不允许此操作）
└── ProductionBizException        — 业务异常（需新增，统一异常码）
    ├── TaskStatusException       — 状态机非法转换
    ├── DeviceBindException       — 设备绑定失败（被占用/不存在）
    └── TaskTimeoutException      — 操作超时
```

### 10.2 新增 ProductionBizException

```java
package cn.org.openygt.production.exception;

import lombok.Getter;

@Getter
public class ProductionBizException extends RuntimeException {
    private final int code;

    public ProductionBizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public ProductionBizException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    // 预定义异常码
    public static final int TASK_NOT_FOUND = 1001;
    public static final int STATUS_MISMATCH = 1002;
    public static final int DEVICE_OCCUPIED = 1003;
    public static final int DEVICE_NOT_FOUND = 1004;
    public static final int INSPECTION_RESULT_INVALID = 1005;
    public static final int HANDOVER_INVALID = 1006;
}
```

### 10.3 各场景异常映射

| 场景 | 抛出异常 | HTTP 状态码 | 错误消息示例 |
|------|----------|-------------|-------------|
| 任务不存在 | `IllegalArgumentException` | 400 | 任务不存在 |
| 状态不符 | `IllegalStateException` | 409 | 任务状态不正确，期望: 待煎药，实际: 泡药中 |
| 设备被占用 | `IllegalStateException` | 409 | 设备不是空闲状态，无法绑定 |
| 质检结果非法 | `IllegalArgumentException` | 400 | 未知的质检结果: XXX |
| 强制状态非法 | `IllegalStateException` | 409 | 不支持强制到目标状态 |

### 10.4 全局异常处理（dms-common GlobalExceptionHandler 增强）

```java
@ExceptionHandler(IllegalStateException.class)
public ApiResponse<Void> handleIllegalState(IllegalStateException e) {
    return ApiResponse.error(409, e.getMessage());
}

@ExceptionHandler(IllegalArgumentException.class)
public ApiResponse<Void> handleIllegalArgument(IllegalArgumentException e) {
    return ApiResponse.error(400, e.getMessage());
}
```

---

## 十一、单元测试策略

### 11.1 测试分层

| 层级 | 框架 | 范围 | 数据库 |
|------|------|------|--------|
| 单元测试 | JUnit 5 + Mockito | Service 层状态机方法 | H2 内存数据库 |
| 集成测试 | SpringBootTest + Testcontainers | Controller → Service → Mapper | MySQL 容器 |

### 11.2 关键测试场景

#### 场景 A：正常状态流转（12 步主干）

```java
@Test
@DisplayName("正常流程：待泡药 → 已完成")
void normalFlow_shouldReachCompleted() {
    Long taskId = 1L;
    String operator = "OP001";

    taskService.startSoak(taskId, operator);
    assertStatus(taskId, "泡药中");

    taskService.endSoak(taskId, operator);
    assertStatus(taskId, "待煎药");

    taskService.startDecoct(taskId, "D001", operator);
    assertStatus(taskId, "煎药中");

    taskService.endDecoct(taskId, operator);
    assertStatus(taskId, "待出液");

    taskService.startPour(taskId, operator);
    assertStatus(taskId, "出液中");

    taskService.endPour(taskId, operator);
    assertStatus(taskId, "待包装");

    taskService.startWrap(taskId, "P001", operator);
    assertStatus(taskId, "包装中");

    taskService.endWrap(taskId, operator);
    assertStatus(taskId, "待贴标");

    taskService.confirmLabel(taskId, operator);
    assertStatus(taskId, "待质检");

    taskService.qualityInspect(taskId, InspectionResultType.PASS, operator, null);
    assertStatus(taskId, "待交接");

    taskService.handover(taskId, 10, "自取", "患者A", null, true);
    assertStatus(taskId, "已完成");
}
```

#### 场景 B：质检分支全覆盖

```java
@Test
@DisplayName("质检：返工应预留设备，不释放")
void qualityInspect_rework_shouldReserveDevice() {
    // Given: 任务在待质检，已绑定煎药机 deviceId=100
    Task task = prepareTask(TaskStatus.WAIT_QC, 100L);

    // When: 返工
    taskService.qualityInspect(task.getId(), InspectionResultType.REWORK, "QC001", "浓度不足");

    // Then: 状态回退到待煎药，设备被预留
    Task updated = taskMapper.selectById(task.getId());
    assertThat(updated.getStatus()).isEqualTo("待煎药");
    verify(equipmentService).reserveDevice(task.getId(), 100L);
    verify(equipmentService, never()).releaseDevice(any());
}

@Test
@DisplayName("质检：通过应释放设备")
void qualityInspect_pass_shouldReleaseDevice() {
    Task task = prepareTask(TaskStatus.WAIT_QC, 100L);
    taskService.qualityInspect(task.getId(), InspectionResultType.PASS, "QC001", null);
    verify(equipmentService).releaseDevice(100L);
}

@Test
@DisplayName("质检：让步放行应标记异常并释放设备")
void qualityInspect_concession_shouldMarkException() {
    Task task = prepareTask(TaskStatus.WAIT_QC, 100L);
    taskService.qualityInspect(task.getId(), InspectionResultType.CONCESSION, "QC001", "颜色偏差");
    Task updated = taskMapper.selectById(task.getId());
    assertThat(updated.getIsException()).isEqualTo(1);
    assertThat(updated.getExceptionReason()).isEqualTo("颜色偏差");
    verify(equipmentService).releaseDevice(100L);
}

@Test
@DisplayName("质检：报废应标记异常并释放设备")
void qualityInspect_scrap_shouldMarkException() {
    Task task = prepareTask(TaskStatus.WAIT_QC, 100L);
    taskService.qualityInspect(task.getId(), InspectionResultType.SCRAP, "QC001", "污染");
    Task updated = taskMapper.selectById(task.getId());
    assertThat(updated.getStatus()).isEqualTo("已报废");
    assertThat(updated.getIsException()).isEqualTo(1);
    verify(equipmentService).releaseDevice(100L);
}
```

#### 场景 C：超时告警

```java
@Test
@DisplayName("全状态超时：煎药中持续 121 分钟应触发告警")
void taskTimeout_decocting_shouldRaiseAlarm() {
    // Given: 煎药中，stage_start_time = 121 分钟前
    Task task = new Task();
    task.setStatus("煎药中");
    task.setStageStartTime(Date.from(Instant.now().minus(121, ChronoUnit.MINUTES)));
    when(taskMapper.selectNonTerminalTasks()).thenReturn(List.of(task));
    when(configService.getInt("timeout.DECOCTING.minutes", 120)).thenReturn(120);

    // When
    taskTimeoutScheduler.checkAllTimeout();

    // Then: 触发告警，但不改状态
    verify(alarmService).raiseTaskTimeoutAlarm(task.getId(), "煎药中", 121, 120);
    verify(historyMapper).insert(argThat(h -> h.getRemark().contains("超时告警")));
    verify(taskMapper, never()).updateById(any());
}
```

#### 场景 D：并发设备绑定

```java
@Test
@DisplayName("并发绑定：两个任务同时绑定同一设备，应只有一个成功")
void concurrentDeviceBind_shouldOnlyOneSucceed() throws InterruptedException {
    Long task1 = 1L, task2 = 2L;
    String deviceCode = "D001";
    CountDownLatch latch = new CountDownLatch(2);
    AtomicInteger successCount = new AtomicInteger(0);
    AtomicInteger failCount = new AtomicInteger(0);

    Runnable r1 = () -> {
        try {
            taskService.startDecoct(task1, deviceCode, "OP1");
            successCount.incrementAndGet();
        } catch (IllegalStateException e) {
            failCount.incrementAndGet();
        } finally {
            latch.countDown();
        }
    };
    Runnable r2 = () -> {
        try {
            taskService.startDecoct(task2, deviceCode, "OP2");
            successCount.incrementAndGet();
        } catch (IllegalStateException e) {
            failCount.incrementAndGet();
        } finally {
            latch.countDown();
        }
    };

    new Thread(r1).start();
    new Thread(r2).start();
    latch.await(5, TimeUnit.SECONDS);

    assertThat(successCount.get()).isEqualTo(1);
    assertThat(failCount.get()).isEqualTo(1);
}
```

#### 场景 E：字段精简验证

```java
@Test
@DisplayName("startDecoct 不应再设置 decoctStartTime，应设置 stageStartTime")
void startDecoct_shouldSetStageStartTimeNotDecoctStartTime() {
    Task task = taskService.startDecoct(1L, "D001", "OP1");
    
    // 旧字段已不存在
    // assertThat(task.getDecoctStartTime()).isNotNull(); // ❌ 编译错误，字段已删除
    
    // 新通用字段
    assertThat(task.getStageStartTime()).isNotNull();
}
```

### 11.3 Mock 策略

| 依赖 | Mock 方式 | 说明 |
|------|-----------|------|
| `EquipmentService` | `@MockBean` | 模拟设备绑定/释放/预留，验证调用次数和参数 |
| `PrintService` | `@MockBean` | 模拟打印委托 |
| `TaskMapper` / `StepLogMapper` | `@SpyBean` 或真实 Mapper | H2 数据库执行真实 SQL |
| `ConfigService` | `@MockBean` | 模拟 sys_config 返回值 |
| `AlarmService` | `@MockBean` | 验证超时告警触发 |

---

## 十二、迁移路径

### 12.1 需要修改的文件清单

| # | 文件路径 | 修改内容 |
|---|----------|----------|
| 1 | `dms-common/src/main/java/cn/org/openygt/common/entity/BaseAuditEntity.java` | **新增**：无 `@TableLogic` 的审计基类 |
| 2 | `dms-common/src/main/java/cn/org/openygt/common/enums/InspectionResultType.java` | **新增**：质检结果枚举 |
| 3 | `dms-common/src/main/java/cn/org/openygt/common/service/ProductionQueryService.java` | **新增**：SPI 接口定义 |
| 4 | `dms-common/src/main/java/cn/org/openygt/common/service/EquipmentService.java` | **新增**：`reserveDevice(Long taskId, Long deviceId)` 方法 |
| 5 | `dms-production/src/main/java/cn/org/openygt/production/entity/Task.java` | **修改**：删除 8 个步骤时间字段，仅保留 `stageStartTime` + `completeTime` |
| 6 | `dms-production/src/main/java/cn/org/openygt/production/entity/TaskStatusHistory.java` | **修改**：继承 `BaseAuditEntity`（替换无继承） |
| 7 | `dms-production/src/main/java/cn/org/openygt/production/entity/StepLog.java` | **修改**：`deviceId` 类型从 `String` 改为 `Long` |
| 8 | `dms-production/src/main/java/cn/org/openygt/production/mapper/TaskMapper.java` | **新增**：`selectByIdForUpdate`, `selectNonTerminalTasks` |
| 9 | `dms-production/src/main/java/cn/org/openygt/production/service/TaskService.java` | **修改**：`qualityInspect` 参数 `String result` → `InspectionResultType result` |
| 10 | `dms-production/src/main/java/cn/org/openygt/production/service/impl/TaskServiceImpl.java` | **重构**：见下方详细说明 |
| 11 | `dms-production/src/main/java/cn/org/openygt/production/scheduler/SoakTimeoutScheduler.java` | **修改**：从 sys_config 读超时阈值 |
| 12 | `dms-production/src/main/java/cn/org/openygt/production/scheduler/TaskTimeoutScheduler.java` | **新增**：全状态超时告警扫描 |
| 13 | `dms-production/src/main/java/cn/org/openygt/production/service/impl/ProductionQueryServiceImpl.java` | **新增**：SPI 实现 |
| 14 | `dms-production/src/main/java/cn/org/openygt/production/controller/TaskController.java` | **修改**：`qualityInspect` 接收 `InspectionResultType` |
| 15 | `dms-production/src/main/java/cn/org/openygt/production/dto/QualityInspectRequest.java` | **修改**：`result` 字段类型改为 `InspectionResultType` |
| 16 | `dms-equipment/src/main/java/cn/org/openygt/equipment/service/impl/EquipmentServiceImpl.java` | **新增**：实现 `reserveDevice` |
| 17 | `dms-equipment/src/main/java/cn/org/openygt/equipment/mapper/EqDeviceMapper.java` | **新增**：`selectForUpdate` |

### 12.2 TaskServiceImpl 重构详细步骤

1. **删除 `synchronized` 关键字**：`startDecoct`, `startWrap`, `bindDevice` 三个方法。
2. **引入悲观锁**：在三个方法开头调用 `taskMapper.selectByIdForUpdate(taskId)`。
3. **删除旧字段引用**：移除所有 `setSoakStartTime`/`setSoakEndTime`/`setDecoctStartTime`/`setDecoctEndTime`/`setPourStartTime`/`setPourEndTime`/`setWrapStartTime`/`setWrapEndTime`，统一替换为 `setStageStartTime(new Date())`。
4. **重构 `doQualityInspect`**：
   - 参数改为 `InspectionResultType result`
   - `switch` 改为枚举 switch（Java 14+）或 if-else
   - REWORK 分支增加 `equipmentService.reserveDevice(taskId, task.getDecoctDeviceId())`
   - PASS/CONCESSION/SCRAP 分支增加 `equipmentService.releaseDevice(task.getDecoctDeviceId())`
5. **重构 `createStepLog`**：`deviceId` 参数改为 `Long` 类型。
6. **新增 `getTaskOrThrow` 的悲观锁版本**：`getTaskForUpdateOrThrow`。

### 12.3 数据库迁移脚本

> 文件：`docs-iot/detailed-design/数据库迁移-V5-production.sql`

**SQLite**：
```sql
-- V5__prod_field_cleanup.sql (SQLite)
-- 1. 备份旧字段数据到 step_log（如需要历史数据保留，需数据迁移脚本，此处仅结构变更）
-- 2. 删除冗余字段
ALTER TABLE prod_task DROP COLUMN soak_start_time;
ALTER TABLE prod_task DROP COLUMN soak_end_time;
ALTER TABLE prod_task DROP COLUMN decoct_start_time;
ALTER TABLE prod_task DROP COLUMN decoct_end_time;
ALTER TABLE prod_task DROP COLUMN pour_start_time;
ALTER TABLE prod_task DROP COLUMN pour_end_time;
ALTER TABLE prod_task DROP COLUMN wrap_start_time;
ALTER TABLE prod_task DROP COLUMN wrap_end_time;

-- 3. 新增通用字段（如不存在）
-- ALTER TABLE prod_task ADD COLUMN stage_start_time DATETIME; -- 已存在则跳过

-- 4. step_log device_id 类型变更（SQLite 不支持 ALTER COLUMN，需重建表）
-- 详见完整迁移脚本
```

**MySQL**：
```sql
-- V5__prod_field_cleanup.sql (MySQL)
ALTER TABLE prod_task 
    DROP COLUMN soak_start_time,
    DROP COLUMN soak_end_time,
    DROP COLUMN decoct_start_time,
    DROP COLUMN decoct_end_time,
    DROP COLUMN pour_start_time,
    DROP COLUMN pour_end_time,
    DROP COLUMN wrap_start_time,
    DROP COLUMN wrap_end_time;

ALTER TABLE prod_step_log MODIFY COLUMN device_id BIGINT;
```

### 12.4 编译检查清单

- [ ] `dms-common` 编译通过（新增 `BaseAuditEntity`、`InspectionResultType`、`ProductionQueryService`）
- [ ] `dms-equipment` 编译通过（新增 `reserveDevice` 实现）
- [ ] `dms-production` 编译通过（字段精简、枚举替换、悲观锁）
- [ ] `dms-quality` 编译通过（调用 `ProductionQueryService` 替代直接 import）
- [ ] `dms-print` 编译通过（调用 `ProductionQueryService` 替代直接 import）
- [ ] `mvn test -pl dms-production` 通过
- [ ] 集成测试：`startDecoct` 并发绑定同一设备，仅一个成功

---

## 附录 A：新增/修改枚举和常量汇总

### A.1 InspectionResultType（dms-common）

```java
package cn.org.openygt.common.enums;

public enum InspectionResultType {
    PASS,        // 通过
    CONCESSION,  // 让步放行
    REWORK,      // 返工
    SCRAP        // 报废
}
```

### A.2 BaseAuditEntity（dms-common）

```java
package cn.org.openygt.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.util.Date;

/**
 * 审计实体基类（无逻辑删除）。
 * 用于 prod_task_status_history 等审计表。
 */
@Data
public abstract class BaseAuditEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String tenantId = "default";
    private Date createdAt;
    private Date updatedAt;
}
```

### A.3 EquipmentService 新增方法

```java
/**
 * 预留设备给指定任务（返工场景）。
 * 设备状态变为 RESERVED，关联到 taskId，阻止其他任务绑定。
 *
 * @param taskId   任务 ID
 * @param deviceId 设备 ID
 * @throws IllegalStateException 设备不在 IDLE 状态
 */
void reserveDevice(Long taskId, Long deviceId);
```

---

> **文档结束**。开发人员应按此文档逐项实施，每完成一项在迁移清单中打勾。
