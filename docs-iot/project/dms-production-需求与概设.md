# dms-production 需求与概要设计

> 模块类型：核心业务模块  
> 包名：`cn.org.openygt.production`  
> 表前缀：`prod_`  
> API 前缀：`/api/v1/prod`  
> 职责：煎药全流程生产执行，包括处方接收、任务状态机驱动、工序记录、交接管理、强制操作、自动超时/温度推进。  
> 版本：V1.4（专家评审04后修订版）

---

## 一、模块定位

`dms-production` 是系统的**业务核心**，实现了从处方接收到最终交接的完整 12 步煎药生产流程。通过严格的状态机控制任务流转，记录每一步的工序日志和状态历史，并与其他模块协同完成设备绑定、打印、质检等操作。

**模块边界**：
- ✅ 负责 `prod_` 前缀表的管理
- ✅ 核心状态机的唯一控制者
- ✅ 调用 `EquipmentService` 进行设备绑定/释放/温度处理
- ✅ 调用 `PrintService` 提交打印任务
- ✅ 调用 `QualityService` 执行质检
- ❌ 不直接操作 `eq_`、`prt_`、`qt_` 表（必须通过 SPI）
- ❌ 不管理医院/方案/用户等主数据

---

## 二、需求清单

### 2.1 已实现需求

| 需求编号 | 需求名称 | 优先级 | 状态 | 验收标准 |
|---------|---------|--------|------|---------|
| PROD-001 | 处方接收与创建 | P0 | ✅ | 关联医院+方案，生成任务 |
| PROD-002 | 12 步状态机核心 | P0 | ✅ | 完整流转：待泡药 → 已完成/报废 |
| PROD-003 | 设备绑定与释放 | P0 | ✅ | 煎药机/包装机绑定，任务结束自动释放 |
| PROD-004 | 工序记录（StepLog） | P0 | ✅ | 每阶段开始/结束记录 |
| PROD-005 | 状态变更审计历史 | P0 | ✅ | 每次状态变更写入 history |
| PROD-006 | 工时记录 | P0 | ✅ | 各阶段操作人+耗时 |
| PROD-007 | 交接管理 | P0 | ✅ | 支持多次交接、部分完成/最终完成 |
| PROD-008 | 强制状态变更 | P0 | ✅ | 管理员跳转状态，记录原因 |
| PROD-009 | 泡药超时自动推进 | P0 | ✅ | 定时调度扫描，超时时自动调用 endSoak |
| PROD-010 | 温度上报自动推进 | P0 | ✅ | 设备温度上报时，待煎药 → 煎药中 |

### 2.2 待开发需求

| 需求编号 | 需求名称 | 优先级 | 状态 | 验收标准 |
|---------|---------|--------|------|---------|
| **PROD-016** | **`prod_task`字段精简：步骤时间迁到`step_log`** | **P1** | ⏳ | **主表只留当前状态，历史时间由step_log推导** |
| **PROD-017** | **状态机补全：全状态超时配置+告警** | **P1** | ⏳ | **每个状态配超时阈值，超时自动告警+人工确认，不自动跳** |
| **PROD-018** | **返工设备预留（不释放）** | **P0** | ⏳ | **调用`EquipmentService.reserveDevice()`标记预留** |
| **PROD-019** | **并发控制：synchronized→数据库悲观锁** | **P1** | ⏳ | **设备绑定用SELECT FOR UPDATE** |
| PROD-011 | 处方批量导入 | P2 | ⏳ | Excel/HIS 接口导入 |
| PROD-012 | 处方退单 | P2 | ⏳ | 未开始生产的处方可退单 |
| PROD-013 | 任务优先级 | P2 | ⏳ | 紧急任务插队 |
| PROD-014 | 损耗记录 | P2 | ⏳ | 各阶段损耗量记录 |
| PROD-015 | 生产看板 | P2 | ⏳ | 实时任务状态大屏数据接口 |

---

## 三、数据库设计

### 3.1 prod_prescription（处方/医嘱表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | INTEGER | PK, AUTOINCREMENT | 主键 |
| tenant_id | VARCHAR(32) | DEFAULT 'default' | 多租户预留 |
| hospital_id | INTEGER | FK → md_hospital | 医院 ID |
| scheme_id | INTEGER | FK → md_decoct_scheme | 煎药方案 ID |
| prescription_no | VARCHAR(50) | UNIQUE | 处方编号 |
| patient_name | VARCHAR(50) | | 患者姓名 |
| patient_phone | VARCHAR(20) | | 患者电话 |
| total_dose | INTEGER | | 总剂数 |
| status | VARCHAR(20) | DEFAULT 'PENDING' | PENDING / PROCESSING / COMPLETED / CANCELLED |
| created_at | DATETIME | | 创建时间 |
| updated_at | DATETIME | | 更新时间 |
| deleted | INTEGER | DEFAULT 0 | 逻辑删除 |

**索引**：`UNIQUE(prescription_no)`

### 3.2 prod_task（生产任务表）— 状态机核心

> **评审修订**：原设计有12个时间字段（soak_start/end, decoct_start/end等），过于冗余。  
> **修正方案**：主表只保留**当前阶段**的时间和核心终态时间，历史步骤时间由 `prod_step_log` 推导。减少字段冗余，提升表结构清晰度。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | INTEGER | PK, AUTOINCREMENT | 主键 |
| tenant_id | VARCHAR(32) | DEFAULT 'default' | 多租户预留 |
| prescription_id | INTEGER | FK → prod_prescription | 处方 ID |
| status | VARCHAR(20) | NOT NULL | 当前状态（TaskStatus 枚举值） |
| decoct_device_id | INTEGER | FK → eq_device | 煎药机 ID |
| package_device_id | INTEGER | FK → eq_device | 包装机 ID |
| current_temp | DECIMAL(5,2) | | 当前温度 |
| current_stage_duration | INTEGER | | 当前阶段已耗时（分钟） |
| operator_id | VARCHAR(50) | | 当前操作人 |
| print_status | VARCHAR(20) | DEFAULT 'PENDING' | PENDING / PRINTING / SUCCESS / FAILED |
| is_exception | INTEGER | DEFAULT 0 | 是否异常（0/1） |
| exception_reason | VARCHAR(200) | | 异常原因 |
| handover_type | VARCHAR(50) | | 交接方式 |
| handover_user | VARCHAR(50) | | 交接人 |
| handover_time | DATETIME | | 交接时间 |
| ~~soak_start_time~~ | ~~DATETIME~~ | | ~~迁到 step_log~~ |
| ~~soak_end_time~~ | ~~DATETIME~~ | | ~~迁到 step_log~~ |
| ~~decoct_start_time~~ | ~~DATETIME~~ | | ~~迁到 step_log~~ |
| ~~decoct_end_time~~ | ~~DATETIME~~ | | ~~迁到 step_log~~ |
| ~~pour_start_time~~ | ~~DATETIME~~ | | ~~迁到 step_log~~ |
| ~~pour_end_time~~ | ~~DATETIME~~ | | ~~迁到 step_log~~ |
| ~~wrap_start_time~~ | ~~DATETIME~~ | | ~~迁到 step_log~~ |
| ~~wrap_end_time~~ | ~~DATETIME~~ | | ~~迁到 step_log~~ |
| stage_start_time | DATETIME | | **当前阶段开始时间（通用）** |
| complete_time | DATETIME | | 完成时间（终态） |
| created_at | DATETIME | | 创建时间 |
| updated_at | DATETIME | | 更新时间 |
| deleted | INTEGER | DEFAULT 0 | 逻辑删除 |

**索引**：`INDEX(status)`, `INDEX(decoct_device_id)`, `INDEX(prescription_id)`

> **时间字段迁移说明**：泡药/煎药/出液/包装等步骤的开始/结束时间，统一记录到 `prod_step_log` 的 `started_at` / `ended_at` 字段。`prod_task` 仅保留当前阶段的 `stage_start_time` 用于计算耗时，以及 `complete_time` 记录终态时间。查询历史时间时通过 `step_log` 关联获取。

### 3.3 prod_task_status_history（状态变更历史表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | INTEGER | PK, AUTOINCREMENT | 主键 |
| tenant_id | VARCHAR(32) | DEFAULT 'default' | 多租户预留 |
| task_id | INTEGER | NOT NULL | 任务 ID |
| from_status | VARCHAR(20) | | 变更前状态 |
| to_status | VARCHAR(20) | | 变更后状态 |
| operator_id | VARCHAR(50) | | 操作人 |
| operate_time | DATETIME | | 操作时间 |
| remark | TEXT | | 备注 |

> **评审修订03**：状态变更历史是审计数据，**不应逻辑删除**（无 `deleted` 字段）。`TaskStatusHistory` 实体继承 `BaseAuditEntity`（不含 `@TableLogic`），而非 `BaseEntity`。审计数据一旦被逻辑删除，状态机的完整性追溯就断了。

**索引**：`INDEX(task_id, operate_time)`

### 3.4 prod_step_log（工序记录表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | INTEGER | PK, AUTOINCREMENT | 主键 |
| tenant_id | VARCHAR(32) | DEFAULT 'default' | 多租户预留 |
| task_id | INTEGER | NOT NULL | 任务 ID |
| step_type | VARCHAR(20) | | SOAK / DECOCT / POUR / WRAP / LABEL / INSPECT / HANDOVER |
| device_id | INTEGER | | **使用的设备 ID（Long，与 prod_task.decoct_device_id 类型统一）** |
| operator_id | VARCHAR(50) | | 操作人 |

> **评审修订03**：`device_id` 原定义为 `VARCHAR(50)`（存设备编码），但 `prod_task.decoct_device_id` 是 `INTEGER`（存设备 ID）。同一模块内同一概念用了两种标识方式，查询关联时需要额外转换。**统一为 `INTEGER`（Long）存设备 ID**，与 `EquipmentService` SPI 的参数类型一致。
| started_at | DATETIME | | 开始时间 |
| ended_at | DATETIME | | 结束时间 |
| result | VARCHAR(20) | DEFAULT '正常' | 正常 / 异常 / 暂停 |
| abort_reason | VARCHAR(200) | | 中断/异常原因 |
| is_paused | INTEGER | DEFAULT 0 | 是否暂停 |
| pause_reason | VARCHAR(200) | | 暂停原因 |
| pause_duration | INTEGER | DEFAULT 0 | 暂停时长（分钟） |
| parent_id | INTEGER | | 父记录 ID（返工关联） |
| created_at | DATETIME | | 创建时间 |
| updated_at | DATETIME | | 更新时间 |
| deleted | INTEGER | DEFAULT 0 | 逻辑删除 |

**索引**：`INDEX(task_id, step_type, started_at)`

### 3.5 prod_work_record（工时记录表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | INTEGER | PK, AUTOINCREMENT | 主键 |
| tenant_id | VARCHAR(32) | DEFAULT 'default' | 多租户预留 |
| task_id | INTEGER | NOT NULL | 任务 ID |
| operator_id | VARCHAR(50) | | 操作人 ID |
| operator_name | VARCHAR(50) | | 操作人姓名 |
| action | VARCHAR(20) | | SOAK / DECOCT / POUR / WRAP / LABEL / INSPECT / HANDOVER |
| work_time | INTEGER | | 工时（分钟） |
| created_at | DATETIME | | 创建时间 |
| deleted | INTEGER | DEFAULT 0 | 逻辑删除 |

### 3.6 prod_handover_detail（交接明细表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | INTEGER | PK, AUTOINCREMENT | 主键 |
| tenant_id | VARCHAR(32) | DEFAULT 'default' | 多租户预留 |
| task_id | INTEGER | NOT NULL | 任务 ID |
| bag_count | INTEGER | | 本次交接袋数 |
| handover_type | VARCHAR(50) | | 交接方式（如：自取/快递/配送） |
| handover_user | VARCHAR(50) | | 交接人 |
| handover_time | DATETIME | | 交接时间 |
| remark | TEXT | | 备注 |
| created_at | DATETIME | | 创建时间 |
| deleted | INTEGER | DEFAULT 0 | 逻辑删除 |

---

## 四、核心状态机设计

### 4.1 状态定义

使用 `cn.org.openygt.common.enums.TaskStatus` 枚举：

```java
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
```

### 4.2 状态流转矩阵

| 当前状态 | 允许的操作 | 目标状态 | 触发条件 |
|---------|-----------|---------|---------|
| 待泡药 | startSoak | 泡药中 | 操作人确认开始泡药 |
| 泡药中 | endSoak | 待煎药 | 操作人确认 / 超时自动 |
| 待煎药 | startDecoat | 煎药中 | 绑定煎药机 / 温度上报自动 |
| 煎药中 | endDecoct | 待出液 | 煎药完成；auto级设备自动出液 |
| 待出液 | startPour | 出液中 | 操作人确认；auto级自动 |
| 出液中 | endPour | 待包装 | 出液完成；auto级自动包装 |
| 待包装 | startWrap | 包装中 | 绑定包装机 |
| 包装中 | endWrap | 待贴标 | 包装完成 |
| 待贴标 | confirmLabel | 待质检 | 贴标完成 |
| 待质检 | qualityInspect → 通过 | 待交接 | 质检通过 |
| 待质检 | qualityInspect → 让步 | 待交接 | 标记异常 |
| 待质检 | qualityInspect → 返工 | 待煎药 | 回退重新煎药 |
| 待质检 | qualityInspect → 报废 | 已报废 | 终止 |
| 待交接 | handover (非最终) | 已部分完成 | 多次交接 |
| 待交接 | handover (最终) | 已完成 | 最终交接 |
| 已部分完成 | handover (最终) | 已完成 | 最终交接 |

### 4.3 自动流转规则

1. **泡药超时自动推进**：`SoakTimeoutScheduler` 每分钟扫描，若 `stage_start_time` 超过 `sys_config.soak.timeout.minutes`（默认 30 分钟），自动调用 `endSoak()`。
2. **温度上报自动推进**：`updateTemperature()` 收到设备温度上报时，若任务状态为"待煎药"，自动变为"煎药中"。
3. **auto 级设备自动级联**：`endDecoct` 后若煎药机 `auto_level=auto`，自动调用 `startPour`；`endPour` 后同理自动调用 `startWrap`。

### 4.4 全状态超时配置与告警（PROD-017，评审新增）

> **专家评审结论**：状态机缺全状态超时，需每个状态配超时阈值，超时时自动告警+人工确认，**不自动跳转**（防误操作）。

**超时配置表（`sys_config` 扩展）**：

| config_key | 默认值 | 说明 |
|-----------|--------|------|
| `timeout.WAIT_SOAK.minutes` | `1440` | 待泡药超时时长（24小时） |
| `timeout.SOAKING.minutes` | `30` | 泡药中超时时长 |
| `timeout.WAIT_DECOCT.minutes` | `60` | 待煎药超时时长 |
| `timeout.DECOCTING.minutes` | `120` | 煎药中超时时长 |
| `timeout.WAIT_POUR.minutes` | `30` | 待出液超时时长 |
| `timeout.POURING.minutes` | `20` | 出液中超时时长 |
| `timeout.WAIT_WRAP.minutes` | `60` | 待包装超时时长 |
| `timeout.WRAPPING.minutes` | `30` | 包装中超时时长 |
| `timeout.WAIT_LABEL.minutes` | `30` | 待贴标超时时长 |
| `timeout.WAIT_QC.minutes` | `60` | 待质检超时时长 |
| `timeout.WAIT_HANDOVER.minutes` | `1440` | 待交接超时时长（24小时） |

**超时处理流程**：

```
TaskTimeoutScheduler 每分钟扫描
    ↓
查询状态不在终态（已完成/已部分完成/已报废）的任务
    ↓
计算当前状态持续时间 = now - stage_start_time
    ↓
若持续时间 > 对应状态的超时阈值
    ↓
写入 prod_task_status_history（标记为【超时告警】）
    ↓
产生系统告警（推送到前端/大屏）
    ↓
**不自动跳转状态**，等待人工确认或强制操作
```

### 4.4 强制操作

`forceStatus()` 允许管理员绕过正常状态流转，直接将任务置为任意状态。

**约束**：
- 必须记录操作人、原因
- 必须写入 `prod_task_status_history`，标记为【强制操作】
- 若强制置为"煎药中"或"包装中"，需同步绑定设备

---

## 五、接口设计

### 5.1 处方管理

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/prod/prescriptions` | 创建处方 |
| GET | `/api/v1/prod/prescriptions/{id}` | 查询处方 |
| GET | `/api/v1/prod/prescriptions` | 分页查询 |

### 5.2 任务状态机

| 方法 | 路径 | 说明 | 请求体 |
|------|------|------|--------|
| POST | `/api/v1/prod/tasks/{id}/soak/start` | 开始泡药 | `{operatorId}` |
| POST | `/api/v1/prod/tasks/{id}/soak/end` | 结束泡药 | `{operatorId}` |
| POST | `/api/v1/prod/tasks/{id}/decoct/start` | 开始煎药 | `{deviceCode, operatorId}` |
| POST | `/api/v1/prod/tasks/{id}/decoct/end` | 结束煎药 | `{operatorId}` |
| POST | `/api/v1/prod/tasks/{id}/pour/start` | 开始出液 | `{operatorId}` |
| POST | `/api/v1/prod/tasks/{id}/pour/end` | 结束出液 | `{operatorId}` |
| POST | `/api/v1/prod/tasks/{id}/wrap/start` | 开始包装 | `{deviceCode, operatorId}` |
| POST | `/api/v1/prod/tasks/{id}/wrap/end` | 结束包装 | `{operatorId}` |
| POST | `/api/v1/prod/tasks/{id}/label` | 确认贴标 | `{operatorId}` |
| POST | `/api/v1/prod/tasks/{id}/inspect` | 质检 | `{result, operatorId, remark}` |
| POST | `/api/v1/prod/tasks/{id}/handover` | 交接 | `{bagCount, handoverType, handoverUser, remark, isFinal}` |

### 5.3 任务查询与管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/prod/tasks` | 分页查询（status/deviceId 过滤） |
| GET | `/api/v1/prod/tasks/{id}` | 查询任务详情 |
| POST | `/api/v1/prod/tasks/{id}/force` | 强制变更状态 |
| POST | `/api/v1/prod/tasks/{id}/bind` | 绑定设备 |

### 5.4 打印委托

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/prod/tasks/{id}/print` | 提交打印 |
| POST | `/api/v1/prod/tasks/{id}/print/retry` | 重试打印 |
| GET | `/api/v1/prod/tasks/print-queue` | 打印队列查询 |

### 5.5 工序与交接记录

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/prod/tasks/{id}/steps` | 查询工序记录 |
| GET | `/api/v1/prod/tasks/{id}/handovers` | 查询交接明细 |
| POST | `/api/v1/prod/steps/{id}/pause` | 暂停工序 |
| POST | `/api/v1/prod/steps/{id}/resume` | 恢复工序 |

---

## 六、核心类结构

```
cn.org.openygt.production
├── controller
│   ├── PrescriptionController.java
│   └── TaskController.java
├── entity
│   ├── Prescription.java
│   ├── Task.java
│   ├── TaskStatusHistory.java
│   ├── WorkRecord.java
│   ├── StepLog.java
│   └── HandoverDetail.java
├── mapper
│   ├── PrescriptionMapper.java
│   ├── TaskMapper.java
│   ├── TaskStatusHistoryMapper.java
│   ├── WorkRecordMapper.java
│   ├── StepLogMapper.java
│   └── HandoverDetailMapper.java
├── service
│   ├── PrescriptionService.java / PrescriptionServiceImpl.java
│   ├── TaskService.java / TaskServiceImpl.java   # 状态机核心
│   └── StepLogService.java / StepLogServiceImpl.java
├── scheduler
│   └── SoakTimeoutScheduler.java   # 泡药超时自动推进
└── dto
    ├── PrescriptionCreateRequest.java
    ├── DecoctStartRequest.java
    ├── DeviceBindRequest.java
    ├── ForceRequest.java
    ├── HandoverRequest.java
    ├── QualityInspectRequest.java
    ├── SoakRequest.java
    └── StageRequest.java
```

---

## 七、核心流程

### 7.1 生产任务创建流程

```
接收处方创建请求
    ↓
校验 hospital_id 和 scheme_id 存在
    ↓
创建 prod_prescription
    ↓
为每剂创建 prod_task（status = 待泡药）
    ↓
返回处方 + 任务列表
```

### 7.2 正常状态流转流程（以煎药为例）

```
POST /tasks/{id}/decoct/start
    ↓
TaskService.startDecoct(taskId, deviceCode, operatorId)
    ↓
校验任务状态 = "待煎药"
    ↓
EquipmentService.getDeviceId(deviceCode)
    ↓
若不存在 → EquipmentService.getOrCreateDevice(deviceCode, 1)
    ↓
校验设备状态 = "idle"
    ↓
EquipmentService.updateDeviceStatus(deviceId, "running")
    ↓
transition(task, "煎药中", operatorId, remark)
    ↓
更新 task.decoct_device_id / decoct_start_time
    ↓
写入 prod_task_status_history
    ↓
写入 prod_work_record
    ↓
写入 prod_step_log
    ↓
返回更新后的 Task
```

### 7.3 质检分支流程

```
POST /tasks/{id}/inspect (result = ?)
    ↓
TaskService.qualityInspect(taskId, result, operatorId, remark)
    ↓
校验状态 = "待质检"
    ↓
// result 统一使用 InspectionResultType 枚举名：PASS / CONCESSION / REWORK / SCRAP
switch(result):
    case PASS:
        transition → "待交接"
        EquipmentService.releaseDevice(decoctDeviceId)
    case CONCESSION:
        transition → "待交接"
        task.is_exception = 1
        task.exception_reason = remark
        EquipmentService.releaseDevice(decoctDeviceId)
    case REWORK:
        transition → "待煎药"
        **EquipmentService.reserveDevice(taskId, decoctDeviceId)**
        // 设备不释放，标记预留，防止被其他任务抢占
    case SCRAP:
        transition → "已报废"
        task.is_exception = 1
        task.exception_reason = remark
        EquipmentService.releaseDevice(decoctDeviceId)
    ↓
写入 history / step_log / work_record
    ↓
返回 Task
```

> **评审修订**：返工分支**不释放设备**，调用 `EquipmentService.reserveDevice(taskId, deviceId)` 将设备标记为预留状态，防止被其他任务抢占。通过/让步/报废则正常释放设备。  
> **评审修订03**：`result` 参数统一使用 `InspectionResultType` 枚举名（`PASS`, `CONCESSION`, `REWORK`, `SCRAP`），禁止使用中文字符串做 switch-case。

---

## 八、开发规范

1. **状态机是核心资产**：所有状态变更必须通过 `TaskService` 的方法完成，禁止任何绕过状态机的直接数据库 UPDATE。
2. **事务边界**：每个状态流转方法必须有 `@Transactional`，确保 `task` 更新、`history` 写入、`step_log` 写入原子性。
3. **并发控制（评审强制修订）**：禁止继续使用 `synchronized`。设备绑定/释放改为数据库悲观锁：
   ```java
   @Transactional
   public Task startDecoct(Long taskId, String deviceCode, String operatorId) {
       // ✅ 修订后：已删除 synchronized 关键字
       // 1. 先查询任务并校验状态
       Task task = taskMapper.selectById(taskId);
       assertStatus(task, "待煎药");
       // 2. 数据库悲观锁查询设备
       EqDevice device = eqDeviceMapper.selectForUpdate(deviceId);
       // 3. 校验设备状态
       if (!"IDLE".equals(device.getStatus())) {
           throw new IllegalStateException("设备已被占用");
       }
       // 4. 更新设备状态
       device.setStatus("RUNNING");
       eqDeviceMapper.updateById(device);
       // ... 后续状态流转
   }
   ```
   > **评审修订03**：代码示例中已彻底删除 `synchronized` 关键字，与文字要求保持一致。
4. **SPI 调用**：禁止直接 `import` `dms-equipment/print/quality` 的内部类，统一使用 `@Autowired` 注入 `dms-common` 的 SPI 接口。
5. **时间计算**：使用 `TimeUnit.MILLISECONDS.toMinutes()`，结果为整数分钟，不保留秒。
6. **日志记录**：每个状态变更必须同时写入 `prod_task_status_history` 和 `prod_step_log`（或 `prod_work_record`）。
7. **字段精简**：步骤时间统一写入 `prod_step_log`，`prod_task` 不再保留各阶段独立的时间字段。

---

## 九、集成检查清单

- [ ] `prod_` 前缀业务表（`prod_prescription`、`prod_task`、`prod_step_log`、`prod_work_record`、`prod_handover_detail`）继承 `BaseEntity`
- [ ] `prod_task_status_history` 继承 `BaseAuditEntity`（不含逻辑删除）
- [ ] `TaskServiceImpl` 完整实现了状态机，无状态遗漏
- [ ] 状态变更同时写入 `history`、`step_log`、`work_record`
- [ ] 设备绑定/释放通过 `EquipmentService` SPI 完成
- [ ] 打印通过 `PrintService` SPI 完成
- [ ] 质检通过 `QualityService` SPI 完成
- [ ] `SoakTimeoutScheduler` 每分钟执行，超时时长从 `sys_config` 读取
- [ ] `mvn test` 单测通过，核心状态机方法有单元测试
