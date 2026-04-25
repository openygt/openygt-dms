# dms-print 需求与概要设计

> 模块类型：业务模块  
> 包名：`cn.org.openygt.print`  
> 表前缀：`prt_`  
> API 前缀：`/api/v1/prt`  
> 职责：打印任务管理、打印队列、失败重试、打印执行记录。

---

## 一、模块定位

`dms-print` 是系统的**打印中心**，负责管理煎药标签的打印任务。当生产任务到达"待贴标"阶段时，`dms-production` 通过 SPI 调用提交打印任务。`dms-print` 维护打印队列，驱动打印机执行，并记录打印结果。

**模块边界**：
- ✅ 负责 `prt_` 前缀表的管理
- ✅ 实现 `dms-common` 中的 `PrintService` SPI 接口
- ✅ 提供打印队列查询、重试、记录 API
- ❌ 不直接修改 `prod_task` 的状态
- ❌ 不直接操作 `prod_task` 表（历史债务需清理）

**重要历史债务**：当前 `dms-print` 的 `PrintServiceImpl` 中直接 `import` 了 `dms-production` 的 `TaskMapper`、`TaskStatusHistoryMapper`、`Task` 实体，严重违反模块边界。必须在当前迭代中彻底清理。

---

## 二、需求清单

### 2.1 已实现需求

| 需求编号 | 需求名称 | 优先级 | 状态 | 验收标准 |
|---------|---------|--------|------|---------|
| PRT-001 | 打印任务提交 | P0 | ✅ | dms-production 调用 SPI 提交 |
| PRT-002 | 打印队列查询 | P0 | ✅ | 查询待打印任务列表 |
| PRT-003 | 打印失败重试 | P0 | ✅ | 支持重试，记录重试次数 |
| PRT-004 | 打印执行记录 | P0 | ✅ | 记录每次打印结果和错误信息 |

### 2.2 待开发需求

| 需求编号 | 需求名称 | 优先级 | 状态 | 验收标准 |
|---------|---------|--------|------|---------|
| PRT-005 | 打印机管理 | P1 | ⏳ | 打印机注册、状态监控、驱动配置 |
| PRT-006 | 打印模板管理 | P1 | ⏳ | 标签模板 CRUD、字段绑定 |
| PRT-007 | 批量打印 | P1 | ⏳ | 一次提交多个任务的打印 |
| PRT-008 | 打印任务取消 | P1 | ⏳ | 未执行前可取消 |
| PRT-009 | 打印统计报表 | P2 | ⏳ | 打印成功率、打印机利用率 |

---

## 三、数据库设计

### 3.1 prt_task（打印任务表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | INTEGER | PK, AUTOINCREMENT | 主键 |
| tenant_id | VARCHAR(32) | DEFAULT 'default' | 多租户预留 |
| task_id | INTEGER | NOT NULL | 关联的生产任务 ID（逻辑关联 prod_task） |
| device_code | VARCHAR(50) | | 打印机设备编码 |
| operator_id | VARCHAR(50) | | 提交人 |
| status | VARCHAR(20) | DEFAULT 'PENDING' | PENDING / PRINTING / SUCCESS / FAILED / CANCELLED |
| copies | INTEGER | DEFAULT 1 | 打印份数 |
| retry_count | INTEGER | DEFAULT 0 | 已重试次数 |
| max_retry | INTEGER | DEFAULT 3 | 最大重试次数 |
| template_id | INTEGER | | 标签模板 ID（预留） |
| created_at | DATETIME | | 创建时间 |
| updated_at | DATETIME | | 更新时间 |
| deleted | INTEGER | DEFAULT 0 | 逻辑删除 |

**索引**：`INDEX(task_id)`, `INDEX(status)`

### 3.2 prt_record（打印执行记录表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | INTEGER | PK, AUTOINCREMENT | 主键 |
| tenant_id | VARCHAR(32) | DEFAULT 'default' | 多租户预留 |
| print_task_id | INTEGER | NOT NULL | 打印任务 ID |
| result | VARCHAR(20) | | SUCCESS / FAILED |
| error_message | TEXT | | 失败原因 |
| printer_code | VARCHAR(50) | | 实际执行的打印机编码 |
| **operator_id** | **VARCHAR(50)** | | **触发打印/重试的操作人** |
| printed_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | 打印时间 |
| deleted | INTEGER | DEFAULT 0 | 逻辑删除 |

**索引**：`INDEX(print_task_id)`

> **评审修订03**：`prt_record` 新增 `operator_id` 字段，记录是谁触发的打印/重试，满足审计追溯要求。

### 3.3 预留表

| 表名 | 说明 | 阶段 |
|------|------|------|
| prt_printer | 打印机档案 | 阶段二 |
| prt_template | 标签模板 | 阶段二 |
| prt_template_field | 模板字段绑定 | 阶段二 |

---

## 四、接口设计

### 4.1 SPI 接口（由 dms-production 调用）

> 定义位置：`dms-common.PrintService`

```java
public interface PrintService {
    void submitPrintTask(Long taskId, String deviceCode, String operatorId);
    void retryPrint(Long taskId, String deviceCode, String operatorId);
    List<PrintTaskDTO> getPrintQueue();
}
```

> **评审修订03**：`getPrintQueue()` 返回值从 `Object` 收紧为 `List<PrintTaskDTO>`。`PrintTaskDTO` 定义在 `dms-common.dto` 包下。

#### submitPrintTask 实现要求

1. **幂等性**：同一 `taskId` 的重复提交，应返回已有任务或更新状态，不创建重复记录。
2. **创建打印任务**：向 `prt_task` 插入记录，`status = PENDING`
3. **触发打印**：若打印机在线，立即下发打印指令；否则等待队列调度。
4. **不修改 prod_task**：打印完成后通过回调或 SPI 反向通知 `dms-production` 更新 `print_status`（可选，当前由 production 轮询或自行维护）。

#### retryPrint 实现要求

1. **校验**：`retry_count < max_retry`
2. **重试**：`status` 重置为 `PENDING`，`retry_count + 1`
3. **记录**：写入 `prt_record` 标记为重试发起

### 4.2 REST API

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/prt/tasks/{taskId}/submit` | 手动提交打印任务 |
| POST | `/api/v1/prt/tasks/{taskId}/retry` | 手动重试打印 |
| POST | `/api/v1/prt/tasks/{taskId}/cancel` | 取消打印任务（PRT-008） |
| GET | `/api/v1/prt/queue` | 查询打印队列（PENDING/FAILED） |
| GET | `/api/v1/prt/records` | 查询打印记录 |
| GET | `/api/v1/prt/tasks/{taskId}/status` | 查询打印任务状态 |

---

## 五、核心类结构

```
cn.org.openygt.print
├── controller
│   └── PrintController.java
├── entity
│   ├── PrintTask.java
│   └── PrintRecord.java
├── mapper
│   ├── PrintTaskMapper.java
│   └── PrintRecordMapper.java
├── service
│   ├── PrintServiceImpl.java       # 实现 dms-common.PrintService SPI
│   └── PrintQueueService.java      # 队列调度、重试策略
└── dto
    └── PrintSubmitRequest.java
```

### 历史债务修正计划

| 问题 | 影响 | 修正方案 |
|------|------|---------|
| `PrintServiceImpl` import `TaskMapper` | 跨模块数据访问违规 | 删除 `TaskMapper` 依赖，打印如需任务信息通过 `TaskService` SPI 获取或前端传参 |
| `PrintServiceImpl` import `TaskStatusHistoryMapper` | 同上 | 删除引用，状态历史由 `dms-production` 维护 |
| `PrintServiceImpl` import `Task` 实体 | 同上 | 删除引用，使用 `taskId` 逻辑关联 |

**修正后的 PrintServiceImpl 依赖**：

```java
@Service
@RequiredArgsConstructor
public class PrintServiceImpl implements PrintService {
    private final PrintTaskMapper printTaskMapper;
    private final PrintRecordMapper printRecordMapper;
    // 禁止引入 dms-production 的 Mapper/Entity
}
```

---

## 六、核心流程

### 6.1 打印任务提交流程

```
dms-production 调用 PrintService.submitPrintTask(taskId, deviceCode, operatorId)
    ↓
校验 taskId 是否已有未完成的 prt_task（幂等）
    ↓
创建 PrintTask（status = PENDING, retry_count = 0）
    ↓
若打印机在线 → 立即发送打印指令
    ↓
打印机返回结果 → 写入 PrintRecord
    ↓
更新 PrintTask.status = SUCCESS / FAILED
    ↓
若 FAILED 且 retry_count < max_retry → 进入重试队列
```

### 6.2 打印重试流程（评审确认：必须闭环）

> **专家评审对齐结论**：设计已覆盖，但需确认 PrintServiceImpl 中重试逻辑真正闭环（失败→改PENDING→retry_count++→重新下发）。

```
定时调度器 / 手动调用 retryPrint()
    ↓
查询 FAILED 且 retry_count < max_retry 的任务
    ↓
retry_count++，status 重置为 PENDING
    ↓
重新下发打印指令
    ↓
若打印机响应成功：
    status = SUCCESS
    写入 PrintRecord(result=SUCCESS)
    // 可选：回调 dms-production 更新 print_status
    
若打印机响应失败：
    写入 PrintRecord(result=FAILED, errorMessage=...)
    若 retry_count >= max_retry：
        status = FAILED（终态，需人工干预）
    否则：
        status = FAILED（等待下次重试调度）
    ↓
更新 PrintTask.status / retry_count
```

**重试闭环检查清单**：
- [ ] 失败任务状态正确回退到 PENDING
- [ ] retry_count 每次重试正确递增
- [ ] 超过 max_retry 后状态固定为 FAILED，不再自动重试
- [ ] 每次重试（无论成败）都写入 prt_record
- [ ] 重试调度器有异常捕获，单任务失败不影响其他任务

---

## 七、开发规范

1. **彻底解耦**：当前迭代必须清理所有对 `dms-production` Mapper/Entity 的直接引用。
2. **幂等提交**：同一 `taskId` 重复提交打印，不创建重复 `prt_task` 记录。
3. **重试上限**：默认最大重试 3 次，超过后状态固定为 `FAILED`，需人工干预。
4. **异步打印**：打印指令下发应为异步操作，避免阻塞 SPI 调用方。
5. **打印机抽象**：阶段二引入 `prt_printer` 表和打印机驱动抽象，当前可直接操作串口/USB 打印机或调用系统打印服务。

---

## 八、集成检查清单

- [ ] `PrintServiceImpl` 完整实现了 `dms-common.PrintService` 接口
- [ ] 所有 `prt_` 表实体继承 `BaseEntity`
- [ ] **已彻底清理对 `dms-production` Mapper/Entity 的直接引用**
- [ ] 打印任务提交具有幂等性
- [ ] **重试逻辑完全闭环**：失败→PENDING→retry_count++→重新下发→记录结果→终态判断
- [ ] 重试次数有上限控制（max_retry=3）
- [ ] 打印结果正确记录到 `prt_record`
- [ ] `mvn test` 单测通过
