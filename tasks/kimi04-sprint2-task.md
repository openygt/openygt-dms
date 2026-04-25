# kimi04 任务单: 煎药方案 + 系统配置 + 操作日志

> Sprint 1 任务: 概要设计
> 截止: 收到本任务单后 2 天内
> 产出: `docs/tasks/kimi04-概设.md`

## 背景

dms-masterdata 和 dms-system 模块的数据库表已建好，但部分接口尚未实现。你负责补全这些 CRUD 接口。

## 需求范围

对应需求编号: MD-002, SYS-002, SYS-003

### dms-masterdata: 煎药方案管理
- `md_decoct_scheme` 表已存在
- 需要: Entity + Mapper + Service + Controller
- API: `/api/v1/md/schemes` (CRUD)

### dms-system: 系统配置管理
- `sys_config` 表已存在（config_key, config_value, description）
- 需要: Entity + Mapper + Service + Controller
- API: `/api/v1/sys/configs` (CRUD + 按 key 查询)

### dms-system: 操作日志
- `sys_log` 表已存在（user_id, action, module, detail, ip_address）
- 需要: Entity + Mapper + Service + Controller
- API: `/api/v1/sys/logs` (查询 + 按模块/用户/时间范围过滤)
- 日志写入: 提供 `SysLogService.log()` 方法供其他模块调用

## 概设要求

请按概设模板编写，重点关注:

1. 煎药方案的字段定义（参考现有 `md_decoct_scheme` 表结构）
2. 系统配置的缓存策略（是否需要内存缓存？）
3. 操作日志的写入方式（同步写入还是异步？AOP 切面还是手动调用？）
4. 与 dms-production 的关联（任务创建时关联煎药方案）

## 参考

- 现有表结构: `openygt-dms/dms-app/src/main/resources/db/migration/V4__module_split.sql`
- 现有 masterdata 代码: `openygt-dms/dms-masterdata/`
- 现有 system 代码: `openygt-dms/dms-system/`

## 提交方式

完成后更新 `docs/tasks/kimi04-reply.md` 通知架构师审核。
