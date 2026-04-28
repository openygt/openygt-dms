# 设备端发起的端到端测试计划

> 测试视角：站在设备端（煎药机、包装机、打印机、PDA、时钟/网络信号）向系统发输入，验证系统反馈和后续流程正确性。
> 测试环境：真实 MySQL + 真实 EMQX 5.8.1 + 运行中的应用（端口 8080）

---

## 一、测试场景总览

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         设备端发起输入 → 系统反馈                              │
├─────────────────────────────────────────────────────────────────────────────┤
│ ① 煎药机(MQTT)  →  状态/温度/故障/心跳/离线  →  设备状态表 + 任务自动推进      │
│ ② 包装机(REST)  →  绑定/开始/结束包装        →  任务状态 + 设备占用释放        │
│ ③ 标签打印机    →  打印任务提交               →  PrintTask + 设备状态 RUNNING  │
│ ④ 处方打印机    →  处方打印请求               →  PrintRecord                 │
│ ⑤ PDA(REST)     →  扫码交接/质检/泡药起止    →  任务状态 + 交接记录           │
│ ⑥ 时钟/定时任务  →  心跳超时/泡药超时扫描     →  设备离线 + 任务自动推进       │
│ ⑦ 网络信号(MQTT) →  断线重连/异常Topic        →  连接恢复/格式不匹配丢弃       │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 二、前置数据准备（通过 REST API）

| 步骤 | 操作 | API |
|---|---|---|
| P1 | 登录获取管理员 Token | `POST /api/v1/rbac/auth/login` |
| P2 | 创建测试医院 | `POST /api/v1/md/hospitals` |
| P3 | 创建煎药方案（含温度阈值） | `POST /api/v1/md/schemes` |
| P4 | 预置煎药机设备（EQ001） | `POST /api/v1/eq/devices` |
| P5 | 预置包装机设备（PK001） | `POST /api/v1/eq/devices` |
| P6 | 预置标签打印机（LB001） | `POST /api/v1/eq/devices` |
| P7 | 创建处方 + 任务（状态：待泡药） | `POST /api/v1/prod/prescriptions` + 自动生成任务 |

---

## 三、测试用例清单

### 场景一：煎药机 MQTT 全流程（SC-DECOCT-01 ~ 05）

| 用例ID | 用例名称 | 设备输入 | 预期系统反馈 | 验证方式 |
|---|---|---|---|---|
| SC-DECOCT-01 | 煎药机上线上报 | MQTT: `/openygt/1/EQ001/online` | `eq_device` 状态 = IDLE，若不存在则自动注册 | 查表 + API |
| SC-DECOCT-02 | 煎药机温度上报驱动任务 | MQTT: `/openygt/1/EQ001/status` {"temperature":85.5,"status":"RUNNING"} | 1. `eq_device.current_temp` = 85.5<br>2. 绑定到 EQ001 的待煎药任务自动变为 煎药中 | 查表 + API |
| SC-DECOCT-03 | 煎药机超温告警 | MQTT: `/openygt/1/EQ001/status` {"temperature":120.0} | 1. `eq_device` 状态不变<br>2. `eq_device_alarm` 新增 HIGH_TEMP 告警 | 查表 |
| SC-DECOCT-04 | 煎药机故障上报 | MQTT: `/openygt/1/EQ001/fault` `E101-加热管故障` | 1. `eq_device.status` = FAULT<br>2. `eq_device.fault_code` = E101<br>3. `eq_device_alarm` 新增故障告警 | 查表 |
| SC-DECOCT-05 | 煎药机心跳维持 | 每 20 秒发送 `heartbeat` | `last_heartbeat` 持续更新，30 秒扫描不标记 OFFLINE | 查表 |

### 场景二：包装机设备绑定与释放（SC-WRAP-01 ~ 02）

| 用例ID | 用例名称 | 设备端输入 | 预期系统反馈 | 验证方式 |
|---|---|---|---|---|
| SC-WRAP-01 | 包装机绑定任务 | REST: `POST /api/v1/prod/tasks/{id}/wrap/start` {"deviceCode":"PK001"} | 1. `prod_task.status` = 包装中<br>2. `eq_device.status` = RUNNING | 查表 + API |
| SC-WRAP-02 | 包装结束释放设备 | REST: `POST /api/v1/prod/tasks/{id}/wrap/end` | 1. `prod_task.status` = 待贴标<br>2. `eq_device.status` = IDLE | 查表 + API |

### 场景三：标签打印机（SC-PRINT-01 ~ 02）

| 用例ID | 用例名称 | 设备端输入 | 预期系统反馈 | 验证方式 |
|---|---|---|---|---|
| SC-PRINT-01 | 打印标签任务提交 | REST: `POST /api/v1/prod/tasks/{id}/print` {"deviceCode":"LB001"} | 1. `print_task` 新增记录，status=COMPLETED<br>2. `eq_device.status` = IDLE（同步模拟立即释放）<br>3. `prod_task.print_status` = PRINTED | 查表 + API |
| SC-PRINT-02 | 打印失败重试 | REST: `POST /api/v1/prod/tasks/{id}/print/retry` | `print_task.retry_count` +1，状态回到 COMPLETED | 查表 + API |

### 场景四：PDA 扫码操作（SC-PDA-01 ~ 03）

| 用例ID | 用例名称 | PDA 输入 | 预期系统反馈 | 验证方式 |
|---|---|---|---|---|
| SC-PDA-01 | PDA 泡药开始 | REST: `POST /api/v1/prod/tasks/{id}/soak/start` | `prod_task.status` = 泡药中，`soak_start_time` 有值 | 查表 + API |
| SC-PDA-02 | PDA 扫码质检通过 | REST: `POST /api/v1/prod/tasks/{id}/quality` {"result":"PASS"} | `prod_task.status` = 待交接 | 查表 + API |
| SC-PDA-03 | PDA 扫码交接 | REST: `POST /api/v1/prod/tasks/{id}/handover` | `prod_handover_detail` 新增记录，`status` = 已完成/已部分完成 | 查表 + API |

### 场景五：时钟/定时任务（SC-SCHED-01 ~ 02）

| 用例ID | 用例名称 | 时间输入 | 预期系统反馈 | 验证方式 |
|---|---|---|---|---|
| SC-SCHED-01 | 泡药超时自动推进 | 创建泡药中任务后等待 N 分钟（或修改 soak_start_time 为过去） | `SoakTimeoutScheduler` 扫描后自动调用 `endSoak`，状态变为 待煎药 | 查表 |
| SC-SCHED-02 | 心跳超时设备离线 | 设备上线后停止发送 heartbeat，等待 120 秒 | `HeartbeatCheckScheduler` 扫描后将 `eq_device.status` 改为 OFFLINE | 查表 |

### 场景六：网络信号异常（SC-NET-01 ~ 02）

| 用例ID | 用例名称 | 网络输入 | 预期系统反馈 | 验证方式 |
|---|---|---|---|---|
| SC-NET-01 | MQTT 断线重连 | 断开网络后恢复，Paho 自动重连 | 应用日志出现 `MQTT connected and subscribed` | 日志 |
| SC-NET-02 | 非法 Topic 格式 | MQTT: `/openygt/short/topic`（段数不足 5） | 日志 `Topic格式不匹配`，系统不崩溃，丢弃消息 | 日志 |

---

## 四、执行入口

```bash
cd /data2/docker/decoction/openygt-dms-01/openygt-dms
python3 tests/device-simulation/run_device_e2e.py \
  --base-url http://localhost:8080 \
  --mqtt-host 127.0.0.1 \
  --mqtt-port 1883 \
  --mysql-host 127.0.0.1 \
  --mysql-user root \
  --mysql-password mysql_pwd01 \
  --mysql-db openygt-dms
```

---

## 五、通过标准

1. 所有 MQTT 消息发送后 5 秒内，系统数据库出现预期变更
2. 所有 REST API 调用返回 code=200
3. 定时任务场景在 2 倍周期内触发（如心跳超时 120 秒，最多等待 150 秒）
4. 非法输入不导致系统崩溃或异常状态
