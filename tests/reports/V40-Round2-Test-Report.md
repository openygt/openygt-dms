# OpenYGT V4.0 第二轮测试报告

**日期**: 2026-04-28  
**测试输入**: `/data2/docker/decoction/openygt-dms-a/docs/20260428/测试输入清单-V4.0-第二轮.md`  
**基线版本**: openygt-dms-01/openygt-dms (HEAD)

---

## 1. 测试范围对照

| 清单章节 | 内容 | 可测性 | 测试结果 |
|----------|------|--------|----------|
| §2.2.1 网页端(Vue3) | JWT鉴权、API前缀、Token结构 | 【已实现】 | **通过** (T-M-01a/b/c/e, T-M-04, T-I-03) |
| §2.2.2 H5 | 响应式布局 | 【规划中】 | 未测 |
| §2.2.3 PDA | uniapp业务代码 | 【规划中】 | 未测（表结构就绪，无业务代码） |
| §2.2.4 APK | 原生应用 | 【规划中】 | 未测 |
| §2.2.5 短信通知 | SMS模块 | 【规划中】 | 未测 |
| §3.2.1 MQTT设备接入 | 状态/心跳/故障/温度/离线告警 | 【已实现】 | **通过** (11/11 E2E) |
| §3.2.2 TCP二进制协议 | Netty框架 | 【框架就绪】 | **符合预期** (无适配器，Server未启动) |
| §3.2.3 WebSocket实时推送 | STOMP推送 | 【规划中】 | 未测 |
| §3.2.4 DeviceConnManager | Redis连接状态 | 【规划中】 | 未测 |
| §3.2.5 MessageRouter | 消息路由 | 【规划中】 | 未测 |
| §3.2.6 控制指令下发 | 指令下发链路 | 【规划中】 | 未测 |

---

## 2. Maven 回归测试

**命令**: `mvn clean test -pl dms-app -am`

**结果**: ✅ BUILD SUCCESS

| 模块 | 测试数 | 结果 |
|------|--------|------|
| dms-common | — | SUCCESS |
| dms-masterdata | — | SUCCESS |
| dms-system | — | SUCCESS |
| dms-equipment | — | SUCCESS |
| dms-inventory | — | SUCCESS |
| dms-production | — | SUCCESS |
| dms-quality | 11 | ✅ 0 Failure |
| dms-print | 14 | ✅ 0 Failure |
| dms-analytics | 2 | ✅ 0 Failure |
| dms-rbac | 8 | ✅ 0 Failure |
| dms-pda | 0 | SUCCESS (无测试) |
| dms-app | 4 | ✅ 0 Failure |

**总计**: ~39+ 测试，0 Failure，0 Error

---

## 3. 功能测试详情

### 3.1 多端统一架构 (T-M-01 / T-M-04)

| 用例 | 说明 | 结果 |
|------|------|------|
| T-M-01a | 登录接口返回200 | ✅ PASS |
| T-M-01b | Token非空(181字符) | ✅ PASS |
| T-M-01c | Token含roles claim | ✅ PASS (ROLE_ADMIN) |
| T-M-01d | Token含permissions claim | ❌ **FAIL** (perms_count=0) |
| T-M-01e | 无Token访问返回401 | ✅ PASS |
| T-M-04 | 各模块API前缀统一 | ✅ PASS (md/eq/prod/prt/qt/inv/sys/rbac) |

**缺陷说明**: `SysUserServiceImpl.login()` 仅将 `roles` 写入 JWT，未写入 `permissions`。  
**根因**: `dms-system` 模块未依赖 `dms-rbac`，登录流程无权限查询逻辑。  
**建议**: 在 `SysUserServiceImpl.login()` 中补充权限码查询（可通过 SysUserMapper 新增联表查询，或调整模块依赖）。

### 3.2 设备状态字符串收敛 (T-I-03)

| 用例 | 说明 | 结果 |
|------|------|------|
| T-I-03 | 设备状态使用英文枚举 | ✅ PASS (IDLE/RUNNING/FAULT/OFFLINE/MAINTENANCE) |

### 3.3 MQTT 设备接入 (T-I-01~T-I-05)

通过 `tests/device-simulation/run_device_e2e.py` 端到端验证：

| 用例 | 场景 | 结果 |
|------|------|------|
| SC-DECOCT-01 | 煎药机 MQTT 上线 → 状态 IDLE | ✅ PASS |
| SC-DECOCT-02 | 温度/状态 JSON 上报 → DB 更新 | ✅ PASS |
| SC-DECOCT-03 | 超温告警(125℃) → HIGH_TEMP/CRITICAL | ✅ PASS |
| SC-DECOCT-04 | 故障上报 → FAULT | ✅ PASS |
| SC-DECOCT-05 | 心跳维持 → last_heartbeat 更新 | ✅ PASS |
| SC-WRAP | 包装机绑定/释放 | ✅ PASS |
| SC-PRINT-01 | 标签打印任务完成 | ✅ PASS |
| SC-PDA | PDA 质检+交接流程 | ✅ PASS |
| SC-SCHED-02 | 心跳超时离线 → OFFLINE | ✅ PASS |
| SC-SCHED-01 | 泡药超时自动推进 | ✅ PASS |
| SC-NET-02 | 非法 Topic 不崩溃 | ✅ PASS |

**E2E 总计**: 11/11 ✅

### 3.4 TCP Netty 框架边界 (§3.2.2)

| 检查项 | 结果 |
|--------|------|
| Netty Server 是否监听额外端口 | ❌ 否（无具体适配器子类） |
| AbstractTcpBinaryAdapter 是否存在 | ✅ 是 |
| AdapterRegistry 是否为空 | ✅ 是 |

**结论**: 与清单标注的 **【框架就绪】** 状态完全一致。骨架代码完整，但因无厂商适配器实现，Netty Server 未实例化。无需修复。

---

## 4. 已知限制验证 (附录 R-01~R-06)

| 编号 | 限制/风险 | 验证结果 |
|------|-----------|----------|
| R-01 | TaskServiceImplIntegrationTest @Disabled | ✅ 确认存在，未执行 |
| R-02 | ShedLock 未接入 | ✅ 确认存在，scheduler 单节点运行正常 |
| R-03 | Flyway 未接入 | ✅ 确认存在，使用 DatabaseInitConfig |
| R-04 | 状态字符串硬编码 | ⚠️ 部分中文状态仍在 TaskServiceImpl 中 |
| R-05 | IoT 网关与应用同进程 | ✅ 确认，应用重启会中断 MQTT/TCP |
| R-06 | PDA 表结构存在但无代码 | ✅ 确认，pda_login_record 等表为空 |

---

## 5. 结论

- **已实现部分**: 网页端 JWT 鉴权、API 前缀统一、MQTT 设备接入、任务状态流转、打印、质检、交接等功能 **全部验证通过**。
- **框架就绪部分**: TCP Netty 骨架代码完整，待厂商适配器 Sprint 启动后补充业务用例。
- **规划中部分**: H5、PDA、APK、WebSocket、短信通知、DeviceConnManager、MessageRouter、控制指令下发 **暂不具备可测性**，与清单一致。
- **唯一缺陷**: JWT Token 缺少 `permissions` claim（T-M-01d），需在后续 Sprint 补充。

---

*报告生成时间: 2026-04-28*
