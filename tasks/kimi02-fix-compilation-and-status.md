# kimi02 任务单: 修复编译错误 + 统一状态名称

> 优先级: P0 (阻断性)
> 截止: 立即
> 依赖: 无

## 任务概述

当前 `mvn compile` 失败，原因是 `TaskServiceImpl` 未实现 `TaskService` 接口中的 8 个方法。
同时状态名称存在英文/中文混用问题，需要统一为中文。

## 任务 1: 统一状态名称为中文

将 `TaskServiceImpl.java` 中所有英文状态名替换为中文，对照表:

| 英文 (当前) | 中文 (目标) |
|------------|------------|
| PENDING | 待泡药 |
| SOAKING | 泡药中 |
| SOAKED | 待煎药 |
| PROCESSING | 煎药中 |
| DECOCTED | 待出液 |
| POURING | 出液中 |
| WRAPPING (endPour后) | 待包装 |
| WRAPPING (startWrap后) | 包装中 |
| COMPLETED (endWrap后) | 待贴标 |
| BOUND | 待煎药 |

### 需要修改的文件:
1. `src/main/java/com/decoction/service/impl/TaskServiceImpl.java` - 主要修改
2. `src/main/java/com/decoction/controller/PdaController.java` - 第44行的英文状态列表
3. `src/main/java/com/decoction/scheduler/SoakTimeoutScheduler.java` - 第33行 "SOAKING" → "泡药中"

### 关键修改点:

**TaskServiceImpl.java 状态转换修改:**

```java
// startSoak: PENDING → SOAKING 改为 待泡药 → 泡药中
assertStatus(task, "待泡药");
transition(task, "泡药中", operatorId, "开始泡药");

// endSoak: SOAKING → SOAKED 改为 泡药中 → 待煎药
assertStatus(task, "泡药中");
transition(task, "待煎药", operatorId, "结束泡药");

// startDecoct: SOAKED → PROCESSING 改为 待煎药 → 煎药中
assertStatus(task, "待煎药");
transition(task, "煎药中", operatorId, "开始煎药...");

// endDecoct: PROCESSING → DECOCTED 改为 煎药中 → 待出液
assertStatus(task, "煎药中");
transition(task, "待出液", operatorId, "煎药结束");

// startPour: DECOCTED → POURING 改为 待出液 → 出液中
assertStatus(task, "待出液");
transition(task, "出液中", operatorId, "开始出液");

// endPour: POURING → WRAPPING 改为 出液中 → 待包装 (注意不是包装中!)
assertStatus(task, "出液中");
transition(task, "待包装", operatorId, "出液结束");

// startWrap: WRAPPING → WRAPPING 改为 待包装 → 包装中
assertStatus(task, "待包装");
transition(task, "包装中", operatorId, "开始包装...");

// endWrap: WRAPPING → COMPLETED 改为 包装中 → 待贴标 (注意不是已完成!)
assertStatus(task, "包装中");
transition(task, "待贴标", operatorId, "包装结束");
// 注意: endWrap 不再设置 completeTime，因为流程还没结束
```

**endWrap 特别注意:**
- 删除 `task.setCompleteTime(new Date())` 这行
- 状态转到 `"待贴标"` 而非 `"COMPLETED"`
- 自动打印逻辑保留，但打印失败不影响状态转换

**bindDevice 修改:**
```java
// 状态判断改为中文
if (!"待泡药".equals(task.getStatus()) && !"待煎药".equals(task.getStatus())) {
    throw new IllegalStateException("任务状态不允许绑定设备");
}
// BOUND → 待煎药
task.setStatus("待煎药");
recordHistory(taskId, fromStatus, "待煎药", null, "绑定设备: " + deviceCode);
```

**updateTemperature 修改:**
```java
if ("待煎药".equals(task.getStatus())) {
    task.setStatus("煎药中");
    recordHistory(task.getId(), "待煎药", "煎药中", null, "温度上报自动推进");
}
```

**printLabel/queryPrintTasks 修改:**
```java
// "COMPLETED" → "已完成" (但注意: 打印应该在待贴标之后，需要确认业务逻辑)
// 暂时保持 printLabel 中的状态检查为 "待贴标" 或之后的状态
```

## 任务 2: 实现 8 个缺失方法

在 `TaskServiceImpl.java` 中添加以下注入和方法。

### 新增 Mapper 注入:
```java
private final StepLogMapper stepLogMapper;
private final HandoverDetailMapper handoverDetailMapper;
```

### 实现方法 (参考 openygt-dms):

#### 2.1 confirmLabel
```java
@Override
@Transactional
public Task confirmLabel(Long taskId, String operatorId) {
    Task task = getTaskOrThrow(taskId);
    assertStatus(task, "待贴标");
    transition(task, "待质检", operatorId, "贴标完成");
    taskMapper.updateById(task);
    createStepLog(taskId, "LABEL", null, operatorId, null);
    closeLastStepLog(taskId, "LABEL", "正常", null);
    return task;
}
```

#### 2.2 qualityInspect
```java
@Override
@Transactional
public Task qualityInspect(Long taskId, String result, String operatorId, String remark) {
    Task task = getTaskOrThrow(taskId);
    assertStatus(task, "待质检");
    switch (result) {
        case "通过":
            transition(task, "待交接", operatorId, "质检通过" + (remark != null ? ": " + remark : ""));
            break;
        case "让步放行":
            transition(task, "待交接", operatorId, "质检让步放行" + (remark != null ? ": " + remark : ""));
            task.setIsException(1);
            task.setExceptionReason(remark);
            break;
        case "返工":
            transition(task, "待煎药", operatorId, "质检返工" + (remark != null ? ": " + remark : ""));
            break;
        case "报废":
            transition(task, "已报废", operatorId, "质检报废" + (remark != null ? ": " + remark : ""));
            task.setIsException(1);
            task.setExceptionReason(remark);
            break;
        default:
            throw new IllegalArgumentException("未知的质检结果: " + result);
    }
    taskMapper.updateById(task);
    createStepLog(taskId, "INSPECT", null, operatorId, null);
    closeLastStepLog(taskId, "INSPECT", result, remark);
    return task;
}
```

#### 2.3 handover
```java
@Override
@Transactional
public Task handover(Long taskId, Integer bagCount, String handoverType, String handoverUser, String remark, Boolean isFinal) {
    Task task = getTaskOrThrow(taskId);
    if (!"待交接".equals(task.getStatus()) && !"已部分完成".equals(task.getStatus()) && !"已完成".equals(task.getStatus())) {
        throw new IllegalStateException("任务不在待交接状态，当前状态: " + task.getStatus());
    }
    HandoverDetail detail = new HandoverDetail();
    detail.setTaskId(taskId);
    detail.setBagCount(bagCount);
    detail.setHandoverType(handoverType);
    detail.setHandoverUser(handoverUser);
    detail.setHandoverTime(new Date());
    detail.setRemark(remark);
    handoverDetailMapper.insert(detail);

    task.setHandoverType(handoverType);
    task.setHandoverUser(handoverUser);
    task.setHandoverTime(new Date());

    boolean finalFlag = isFinal != null && isFinal;
    String targetStatus = finalFlag ? "已完成" : "已部分完成";
    transition(task, targetStatus, handoverUser, "扫码交接: " + handoverType + ", 袋数=" + bagCount + (finalFlag ? " (完成)" : " (部分)"));
    if (finalFlag) {
        task.setCompleteTime(new Date());
    }
    taskMapper.updateById(task);
    createStepLog(taskId, "HANDOVER", null, handoverUser, null);
    closeLastStepLog(taskId, "HANDOVER", "正常", remark);
    return task;
}
```

#### 2.4 pauseStep / resumeStep / queryStepLogs / queryHandoverDetails
```java
@Override
@Transactional
public StepLog pauseStep(Long stepLogId, String reason) {
    StepLog step = stepLogMapper.selectById(stepLogId);
    if (step == null) throw new IllegalArgumentException("工序记录不存在");
    step.setIsPaused(1);
    step.setPauseReason(reason);
    stepLogMapper.updateById(step);
    return step;
}

@Override
@Transactional
public StepLog resumeStep(Long stepLogId) {
    StepLog step = stepLogMapper.selectById(stepLogId);
    if (step == null) throw new IllegalArgumentException("工序记录不存在");
    int pausedMinutes = calculateDuration(step.getUpdatedAt(), new Date());
    step.setPauseDuration((step.getPauseDuration() != null ? step.getPauseDuration() : 0) + pausedMinutes);
    step.setIsPaused(0);
    stepLogMapper.updateById(step);
    return step;
}

@Override
public List<StepLog> queryStepLogs(Long taskId) {
    LambdaQueryWrapper<StepLog> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(StepLog::getTaskId, taskId).orderByAsc(StepLog::getStartedAt);
    return stepLogMapper.selectList(wrapper);
}

@Override
public List<HandoverDetail> queryHandoverDetails(Long taskId) {
    LambdaQueryWrapper<HandoverDetail> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(HandoverDetail::getTaskId, taskId).orderByDesc(HandoverDetail::getHandoverTime);
    return handoverDetailMapper.selectList(wrapper);
}
```

#### 2.5 forceStatus
```java
@Override
@Transactional
public Task forceStatus(Long taskId, String targetStatus, String operatorId, String deviceCode, String remark) {
    Task task = getTaskOrThrow(taskId);
    String oldStatus = task.getStatus();
    Date now = new Date();
    if ("煎药中".equals(targetStatus) && deviceCode != null && !deviceCode.isEmpty()) {
        Device device = getOrCreateDevice(deviceCode, 1);
        device.setStatus("BUSY");
        deviceMapper.updateById(device);
        task.setDecoctDeviceId(device.getId());
        task.setDecoctStartTime(now);
    } else if ("包装中".equals(targetStatus) && deviceCode != null && !deviceCode.isEmpty()) {
        Device device = getOrCreateDevice(deviceCode, 2);
        device.setStatus("BUSY");
        deviceMapper.updateById(device);
        task.setPackageDeviceId(device.getId());
        task.setWrapStartTime(now);
    }
    task.setStatus(targetStatus);
    task.setOperatorId(operatorId);
    taskMapper.updateById(task);
    recordHistory(taskId, oldStatus, targetStatus, operatorId, "【强制操作】" + oldStatus + " → " + targetStatus + (remark != null ? " | " + remark : ""));
    return task;
}
```

### 新增辅助方法:
```java
private void createStepLog(Long taskId, String stepType, String deviceId, String operatorId, Long parentId) {
    StepLog step = new StepLog();
    step.setTaskId(taskId);
    step.setStepType(stepType);
    step.setDeviceId(deviceId);
    step.setOperatorId(operatorId);
    step.setParentId(parentId);
    step.setStartedAt(new Date());
    step.setResult("正常");
    stepLogMapper.insert(step);
}

private void closeLastStepLog(Long taskId, String stepType, String result, String abortReason) {
    LambdaQueryWrapper<StepLog> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(StepLog::getTaskId, taskId)
           .eq(StepLog::getStepType, stepType)
           .isNull(StepLog::getEndedAt)
           .orderByDesc(StepLog::getStartedAt)
           .last("LIMIT 1");
    StepLog step = stepLogMapper.selectOne(wrapper);
    if (step != null) {
        step.setEndedAt(new Date());
        step.setResult(result);
        step.setAbortReason(abortReason);
        stepLogMapper.updateById(step);
    }
}

private Device getOrCreateDevice(String deviceCode, int deviceType) {
    Device device = deviceMapper.selectOne(
            new LambdaQueryWrapper<Device>().eq(Device::getDeviceCode, deviceCode));
    if (device == null) {
        device = new Device();
        device.setDeviceCode(deviceCode);
        device.setName(deviceCode);
        device.setDeviceType(deviceType);
        device.setStatus("IDLE");
        device.setAlarmMinTemp(BigDecimal.ZERO);
        device.setAlarmMaxTemp(BigDecimal.valueOf(120));
        deviceMapper.insert(device);
    }
    return device;
}
```

## 任务 3: 在现有阶段方法中添加 StepLog 记录

参考 openygt-dms，在每个阶段的 start/end 方法中添加 StepLog 记录:

```java
// startSoak 末尾添加:
createStepLog(taskId, "SOAK", null, operatorId, null);

// endSoak 末尾添加:
closeLastStepLog(taskId, "SOAK", "正常", null);

// startDecoct 末尾添加:
createStepLog(taskId, "DECOCT", deviceCode, operatorId, null);

// endDecoct 末尾添加:
closeLastStepLog(taskId, "DECOCT", "正常", null);

// startPour 末尾添加:
createStepLog(taskId, "POUR", null, operatorId, null);

// endPour 末尾添加:
closeLastStepLog(taskId, "POUR", "正常", null);

// startWrap 末尾添加:
createStepLog(taskId, "WRAP", deviceCode, operatorId, null);

// endWrap 末尾添加:
closeLastStepLog(taskId, "WRAP", "正常", null);
```

## 验收标准

1. `mvn compile` 通过
2. `mvn test` 全部通过
3. 所有状态名称统一为中文
4. 完整流程: 待泡药→泡药中→待煎药→煎药中→待出液→出液中→待包装→包装中→待贴标→待质检→待交接→已完成
5. StepLog 在每个阶段都有记录
