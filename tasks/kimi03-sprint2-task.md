# kimi03 任务单: MQTT 适配器 + 设备影子 + 心跳

> Sprint 1 任务: 概要设计
> 截止: 收到本任务单后 2 天内
> 产出: `docs/tasks/kimi03-概设.md`

## 背景

当前 dms-equipment 的 MQTT 接入是硬编码的消息处理，缺少适配器抽象和设备影子机制。你负责 MQTT 侧的适配器实现、设备影子和心跳超时检测。

## 需求范围

对应需求编号: EQ-010, EQ-011, EQ-013, EQ-014

- 仟方煎药机 MQTT 适配器
- 厚达煎药机 MQTT 适配器
- 设备影子（reported/desired 双态缓存）
- 心跳超时自动标记离线

## 概设要求

请按概设模板编写，重点关注:

1. MQTT 适配器如何复用 kimi02 设计的 `DeviceAdapter` 接口（与 kimi02 协调）
2. 仟方和厚达的 MQTT Topic 命名差异如何处理
3. 设备影子数据结构设计（reported state + desired state + version）
4. 影子存储方式（内存缓存 + 数据库持久化？还是纯数据库？）
5. 心跳调度器设计（`@Scheduled` 定时扫描？间隔多少？）
6. 与现有 `MqttConfig` 的集成方式

## 参考

- 现有 MQTT 配置: `openygt-dms/dms-equipment/src/main/java/cn/org/openygt/equipment/config/MqttConfig.java`
- 需求文档中的设备影子定义: `docs-iot/requirements/需求规格说明书.md` 第 3.2.3 节

## 提交方式

完成后更新 `docs/tasks/kimi03-reply.md` 通知架构师审核。
