# 工程师C 修复回归报告

> **被测 Commit**: `476c138c fix(v2/alarm): 修复 BUG-C-01 / NOTE-C-01`  
> **基线 Commit**: `0bdf7cf0 feat(v2/alarm): 告警改系统内通知（去自动拨号）`  
> **测试人**: 工程师F  
> **回归时间**: 2026-04-27 15:41~15:46  
> **结论**: ❌ **服务启动失败（循环依赖），单元测试通过，禁止合流**

---

## 1. 验证结果

### 1.1 后端单元测试

```text
dms-equipment: Tests run: 62, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS
```

- EquipmentServiceImplTest: 42 个通过
- EqDeviceAlarmServiceImplTest: 8 个通过
- HeartbeatCheckSchedulerTest: 7 个通过
- MqttConfigTest: 5 个通过

**单测结果: 62/62 ✅**

### 1.2 服务启动测试

| 检查项 | 结果 |
|--------|------|
| 空库启动 | ❌ **失败** |
| 存量库启动 | ❌ **失败** |
| MQTT 端到端（温度告警） | 无法执行（服务未启动） |
| MQTT 端到端（离线告警） | 无法执行（服务未启动） |

**启动结果: 失败 ❌**

---

## 2. 阻塞性问题：循环依赖

### 错误信息

```
APPLICATION FAILED TO START

The dependencies of some of the beans in the application context form a cycle:

   eqDeviceController
┌─────┐
|  equipmentServiceImpl
↑     ↓
|  eqDeviceAlarmServiceImpl
└─────┘

Action: Update your application to remove the dependency cycle.
```

### 根因分析

工程师C 在修复 BUG-C-01 时，将 `EquipmentServiceImpl.checkTemperatureAlarm()` 中的直接 mapper 调用改为调用 `EqDeviceAlarmService.createAlarm()`，导致：

- **equipmentServiceImpl → eqDeviceAlarmServiceImpl**（新增依赖）
- **eqDeviceAlarmServiceImpl → equipmentServiceImpl**（原有依赖，构造函数注入）

形成构造器注入循环依赖，Spring Boot 2.7 默认禁止，即使设置 `allow-circular-references=true` 也无法破解构造器循环。

### 建议修复方案（3选1）

| 方案 | 改动量 | 说明 |
|------|--------|------|
| **A. @Lazy 延迟注入** | 小 | 在 `EqDeviceAlarmServiceImpl` 的 `EquipmentService` 构造器参数上加 `@Lazy` |
| **B. 移除 EqDeviceAlarmServiceImpl 对 EquipmentService 的依赖** | 中 | `checkTemperatureAlarm()` 方法不需要 `EquipmentService`，传入 `deviceId` 和 `threshold` 即可 |
| **C. 提取 AlarmEventPublisher 事件层** | 大 | `EquipmentServiceImpl` 发布事件，`EqDeviceAlarmServiceImpl` 监听，彻底解耦 |

**推荐方案A（最快）或方案B（最干净）。**

---

## 3. NOTE-C-01 验证

无法验证，因为服务未启动。但从代码 diff 看，`V2__refactor.sql` 已补充 `is_resolved` / `resolved_at` 列，逻辑正确。

---

## 4. 回归建议

工程师C 必须先修复 **循环依赖** 并确保服务能正常启动后，方可重新提交。重新提交后我将立即执行：

1. `mvn test -pl dms-equipment`（单测回归）
2. 空库启动 + MQTT 端到端验证（温度告警 + 站内通知写入）
3. 全量冒烟（RBAC + 告警链路）

**当前状态: 禁止合流到 `integration`。**
