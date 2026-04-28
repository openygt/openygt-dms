# REVIEW-2026-04-28 修复项 测试计划

> 评审基线: `docs/20260428/REVIEW-2026-04-28.md`  
> 测试负责人: 测试工程师  
> 状态: 🟡 待开发完成后执行  
> 写权限目录: 严格限制在 `/data2/docker/decoction/openygt-dms-01/openygt-dms/tests/` 下

---

## 一、测试范围

本次测试针对 REVIEW-2026-04-28 中 **P0 / P1 级别修复项** 的验证，确保文档-代码不一致问题（D1~D6）及技术债务（L1~L4、N1~N7）在代码层面的修复达到可发布标准。

| 优先级 | 修复项 | 代码变更点 | 测试方式 |
|---|---|---|---|
| P0 | **L1 + N4** JWT 密钥环境变量注入 | `application.yml` + `JwtConfig.java` + `JwtUtil.initSecret()` | 单元测试 + 集成启动测试 |
| P0 | **L2 + N2** API 前缀统一 + AuthInterceptor 范围扩大 | `WebMvcConfig.java` + `InventoryModule.API_PREFIX` + `ConsumeRecordController` | API 回归测试 + 白盒扫描 |
| P0 | **D5** 前后端登录路径对齐 | `AuthController` 移动到 `/api/v1/rbac/auth` | API 冒烟测试 |
| P0 | **N1** 补 SecurityFilterChain 配置 | `SecurityConfig.java` 新增 `filterChain` Bean | 集成启动测试 + 安全扫描 |
| P1 | **D3** MQTT topic 默认值修复 | `application.yml` `topic-subscription: +/+/+` | 单元测试 + 配置校验 |
| P1 | **D4** 设备状态枚举 RESERVED | `DeviceStatus.java`（如开发选择补齐） | 枚举值校验测试 |
| P1 | **N3** Controller 路径常量统一 | 各 `*Controller.java` `@RequestMapping` | 白盒静态扫描脚本 |
| P1 | **N6** 测试补齐（dms-print / dms-rbac / dms-quality） | 各模块 `src/test/java` | 现有测试全量通过 + 增量测试 |
| P2 | **L3** ShedLock 分布式锁 | 调度器加 `@SchedulerLock`（如本次 Sprint 纳入） | 集成测试（如纳入范围） |
| P2 | **L4** Flyway 迁移 | `DatabaseInitConfig` → Flyway（如本次 Sprint 纳入） | 数据库迁移测试（如纳入范围） |

**本次不测（纯文档修订，无代码变更）**：D1（frontend 目录说明）、D2（数据库描述）。

---

## 二、测试策略

```
┌─────────────────────────────────────────────────────────────┐
│                    分层测试策略                              │
├─────────────────────────────────────────────────────────────┤
│ L1 单元测试层                                                │
│    ├── JwtUtil 密钥注入与 Token 生命周期 (JwtUtilTest)       │
│    ├── MQTT Topic 拼接规则 (MqttConfigTest)                  │
│    ├── SecurityFilterChain Bean 加载 (SecurityConfigTest)    │
│    └── DeviceStatus 枚举完备性 (DeviceStatusTest)            │
├─────────────────────────────────────────────────────────────┤
│ L2 集成启动层                                                │
│    ├── Spring Context 启动成功（无 Bean 冲突）               │
│    ├── 弱密钥启动告警日志断言                                 │
│    ├── 拦截器注册顺序与白名单生效                             │
│    └── SecurityFilterChain 与 AuthInterceptor 共存无冲突     │
├─────────────────────────────────────────────────────────────┤
│ L3 API 回归层                                                │
│    ├── 登录链路：/api/v1/rbac/auth/login                     │
│    ├── 鉴权覆盖：/api/v1/inv/consume 必须 401（无Token）     │
│    ├── 旧路径兼容性：/api/v1/auth/login 是否仍可用（兼容期） │
│    ├── 公开接口白名单：/api/v1/rbac/auth/login、/error       │
│    └── RBAC / 生产 / 设备 / 库存 核心接口冒烟                 │
├─────────────────────────────────────────────────────────────┤
│ L4 静态扫描层                                                │
│    ├── Controller @RequestMapping 常量引用率扫描             │
│    ├── API 路径版本号一致性扫描（必须含 /v1）                │
│    └── 禁止出现 `/api/inventory/consume` 硬编码路径           │
└─────────────────────────────────────────────────────────────┘
```

---

## 三、测试用例清单

### 3.1 JWT 安全专项 (L1 + N4)

| 用例ID | 用例名称 | 前置条件 | 操作步骤 | 预期结果 |
|---|---|---|---|---|
| JWT-01 | 默认密钥启动告警 | 未设置 `JWT_SECRET` 环境变量 | 启动应用 | log 中出现 WARN：`JWT 使用默认/弱密钥` |
| JWT-02 | 环境变量密钥注入生效 | 设置 `JWT_SECRET=StrongSecretKeyForTestingAtLeast32Bytes!` | 启动应用，调用登录接口 | Token 可用新密钥解析验证 |
| JWT-03 | 密钥长度校验拒绝 | 设置 `JWT_SECRET=short` | 启动应用 | 启动失败或 ERROR 日志：`密钥长度不足` |
| JWT-04 | Token 生成与解析 | 应用正常启动 | 调用 `JwtUtil.generateToken` 再 `parseToken` | Claims 与原值一致 |
| JWT-05 | Token 过期失效 | 生成 Token 后等待过期（或调低 expiration） | 用过期 Token 访问受保护接口 | 返回 401，提示 Token 无效或已过期 |
| JWT-06 | 非法 Token 拦截 | 应用正常启动 | 携带 `Bearer invalid.token.here` 访问 | 返回 401 |

### 3.2 API 路径与鉴权一致性 (L2 + N2 + N3 + D5)

| 用例ID | 用例名称 | 前置条件 | 操作步骤 | 预期结果 |
|---|---|---|---|---|
| API-01 | 新登录路径可用 | 应用启动 | POST `/api/v1/rbac/auth/login` | 返回 200，含 token |
| API-02 | 库存消耗接口在 v1 下 | 应用启动 | GET `/api/v1/inv/consume/list` 无Token | 返回 401（被拦截） |
| API-03 | 旧库存路径已废弃 | 应用启动 | GET `/api/inventory/consume/list` 无Token | 返回 404 或 401（不再使用旧路径） |
| API-04 | 全部 /api/** 受保护 | 应用启动 | 遍历各模块 GET/POST 无Token | 非白名单接口均返回 401 |
| API-05 | 白名单接口放行 | 应用启动 | GET `/error`、POST `/api/v1/rbac/auth/login` 无Token | 不返回 401（login 正常 200，error 按业务） |
| API-06 | Controller 常量引用率 ≥80% | 源码就绪 | 运行 `api_path_consistency_test.py` | 硬编码 `/api/v1/...` 字符串数量 ≤3 |

### 3.3 MQTT Topic 配置 (D3)

| 用例ID | 用例名称 | 前置条件 | 操作步骤 | 预期结果 |
|---|---|---|---|---|
| MQTT-01 | Topic 拼接符合文档模型 | 应用启动（MQTT Broker 可连可不连） | 读取 `MqttConfig` 订阅 topic 字符串 | 格式为 `/openygt/tenantId/deviceCode/messageType` 的通配形式 |
| MQTT-02 | 默认订阅通配符 | `application.yml` 无显式 topic-subscription | 启动时读取配置 | 默认值为 `+/+/+` |
| MQTT-03 | 消息路由解析 | 模拟发送 topic `/openygt/1/DEV001/status` | 触发 `handleMessage` | 正确解析 tenantId=1, deviceCode=DEV001, messageType=status |

### 3.4 SecurityFilterChain (N1)

| 用例ID | 用例名称 | 前置条件 | 操作步骤 | 预期结果 |
|---|---|---|---|---|
| SEC-01 | SecurityFilterChain Bean 存在 | 应用启动 | Spring Context 中获取 `SecurityFilterChain` | 不为 null |
| SEC-02 | CSRF 已禁用 | 应用启动 | POST 任意接口 | 不因 CSRF 返回 403 |
| SEC-03 | CORS 已启用 | 应用启动 | OPTIONS 预检请求 | 返回正确 CORS 头 |
| SEC-04 | 匿名访问公开路径 | 应用启动 | GET `/actuator/health`（如有）或 `/error` | 不触发 Spring Security 认证页 |

### 3.5 设备状态枚举 (D4)

| 用例ID | 用例名称 | 前置条件 | 操作步骤 | 预期结果 |
|---|---|---|---|---|
| DEV-01 | 枚举值完整 | 代码编译通过 | 反射读取 `DeviceStatus` 所有枚举常量 | 包含 IDLE、RUNNING、FAULT、OFFLINE、MAINTENANCE；如开发补 RESERVED 则额外包含 |

---

## 四、测试环境

### 4.1 硬件/容器

- Java 8+（与生产一致）
- Maven 3.8+
- Python 3.10+（用于 API 回归脚本）
- SQLite 或 MySQL 5.7+（根据当前 `application.yml` 配置）

### 4.2 配置文件

测试期间使用 `tests/regression/test-env.yml`（见同目录文件）：
- 数据库：SQLite 内存模式（快速启动）
- MQTT：关闭自动连接或指向 mock broker
- JWT：测试专用强密钥
- 日志级别：DEBUG（便于断言启动日志）

### 4.3 数据准备

- 预置用户：admin / admin123（全权限）
- 预置低权限用户：jgy001 / admin123（用于 API-04）
- 预置设备：至少 1 条设备数据（用于 MQTT-03）

---

## 五、入口与执行命令

```bash
# 1. 编译与现有单元测试（必须在项目根目录执行）
mvn clean test

# 2. 集成启动测试（使用测试配置）
cp tests/regression/test-env.yml dms-app/src/main/resources/application.yml
mvn spring-boot:run -pl dms-app &
sleep 30

# 3. API 回归测试
python3 tests/regression/review_fixes_regression.py \
  --base-url http://localhost:8080 \
  --user admin \
  --password admin123

# 4. 静态扫描
python3 tests/regression/api_path_consistency_test.py

# 5. 生成报告
python3 tests/regression/generate_report.py
```

---

## 六、通过标准

| 检查项 | 通过标准 |
|---|---|
| 单元测试 | `mvn test` 全量通过，无 FAIL、无 ERROR |
| 启动测试 | 应用正常启动，端口 8080 响应，无 Bean 创建异常 |
| API 冒烟 | `review_fixes_regression.py` 通过率 100% |
| 静态扫描 | `api_path_consistency_test.py` 无 HIGH 级别告警 |
| 安全基线 | JWT 弱密钥启动必告警；非白名单接口无 Token 必 401 |

---

## 七、风险与备注

1. **开发进行中**：开发工程师尚未提交全部修复，本计划为“待执行”状态，开发完成后由测试工程师触发。
2. **旧路径兼容**：如 `/api/v1/auth/login` 需保留兼容期，请在 `AuthController` 中加 `@Deprecated` 转发，API-03 用例需调整。
3. **ShedLock / Flyway**：如本次 Sprint 不纳入，对应用例标记为 SKIP，不阻塞发布。
4. **测试目录隔离**：所有新增脚本、报告、配置均写入 `tests/` 目录，不污染源码树。
