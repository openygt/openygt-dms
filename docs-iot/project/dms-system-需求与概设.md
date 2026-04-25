# dms-system 需求与概要设计

> 模块类型：业务模块  
> 包名：`cn.org.openygt.system`  
> 表前缀：`sys_`  
> API 前缀：`/api/v1/sys`  
> 职责：系统级支撑能力，包括用户管理、系统配置、操作日志审计、RBAC 权限控制、**安全认证（JWT/X-Token）**、**数据加密**。  
> 版本：V1.4（专家评审04后修订版）

---

## 一、模块定位

`dms-system` 提供煎药室管理系统的**系统基础设施**，所有与具体煎药业务无关的通用系统能力均归属此模块。

**模块边界**：
- ✅ 负责 `sys_` 前缀表的 CRUD
- ✅ 提供操作日志 AOP 切面（供其他模块引用注解使用）
- ❌ 不参与生产状态机
- ❌ 不直接操作设备
- ❌ 仅依赖 `dms-common`

---

## 二、需求清单

### 2.1 已实现需求

| 需求编号 | 需求名称 | 优先级 | 状态 | 验收标准 |
|---------|---------|--------|------|---------|
| SYS-001 | 用户管理 CRUD | P0 | ✅ | 增删改查、用户名唯一、密码加密 |

### 2.2 待开发需求

| 需求编号 | 需求名称 | 优先级 | 状态 | 验收标准 |
|---------|---------|--------|------|---------|
| **SYS-005** | **登录认证（JWT/X-Token）** | **P0** | ⏳ | **医疗系统无认证违法，必须MVP完成** |
| SYS-002 | 系统配置管理 | P1 | ⏳ | 键值对 CRUD、按 key 查询、缓存 |
| SYS-003 | 操作日志审计 | P1 | ⏳ | AOP 切面自动记录、按用户/模块/时间查询 |
| SYS-004 | RBAC 权限控制 | P2 | ⏳ | 角色表、权限表、菜单表、注解拦截 |
| SYS-006 | 密码策略 | P2 | ⏳ | 复杂度校验、定期更换提醒 |
| SYS-007 | 数据字典 | P2 | ⏳ | 通用字典项管理（如煎药模式、交接方式） |
| **SYS-008** | **数据加密（AES存储+TLS传输）** | **P2** | ⏳ | 患者处方、手机号加密存储，传输层TLS |
| **SYS-009** | **多租户拦截器** | **P1** | ⏳ | `TenantLineInnerInterceptor`，MVP返回"default" |

---

## 三、数据库设计

### 3.1 sys_user（系统用户表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | INTEGER | PK, AUTOINCREMENT | 主键 |
| tenant_id | VARCHAR(32) | DEFAULT 'default' | 多租户预留 |
| username | VARCHAR(50) | NOT NULL, UNIQUE | 登录用户名 |
| password | VARCHAR(128) | | 密码（BCrypt 加密） |
| real_name | VARCHAR(50) | | 真实姓名 |
| phone | VARCHAR(20) | | 手机号 |
| status | VARCHAR(20) | DEFAULT 'ACTIVE' | ACTIVE / DISABLED |
| created_at | DATETIME | | 创建时间 |
| updated_at | DATETIME | | 更新时间 |
| deleted | INTEGER | DEFAULT 0 | 逻辑删除 |

**索引**：`UNIQUE(username)`

### 3.2 sys_config（系统配置表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | INTEGER | PK, AUTOINCREMENT | 主键 |
| tenant_id | VARCHAR(32) | DEFAULT 'default' | 多租户预留 |
| config_key | VARCHAR(100) | NOT NULL, UNIQUE | 配置键 |
| config_value | TEXT | | 配置值 |
| description | VARCHAR(200) | | 说明 |
| created_at | DATETIME | | 创建时间 |
| updated_at | DATETIME | | 更新时间 |
| deleted | INTEGER | DEFAULT 0 | 逻辑删除 |

**索引**：`UNIQUE(config_key)`

**预设配置项**：

| config_key | config_value | 说明 |
|-----------|-------------|------|
| `soak.timeout.minutes` | `30` | 泡药超时时长（分钟） |
| `soak.auto.advance` | `true` | 泡药超时是否自动推进 |
| `device.heartbeat.timeout` | `300` | 设备心跳超时（秒） |
| `temp.alarm.high` | `120` | 高温告警阈值（℃） |
| `temp.alarm.low` | `50` | 低温告警阈值（℃） |

### 3.3 sys_log（操作日志表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | INTEGER | PK, AUTOINCREMENT | 主键 |
| tenant_id | VARCHAR(32) | DEFAULT 'default' | **多租户审计必填** |
| user_id | VARCHAR(50) | | 操作用户 ID |
| action | VARCHAR(100) | | 操作描述 |
| module | VARCHAR(50) | | 功能模块 |
| detail | TEXT | | 操作详情（JSON） |
| ip_address | VARCHAR(50) | | IP 地址 |
| created_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | 操作时间 |

> **评审修订03**：`SysLog` 实体**不继承 `BaseEntity`**，改继承 `BaseAuditEntity`（不含 `deleted` 字段和 `@TableLogic`）。操作日志是审计数据，逻辑删除会破坏审计完整性。  
> `sys_log` 补上 `tenant_id`（专家评审第18项），多租户后审计需区分医院。  
> `sys_log` 不做逻辑删除（无 `deleted` 字段），用于审计追溯。

### 3.4 RBAC 预留表（阶段二）

| 表名 | 说明 |
|------|------|
| sys_role | 角色表 |
| sys_permission | 权限表 |
| sys_role_permission | 角色权限关联 |
| sys_user_role | 用户角色关联 |
| sys_menu | 菜单/功能点表 |

---

## 四、接口设计

### 4.1 用户管理

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/sys/users` | 创建用户 |
| GET | `/api/v1/sys/users/{id}` | 查询用户 |
| PUT | `/api/v1/sys/users/{id}` | 更新用户 |
| DELETE | `/api/v1/sys/users/{id}` | 删除用户 |
| GET | `/api/v1/sys/users` | 分页查询（支持 username/real_name 模糊搜索） |

**密码规则**：创建/修改用户时，密码使用 `BCryptPasswordEncoder` 加密存储。

### 4.2 系统配置

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/sys/configs` | 创建配置 |
| GET | `/api/v1/sys/configs/{key}` | 按 key 查询配置值 |
| PUT | `/api/v1/sys/configs/{key}` | 更新配置值 |
| DELETE | `/api/v1/sys/configs/{key}` | 删除配置 |
| GET | `/api/v1/sys/configs` | 查询所有配置 |

**缓存建议**：`config_key → config_value` 可放入本地缓存（如 `ConcurrentHashMap`），变更时刷新。

### 4.3 操作日志

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/sys/logs` | 分页查询日志（支持 userId/module/dateRange） |

**AOP 切面设计**：

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperationLog {
    String action();
    String module();
}

@Aspect
@Component
public class OperationLogAspect {
    @Around("@annotation(operationLog)")
    public Object around(ProceedingJoinPoint point, OperationLog operationLog) {
        // 记录操作前状态
        Object result = point.proceed();
        // 异步写入 sys_log
        return result;
    }
}
```

使用方式：

```java
@OperationLog(action = "开始煎药", module = "production")
public Task startDecoct(...) { ... }
```

### 4.4 认证接口（P0，专家评审强制提升）

> **专家评审结论**：医疗系统无认证违反《个保法》《GSP》，认证必须从阶段二提升为P0。

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/sys/auth/login` | 用户名密码登录，返回 JWT |
| POST | `/api/v1/sys/auth/logout` | 登出，Token 加入黑名单 |
| GET | `/api/v1/sys/auth/me` | 获取当前登录用户信息 |
| POST | `/api/v1/sys/auth/refresh` | 刷新 Token |

**JWT 拦截器设计**：

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain chain) {
        // 1. 从 Header 提取 Bearer Token
        // 2. 验签 + 过期检查
        // 3. 将用户信息写入 SecurityContext（或 ThreadLocal）
        // 4. 放行或返回 401
    }
}
```

**保护范围**：除 `/api/v1/sys/auth/**` 和 `/health` 外，所有 API 必须携带有效 Token。

### 4.5 数据加密（P2）

**存储加密**：
- 患者姓名、手机号、地址等敏感字段使用 AES-256 加密存储
- 加密密钥通过 `sys_config` 管理，支持定期轮换
- 加密/解密工具类：`cn.org.openygt.common.util.AesUtil`

**传输加密**：
- 生产环境必须启用 TLS/HTTPS
- 开发环境可放行 HTTP，但需明确标记

---

## 五、核心类结构

```
cn.org.openygt.system
├── controller
│   ├── SysUserController.java
│   ├── SysConfigController.java
│   └── SysLogController.java
├── entity
│   ├── SysUser.java
│   ├── SysConfig.java
│   └── SysLog.java
├── mapper
│   ├── SysUserMapper.java
│   ├── SysConfigMapper.java
│   └── SysLogMapper.java
├── service
│   ├── SysUserService.java / SysUserServiceImpl.java
│   ├── SysConfigService.java / SysConfigServiceImpl.java
│   └── SysLogService.java / SysLogServiceImpl.java
├── aspect
│   └── OperationLogAspect.java
└── annotation
    └── OperationLog.java
```

---

## 六、开发规范

1. **用户密码必须加密**：禁止明文存储，使用 `BCryptPasswordEncoder`。
2. **用户名唯一**：创建和更新时校验 `username` 唯一性。
3. **操作日志异步写入**：避免阻塞业务线程，可使用 `@Async` 或 Spring 事件。
4. **sys_log 不逻辑删除**：审计表保留完整历史，定期归档即可。
5. **配置变更刷新缓存**：修改 `sys_config` 后，需清除或更新本地缓存。

---

## 七、集成检查清单

- [ ] `SysUser`, `SysConfig` 继承 `BaseEntity`
- [ ] `SysLog` 继承 `BaseAuditEntity`（不含逻辑删除）
- [ ] 密码使用 BCrypt 加密
- [ ] `username` 和 `config_key` 有唯一性校验
- [ ] 操作日志 AOP 切面已配置并生效
- [ ] 所有 API 返回 `ApiResponse<T>`
- [ ] `mvn test` 单测通过
