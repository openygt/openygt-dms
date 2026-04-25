# kimi04 详细设计 - 煎药方案 + 系统配置 + 操作日志

> 负责人: kimi04
> 对应概设: `docs/tasks/kimi04-概设.md`
> 日期: 2026-04-25
> **重要修正**: 基于 decoction 单体项目实际代码调研，包名使用 `com.decoction`，表名保持与现有数据库一致。

---

## 0. 现状调研结论

| 模块 | 现状 | 动作 |
|------|------|------|
| DecoctScheme | Entity/Mapper/Service/Controller **已存在**，表 `decoct_scheme` 已存在 | **改造**: 修正 API 路径、增加 keyword 查询、统一异常处理 |
| SysConfig | **不存在**，表 `sys_config` **不存在** | **新建**: Entity/Mapper/Service/Controller + 新增表 |
| SysLog | **不存在**，表 `sys_log` **不存在** | **新建**: Entity/Mapper/Service/Controller/AOP/Async + 新增表 |

---

## 1. 煎药方案 (DecoctScheme) — 改造设计

### 1.1 现有代码问题清单

| 问题 | 现有实现 | 目标实现 |
|------|---------|---------|
| API 路径 | `/api/decoct-schemes` | `/api/v1/md/schemes`（与 openygt-dms 对齐） |
| 列表查询 | 无 keyword 参数 | 支持 name/description 模糊搜索 |
| 异常处理 | Controller 层返回 404 | Service 层抛 `IllegalArgumentException`，全局异常处理器统一处理 |
| 事务 | Service 无 `@Transactional` | create/update/delete 加 `@Transactional` |

### 1.2 改造后代码

**Entity**（`com.decoction.entity.DecoctScheme`）— 无需修改，已正确映射 `decoct_scheme` 表：
```java
@Data
@TableName("decoct_scheme")
public class DecoctScheme {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Integer schemeType;
    private Integer decoctTimes;
    private Integer pressure;
    private BigDecimal upperWater;
    private Integer heatingTime;
    private Integer preHeatingTime;
    private Integer postHeatingTime;
    private String description;
    @TableLogic
    private Integer deleted;
    private Date createdAt;
    private Date updatedAt;
}
```

**Service Interface**（`com.decoction.service.DecoctSchemeService`）— 增加 keyword 参数：
```java
public interface DecoctSchemeService {
    DecoctScheme create(DecoctScheme scheme);
    DecoctScheme update(Long id, DecoctScheme scheme);
    DecoctScheme getById(Long id);
    IPage<DecoctScheme> list(String keyword, int page, int size);  // ← 增加 keyword
    void delete(Long id);
}
```

**Service Impl**（`com.decoction.service.impl.DecoctSchemeServiceImpl`）— 关键变更：
```java
@Slf4j
@Service
@RequiredArgsConstructor
public class DecoctSchemeServiceImpl implements DecoctSchemeService {

    private final DecoctSchemeMapper decoctSchemeMapper;

    @Override
    @Transactional  // ← 新增
    public DecoctScheme create(DecoctScheme scheme) {
        decoctSchemeMapper.insert(scheme);
        return scheme;
    }

    @Override
    @Transactional  // ← 新增
    public DecoctScheme update(Long id, DecoctScheme scheme) {
        DecoctScheme existing = decoctSchemeMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("煎药方案不存在: " + id);  // ← 抛异常替代返回 null
        }
        scheme.setId(id);
        decoctSchemeMapper.updateById(scheme);
        return decoctSchemeMapper.selectById(id);
    }

    @Override
    public DecoctScheme getById(Long id) {
        DecoctScheme scheme = decoctSchemeMapper.selectById(id);
        if (scheme == null) {
            throw new IllegalArgumentException("煎药方案不存在: " + id);  // ← 抛异常
        }
        return scheme;
    }

    @Override
    public IPage<DecoctScheme> list(String keyword, int page, int size) {
        LambdaQueryWrapper<DecoctScheme> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {  // ← 新增 keyword 模糊查询
            wrapper.like(DecoctScheme::getName, keyword)
                   .or()
                   .like(DecoctScheme::getDescription, keyword);
        }
        wrapper.orderByDesc(DecoctScheme::getCreatedAt);
        return decoctSchemeMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    @Transactional  // ← 新增
    public void delete(Long id) {
        decoctSchemeMapper.deleteById(id);
    }
}
```

**Controller**（`com.decoction.controller.DecoctSchemeController`）— 路径修正 + 移除 null 检查：
```java
@RestController
@RequestMapping("/api/v1/md/schemes")  // ← 修正路径
@RequiredArgsConstructor
public class DecoctSchemeController {

    private final DecoctSchemeService decoctSchemeService;

    @PostMapping
    public ApiResponse<DecoctScheme> create(@RequestBody DecoctScheme scheme) {
        return ApiResponse.success(decoctSchemeService.create(scheme));
    }

    @PutMapping("/{id}")
    public ApiResponse<DecoctScheme> update(@PathVariable Long id, @RequestBody DecoctScheme scheme) {
        return ApiResponse.success(decoctSchemeService.update(id, scheme));
    }

    @GetMapping("/{id}")
    public ApiResponse<DecoctScheme> getById(@PathVariable Long id) {
        return ApiResponse.success(decoctSchemeService.getById(id));  // ← null 检查移到 Service
    }

    @GetMapping
    public ApiResponse<IPage<DecoctScheme>> list(
            @RequestParam(required = false) String keyword,  // ← 新增
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(decoctSchemeService.list(keyword, page, size));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        decoctSchemeService.delete(id);
        return ApiResponse.success();
    }
}
```

---

## 2. 系统配置 (SysConfig) — 新建设计

### 2.1 数据库变更

**新增迁移脚本**: `src/main/resources/db/migration/V4__sys_config.sql`

```sql
CREATE TABLE IF NOT EXISTS sys_config (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    config_key VARCHAR(100) NOT NULL UNIQUE,
    config_value TEXT,
    description VARCHAR(200),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

> **注**: 单体项目暂无 tenant_id 字段，与现有表风格保持一致。

**修改 DatabaseInitConfig** — 新增 V4 执行：
```java
Resource v4 = new ClassPathResource("db/migration/V4__sys_config.sql");
ScriptUtils.executeSqlScript(conn, v4);
```

### 2.2 Entity

```java
package com.decoction.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("sys_config")
public class SysConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String configKey;
    private String configValue;
    private String description;
    private Date createdAt;
    private Date updatedAt;
}
```

### 2.3 Mapper

```java
package com.decoction.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.decoction.entity.SysConfig;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysConfigMapper extends BaseMapper<SysConfig> {
}
```

### 2.4 Service

```java
package com.decoction.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.decoction.entity.SysConfig;

public interface SysConfigService {
    SysConfig create(SysConfig config);
    SysConfig update(Long id, SysConfig config);
    SysConfig getById(Long id);
    String getValue(String configKey);  // 按 key 查询，带缓存
    IPage<SysConfig> list(String keyword, int page, int size);
    void delete(Long id);
}
```

```java
package com.decoction.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.decoction.entity.SysConfig;
import com.decoction.mapper.SysConfigMapper;
import com.decoction.service.SysConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SysConfigServiceImpl implements SysConfigService {

    private final SysConfigMapper configMapper;

    @Override
    @Transactional
    public SysConfig create(SysConfig config) {
        configMapper.insert(config);
        return config;
    }

    @Override
    @Transactional
    @CacheEvict(value = "sysConfig", key = "#config.configKey")
    public SysConfig update(Long id, SysConfig config) {
        SysConfig existing = configMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("配置项不存在: " + id);
        }
        config.setId(id);
        configMapper.updateById(config);
        return configMapper.selectById(id);
    }

    @Override
    public SysConfig getById(Long id) {
        SysConfig config = configMapper.selectById(id);
        if (config == null) {
            throw new IllegalArgumentException("配置项不存在: " + id);
        }
        return config;
    }

    @Override
    @Cacheable(value = "sysConfig", key = "#configKey", unless = "#result == null")
    public String getValue(String configKey) {
        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysConfig::getConfigKey, configKey);
        SysConfig config = configMapper.selectOne(wrapper);
        return config != null ? config.getConfigValue() : null;
    }

    @Override
    public IPage<SysConfig> list(String keyword, int page, int size) {
        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(SysConfig::getConfigKey, keyword)
                   .or()
                   .like(SysConfig::getDescription, keyword);
        }
        wrapper.orderByDesc(SysConfig::getCreatedAt);
        return configMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    @Transactional
    @CacheEvict(value = "sysConfig", allEntries = true)  // 全清，简化实现
    public void delete(Long id) {
        configMapper.deleteById(id);
    }
}
```

### 2.5 Controller

```java
package com.decoction.controller;

import com.decoction.dto.ApiResponse;
import com.decoction.entity.SysConfig;
import com.decoction.service.SysConfigService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sys/configs")
@RequiredArgsConstructor
public class SysConfigController {

    private final SysConfigService configService;

    @PostMapping
    public ApiResponse<SysConfig> create(@RequestBody SysConfig config) {
        return ApiResponse.success(configService.create(config));
    }

    @PutMapping("/{id}")
    public ApiResponse<SysConfig> update(@PathVariable Long id, @RequestBody SysConfig config) {
        return ApiResponse.success(configService.update(id, config));
    }

    @GetMapping("/{id}")
    public ApiResponse<SysConfig> getById(@PathVariable Long id) {
        return ApiResponse.success(configService.getById(id));
    }

    @GetMapping("/key/{configKey}")
    public ApiResponse<String> getValue(@PathVariable String configKey) {
        return ApiResponse.success(configService.getValue(configKey));
    }

    @GetMapping
    public ApiResponse<IPage<SysConfig>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(configService.list(keyword, page, size));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        configService.delete(id);
        return ApiResponse.success();
    }
}
```

### 2.6 CacheConfig

```java
package com.decoction.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {
}
```

---

## 3. 操作日志 (SysLog) — 新建设计

### 3.1 数据库变更

**追加到 V4 迁移脚本** `src/main/resources/db/migration/V4__sys_config.sql`：

```sql
-- sys_log 已在上方，此处追加 sys_log
CREATE TABLE IF NOT EXISTS sys_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id VARCHAR(50),
    action VARCHAR(100),
    module VARCHAR(50),
    detail TEXT,
    ip_address VARCHAR(50),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

> **命名调整**: 由于 DatabaseInitConfig 按顺序执行 V1→V2→V3→V4，建议将 sys_config 和 sys_log 的创建放在同一个 V4 文件中。原 `V4__quality.sql` 不存在，因此不会冲突。

### 3.2 Entity

```java
package com.decoction.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("sys_log")
public class SysLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String userId;
    private String action;
    private String module;
    private String detail;
    private String ipAddress;
    private Date createdAt;
}
```

### 3.3 Mapper

```java
package com.decoction.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.decoction.entity.SysLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysLogMapper extends BaseMapper<SysLog> {
}
```

### 3.4 Service

```java
package com.decoction.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.decoction.entity.SysLog;

import java.util.Date;

public interface SysLogService {
    void log(String userId, String action, String module, String detail, String ipAddress);
    IPage<SysLog> list(String module, String userId, Date startTime, Date endTime, int page, int size);
}
```

```java
package com.decoction.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.decoction.entity.SysLog;
import com.decoction.mapper.SysLogMapper;
import com.decoction.service.SysLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@RequiredArgsConstructor
public class SysLogServiceImpl implements SysLogService {

    private final SysLogMapper sysLogMapper;

    @Override
    @Async("sysLogExecutor")
    public void log(String userId, String action, String module, String detail, String ipAddress) {
        SysLog log = new SysLog();
        log.setUserId(userId);
        log.setAction(action);
        log.setModule(module);
        log.setDetail(detail);
        log.setIpAddress(ipAddress);
        log.setCreatedAt(new Date());
        sysLogMapper.insert(log);
    }

    @Override
    public IPage<SysLog> list(String module, String userId, Date startTime, Date endTime, int page, int size) {
        LambdaQueryWrapper<SysLog> wrapper = new LambdaQueryWrapper<>();
        if (module != null && !module.isEmpty()) {
            wrapper.eq(SysLog::getModule, module);
        }
        if (userId != null && !userId.isEmpty()) {
            wrapper.eq(SysLog::getUserId, userId);
        }
        if (startTime != null) {
            wrapper.ge(SysLog::getCreatedAt, startTime);
        }
        if (endTime != null) {
            wrapper.le(SysLog::getCreatedAt, endTime);
        }
        wrapper.orderByDesc(SysLog::getCreatedAt);
        return sysLogMapper.selectPage(new Page<>(page, size), wrapper);
    }
}
```

### 3.5 Controller

```java
package com.decoction.controller;

import com.decoction.dto.ApiResponse;
import com.decoction.entity.SysLog;
import com.decoction.service.SysLogService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

@RestController
@RequestMapping("/api/v1/sys/logs")
@RequiredArgsConstructor
public class SysLogController {

    private final SysLogService sysLogService;

    @GetMapping
    public ApiResponse<IPage<SysLog>> list(
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date endTime,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(sysLogService.list(module, userId, startTime, endTime, page, size));
    }
}
```

### 3.6 AOP 注解与切面

```java
package com.decoction.system.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LogOperation {
    String module();
    String action();
}
```

```java
package com.decoction.system.aspect;

import com.decoction.system.annotation.LogOperation;
import com.decoction.service.SysLogService;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;

@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final SysLogService sysLogService;

    @Pointcut("@annotation(com.decoction.system.annotation.LogOperation)")
    public void logPointCut() {}

    @AfterReturning("logPointCut()")
    public void doAfterReturning(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        LogOperation annotation = method.getAnnotation(LogOperation.class);

        String ip = "unknown";
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                ip = request.getRemoteAddr();
            }
        } catch (Exception ignored) {}

        sysLogService.log("SYSTEM", annotation.action(), annotation.module(), method.getName(), ip);
    }
}
```

### 3.7 异步线程池配置

```java
package com.decoction.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean("sysLogExecutor")
    public Executor sysLogExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("syslog-");
        executor.setRejectedExecutionHandler(new ThreadPoolTaskExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
```

---

## 4. 跨模块调用设计

### 4.1 dms-production 调用 SysLogService

在 `TaskServiceImpl`（或相关 Service）中注入 `SysLogService`：

```java
@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {
    private final SysLogService sysLogService;
    // ... 其他依赖

    @Override
    @Transactional
    public Task startSoak(Long taskId, String operatorId) {
        // ... 原有业务逻辑
        sysLogService.log(operatorId, "START_SOAK", "production",
                "任务" + taskId + "开始泡药", "127.0.0.1");
        return task;
    }
}
```

### 4.2 AOP 使用示例

在 Controller 方法上添加注解：
```java
@LogOperation(module = "production", action = "CREATE_TASK")
@PostMapping("/api/v1/prod/tasks/{taskId}/soak/start")
public ApiResponse<Task> startSoak(...) { ... }
```

---

## 5. 文件清单汇总

### 改造文件

| 文件路径 | 动作 | 变更点 |
|----------|------|--------|
| `src/main/java/com/decoction/service/DecoctSchemeService.java` | 修改 | list 增加 keyword 参数 |
| `src/main/java/com/decoction/service/impl/DecoctSchemeServiceImpl.java` | 修改 | @Transactional、keyword 查询、异常处理 |
| `src/main/java/com/decoction/controller/DecoctSchemeController.java` | 修改 | 路径 `/api/v1/md/schemes`、keyword 参数、移除 null 检查 |
| `src/main/java/com/decoction/config/DatabaseInitConfig.java` | 修改 | 追加 V4 脚本执行 |

### 新增文件

| 文件路径 | 说明 |
|----------|------|
| `src/main/resources/db/migration/V4__sys_config.sql` | sys_config + sys_log 建表 |
| `src/main/java/com/decoction/entity/SysConfig.java` | 系统配置实体 |
| `src/main/java/com/decoction/entity/SysLog.java` | 操作日志实体 |
| `src/main/java/com/decoction/mapper/SysConfigMapper.java` | 配置 Mapper |
| `src/main/java/com/decoction/mapper/SysLogMapper.java` | 日志 Mapper |
| `src/main/java/com/decoction/service/SysConfigService.java` | 配置 Service 接口 |
| `src/main/java/com/decoction/service/SysLogService.java` | 日志 Service 接口 |
| `src/main/java/com/decoction/service/impl/SysConfigServiceImpl.java` | 配置 Service 实现（含缓存） |
| `src/main/java/com/decoction/service/impl/SysLogServiceImpl.java` | 日志 Service 实现（含异步） |
| `src/main/java/com/decoction/controller/SysConfigController.java` | 配置 Controller |
| `src/main/java/com/decoction/controller/SysLogController.java` | 日志 Controller |
| `src/main/java/com/decoction/config/CacheConfig.java` | @EnableCaching |
| `src/main/java/com/decoction/config/AsyncConfig.java` | 异步线程池 |
| `src/main/java/com/decoction/system/annotation/LogOperation.java` | AOP 注解 |
| `src/main/java/com/decoction/system/aspect/OperationLogAspect.java` | AOP 切面 |

---

## 6. 测试方案

### 6.1 单元测试

| 测试类 | 覆盖内容 |
|--------|---------|
| `DecoctSchemeServiceImplTest` | keyword 模糊查询、@Transactional 回滚、异常抛出 |
| `SysConfigServiceImplTest` | CRUD、@Cacheable 命中/失效、getValue |
| `SysLogServiceImplTest` | @Async 异步写入、条件查询构造 |

### 6.2 集成测试

| 测试类 | 覆盖内容 |
|--------|---------|
| `DecoctSchemeControllerTest` | `/api/v1/md/schemes` 端到端、keyword 过滤、分页 |
| `SysConfigControllerTest` | `/api/v1/sys/configs` 端到端、`/key/{configKey}` 缓存 |
| `SysLogControllerTest` | 时间范围过滤、模块过滤 |

### 6.3 AOP 测试

```java
@Test
public void testLogAspect() throws Exception {
    mockMvc.perform(post("/api/v1/md/schemes")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"测试方案\",\"heatingTime\":30}"));
    Thread.sleep(500);
    // 验证 sys_log 表中有记录
}
```

---

## 7. 开发顺序与工作量估算

| 顺序 | 任务 | 预估工时 | 说明 |
|------|------|---------|------|
| 1 | V4 迁移脚本 + DatabaseInitConfig 修改 | 0.5h | 新增 sys_config/sys_log 表 |
| 2 | DecoctScheme 改造 | 1h | 路径、keyword、@Transactional |
| 3 | SysConfig 全套（含缓存） | 2h | Entity→Controller + CacheConfig |
| 4 | SysLog 全套（含AOP+Async） | 3h | Entity→Controller + AsyncConfig + Aspect |
| 5 | 单元测试 + 集成测试 | 2h | 全覆盖 |
| **合计** | | **8.5h** | 约 1.5 个工作日 |
