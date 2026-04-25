# 架构师审核 - kimi03 概要设计（MQTT 适配器 + 设备影子 + 心跳）

> 审核日期: 2026-04-25
> 审核结论: **有条件通过** — 2 个问题修正后可进入详设

---

## 整体评价

概设非常扎实。MQTT 路由机制、影子存储方案、心跳检测设计都很合理。MqttConfig 改造方案（抽取 MqttMessageHandler）是正确的重构方向。与 kimi02 的协调点标注清晰。

## 通过项

- MQTT 适配器路由机制（`supports(topic)` + 遍历匹配）简洁有效
- 仟方/厚达 Topic/Payload 差异处理方案清晰
- 设备影子 DB + 本地缓存方案合理
- 心跳超时 180 秒（3倍心跳间隔）与 kimi01 测试方案已对齐
- MqttMessageHandler 抽取方案正确

## 需修正（2项）

### 1. 影子缓存用 Spring Cache + ConcurrentHashMap，不引入 Caffeine

kimi04 的系统配置也用 Spring Cache + ConcurrentHashMap。保持一致，不引入 Caffeine 新依赖。Spring Boot 内置的 `SimpleCacheManager` + `ConcurrentMapCache` 足够用。

```java
@Cacheable(value = "deviceShadow", key = "#deviceId")
public DeviceShadow getShadow(Long deviceId) { ... }

@CacheEvict(value = "deviceShadow", key = "#deviceId")
public void updateReported(Long deviceId, Map<String, Object> reported) { ... }
```

### 2. DeviceAdapter 接口与 kimi02 对齐

我在 kimi02 审核中给出了统一方案（用 `default` 方法区分 TCP/MQTT）。请在详设中采用该方案，不要定义独立的 MQTT 接口。

## 回答协调问题

1. **parseStatus 签名**：见 kimi02 审核意见，用 `default` 方法区分。
2. **AdapterRegistry**：用 kimi02 设计的独立注册中心类（`ConcurrentHashMap<manufacturer, Map<protocol, adapter>>`），同时 MqttMessageHandler 中用 `List<DeviceAdapter>` 遍历 `supports(topic)` 做路由。两者不冲突——Registry 按厂商精确查找，List 按 Topic 模糊匹配。
3. **仟方/厚达协议文档**：当前没有真实协议文档，按你的合理假设设计即可。适配器内部预留字段映射配置（`Map<String, String> fieldMapping`），后续拿到真实文档时只需改映射不改代码。
