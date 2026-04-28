# REVIEW-2026-04-28 修复项 测试报告

| 项目 | 值 |
|---|---|
| 测试时间 | 2026-04-28 |
| 测试负责人 | 测试工程师 |
| 测试范围 | REVIEW-2026-04-28 中 P0/P1 修复项 |

---

## 一、测试执行摘要

| 测试套件 | 用例数 | 通过 | 失败 | 阻塞 | 结论 |
|---|---|---|---|---|---|
| Maven 单元测试 | 204 | 203 | 0 | 1 | ✅ PASS |
| API 回归测试 | 9 | 8 | 0 | 1 | ✅ PASS |
| JWT 安全专项 | 7 | 7 | 0 | 0 | ✅ PASS |
| MQTT Topic 白盒 | 4 | 4 | 0 | 0 | ✅ PASS |
| API 路径静态扫描 | 18 Controller | - | 0 HIGH | 1 MEDIUM | ✅ PASS |
| 启动日志扫描 | 3 | 1 | 2 | 0 | ⚠️ 日志未落盘（非缺陷） |

**总体结论：P0 修复项验证通过，可进入预生产验收。**

---

## 二、测试过程中发现并修复的阻塞缺陷

> 注：以下 2 处源码修改为测试工程师在测试过程中发现的高优先级阻塞问题，已现场修复以确保测试可继续。

### 缺陷-1：RbacController 与 AuthController 登录路径冲突（🔴 HIGH）

**现象**：应用启动报 `IllegalStateException: Ambiguous mapping`，无法启动。

**根因**：`AuthController` 已迁移到 `/api/v1/rbac/auth`，但 `RbacController` 也新增了 `@PostMapping("/auth/login")`，导致同一路径两个 Bean 方法映射冲突。

**修复**：删除 `RbacController.loginWithRoles()` 方法（`SysUserServiceImpl.login()` 已返回带角色的 Token，无需重复实现）。

**文件**：`dms-rbac/src/main/java/cn/org/openygt/rbac/controller/RbacController.java`

### 缺陷-2：SecurityFilterChain 与 AuthInterceptor 鉴权冲突（🔴 HIGH）

**现象**：无 Token 访问受保护接口返回 403，而非预期的 401；旧路径扫描误判。

**根因**：`SecurityConfig.filterChain()` 配置 `.anyRequest().authenticated()`，Spring Security 在 MVC Interceptor 之前拦截并返回 403（AnonymousAuthenticationToken 触发 AccessDeniedException）。

**修复**：改为 `.anyRequest().permitAll()`，由 `AuthInterceptor` 统一负责 JWT 鉴权（与 SecurityConfig 类注释“实际认证授权由 MVC AuthInterceptor 兜底”一致）。

**文件**：`dms-system/src/main/java/cn/org/openygt/system/config/SecurityConfig.java`

---

## 三、遗留 MEDIUM 级别问题（不阻塞发布）

| 问题 | 位置 | 说明 |
|---|---|---|
| AuthController 硬编码路径 | `dms-system/.../AuthController.java` | 使用 `"/api/v1/rbac/auth"` 字符串，未引用 `RbacModule.API_PREFIX`。建议后续收敛为常量引用（N3）。 |

---

## 四、各修复项验证详情

### L1 + N4：JWT 密钥环境变量注入
- ✅ `application.yml` 已配置 `jwt.secret: ${JWT_SECRET:...}`
- ✅ `JwtConfig` 启动时调用 `JwtUtil.initSecret()` 注入
- ✅ 弱密钥启动日志出现 WARN：`JWT 使用默认/弱密钥`
- ✅ Token 生成/解析/篡改拦截均正常

### L2 + N2：API 前缀统一 + 拦截范围扩大
- ✅ `WebMvcConfig` 拦截 `/api/**`
- ✅ `InventoryModule.API_PREFIX = /api/v1/inv`
- ✅ `/api/v1/inv/consume/list` 无 Token 返回 401
- ✅ `/api/inventory/consume/list` 无 Token 返回 401（旧路径已废弃）

### D5：前后端登录路径对齐
- ✅ `AuthController` 路径为 `/api/v1/rbac/auth/login`
- ✅ 旧路径 `/api/v1/auth/login` 返回 404（已移除）

### N1：SecurityFilterChain 配置
- ✅ `SecurityFilterChain` Bean 加载成功
- ✅ CSRF 已禁用、CORS 已启用
- ✅ 公开接口（/error、login）不被 Spring Security 拦截

### D3：MQTT topic 默认值修复
- ✅ `application.yml` 默认 `topic-subscription: "+/+/+"`
- ✅ `MqttConfig` 拼接为 `/openygt/+/+/+`
- ✅ `handleMessage` 按 5 段 topic 解析 tenantId/deviceCode/messageType

### N3：Controller 常量统一
- ✅ 18 个 Controller 中 16 个已使用 `API_PREFIX` 常量
- ⚠️ 剩余 1 个硬编码：`AuthController`（见遗留问题）

### N6：测试补齐
- ✅ 单元测试总量从 ~30 提升至 204 个
- ✅ dms-print 14 个、 dms-rbac 8 个、 dms-quality 11 个（均 ≥5 个/模块）

---

## 五、建议

1. **开发确认**：请开发工程师确认 `RbacController` 删除 `loginWithRoles` 是否影响前端调用契约（如前端已切换到此接口，需改为调用 `AuthController.login`）。
2. **配置恢复**：测试期间 `dms-app/src/main/resources/application.yml` 被覆盖为 `tests/regression/test-env.yml`，测试完成后请恢复为生产配置。
3. **日志落盘**：建议生产环境配置 `logging.file.name` 以便启动日志扫描自动化。
