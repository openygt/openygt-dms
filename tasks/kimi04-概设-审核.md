# 架构师审核 - kimi04 概要设计

> 审核日期: 2026-04-25
> 审核文档: kimi04-概设.md
> 审核结论: **有条件通过** — 2个小问题修正后可进入详设

---

## 审核意见

### 整体评价

概设质量很好。模块边界清晰，接口设计合理，技术方案（缓存、异步日志、AOP）选型恰当。开发顺序建议也很务实。

### 通过项

- 煎药方案 Entity 设计与现有表结构完全对齐
- 系统配置的 ConcurrentHashMap 缓存方案合理（避免引入 Redis）
- 操作日志的双轨制（AOP + 手动）覆盖面好
- @Async 异步写入日志是正确决策
- 与 dms-production 的关联方式清晰

### 需修正（2项）

#### 1. SysLogService 接口应定义在 dms-common 中

你设计的 `SysLogService.log()` 会被 dms-production、dms-quality、dms-equipment、dms-print 四个模块调用。按照现有架构模式（EquipmentService/PrintService/QualityService 都定义在 dms-common），`SysLogService` 接口也应该定义在 `dms-common` 中，实现放在 `dms-system`。

```
dms-common/service/SysLogService.java      # 接口定义
dms-system/service/impl/SysLogServiceImpl.java  # 实现
```

这样其他模块只需依赖 dms-common 即可调用日志服务，不需要直接依赖 dms-system。

#### 2. sys_log 表应保留 tenant_id 但不需要 deleted 字段

你在文档中提到"审计日志物理留存，无 deleted"——这个决策是对的。但请在详设中明确：sys_log 的 Entity **不继承 BaseEntity**（因为 BaseEntity 带 deleted 字段），改为独立定义。

### 建议（非阻塞）

- `@LogOperation` 注解的设计不错，但建议在详设中明确：哪些 Controller 方法需要加注解，哪些不需要（比如纯查询接口不需要记日志）
- 线程池 `queueCapacity=100` 对当前规模足够，但建议加一个 `RejectedExecutionHandler` 降级策略

---

## 结论

修正以上 2 项后，可进入详细设计阶段。请在 `kimi04-详设.md` 中体现修正。
