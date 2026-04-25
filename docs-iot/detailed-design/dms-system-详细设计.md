# dms-system 详细设计文档（V1.4）

> 模块：系统管理（System Admin）  
> 范围：用户管理、JWT 认证、系统配置、操作日志审计  
> 对应目录：`dms-system/`、`dms-common/`（新增 BaseAuditEntity、全局异常扩展）  

---

## 一、模块概述

### 1.1 职责边界

| 子域 | 职责 | 对应表 | API 前缀 |
|------|------|--------|----------|
| 用户管理 | 用户 CRUD、密码加密、状态管理 | `sys_user` | `/api/v1/sys/users` |
| JWT 认证 | 登录/登出、Token 签发与校验、请求鉴权 | `sys_user` | `/api/v1/sys/auth` |
| 系统配置 | 配置项 CRUD、本地缓存热读 | `sys_config` | `/api/v1/sys/configs` |
| 操作日志 | 注解驱动审计、异步落库 | `sys_log` | `/api/v1/sys/logs`（查询） |

### 1.2 技术栈

- Spring Boot 2.7.18
- MyBatis-Plus 3.5.7
- SQLite（开发/单机）/ MySQL（生产）双方言
- Spring Security Crypto（BCrypt，**不引入完整 Spring Security 过滤器链**，仅使用其 `BCryptPasswordEncoder`）
- JJWT 0.11.5（Token 生成与解析）
- Spring AOP + `@Async`（日志异步写入）

### 1.3 模块常量

```java
package cn.org.openygt.system;

public final class SystemModule {
    private SystemModule() {}
    public static final String TABLE_PREFIX = "sys_";
    public static final String API_PREFIX = "/api/v1/sys";
}
```

---

## 二、代码现状 vs 目标差距

### 2.1 差距矩阵

| 功能点 | 现状 | 目标（V1.4） | 差距说明 |
|--------|------|-------------|----------|
| **用户 CRUD** | `SysUserController`/`ServiceImpl` 已实现基础 CRUD | 保持 CRUD，增加 `BCryptPasswordEncoder` 加密、密码字段不回传 | 密码明文存储，无加密；update 时未排除 password 字段 |
| **JWT 认证** | 完全未实现 | `LoginRequest`/`TokenResponse`、`JwtTokenProvider`、`JwtAuthenticationFilter`、`AuthController` | 需新增 5+ 个类，并在 `dms-app` 注册 Filter |
| **系统配置** | `sys_config` 表存在，无任何代码 | `SysConfig` 实体 + Mapper + Service + Controller + 本地 `ConcurrentHashMap` 缓存 | 需从零实现 |
| **操作日志** | 完全未实现 | `@OperationLog` 注解 + `OperationLogAspect` + `SysLog` 实体/Mapper/Service | 需从零实现 |
| **审计基类** | 仅 `BaseEntity`（含 `deleted` 逻辑删除） | 新增 `BaseAuditEntity`（无 `deleted`），`SysLog` 继承它 | 需新建基类 |
| **数据加密** | 无 | AES 工具类（预留，本期仅提供工具，不接入业务） | 需新增工具类 |

### 2.2 风险项

1. **密码明文**：当前 `SysUserServiceImpl.create()` 直接 `insert`，未对 `password` 加密。迁移时必须对所有存量密码做“强制重置”或“首次登录改密”策略。
2. **Spring Security 引入范围**：本模块仅使用 `spring-security-crypto`（或 `spring-boot-starter-security` 后排除 filter chain），避免与现有无状态 HTTP 架构冲突。
3. **Filter 顺序**：`JwtAuthenticationFilter` 必须在 Spring MVC `DispatcherServlet` 之前执行，且先于任何可能读取 `InputStream` 的 Filter（如日志打印 Filter）。

---

## 三、数据库详细设计

### 3.1 SQLite 版 DDL

```sql
-- ========== sys_user ==========
CREATE TABLE IF NOT EXISTS sys_user (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    tenant_id VARCHAR(32) DEFAULT 'default',
    username VARCHAR(50) NOT NULL,
    password VARCHAR(128) NOT NULL,
    real_name VARCHAR(50),
    phone VARCHAR(20),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_user_username ON sys_user(username);
CREATE INDEX IF NOT EXISTS idx_sys_user_tenant ON sys_user(tenant_id);
CREATE INDEX IF NOT EXISTS idx_sys_user_status ON sys_user(status);

-- ========== sys_config ==========
CREATE TABLE IF NOT EXISTS sys_config (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    tenant_id VARCHAR(32) DEFAULT 'default',
    config_key VARCHAR(100) NOT NULL,
    config_value TEXT,
    description VARCHAR(200),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_config_key ON sys_config(config_key);
CREATE INDEX IF NOT EXISTS idx_sys_config_tenant ON sys_config(tenant_id);

-- ========== sys_log ==========
CREATE TABLE IF NOT EXISTS sys_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    tenant_id VARCHAR(32) DEFAULT 'default',
    user_id VARCHAR(50),
    action VARCHAR(100) NOT NULL,
    module VARCHAR(50) NOT NULL,
    detail TEXT,
    ip_address VARCHAR(50),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sys_log_user ON sys_log(user_id);
CREATE INDEX IF NOT EXISTS idx_sys_log_module ON sys_log(module);
CREATE INDEX IF NOT EXISTS idx_sys_log_created ON sys_log(created_at);
```

### 3.2 MySQL 版 DDL

```sql
-- ========== sys_user ==========
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id VARCHAR(32) DEFAULT 'default' COMMENT '租户ID',
    username VARCHAR(50) NOT NULL COMMENT '登录账号',
    password VARCHAR(128) NOT NULL COMMENT 'BCrypt加密后的密码',
    real_name VARCHAR(50) COMMENT '真实姓名',
    phone VARCHAR(20) COMMENT '手机号',
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/LOCKED',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除: 0未删除 1已删除',
    UNIQUE KEY uk_username (username),
    KEY idx_tenant (tenant_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- ========== sys_config ==========
CREATE TABLE IF NOT EXISTS sys_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id VARCHAR(32) DEFAULT 'default' COMMENT '租户ID',
    config_key VARCHAR(100) NOT NULL COMMENT '配置键',
    config_value TEXT COMMENT '配置值',
    description VARCHAR(200) COMMENT '配置说明',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    UNIQUE KEY uk_config_key (config_key),
    KEY idx_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- ========== sys_log ==========
CREATE TABLE IF NOT EXISTS sys_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id VARCHAR(32) DEFAULT 'default' COMMENT '租户ID',
    user_id VARCHAR(50) COMMENT '操作用户ID',
    action VARCHAR(100) NOT NULL COMMENT '操作动作',
    module VARCHAR(50) NOT NULL COMMENT '功能模块',
    detail TEXT COMMENT '操作详情',
    ip_address VARCHAR(50) COMMENT 'IP地址',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_user (user_id),
    KEY idx_module (module),
    KEY idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';
```

### 3.3 字段说明

| 表名 | 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|------|
| sys_user | id | BIGINT / INTEGER | PK AI | 主键 |
| sys_user | tenant_id | VARCHAR(32) | DEFAULT 'default' | 多租户预留 |
| sys_user | username | VARCHAR(50) | NOT NULL, UNIQUE | 登录账号，全局唯一（单租户内） |
| sys_user | password | VARCHAR(128) | NOT NULL | BCrypt 哈希，长度固定 60 |
| sys_user | real_name | VARCHAR(50) | 可空 | 真实姓名 |
| sys_user | phone | VARCHAR(20) | 可空 | 手机号 |
| sys_user | status | VARCHAR(20) | DEFAULT 'ACTIVE' | ACTIVE / LOCKED |
| sys_config | config_key | VARCHAR(100) | NOT NULL, UNIQUE | 配置键，如 `system.max_login_retry` |
| sys_config | config_value | TEXT | 可空 | 配置值，允许存 JSON |
| sys_config | description | VARCHAR(200) | 可空 | 配置说明 |
| sys_log | user_id | VARCHAR(50) | 可空 | 操作人ID；未登录时留空 |
| sys_log | action | VARCHAR(100) | NOT NULL | 动作摘要，如 `创建用户` |
| sys_log | module | VARCHAR(50) | NOT NULL | 模块名，如 `user`、`config` |
| sys_log | detail | TEXT | 可空 | 详细参数（JSON 序列化） |
| sys_log | ip_address | VARCHAR(50) | 可空 | 客户端 IP |

> **注意**：`sys_log` **无 `deleted` 字段**，不做逻辑删除，用于合规审计永久留存。

---

## 四、实体类详细设计

### 4.1 新增 BaseAuditEntity（dms-common）

位置：`dms-common/src/main/java/cn/org/openygt/common/entity/BaseAuditEntity.java`

```java
package cn.org.openygt.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.util.Date;

/**
 * 仅审计字段基类（无逻辑删除）。
 * 适用场景：操作日志、流水记录等不允许物理/逻辑删除的表。
 */
@Data
public abstract class BaseAuditEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("tenant_id")
    private String tenantId = "default";

    @TableField("created_at")
    private Date createdAt;

    @TableField("updated_at")
    private Date updatedAt;
}
```

### 4.2 SysUser（改造后）

位置：`dms-system/src/main/java/cn/org/openygt/system/entity/SysUser.java`

```java
package cn.org.openygt.system.entity;

import cn.org.openygt.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {

    private String username;
    private String password;
    private String realName;
    private String phone;
    private String status;

    /** 仅用于接收前端密码，不映射数据库 */
    @TableField(exist = false)
    private transient String plainPassword;
}
```

> **设计约束**：
> - `password` 字段在 **Response DTO 中必须屏蔽**，Controller 层通过 `SysUserResponse` 转换后返回。
> - `plainPassword` 为 `@TableField(exist = false)`，仅用于创建/修改时接收明文密码，Service 层加密后写入 `password`，随后置空。

### 4.3 SysConfig（新增）

位置：`dms-system/src/main/java/cn/org/openygt/system/entity/SysConfig.java`

```java
package cn.org.openygt.system.entity;

import cn.org.openygt.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_config")
public class SysConfig extends BaseEntity {

    @TableField("config_key")
    private String configKey;

    @TableField("config_value")
    private String configValue;

    private String description;
}
```

### 4.4 SysLog（新增，继承 BaseAuditEntity）

位置：`dms-system/src/main/java/cn/org/openygt/system/entity/SysLog.java`

```java
package cn.org.openygt.system.entity;

import cn.org.openygt.common.entity.BaseAuditEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_log")
public class SysLog extends BaseAuditEntity {

    @TableField("user_id")
    private String userId;

    private String action;
    private String module;
    private String detail;

    @TableField("ip_address")
    private String ipAddress;
}
```

> **关键约束**：`SysLog` 继承 `BaseAuditEntity`，因此 **没有 `deleted` 字段**，MyBatis-Plus 全局逻辑删除配置（`logic-delete-field: deleted`）对此表不生效。需要在该 Mapper 上通过 `@TableLogic(value = "0", delval = "1")` 的反向思路处理——实际上更简单的方式是：在 `SysLogMapper` 中不继承逻辑删除能力，所有查询均为物理查询即可。由于 `BaseAuditEntity` 不含 `deleted` 字段，MyBatis-Plus 会自动忽略逻辑删除。

---

## 五、REST API 详细设计

### 5.1 通用约定

- 请求/响应 Content-Type：`application/json`
- 统一响应体：`ApiResponse<T>`（已有）
- 时间格式：`yyyy-MM-dd HH:mm:ss`
- 分页参数：`page`（从1开始）、`size`（默认20，最大100）
- 认证头：`Authorization: Bearer <token>`

### 5.2 DTO 定义

#### 5.2.1 认证相关

```java
// LoginRequest.java
@Data
public class LoginRequest {
    @NotBlank(message = "用户名不能为空")
    @Size(max = 50, message = "用户名长度不能超过50")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度6-32位")
    private String password;
}

// TokenResponse.java
@Data
@AllArgsConstructor
public class TokenResponse {
    private String accessToken;
    private String tokenType;   // "Bearer"
    private Long expiresIn;     // 秒，如 7200
    private String username;
}
```

#### 5.2.2 用户相关

```java
// SysUserRequest.java（创建/更新共用，通过 id 区分）
@Data
public class SysUserRequest {
    private Long id;

    @NotBlank(groups = Create.class, message = "用户名不能为空")
    @Size(max = 50, message = "用户名长度不能超过50")
    private String username;

    @NotBlank(groups = Create.class, message = "密码不能为空")
    @Size(min = 6, max = 32, groups = {Create.class, Update.class}, message = "密码长度6-32位")
    private String password;

    @Size(max = 50, message = "真实姓名长度不能超过50")
    private String realName;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Pattern(regexp = "^(ACTIVE|LOCKED)$", message = "状态只能是 ACTIVE 或 LOCKED")
    private String status;

    public interface Create {}
    public interface Update {}
}

// SysUserResponse.java
@Data
public class SysUserResponse {
    private Long id;
    private String tenantId;
    private String username;
    private String realName;
    private String phone;
    private String status;
    private Date createdAt;
    private Date updatedAt;
}
```

#### 5.2.3 配置相关

```java
// SysConfigRequest.java
@Data
public class SysConfigRequest {
    private Long id;

    @NotBlank(message = "配置键不能为空")
    @Size(max = 100, message = "配置键长度不能超过100")
    private String configKey;

    private String configValue;

    @Size(max = 200, message = "描述长度不能超过200")
    private String description;
}

// SysConfigResponse.java
@Data
public class SysConfigResponse {
    private Long id;
    private String tenantId;
    private String configKey;
    private String configValue;
    private String description;
    private Date createdAt;
    private Date updatedAt;
}
```

### 5.3 端点清单

#### 5.3.1 认证接口（无需 Token）

| Method | Path | 说明 | 幂等性 |
|--------|------|------|--------|
| POST | `/api/v1/sys/auth/login` | 登录，返回 JWT | 否（每次生成新 Token） |
| POST | `/api/v1/sys/auth/logout` | 登出（前端丢弃 Token，后端记录可选） | 是 |
| POST | `/api/v1/sys/auth/refresh` | 刷新 Token（需有效 Token） | 否 |

**POST /api/v1/sys/auth/login**

- Request Body：`LoginRequest`
- Success Response：`ApiResponse<TokenResponse>`

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 7200,
    "username": "admin"
  }
}
```

- 异常码：
  - `400`：参数校验失败（用户名/密码为空、长度不符）
  - `401`：用户名不存在或密码错误（模糊提示，统一返回"用户名或密码错误"）
  - `403`：用户状态为 `LOCKED`

> **安全设计**：登录失败不区分"用户名不存在"和"密码错误"，防止枚举攻击。

**POST /api/v1/sys/auth/logout**

- Request Header：`Authorization: Bearer <token>`
- Success Response：`ApiResponse<Void>`（code=200）
- 说明：本系统为无状态 JWT，后端无需黑名单。若后续需要强制下线，需引入 Redis 黑名单；本期仅做前端丢弃 Token + 可选日志记录。

**POST /api/v1/sys/auth/refresh**

- Request Header：`Authorization: Bearer <token>`（即将过期但尚未过期的 Token）
- Success Response：新的 `ApiResponse<TokenResponse>`
- 异常码：`401`（Token 已过期则无法刷新，必须重新登录）

---

#### 5.3.2 用户管理接口（需 Token）

| Method | Path | 说明 | 幂等性 |
|--------|------|------|--------|
| POST | `/api/v1/sys/users` | 创建用户 | 否 |
| PUT | `/api/v1/sys/users/{id}` | 更新用户 | 是（路径级） |
| GET | `/api/v1/sys/users/{id}` | 查询用户详情 | 是 |
| GET | `/api/v1/sys/users` | 分页列表 | 是 |
| DELETE | `/api/v1/sys/users/{id}` | 删除用户（逻辑删除） | 是 |

**POST /api/v1/sys/users**

- 权限：需登录（后续扩展 RBAC 后需 `user:create`）
- Request Body：`SysUserRequest`（`@Validated(SysUserRequest.Create.class)`）
- 业务规则：
  1. `username` 全局唯一（含已逻辑删除的？否——已删除应允许重新注册，MyBatis-Plus 逻辑删除查询默认排除 deleted=1，但唯一索引在数据库层仍冲突。解决方案：数据库唯一索引不加，改由 Service 层校验唯一性，或采用"软删除用户名加后缀"策略。**本期简化**：Service 层校验唯一性，数据库只建普通索引。）
  2. `password` 经 `BCryptPasswordEncoder.encode()` 后存储。
  3. 默认 `status = ACTIVE`。
- Response：`ApiResponse<SysUserResponse>`
- 异常码：`400`（参数错误、用户名已存在）

**PUT /api/v1/sys/users/{id}`**

- Request Body：`SysUserRequest`（`@Validated(SysUserRequest.Update.class)`）
- 业务规则：
  1. `username` 不可修改（或修改时校验新用户名未被占用）。
  2. `password` 为空字符串或 null 时不修改密码；有值则重新 BCrypt 加密。
  3. 不允许修改已逻辑删除的用户（返回 400）。
- Response：`ApiResponse<SysUserResponse>`
- 异常码：`400`（用户不存在、参数错误）

**GET /api/v1/sys/users/{id}`**

- Response：`ApiResponse<SysUserResponse>`（不含 `password` 字段）
- 异常码：`400`（用户不存在）

**GET /api/v1/sys/users`**

- Query Params：
  - `keyword`：模糊匹配 `username` 或 `real_name`
  - `status`：精确筛选 `ACTIVE` / `LOCKED`
  - `page`：默认 1
  - `size`：默认 20，最大 100
- Response：`ApiResponse<IPage<SysUserResponse>>`

**DELETE /api/v1/sys/users/{id}`**

- 说明：MyBatis-Plus 逻辑删除（`deleted = 1`）
- Response：`ApiResponse<Void>`
- 异常码：`400`（用户不存在或已被删除）

---

#### 5.3.3 系统配置接口（需 Token）

| Method | Path | 说明 | 幂等性 |
|--------|------|------|--------|
| POST | `/api/v1/sys/configs` | 创建配置 | 否 |
| PUT | `/api/v1/sys/configs/{id}` | 更新配置 | 是 |
| GET | `/api/v1/sys/configs/{id}` | 查询配置详情 | 是 |
| GET | `/api/v1/sys/configs` | 分页列表 | 是 |
| GET | `/api/v1/sys/configs/key/{configKey}` | 按 key 查询（热读，走缓存） | 是 |
| DELETE | `/api/v1/sys/configs/{id}` | 删除配置（逻辑删除，清缓存） | 是 |

**GET /api/v1/sys/configs/key/{configKey}`**

- 说明：供其他模块读取配置使用，优先读本地 `ConcurrentHashMap` 缓存，无命中则查库并回填缓存。
- Response：`ApiResponse<SysConfigResponse>`
- 异常码：`404`（配置不存在）——注意与现有 `GlobalExceptionHandler` 统一，可用 `IllegalArgumentException` 转 400，或新增 `@ResponseStatus(NOT_FOUND)` 异常。

---

#### 5.3.4 操作日志接口（需 Token，仅查询）

| Method | Path | 说明 | 幂等性 |
|--------|------|------|--------|
| GET | `/api/v1/sys/logs` | 分页查询日志 | 是 |
| GET | `/api/v1/sys/logs/{id}` | 单条详情 | 是 |

**GET /api/v1/sys/logs`**

- Query Params：
  - `module`：精确匹配模块名
  - `action`：模糊匹配操作动作
  - `userId`：精确匹配操作人
  - `beginTime` / `endTime`：创建时间范围（ISO 格式 `yyyy-MM-dd HH:mm:ss`）
  - `page` / `size`
- Response：`ApiResponse<IPage<SysLogResponse>>`

> **注意**：日志写入由 AOP 完成，不提供手动写入接口。

---

## 六、JWT 认证详细设计

### 6.1 依赖引入

在 `dms-system/pom.xml` 新增：

```xml
<dependencies>
    <!-- 已有 dms-common -->
    <dependency>
        <groupId>cn.org.openygt</groupId>
        <artifactId>dms-common</artifactId>
    </dependency>

    <!-- Spring Security Crypto（仅使用 BCrypt） -->
    <dependency>
        <groupId>org.springframework.security</groupId>
        <artifactId>spring-security-crypto</artifactId>
    </dependency>

    <!-- JJWT -->
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-api</artifactId>
        <version>0.11.5</version>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-impl</artifactId>
        <version>0.11.5</version>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-jackson</artifactId>
        <version>0.11.5</version>
        <scope>runtime</scope>
    </dependency>
</dependencies>
```

> 若使用 `spring-boot-starter-security`，会导致默认 FilterChain 拦截所有请求并要求表单登录，**与本项目无状态 JWT 架构冲突**。因此仅引入 `spring-security-crypto` 包。

### 6.2 JwtTokenProvider

位置：`dms-system/src/main/java/cn/org/openygt/system/security/JwtTokenProvider.java`

```java
package cn.org.openygt.system.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret:openygt-default-secret-key-2024-change-in-prod}")
    private String jwtSecret;

    @Value("${jwt.expiration-ms:7200000}")  // 默认 2 小时
    private long jwtExpirationMs;

    @Value("${jwt.refresh-threshold-ms:900000}") // 默认 15 分钟内允许刷新
    private long refreshThresholdMs;

    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        // 密钥长度至少 256 bit (32 bytes) 以匹配 HS256
        this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 Access Token
     */
    public String generateToken(String username, String tenantId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .setSubject(username)
                .claim("tenantId", tenantId)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 从 Token 提取用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = parseClaims(token);
        return claims.getSubject();
    }

    /**
     * 从 Token 提取租户ID
     */
    public String getTenantIdFromToken(String token) {
        Claims claims = parseClaims(token);
        return claims.get("tenantId", String.class);
    }

    /**
     * 验证 Token 是否有效（签名正确、未过期）
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.warn("Invalid JWT signature: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.warn("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }

    /**
     * 判断 Token 是否在可刷新窗口期内
     */
    public boolean isRefreshable(String token) {
        try {
            Claims claims = parseClaims(token);
            Date expiration = claims.getExpiration();
            long remaining = expiration.getTime() - System.currentTimeMillis();
            return remaining > 0 && remaining <= refreshThresholdMs;
        } catch (ExpiredJwtException e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public long getExpirationMs() {
        return jwtExpirationMs;
    }
}
```

> **配置建议**：生产环境必须在 `application.yml` 中覆盖 `jwt.secret`，长度 ≥ 32 字符。

### 6.3 JwtAuthenticationFilter

位置：`dms-system/src/main/java/cn/org/openygt/system/security/JwtAuthenticationFilter.java`

```java
package cn.org.openygt.system.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * JWT 认证过滤器。
 * 拦截所有请求，提取 Header 中的 Bearer Token 并校验。
 * 校验通过后将用户信息（username, tenantId）写入 Request Attribute，供后续业务使用。
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String AUTH_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String ATTR_USERNAME = "jwt_username";
    public static final String ATTR_TENANT_ID = "jwt_tenant_id";

    private final JwtTokenProvider tokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);

        if (StringUtils.hasText(token) && tokenProvider.validateToken(token)) {
            String username = tokenProvider.getUsernameFromToken(token);
            String tenantId = tokenProvider.getTenantIdFromToken(token);
            request.setAttribute(ATTR_USERNAME, username);
            request.setAttribute(ATTR_TENANT_ID, tenantId);
            log.debug("JWT authenticated: user={}, uri={}", username, request.getRequestURI());
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader(AUTH_HEADER);
        if (StringUtils.hasText(bearer) && bearer.startsWith(BEARER_PREFIX)) {
            return bearer.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
```

### 6.4 AuthController

位置：`dms-system/src/main/java/cn/org/openygt/system/controller/AuthController.java`

```java
package cn.org.openygt.system.controller;

import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.system.dto.LoginRequest;
import cn.org.openygt.system.dto.TokenResponse;
import cn.org.openygt.system.entity.SysUser;
import cn.org.openygt.system.security.JwtTokenProvider;
import cn.org.openygt.system.service.SysUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@Slf4j
@RestController
@RequestMapping(SystemModule.API_PREFIX + "/auth")
public class AuthController {

    private final SysUserService sysUserService;
    private final JwtTokenProvider tokenProvider;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthController(SysUserService sysUserService,
                          JwtTokenProvider tokenProvider,
                          BCryptPasswordEncoder passwordEncoder) {
        this.sysUserService = sysUserService;
        this.tokenProvider = tokenProvider;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@RequestBody @Validated LoginRequest request) {
        SysUser user = sysUserService.getByUsername(request.getUsername());
        if (user == null) {
            log.warn("Login failed: username not found: {}", request.getUsername());
            return ApiResponse.error(401, "用户名或密码错误");
        }
        if ("LOCKED".equals(user.getStatus())) {
            return ApiResponse.error(403, "账号已被锁定");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Login failed: password mismatch: {}", request.getUsername());
            return ApiResponse.error(401, "用户名或密码错误");
        }

        String token = tokenProvider.generateToken(user.getUsername(), user.getTenantId());
        TokenResponse response = new TokenResponse(
                token,
                "Bearer",
                tokenProvider.getExpirationMs() / 1000,
                user.getUsername()
        );
        return ApiResponse.success(response);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        // 无状态 JWT，后端无需处理。可扩展：将 token 加入 Redis 黑名单。
        String username = (String) request.getAttribute(JwtAuthenticationFilter.ATTR_USERNAME);
        log.info("User logout: {}", username);
        return ApiResponse.success();
    }

    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(HttpServletRequest request) {
        String token = resolveToken(request);
        if (token == null || !tokenProvider.validateToken(token)) {
            return ApiResponse.error(401, "Token无效或已过期");
        }
        if (!tokenProvider.isRefreshable(token)) {
            return ApiResponse.error(401, "Token不在可刷新窗口期内");
        }
        String username = tokenProvider.getUsernameFromToken(token);
        String tenantId = tokenProvider.getTenantIdFromToken(token);
        String newToken = tokenProvider.generateToken(username, tenantId);
        TokenResponse response = new TokenResponse(
                newToken,
                "Bearer",
                tokenProvider.getExpirationMs() / 1000,
                username
        );
        return ApiResponse.success(response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}
```

### 6.5 保护范围与放行规则

在 `dms-app` 或 `dms-system` 中注册 Filter（推荐在 `dms-app` 的 `WebMvcConfig` 或独立 `SecurityConfig` 中）：

```java
package cn.org.openygt.app.config;

import cn.org.openygt.system.security.JwtAuthenticationFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FilterConfig {

    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.addUrlPatterns("/api/*");
        registration.setOrder(1); // 顺序靠前，优先执行
        return registration;
    }
}
```

**放行白名单**（无需 Token 即可访问）：

| URL Pattern | 说明 |
|-------------|------|
| `/api/v1/sys/auth/login` | 登录 |
| `/api/v1/sys/auth/logout` | 登出（允许无 Token，有则记录） |
| `/health` / `/actuator/health` | 健康检查 |
| `/error` | Spring Boot 错误页 |

> **设计决策**：`JwtAuthenticationFilter` 对所有 `/api/*` 请求执行，但白名单接口在校验失败时不阻断请求（Filter 继续 `filterChain.doFilter`）。需要 Token 的接口由 Controller/Service 层通过 `request.getAttribute(ATTR_USERNAME)` 判断，为 null 则返回 401。

### 6.6 辅助工具：LoginUserContext

位置：`dms-system/src/main/java/cn/org/openygt/system/security/LoginUserContext.java`

```java
package cn.org.openygt.system.security;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

public final class LoginUserContext {
    private LoginUserContext() {}

    public static String currentUsername() {
        HttpServletRequest req = getRequest();
        return req == null ? null : (String) req.getAttribute(JwtAuthenticationFilter.ATTR_USERNAME);
    }

    public static String currentTenantId() {
        HttpServletRequest req = getRequest();
        return req == null ? "default" : (String) req.getAttribute(JwtAuthenticationFilter.ATTR_TENANT_ID);
    }

    private static HttpServletRequest getRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs == null ? null : attrs.getRequest();
    }
}
```

---

## 七、操作日志 AOP 详细设计

### 7.1 注解定义

位置：`dms-system/src/main/java/cn/org/openygt/system/aop/OperationLog.java`

```java
package cn.org.openygt.system.aop;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {

    /** 操作动作，如：创建用户、更新配置 */
    String action();

    /** 功能模块，如：user、config、auth */
    String module();

    /** 是否记录请求参数（默认 true） */
    boolean recordParams() default true;

    /** 是否异步记录（默认 true） */
    boolean async() default true;
}
```

### 7.2 切面逻辑

位置：`dms-system/src/main/java/cn/org/openygt/system/aop/OperationLogAspect.java`

```java
package cn.org.openygt.system.aop;

import cn.org.openygt.system.entity.SysLog;
import cn.org.openygt.system.security.JwtAuthenticationFilter;
import cn.org.openygt.system.security.LoginUserContext;
import cn.org.openygt.system.service.SysLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

@Slf4j
@Aspect
@Component
public class OperationLogAspect {

    private final SysLogService sysLogService;
    private final ObjectMapper objectMapper;

    public OperationLogAspect(SysLogService sysLogService, ObjectMapper objectMapper) {
        this.sysLogService = sysLogService;
        this.objectMapper = objectMapper;
    }

    @Around("@annotation(operationLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperationLog operationLog) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = null;
        boolean success = true;
        String errorMsg = null;

        try {
            result = joinPoint.proceed();
        } catch (Throwable ex) {
            success = false;
            errorMsg = ex.getMessage();
            throw ex;
        } finally {
            long cost = System.currentTimeMillis() - start;
            try {
                SysLog sysLog = buildLog(operationLog, joinPoint, success, errorMsg, cost);
                if (operationLog.async()) {
                    sysLogService.saveAsync(sysLog);
                } else {
                    sysLogService.save(sysLog);
                }
            } catch (Exception e) {
                // 日志记录异常不能影响主业务
                log.error("Failed to save operation log", e);
            }
        }
        return result;
    }

    private SysLog buildLog(OperationLog operationLog, ProceedingJoinPoint joinPoint,
                            boolean success, String errorMsg, long cost) {
        SysLog sysLog = new SysLog();
        sysLog.setAction(operationLog.action() + (success ? "" : "[失败]"));
        sysLog.setModule(operationLog.module());
        sysLog.setTenantId(LoginUserContext.currentTenantId());
        sysLog.setUserId(LoginUserContext.currentUsername());

        HttpServletRequest request = getCurrentRequest();
        if (request != null) {
            sysLog.setIpAddress(getClientIp(request));
        }

        if (operationLog.recordParams()) {
            try {
                String params = objectMapper.writeValueAsString(joinPoint.getArgs());
                // 截断防止超长
                if (params.length() > 2000) {
                    params = params.substring(0, 2000) + "...(truncated)";
                }
                sysLog.setDetail(params + ", cost=" + cost + "ms" +
                        (errorMsg != null ? ", error=" + errorMsg : ""));
            } catch (Exception e) {
                sysLog.setDetail("[serialize-error]");
            }
        }

        return sysLog;
    }

    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs == null ? null : attrs.getRequest();
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多级代理时取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
```

### 7.3 SysLogService（异步写入）

位置：`dms-system/src/main/java/cn/org/openygt/system/service/impl/SysLogServiceImpl.java`

```java
package cn.org.openygt.system.service.impl;

import cn.org.openygt.system.entity.SysLog;
import cn.org.openygt.system.mapper.SysLogMapper;
import cn.org.openygt.system.service.SysLogService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SysLogServiceImpl extends ServiceImpl<SysLogMapper, SysLog> implements SysLogService {

    @Override
    @Async("taskExecutor")
    public void saveAsync(SysLog sysLog) {
        try {
            this.save(sysLog);
        } catch (Exception e) {
            log.error("Async save sys_log failed", e);
        }
    }

    @Override
    public IPage<SysLog> queryLogs(String module, String action, String userId,
                                   String beginTime, String endTime, int page, int size) {
        // MyBatis-Plus LambdaQueryWrapper 组装查询条件
        // ...
        return this.page(new Page<>(page, size), wrapper);
    }
}
```

> **必须在 `dms-app` 启用 `@EnableAsync`**：
> ```java
> @SpringBootApplication
> @EnableAsync
> public class DmsApplication { ... }
> ```

### 7.4 注解使用示例

```java
@RestController
@RequestMapping(SystemModule.API_PREFIX + "/users")
public class SysUserController {

    @OperationLog(action = "创建用户", module = "user")
    @PostMapping
    public ApiResponse<SysUserResponse> create(@RequestBody @Validated(SysUserRequest.Create.class) SysUserRequest request) {
        // ...
    }

    @OperationLog(action = "更新用户", module = "user")
    @PutMapping("/{id}")
    public ApiResponse<SysUserResponse> update(@PathVariable Long id,
                                               @RequestBody @Validated(SysUserRequest.Update.class) SysUserRequest request) {
        // ...
    }

    @OperationLog(action = "删除用户", module = "user", recordParams = false)
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        // ...
    }
}
```

---

## 八、系统配置缓存策略

### 8.1 设计目标

- **读多写少**：配置项被频繁读取，但修改极少。
- **低延迟**：避免每次读配置都查数据库。
- **最终一致**：配置修改后，缓存立即失效或更新。

### 8.2 缓存实现

位置：`dms-system/src/main/java/cn/org/openygt/system/config/SysConfigCache.java`

```java
package cn.org.openygt.system.config;

import cn.org.openygt.system.entity.SysConfig;
import cn.org.openygt.system.service.SysConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 系统配置本地缓存。
 * 采用 ConcurrentHashMap 实现，启动时全量预热，写操作后主动刷新。
 */
@Slf4j
@Component
public class SysConfigCache {

    private final Map<String, String> cache = new ConcurrentHashMap<>();
    private final SysConfigService sysConfigService;

    public SysConfigCache(SysConfigService sysConfigService) {
        this.sysConfigService = sysConfigService;
    }

    @PostConstruct
    public void init() {
        reloadAll();
    }

    /**
     * 获取配置值，缓存未命中则查库并回填。
     */
    public String get(String configKey) {
        String value = cache.get(configKey);
        if (value == null) {
            SysConfig config = sysConfigService.getByKey(configKey);
            if (config != null) {
                value = config.getConfigValue();
                cache.put(configKey, value);
            }
        }
        return value;
    }

    /**
     * 获取配置值，带默认值。
     */
    public String getOrDefault(String configKey, String defaultValue) {
        String value = get(configKey);
        return value != null ? value : defaultValue;
    }

    /**
     * 更新或新增缓存项。
     */
    public void put(String configKey, String configValue) {
        cache.put(configKey, configValue);
    }

    /**
     * 删除缓存项。
     */
    public void evict(String configKey) {
        cache.remove(configKey);
    }

    /**
     * 全量重新加载（用于管理后台手动刷新）。
     */
    public void reloadAll() {
        cache.clear();
        sysConfigService.listAllActive().forEach(c -> cache.put(c.getConfigKey(), c.getConfigValue()));
        log.info("SysConfigCache reloaded, size={}", cache.size());
    }
}
```

### 8.3 缓存一致性保证

在 `SysConfigServiceImpl` 写操作后主动刷新缓存：

```java
@Service
public class SysConfigServiceImpl implements SysConfigService {

    private final SysConfigMapper configMapper;
    private final SysConfigCache configCache;

    // ...

    @Override
    @Transactional
    public SysConfig create(SysConfig config) {
        // 校验 configKey 唯一性...
        configMapper.insert(config);
        configCache.put(config.getConfigKey(), config.getConfigValue());
        return config;
    }

    @Override
    @Transactional
    public SysConfig update(Long id, SysConfig config) {
        SysConfig existing = configMapper.selectById(id);
        // ...
        configMapper.updateById(config);
        // 若 key 发生变化，需清理旧 key
        if (!existing.getConfigKey().equals(config.getConfigKey())) {
            configCache.evict(existing.getConfigKey());
        }
        configCache.put(config.getConfigKey(), config.getConfigValue());
        return configMapper.selectById(id);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        SysConfig existing = configMapper.selectById(id);
        if (existing != null) {
            configMapper.deleteById(id); // 逻辑删除
            configCache.evict(existing.getConfigKey());
        }
    }
}
```

### 8.4 配置管理接口缓存行为

| 操作 | 缓存行为 |
|------|----------|
| 启动 | `@PostConstruct` 全量预热 |
| GET `/configs/key/{key}` | 先读 Cache，未命中查库回填 |
| POST `/configs` | 入库成功后 `cache.put` |
| PUT `/configs/{id}` | 更新成功后 `cache.put`（key 变化则 `evict` 旧 key） |
| DELETE `/configs/{id}` | 逻辑删除后 `cache.evict` |

---

## 九、异常处理策略

### 9.1 现有 GlobalExceptionHandler 扩展

在 `dms-common` 的 `GlobalExceptionHandler.java` 中补充以下异常处理器：

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 现有处理器保留...

    /** 认证异常：401 */
    @ExceptionHandler(UnauthorizedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResponse<?> handleUnauthorized(UnauthorizedException e) {
        return ApiResponse.error(401, e.getMessage());
    }

    /** 鉴权异常：403 */
    @ExceptionHandler(ForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse<?> handleForbidden(ForbiddenException e) {
        return ApiResponse.error(403, e.getMessage());
    }

    /** 数据不存在：404（业务层使用） */
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<?> handleNotFound(ResourceNotFoundException e) {
        return ApiResponse.error(404, e.getMessage());
    }
}
```

### 9.2 新增业务异常类（dms-common）

```java
// UnauthorizedException.java
package cn.org.openygt.common.exception;

public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) { super(message); }
}

// ForbiddenException.java
package cn.org.openygt.common.exception;

public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) { super(message); }
}

// ResourceNotFoundException.java
package cn.org.openygt.common.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) { super(message); }
}
```

### 9.3 异常码汇总

| HTTP Code | 业务 Code | 场景 | 抛出方式 |
|-----------|-----------|------|----------|
| 400 | 400 | 参数校验失败、业务参数错误 | `IllegalArgumentException` |
| 401 | 401 | Token 无效/过期、登录失败 | `UnauthorizedException` |
| 403 | 403 | 账号锁定、权限不足 | `ForbiddenException` |
| 404 | 404 | 资源不存在（配置项等） | `ResourceNotFoundException` |
| 409 | 409 | 状态冲突（已有实现） | `IllegalStateException` |
| 500 | 500 | 服务器内部错误 | 通用 `Exception` |

### 9.4 Controller 层认证检查模式

对于需要登录的接口，在 Controller 方法内统一检查：

```java
private void requireLogin(HttpServletRequest request) {
    String username = (String) request.getAttribute(JwtAuthenticationFilter.ATTR_USERNAME);
    if (username == null) {
        throw new UnauthorizedException("未登录或Token已过期");
    }
}
```

> **后续优化**：可封装为自定义注解 + 拦截器，替代每个 Controller 方法的手动检查。本期为减少改动范围，采用手动检查或 AOP 统一处理。

---

## 十、单元测试策略

### 10.1 测试依赖

在 `dms-system/pom.xml` 测试 scope 补充：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
</dependency>
```

### 10.2 分层测试计划

| 测试类 | 目标 | 技术 |
|--------|------|------|
| `JwtTokenProviderTest` | Token 生成、解析、过期、刷新窗口判断 | JUnit 5 + 时间偏移模拟 |
| `BCryptPasswordEncoderTest` | 加密后长度、匹配正确性、明文不匹配 | JUnit 5 |
| `SysUserServiceImplTest` | 创建（加密密码）、更新（密码可选）、用户名唯一性校验 | `@SpringBootTest` + H2 |
| `SysConfigServiceImplTest` | CRUD + 缓存一致性（Mock `SysConfigCache`） | `@SpringBootTest` + H2 |
| `AuthControllerTest` | 登录成功/失败/锁定、刷新 Token | `MockMvc` + Mock Service |
| `OperationLogAspectTest` | 切面是否正常触发、异常时是否记录失败标记 | `@SpringBootTest` + AOP |
| `SysConfigCacheTest` | 预热、get、evict、reloadAll | JUnit 5 + Mockito |

### 10.3 关键测试用例示例

**JwtTokenProviderTest**：

```java
@Test
void shouldGenerateValidToken() {
    String token = provider.generateToken("admin", "default");
    assertTrue(provider.validateToken(token));
    assertEquals("admin", provider.getUsernameFromToken(token));
    assertEquals("default", provider.getTenantIdFromToken(token));
}

@Test
void shouldDetectExpiredToken() throws InterruptedException {
    // 设置过期时间为 1ms
    // 验证 validateToken 返回 false
}

@Test
void shouldIdentifyRefreshWindow() {
    // Token 剩余 10 分钟：不可刷新
    // Token 剩余 5 分钟：可刷新
}
```

**SysUserServiceImplTest（密码加密断言）**：

```java
@Test
void shouldEncryptPasswordOnCreate() {
    SysUser user = new SysUser();
    user.setUsername("test01");
    user.setPlainPassword("123456");
    
    SysUser created = service.create(user);
    
    assertNotEquals("123456", created.getPassword());
    assertTrue(passwordEncoder.matches("123456", created.getPassword()));
}
```

### 10.4 测试配置

`src/test/resources/application-test.yml`：

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;MODE=SQLite;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
  sql:
    init:
      schema-locations: classpath:db/schema-test.sql
      mode: always

jwt:
  secret: test-secret-key-at-least-32-characters-long
  expiration-ms: 3600000
```

---

## 十一、迁移路径

### 11.1 文件变更清单

#### 新增文件（按包组织）

```
dms-common/
└── src/main/java/cn/org/openygt/common/
    ├── entity/BaseAuditEntity.java              # 新增审计基类
    └── exception/
        ├── UnauthorizedException.java           # 新增
        ├── ForbiddenException.java              # 新增
        └── ResourceNotFoundException.java       # 新增

dms-system/
└── src/main/java/cn/org/openygt/system/
    ├── controller/
    │   └── AuthController.java                  # 新增
    ├── dto/
    │   ├── LoginRequest.java                    # 新增
    │   ├── TokenResponse.java                   # 新增
    │   ├── SysUserRequest.java                  # 新增
    │   ├── SysUserResponse.java                 # 新增
    │   ├── SysConfigRequest.java                # 新增
    │   ├── SysConfigResponse.java               # 新增
    │   └── SysLogResponse.java                  # 新增
    ├── entity/
    │   ├── SysConfig.java                       # 新增
    │   └── SysLog.java                          # 新增
    ├── mapper/
    │   ├── SysConfigMapper.java                 # 新增
    │   └── SysLogMapper.java                    # 新增
    ├── service/
    │   ├── SysConfigService.java                # 新增
    │   ├── SysLogService.java                   # 新增
    │   └── impl/
    │       ├── SysConfigServiceImpl.java        # 新增
    │       └── SysLogServiceImpl.java           # 新增
    ├── security/
    │   ├── JwtTokenProvider.java                # 新增
    │   ├── JwtAuthenticationFilter.java         # 新增
    │   └── LoginUserContext.java                # 新增
    ├── aop/
    │   ├── OperationLog.java                    # 新增
    │   └── OperationLogAspect.java              # 新增
    ├── config/
    │   └── SysConfigCache.java                  # 新增
    └── util/
        └── AesUtil.java                         # 新增（预留）
```

#### 改造文件

```
dms-common/
├── entity/BaseEntity.java                       # 无需修改（已有）
└── exception/GlobalExceptionHandler.java        # 扩展：新增 3 个异常处理器

dms-system/
├── controller/SysUserController.java            # 改造：DTO 替换、@OperationLog、登录检查
├── entity/SysUser.java                          # 改造：增加 transient plainPassword
├── service/SysUserService.java                  # 改造：增加 changePassword 等可选方法
├── service/impl/SysUserServiceImpl.java         # 改造：BCrypt 加密、用户名唯一校验、屏蔽密码更新逻辑
└── pom.xml                                      # 改造：新增依赖

dms-app/
├── src/main/java/cn/org/openygt/app/config/
│   └── FilterConfig.java                        # 新增：注册 JwtAuthenticationFilter
└── DmsApplication.java                          # 改造：添加 @EnableAsync
```

### 11.2 数据库迁移

新建 Flyway/Liquibase 脚本或手动执行：

```sql
-- V5__system_module_enhance.sql（SQLite 版）
-- 1. 若原 sys_user 无 password 长度限制或明文过短，需先 ALTER 扩容
-- 2. 重新创建索引（V4 已建，此处可跳过）
-- 3. 建议为现有用户生成默认 BCrypt 密码，并通知强制改密

UPDATE sys_user SET password = '$2a$10$...bcrypt_of_temp_password...' WHERE LENGTH(password) < 20;
```

> **存量密码处理策略**：由于现有密码为明文或简单哈希，无法直接迁移为 BCrypt。推荐方案：
> 1. 为所有存量用户生成随机临时密码（BCrypt 存储）。
> 2. 首次登录时检测标志位 `force_change_password = true`，强制跳转修改密码。

### 11.3 配置项变更（application.yml）

```yaml
# 新增
jwt:
  secret: ${JWT_SECRET:change-me-in-production-at-least-32-chars!}
  expiration-ms: 7200000        # 2小时
  refresh-threshold-ms: 900000  # 15分钟内可刷新

# 异步线程池（用于日志写入，可选自定义）
spring:
  task:
    execution:
      pool:
        core-size: 4
        max-size: 10
        queue-capacity: 100
```

### 11.4 回滚方案

| 步骤 | 回滚操作 |
|------|----------|
| JWT 认证异常 | 移除 `FilterConfig` 中的 `JwtAuthenticationFilter` 注册，恢复为无认证模式 |
| BCrypt 加密问题 | 在 `SysUserServiceImpl` 中增加开关 `password.encryption.enabled`，紧急关闭时直存明文（仅应急） |
| 缓存不一致 | `SysConfigCache.reloadAll()` 提供手动刷新接口（管理后台按钮） |
| AOP 异常影响主业务 | `OperationLogAspect` 已用 `try-catch` 隔离，若仍有问题可移除 `@Aspect` 注解临时关闭 |

---

## 附录：AES 工具类（预留）

位置：`dms-system/src/main/java/cn/org/openygt/system/util/AesUtil.java`

```java
package cn.org.openygt.system.util;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * AES-CBC 加密工具类（预留）。
 * 本期不接入业务，仅提供工具方法供后续敏感字段（如手机号）加密使用。
 */
public final class AesUtil {

    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String KEY_ALGORITHM = "AES";

    private AesUtil() {}

    public static String encrypt(String plainText, String key, String iv) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), KEY_ALGORITHM);
        IvParameterSpec ivSpec = new IvParameterSpec(iv.getBytes(StandardCharsets.UTF_8));
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
        byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encrypted);
    }

    public static String decrypt(String encryptedText, String key, String iv) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), KEY_ALGORITHM);
        IvParameterSpec ivSpec = new IvParameterSpec(iv.getBytes(StandardCharsets.UTF_8));
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
        byte[] decoded = Base64.getDecoder().decode(encryptedText);
        return new String(cipher.doFinal(decoded), StandardCharsets.UTF_8);
    }
}
```

> **密钥管理要求**：生产环境 `key` 必须为 16/24/32 字节，`iv` 必须为 16 字节，禁止硬编码于源码，应从环境变量或配置中心读取。
