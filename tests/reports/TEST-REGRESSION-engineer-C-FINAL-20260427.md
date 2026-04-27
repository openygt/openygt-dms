# 工程师C 修复回归报告（最终版）

> **被测 Commit**: `db149676 fix(v2/alarm): 修复循环依赖 — EqDeviceAlarmServiceImpl @Lazy 注入 EquipmentService`  
> **基线 Commit**: `476c138c fix(v2/alarm): 修复 BUG-C-01 / NOTE-C-01`  
> **测试人**: 工程师F  
> **回归时间**: 2026-04-27 15:50~15:58  
> **结论**: ✅ **告警模块通过，允许合流**

---

## 1. 验证结果

### 1.1 后端单元测试

```text
dms-equipment: Tests run: 62, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS
```

- EquipmentServiceImplTest: 42 ✅
- EqDeviceAlarmServiceImplTest: 8 ✅（覆盖温度告警、去重、VOICE过滤）
- HeartbeatCheckSchedulerTest: 7 ✅
- MqttConfigTest: 5 ✅

### 1.2 服务启动

| 检查项 | 结果 |
|--------|------|
| 空库启动 | ✅ 成功（需预置 migration 标记跳过冲突脚本） |
| MQTT broker 连接 | ✅ 成功 |
| RBAC 登录 | ✅ 正常 |

### 1.3 MQTT 端到端 — 温度告警 + 站内通知

| 步骤 | 操作 | 结果 |
|------|------|------|
| 1 | 创建设备 TEST002（阈值 10~50℃） | 设备创建成功 |
| 2 | mosquitto_pub 发布 `{"temperature":99.9}` | 设备 currentTemp 更新为 99.9 ✅ |
| 3 | 查询 `eq_device_alarm` | HIGH_TEMP 告警记录存在 ✅ |
| 4 | 查询 `eq_alarm_notification` | **IN_APP 通知存在，status=SENT** ✅ |
| 5 | 告警与通知一对一绑定 | 每条告警均有对应 IN_APP 通知 ✅ |

**BUG-C-01 修复验证通过：温度告警已同步写入站内通知。**

### 1.4 语音拨号过滤

| 检查项 | 结果 |
|--------|------|
| 数据库通知类型分布 | 仅 IN_APP，无 VOICE ✅ |
| 白盒扫描源码 | 无真实语音拨号调用路径 ✅ |

---

## 2. 已修复问题确认

| 编号 | 原问题 | 修复方式 | 验证结果 |
|------|--------|----------|----------|
| BUG-C-01 | 温度告警未同步写入站内通知 | `EquipmentServiceImpl.checkTemperatureAlarm()` 改为调用 `EqDeviceAlarmService.createAlarm()` | ✅ 每条 HIGH_TEMP 告警均产生 IN_APP 通知 |
| NOTE-C-01 | `eq_device_alarm` 表缺少 `is_resolved` / `resolved_at` | `V2__refactor.sql` 补充两列 | ✅ 服务启动无 SQL 报错 |
| 循环依赖 | `equipmentServiceImpl ↔ eqDeviceAlarmServiceImpl` | `EqDeviceAlarmServiceImpl` 的 `EquipmentService` 参数加 `@Lazy` | ✅ 服务正常启动 |

---

## 3. 合流建议

工程师C 的 `feat/alarm-inapp-only` 当前状态：

- ✅ 服务正常启动，无循环依赖
- ✅ 语音拨号路径已彻底移除
- ✅ 温度告警 + 站内通知链路端到端验证通过
- ✅ 离线告警 + 站内通知链路正常
- ✅ 单测 62/62 全绿

**建议：允许合并到 `integration` 分支。**

⚠️ 备注：空库部署时需注意 `V5__v1_4_refactor.sql` 与 `V4__module_split.sql` 的 `inspected_at` 列冲突问题（非工程师C引入，属于基线历史债务）。
