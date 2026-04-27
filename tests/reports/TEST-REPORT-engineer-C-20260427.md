# 工程师C 代码测试报告

> **被测 Commit**: `0bdf7cf0 feat(v2/alarm): 告警改系统内通知（去自动拨号）`  
> **测试人**: 工程师F  
> **测试时间**: 2026-04-27 15:15~15:30  
> **测试环境**: SQLite + mosquitto MQTT broker / localhost:8080

---

## 1. 结果摘要

| 类别 | 用例数 | 通过 | 失败 | 阻塞 | 结论 |
|------|--------|------|------|------|------|
| 后端单测 | 62 | 62 | 0 | 0 | **通过** |
| MQTT 端到端（温度告警） | 3 | 2 | 0 | 1* | **有条件通过** |
| MQTT 端到端（离线告警） | 2 | 2 | 0 | 0 | **通过** |
| 语音拨号过滤 | 2 | 2 | 0 | 0 | **通过** |

*温度告警站内通知未写入，疑似代码路径不一致（见 BUG-C-01）

---

## 2. 通过的测试

### 2.1 后端单元测试

```text
dms-equipment: Tests run: 62, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS
```

覆盖场景（EqDeviceAlarmServiceImplTest 8 个用例）：
- ✅ 高温告警触发 + IN_APP 站内通知写入
- ✅ 告警冷却期去重（5 分钟内同类告警不重复）
- ✅ 温度正常自动消警（resolveActiveAlarms）
- ✅ 离线告警创建
- ✅ VOICE 类型通知被过滤降级为 IN_APP

### 2.2 MQTT 端到端 — 离线告警

| 步骤 | 操作 | 结果 |
|------|------|------|
| 1 | 创建设备 TEST001（阈值 10~50℃） | 设备创建成功 |
| 2 | 等待心跳扫描超时 | 自动产生 OFFLINE 告警 |
| 3 | 查询 `eq_device_alarm` | 告警记录存在，type=OFFLINE |
| 4 | 查询 `eq_alarm_notification` | IN_APP 通知存在，status=SENT |

### 2.3 MQTT 端到端 — 温度告警

| 步骤 | 操作 | 结果 |
|------|------|------|
| 1 | mosquitto_pub 发布 `{"temperature":99.9}` 到 `/openygt/device/TEST001/status` | 设备 currentTemp 更新为 99.9 ✅ |
| 2 | 查询 `eq_device_alarm` | HIGH_TEMP 告警记录存在 ✅ |
| 3 | 查询 `eq_alarm_notification` | **无对应通知** ⚠️（见 BUG-C-01） |

### 2.4 语音拨号过滤

| 检查项 | 结果 |
|--------|------|
| 白盒扫描源码中 VOICE/SIP/拨打/拨号 关键字 | 未检出真实语音调用路径 ✅ |
| 数据库通知类型分布 | 仅 IN_APP，无 VOICE ✅ |
| `filterVoiceNotifyType()` 逻辑 | VOICE 强制降级为 IN_APP ✅ |

---

## 3. 发现的问题

### BUG-C-01 ⚠️ 温度告警未同步写入站内通知

| 项 | 内容 |
|---|---|
| 现象 | 通过 MQTT 上报超温产生的 HIGH_TEMP 告警，在 `eq_alarm_notification` 表中无对应记录 |
| 根因 | `EquipmentServiceImpl.checkTemperatureAlarm()` 直接调用 `alarmMapper.insert()` 创建告警记录，**未经过** `EqDeviceAlarmService.createAlarm()` → `createInAppNotification()` 路径 |
| 对比 | `HeartbeatCheckScheduler` 产生的 OFFLINE 告警走 `EqDeviceAlarmService.createAlarm()`，正常产生 IN_APP 通知 |
| 影响 | 温度告警仅在 `eq_device_alarm` 中有记录，前端告警看板/站内通知列表可能看不到温度告警 |
| 建议 | 在 `EquipmentServiceImpl.checkTemperatureAlarm()` 末尾注入 `EqDeviceAlarmService` 并调用其 `createInAppNotification(alarmRecord)`，或统一由 `EqDeviceAlarmService` 封装告警创建全流程 |
| 优先级 | P1 |

### NOTE-C-01 数据库兼容性

| 项 | 内容 |
|---|---|
| 问题 | `eq_device_alarm` 表缺少 `is_resolved` / `resolved_at` 列，与实体类 `EqDeviceAlarm` 不一致，导致 SQLite 启动后首次触发告警时 SQL 报错 |
| 复现 | 空库启动 → MQTT 上报温度 → 报错 `no such column: is_resolved` |
| 建议 | 在迁移脚本中补充 `ALTER TABLE eq_device_alarm ADD COLUMN is_resolved INTEGER DEFAULT 0;` 及 `resolved_at` 列，或调整实体与表结构一致 |
| 优先级 | P1（首次部署阻塞） |

---

## 4. 复现命令

```bash
# 1. 启动 mosquitto
systemctl start mosquitto

# 2. 启动服务
cd /data2/docker/decoction/openygt-dms
java -jar dms-app/target/dms-app-1.0.0-SNAPSHOT.jar

# 3. 创建设备
curl -X POST http://localhost:8080/api/v1/eq/devices \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"deviceCode":"TEST001","name":"测试煎药机","deviceType":1,"alarmMaxTemp":50.0,"alarmMinTemp":10.0}'

# 4. 发布超温 MQTT 消息
mosquitto_pub -t "/openygt/device/TEST001/status" -m '{"temperature":99.9}'

# 5. 验证告警与通知
sqlite3 dms.db "SELECT * FROM eq_device_alarm WHERE device_id=55;"
sqlite3 dms.db "SELECT * FROM eq_alarm_notification WHERE alarm_id IN (SELECT id FROM eq_device_alarm WHERE device_id=55);"
```

---

## 5. 合流建议

工程师C 的 `feat/alarm-inapp-only` 当前状态：

- ✅ 语音拨号路径已彻底移除，无残留
- ✅ 离线告警 + 站内通知链路正常
- ✅ 告警去重、消警、VOICE 过滤逻辑单测覆盖
- ⚠️ 温度告警站内通知未写入（BUG-C-01）
- ⚠️ 空库部署需补齐 `eq_device_alarm` 字段（NOTE-C-01）

**建议：修复 BUG-C-01 和 NOTE-C-01 后再合流到 `integration`。若时间紧迫，可接受 BUG-C-01 作为已知缺陷记录，在下一阶段统一修复。**
