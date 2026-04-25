# kimi02 任务单: 设备适配器框架 + TCP 适配器

> Sprint 1 任务: 概要设计
> 截止: 收到本任务单后 2 天内
> 产出: `docs/tasks/kimi02-概设.md`

## 背景

当前 dms-equipment 模块已实现设备基础管理和 MQTT 状态接收，但缺少多厂商设备协议适配能力。你负责设计和实现适配器框架及 TCP 协议适配器。

## 需求范围

对应需求编号: EQ-007, EQ-008, EQ-009（参考 `docs-iot/requirements/需求规格说明书.md`）

- 统一的 `DeviceAdapter` 接口定义
- `AdapterRegistry` 适配器注册中心（按厂商+协议自动路由）
- 东华原煎药机 TCP 二进制适配器
- 三延煎药机 TCP 二进制适配器

## 概设要求

请按 `docs-iot/project/项目计划.md` 中的概设模板编写，重点关注:

1. `DeviceAdapter` 接口方法定义（connect/disconnect/sendCommand/parseStatus）
2. `AdapterRegistry` 如何根据设备的 manufacturer + protocol 自动选择适配器
3. TCP 连接管理（Netty 还是原生 NIO？考虑 JDK8 约束）
4. 与现有 `EquipmentService` 的集成方式
5. 新增的数据库表（如果需要）

## 参考

- 现有代码: `openygt-dms/dms-equipment/src/main/java/cn/org/openygt/equipment/`
- 现有接口: `openygt-dms/dms-common/src/main/java/cn/org/openygt/common/service/EquipmentService.java`
- 架构文档: `docs-iot/architecture/架构设计说明书.md`

## 提交方式

完成后更新 `docs/tasks/kimi02-reply.md` 通知架构师审核。
