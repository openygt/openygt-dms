# kimi03 详细设计 - MQTT 适配器 + 设备影子 + 心跳

| 文档信息 | |
|---------|---|
| 版本 | V1.0 |
| 日期 | 2026-04-25 |
| 作者 | kimi03 |
| 前置文档 | `docs/tasks/kimi03-概设.md`（已审核通过） |
| 对应需求 | EQ-010, EQ-011, EQ-013, EQ-014 |

---

## 1. 详细类设计

### 1.1 DeviceAdapter 接口（与 kimi02 协调确认版）

```java
package com.decoction.common.adapter;

import java.math.BigDecimal;

/**
 * 设备协议适配器接口。
 * 由 dms-equipment 模块实现，Spring 自动扫描注册。
 */
public interface DeviceAdapter {
    
    /** 厂商名称，如 "仟方", "厚达", "东华原", "三延" */
    String getManufacturer();
    
    /** 协议类型：MQTT / TCP */
    String getProtocol();
    
    // ========== TCP 侧 ==========
    void connect(String deviceCode, DeviceConnectionInfo info);
    void disconnect(String deviceCode);
    void sendCommand(String deviceCode, DeviceCommand command);
    boolean isConnected(String deviceCode);
    DeviceStatusReport parseStatus(byte[] rawData);
    
    // ========== MQTT 侧（default 实现，TCP 适配器无需关心） ==========
    default boolean supports(String topic) { return false; }
    default DeviceStatusReport parseMqttMessage(String topic, String payload) {
        throw new UnsupportedOperationException("TCP adapter does not support MQTT parsing");
    }
    
    // ========== 公共 ==========
    default String encodeCommand(DeviceCommand command) {
        return "{}";
    }
}
```

### 1.2 DeviceStatusReport（统一状态传输对象）

```java
package com.decoction.common.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Data
public class DeviceStatusReport {
    private String deviceCode;
    private Long deviceId;
    private BigDecimal temperature;
    private String status;        // IDLE / BUSY / FAULT / OFFLINE
    private String faultCode;
    private String faultMessage;
    private Map<String, Object> extensions = new HashMap<>();  // 厂商扩展字段
    
    /** 转换为影子 reported 格式 */
    public Map<String, Object> toReportedMap() {
        Map<String, Object> map = new HashMap<>();
        if (temperature != null) map.put("temperature", temperature);
        if (status != null) map.put("status", status);
        if (faultCode != null) map.put("faultCode", faultCode);
        map.putAll(extensions);
        return map;
    }
}
```

### 1.3 DeviceCommand（统一指令对象）

```java
package com.decoction.common.dto;

import lombok.Data;
import java.util.Map;

@Data
public class DeviceCommand {
    private String commandType;   // START / STOP / SET_TEMP / RESET
    private Map<String, Object> params;
}
```

### 1.4 AbstractMqttAdapter（MQTT 适配器抽象基类）

```java
package com.decoction.equipment.adapter.mqtt;

import com.decoction.common.adapter.DeviceAdapter;
import com.decoction.common.dto.DeviceStatusReport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

@Slf4j
public abstract class AbstractMqttAdapter implements DeviceAdapter {
    
    protected static final ObjectMapper mapper = new ObjectMapper();
    
    @Override
    public String getProtocol() {
        return "MQTT";
    }
    
    @Override
    public boolean supports(String topic) {
        if (topic == null) return false;
        return topic.startsWith(getTopicPrefix());
    }
    
    /** 子类实现：返回 Topic 前缀，如 "qianfang/" */
    protected abstract String getTopicPrefix();
    
    /** 子类实现：从 Topic 提取 deviceCode */
    protected abstract String extractDeviceCode(String topic);
    
    /** 子类实现：解析温度 */
    protected abstract BigDecimal parseTemperature(JsonNode root);
    
    /** 子类实现：解析状态 */
    protected abstract String parseStatus(JsonNode root);
    
    /** 子类实现：解析故障码 */
    protected abstract String parseFaultCode(JsonNode root);
    
    @Override
    public DeviceStatusReport parseMqttMessage(String topic, String payload) {
        DeviceStatusReport status = new DeviceStatusReport();
        status.setDeviceCode(extractDeviceCode(topic));
        
        try {
            JsonNode root = mapper.readTree(payload);
            status.setTemperature(parseTemperature(root));
            status.setStatus(parseStatus(root));
            status.setFaultCode(parseFaultCode(root));
            
            // 提取扩展字段
            parseExtensions(root, status);
        } catch (Exception e) {
            log.error("MQTT payload 解析失败: topic={}, payload={}", topic, payload, e);
            status.setStatus("FAULT");
            status.setFaultCode("PARSE_ERROR");
            status.setFaultMessage(e.getMessage());
        }
        
        return status;
    }
    
    /** 子类可重写，提取厂商特有字段到 extensions */
    protected void parseExtensions(JsonNode root, DeviceStatusReport status) {
        // 默认空实现
    }
    
    @Override
    public String encodeCommand(DeviceCommand command) {
        try {
            return mapper.writeValueAsString(command.getParams());
        } catch (Exception e) {
            log.error("指令编码失败", e);
            return "{}";
        }
    }
}
```

### 1.5 QianfangAdapter（仟方 MQTT 适配器）

```java
package com.decoction.equipment.adapter.mqtt;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
public class QianfangAdapter extends AbstractMqttAdapter {
    
    private static final String MANUFACTURER = "仟方";
    private static final String TOPIC_PREFIX = "qianfang/";
    
    @Override
    public String getManufacturer() {
        return MANUFACTURER;
    }
    
    @Override
    protected String getTopicPrefix() {
        return TOPIC_PREFIX;
    }
    
    @Override
    protected String extractDeviceCode(String topic) {
        // qianfang/DEV001/telemetry → DEV001
        String[] parts = topic.split("/");
        return parts.length >= 2 ? parts[1] : topic;
    }
    
    @Override
    protected BigDecimal parseTemperature(JsonNode root) {
        JsonNode tempNode = root.path("temp");
        if (tempNode.isMissingNode()) {
            tempNode = root.path("temperature");
        }
        return tempNode.isNumber() ? new BigDecimal(tempNode.asText()) : null;
    }
    
    @Override
    protected String parseStatus(JsonNode root) {
        JsonNode statusNode = root.path("status");
        if (statusNode.isNumber()) {
            // 仟方状态码映射：0=idle, 1=running, 2=fault
            switch (statusNode.asInt()) {
                case 0: return "IDLE";
                case 1: return "BUSY";
                case 2: return "FAULT";
                default: return null;
            }
        }
        if (statusNode.isTextual()) {
            return statusNode.asText().toUpperCase();
        }
        return null;
    }
    
    @Override
    protected String parseFaultCode(JsonNode root) {
        JsonNode faultNode = root.path("fault");
        return faultNode.isTextual() && !faultNode.asText().isEmpty() 
            ? faultNode.asText() : null;
    }
    
    @Override
    protected void parseExtensions(JsonNode root, DeviceStatusReport status) {
        JsonNode modeNode = root.path("mode");
        if (modeNode.isTextual()) {
            status.getExtensions().put("mode", modeNode.asText());
        }
        JsonNode pressureNode = root.path("pressure");
        if (pressureNode.isNumber()) {
            status.getExtensions().put("pressure", pressureNode.asInt());
        }
    }
}
```

### 1.6 HoudaAdapter（厚达 MQTT 适配器）

```java
package com.decoction.equipment.adapter.mqtt;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
public class HoudaAdapter extends AbstractMqttAdapter {
    
    private static final String MANUFACTURER = "厚达";
    private static final String TOPIC_PREFIX = "houda/devices/";
    
    @Override
    public String getManufacturer() {
        return MANUFACTURER;
    }
    
    @Override
    protected String getTopicPrefix() {
        return TOPIC_PREFIX;
    }
    
    @Override
    protected String extractDeviceCode(String topic) {
        // houda/devices/DEV001/properties/report → DEV001
        String[] parts = topic.split("/");
        return parts.length >= 3 ? parts[2] : topic;
    }
    
    @Override
    protected BigDecimal parseTemperature(JsonNode root) {
        JsonNode tempNode = root.path("temperature");
        if (tempNode.isMissingNode()) {
            tempNode = root.path("temp");
        }
        return tempNode.isNumber() ? new BigDecimal(tempNode.asText()) : null;
    }
    
    @Override
    protected String parseStatus(JsonNode root) {
        JsonNode stateNode = root.path("state");
        if (stateNode.isTextual()) {
            String state = stateNode.asText().toLowerCase();
            switch (state) {
                case "idle": case "standby": return "IDLE";
                case "heating": case "running": case "working": return "BUSY";
                case "fault": case "error": return "FAULT";
                default: return state.toUpperCase();
            }
        }
        return null;
    }
    
    @Override
    protected String parseFaultCode(JsonNode root) {
        JsonNode errorNode = root.path("errorCode");
        if (errorNode.isMissingNode()) {
            errorNode = root.path("faultCode");
        }
        return errorNode.isTextual() && !errorNode.asText().isEmpty() 
            ? errorNode.asText() : null;
    }
    
    @Override
    protected void parseExtensions(JsonNode root, DeviceStatusReport status) {
        JsonNode targetTempNode = root.path("targetTemperature");
        if (targetTempNode.isNumber()) {
            status.getExtensions().put("targetTemperature", new BigDecimal(targetTempNode.asText()));
        }
        JsonNode remainingTimeNode = root.path("remainingTime");
        if (remainingTimeNode.isNumber()) {
            status.getExtensions().put("remainingTime", remainingTimeNode.asInt());
        }
    }
}
```

### 1.7 MqttMessageHandler（MQTT 消息处理器）

```java
package com.decoction.equipment.mqtt;

import com.decoction.common.adapter.DeviceAdapter;
import com.decoction.common.dto.DeviceStatusReport;
import com.decoction.common.service.EquipmentService;
import com.decoction.equipment.heartbeat.HeartbeatService;
import com.decoction.equipment.shadow.DeviceShadowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MqttMessageHandler {
    
    private final List<DeviceAdapter> adapters;
    private final EquipmentService equipmentService;
    private final DeviceShadowService shadowService;
    private final HeartbeatService heartbeatService;
    
    /**
     * 处理 MQTT 消息。
     * 1. 路由到对应适配器
     * 2. 解析为统一 DeviceStatusReport
     * 3. 更新设备状态、影子、心跳
     */
    public void handle(String topic, String payload) {
        log.debug("MQTT消息: topic={}, payload={}", topic, payload);
        
        // 1. 路由适配器
        DeviceAdapter adapter = adapters.stream()
            .filter(a -> a.supports(topic))
            .findFirst()
            .orElse(null);
        
        if (adapter == null) {
            log.warn("无适配器处理该 Topic: {}", topic);
            legacyHandle(topic, payload);
            return;
        }
        
        log.info("使用适配器: {}, topic={}", adapter.getManufacturer(), topic);
        
        // 2. 解析状态
        DeviceStatusReport status = adapter.parseMqttMessage(topic, payload);
        if (status.getDeviceCode() == null) {
            log.error("无法从 Topic 提取设备编码: {}", topic);
            return;
        }
        
        Long deviceId = equipmentService.getDeviceId(status.getDeviceCode());
        if (deviceId == null) {
            log.warn("未知设备: {}", status.getDeviceCode());
            return;
        }
        status.setDeviceId(deviceId);
        
        // 3. 更新设备状态
        applyDeviceStatusReport(status);
        
        // 4. 更新影子
        shadowService.updateReported(deviceId, status.toReportedMap());
        
        // 5. 记录心跳
        heartbeatService.recordHeartbeat(deviceId);
    }
    
    private void applyDeviceStatusReport(DeviceStatusReport status) {
        Long deviceId = status.getDeviceId();
        
        if (status.getTemperature() != null) {
            equipmentService.updateTemperature(deviceId, status.getTemperature());
        }
        
        if (status.getStatus() != null) {
            equipmentService.updateDeviceStatusReport(deviceId, status.getStatus());
        }
        
        if (status.getFaultCode() != null) {
            equipmentService.reportFault(deviceId, status.getFaultCode(), 
                status.getFaultMessage() != null ? status.getFaultMessage() : "设备上报故障");
        }
    }
    
    /** 兜底：兼容原有 Topic 格式 device/{code}/{type} */
    private void legacyHandle(String topic, String payload) {
        String[] parts = topic.split("/");
        if (parts.length < 3 || !"device".equals(parts[0])) {
            return;
        }
        String deviceCode = parts[1];
        String messageType = parts[2];
        
        Long deviceId = equipmentService.getDeviceId(deviceCode);
        if (deviceId == null) return;
        
        try {
            switch (messageType) {
                case "status":
                    equipmentService.updateTemperature(deviceId, new BigDecimal(payload.trim()));
                    break;
                case "fault":
                    equipmentService.reportFault(deviceId, payload.trim(), "MQTT上报故障");
                    break;
                case "online":
                    equipmentService.updateDeviceStatusReport(deviceId, "IDLE");
                    break;
                case "offline":
                    equipmentService.updateDeviceStatusReport(deviceId, "OFFLINE");
                    break;
            }
            heartbeatService.recordHeartbeat(deviceId);
        } catch (Exception e) {
            log.error("兜底处理失败: topic={}", topic, e);
        }
    }
}
```

### 1.8 MqttConfig（改造后）

```java
package com.decoction.equipment.config;

import com.decoction.equipment.mqtt.MqttMessageHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class MqttConfig {
    
    @Value("${mqtt.broker-url:tcp://127.0.0.1:1883}")
    private String brokerUrl;
    
    @Value("${mqtt.client-id:dms-backend}")
    private String clientId;
    
    @Value("${mqtt.topic-subscription:device/+/+,qianfang/+/+,houda/devices/+/properties/report}")
    private String topicSubscription;
    
    private final MqttMessageHandler messageHandler;
    private MqttClient mqttClient;
    
    @PostConstruct
    public void init() {
        try {
            mqttClient = new MqttClient(brokerUrl, clientId);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);
            options.setConnectionTimeout(10);
            options.setKeepAliveInterval(20);
            mqttClient.connect(options);
            
            // 支持多 Topic 订阅
            String[] topics = topicSubscription.split(",");
            for (String topic : topics) {
                topic = topic.trim();
                mqttClient.subscribe(topic, (t, msg) -> {
                    messageHandler.handle(t, new String(msg.getPayload()));
                });
                log.info("MQTT已订阅: {}", topic);
            }
        } catch (Exception e) {
            log.error("MQTT连接/订阅失败", e);
        }
    }
    
    @PreDestroy
    public void destroy() {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.disconnect();
                mqttClient.close();
            }
        } catch (Exception e) {
            log.error("MQTT断开失败", e);
        }
    }
}
```

---

## 2. 设备影子详细设计

### 2.1 DeviceShadow（Entity）

```java
package com.decoction.equipment.shadow;

import com.decoction.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("eq_device_shadow")
public class DeviceShadow extends BaseEntity {
    
    private Long deviceId;
    private String reportedState;    // JSON
    private String desiredState;     // JSON
    private Integer version;
    private Date lastReportedAt;
    private Date lastDesiredAt;
}
```

### 2.2 DeviceShadowMapper

```java
package com.decoction.equipment.shadow;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DeviceShadowMapper extends BaseMapper<DeviceShadow> {
    
    @Select("SELECT * FROM eq_device_shadow WHERE device_id = #{deviceId} AND deleted = 0")
    DeviceShadow selectByDeviceId(@Param("deviceId") Long deviceId);
}
```

### 2.3 DeviceShadowServiceImpl

```java
package com.decoction.equipment.shadow;

import com.decoction.common.service.EquipmentService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceShadowServiceImpl implements DeviceShadowService {
    
    private static final ObjectMapper mapper = new ObjectMapper();
    private final DeviceShadowMapper shadowMapper;
    private final EquipmentService equipmentService;
    
    @Override
    @Transactional
    @CacheEvict(value = "deviceShadow", key = "#deviceId")
    public void updateReported(Long deviceId, Map<String, Object> reported) {
        DeviceShadow shadow = getOrCreateShadow(deviceId);
        
        try {
            // 合并 reported（新值覆盖旧值）
            Map<String, Object> existing = parseJson(shadow.getReportedState());
            existing.putAll(reported);
            shadow.setReportedState(toJson(existing));
            shadow.setLastReportedAt(new Date());
            shadow.setVersion(shadow.getVersion() + 1);
            
            shadowMapper.updateById(shadow);
            log.debug("影子 reported 更新: deviceId={}, version={}", deviceId, shadow.getVersion());
        } catch (Exception e) {
            log.error("影子 reported 更新失败: deviceId={}", deviceId, e);
        }
    }
    
    @Override
    @Transactional
    @CacheEvict(value = "deviceShadow", key = "#deviceId")
    public void updateDesired(Long deviceId, Map<String, Object> desired) {
        DeviceShadow shadow = getOrCreateShadow(deviceId);
        
        try {
            // desired 直接覆盖（不合并）
            shadow.setDesiredState(toJson(desired));
            shadow.setLastDesiredAt(new Date());
            shadow.setVersion(shadow.getVersion() + 1);
            
            shadowMapper.updateById(shadow);
            log.info("影子 desired 设置: deviceId={}, desired={}", deviceId, desired);
            
            // 触发指令下发（异步）
            publishDesiredToDevice(deviceId, desired);
        } catch (Exception e) {
            log.error("影子 desired 更新失败: deviceId={}", deviceId, e);
        }
    }
    
    @Override
    @Cacheable(value = "deviceShadow", key = "#deviceId")
    public DeviceShadow getShadow(Long deviceId) {
        return shadowMapper.selectByDeviceId(deviceId);
    }
    
    @Override
    public Object getReportedValue(Long deviceId, String key) {
        DeviceShadow shadow = getShadow(deviceId);
        if (shadow == null || shadow.getReportedState() == null) return null;
        
        Map<String, Object> reported = parseJson(shadow.getReportedState());
        return reported.get(key);
    }
    
    @Override
    @Transactional
    @CacheEvict(value = "deviceShadow", key = "#deviceId")
    public void mergeDesiredToReported(Long deviceId) {
        DeviceShadow shadow = shadowMapper.selectByDeviceId(deviceId);
        if (shadow == null || shadow.getDesiredState() == null) return;
        
        try {
            Map<String, Object> reported = parseJson(shadow.getReportedState());
            Map<String, Object> desired = parseJson(shadow.getDesiredState());
            
            reported.putAll(desired);  // desired 合并到 reported
            shadow.setReportedState(toJson(reported));
            shadow.setDesiredState(null);  // 清空 desired
            shadow.setVersion(shadow.getVersion() + 1);
            
            shadowMapper.updateById(shadow);
            log.info("影子 desired 已合并到 reported: deviceId={}", deviceId);
        } catch (Exception e) {
            log.error("影子合并失败: deviceId={}", deviceId, e);
        }
    }
    
    private DeviceShadow getOrCreateShadow(Long deviceId) {
        DeviceShadow shadow = shadowMapper.selectByDeviceId(deviceId);
        if (shadow == null) {
            shadow = new DeviceShadow();
            shadow.setDeviceId(deviceId);
            shadow.setReportedState("{}");
            shadow.setDesiredState(null);
            shadow.setVersion(0);
            shadowMapper.insert(shadow);
        }
        return shadow;
    }
    
    private void publishDesiredToDevice(Long deviceId, Map<String, Object> desired) {
        // TODO: 通过 MQTT 下发到设备
        // 需要 MqttConfig 提供 publish 能力
        String deviceCode = equipmentService.getDeviceCode(deviceId);
        log.info("待下发指令到设备: deviceCode={}, desired={}", deviceCode, desired);
    }
    
    @SneakyThrows
    private Map<String, Object> parseJson(String json) {
        if (json == null || json.isEmpty()) return new java.util.HashMap<>();
        return mapper.readValue(json, new TypeReference<Map<String, Object>>() {});
    }
    
    @SneakyThrows
    private String toJson(Map<String, Object> map) {
        return mapper.writeValueAsString(map);
    }
}
```

### 2.4 DeviceShadowController

```java
package com.decoction.equipment.controller;

import com.decoction.common.dto.ApiResponse;
import com.decoction.equipment.shadow.DeviceShadow;
import com.decoction.equipment.shadow.DeviceShadowService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/eq/devices")
@RequiredArgsConstructor
public class DeviceShadowController {
    
    private final DeviceShadowService shadowService;
    
    @GetMapping("/{deviceId}/shadow")
    public ApiResponse<DeviceShadow> getShadow(@PathVariable Long deviceId) {
        return ApiResponse.success(shadowService.getShadow(deviceId));
    }
    
    @PutMapping("/{deviceId}/shadow/desired")
    public ApiResponse<Void> setDesired(@PathVariable Long deviceId, 
                                         @RequestBody Map<String, Object> desired) {
        shadowService.updateDesired(deviceId, desired);
        return ApiResponse.success();
    }
}
```

---

## 3. 心跳详细设计

### 3.1 HeartbeatService

```java
package com.decoction.equipment.heartbeat;

import com.decoction.equipment.entity.EqDevice;
import com.decoction.equipment.mapper.EqDeviceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Slf4j
@Service
@RequiredArgsConstructor
public class HeartbeatService {
    
    private final EqDeviceMapper deviceMapper;
    
    @Transactional
    public void recordHeartbeat(Long deviceId) {
        EqDevice device = new EqDevice();
        device.setId(deviceId);
        device.setLastHeartbeatAt(new Date());
        deviceMapper.updateById(device);
        log.debug("心跳记录: deviceId={}", deviceId);
    }
}
```

### 3.2 HeartbeatScheduler

```java
package com.decoction.equipment.heartbeat;

import com.decoction.common.service.EquipmentService;
import com.decoction.equipment.entity.EqDevice;
import com.decoction.equipment.mapper.EqDeviceMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class HeartbeatScheduler {
    
    private final EqDeviceMapper deviceMapper;
    private final EquipmentService equipmentService;
    
    /** 默认心跳超时时间：3分钟 */
    private static final long DEFAULT_TIMEOUT_MS = TimeUnit.MINUTES.toMillis(3);
    
    /**
     * 每分钟检查一次心跳超时。
     * 设备需满足以下条件才检测：
     * 1. 当前状态不是 OFFLINE（避免重复标记）
     * 2. last_heartbeat_at 为空 或 距离现在超过 timeout
     */
    @Scheduled(fixedRate = 60000)
    public void checkHeartbeatTimeout() {
        Date now = new Date();
        long timeoutThreshold = now.getTime() - DEFAULT_TIMEOUT_MS;
        
        LambdaQueryWrapper<EqDevice> wrapper = new LambdaQueryWrapper<>();
        wrapper.ne(EqDevice::getStatus, "OFFLINE")
               .and(w -> w.isNull(EqDevice::getLastHeartbeatAt)
                          .or().lt(EqDevice::getLastHeartbeatAt, new Date(timeoutThreshold)));
        
        List<EqDevice> timeoutDevices = deviceMapper.selectList(wrapper);
        
        for (EqDevice device : timeoutDevices) {
            log.warn("设备心跳超时，标记离线: deviceCode={}, lastHeartbeat={}", 
                device.getDeviceCode(), device.getLastHeartbeatAt());
            equipmentService.updateDeviceStatusReport(device.getId(), "OFFLINE");
        }
        
        if (!timeoutDevices.isEmpty()) {
            log.info("心跳检查完成: 超时设备 {} 台", timeoutDevices.size());
        }
    }
}
```

---

## 4. 数据库迁移脚本（Flyway）

```sql
-- V5__mqtt_adapter_shadow_heartbeat.sql
-- 作者: kimi03
-- 日期: 2026-04-25

-- ========== 设备影子表 ==========
CREATE TABLE IF NOT EXISTS eq_device_shadow (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    tenant_id VARCHAR(32) DEFAULT 'default',
    device_id INTEGER NOT NULL UNIQUE,
    reported_state TEXT DEFAULT '{}',
    desired_state TEXT,
    version INTEGER DEFAULT 0,
    last_reported_at DATETIME,
    last_desired_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_shadow_device_id ON eq_device_shadow(device_id);

-- ========== 设备表扩展：心跳字段 ==========
ALTER TABLE eq_device ADD COLUMN last_heartbeat_at DATETIME;
ALTER TABLE eq_device ADD COLUMN heartbeat_timeout_sec INTEGER DEFAULT 180;

-- ========== 设备表扩展：厂商/协议字段（如不存在） ==========
-- 已有 protocol_type 字段，无需新增
-- 新增 manufacturer 字段用于适配器路由
ALTER TABLE eq_device ADD COLUMN manufacturer VARCHAR(50);
ALTER TABLE eq_device ADD COLUMN adapter_config TEXT;  -- JSON: {"topicPrefix":"qianfang/"}
```

---

## 5. 缓存配置

```java
package com.decoction.equipment.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager("deviceShadow");
        manager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .recordStats());
        return manager;
    }
}
```

> **依赖**：`com.github.ben-manes.caffeine:caffeine:2.9.3`（JDK8 兼容版本）

---

## 6. 单元测试方案

### 6.1 测试类清单

| 测试类 | 被测类 | 说明 |
|--------|--------|------|
| `QianfangAdapterTest` | `QianfangAdapter` | 仟方协议解析 |
| `HoudaAdapterTest` | `HoudaAdapter` | 厚达协议解析 |
| `MqttMessageHandlerTest` | `MqttMessageHandler` | 消息路由与处理 |
| `DeviceShadowServiceTest` | `DeviceShadowServiceImpl` | 影子 CRUD、版本管理 |
| `HeartbeatSchedulerTest` | `HeartbeatScheduler` | 超时检测逻辑 |

### 6.2 关键测试用例

```java
// QianfangAdapterTest
@Test
public void testParseStatus_NumericStatus() {
    String topic = "qianfang/DEV001/telemetry";
    String payload = "{\"temp\": 95, \"status\": 1, \"fault\": \"\"}";
    
    DeviceStatusReport status = adapter.parseMqttMessage(topic, payload);
    
    assertEquals("DEV001", status.getDeviceCode());
    assertEquals(new BigDecimal("95"), status.getTemperature());
    assertEquals("BUSY", status.getStatus());
    assertNull(status.getFaultCode());
}

@Test
public void testParseStatus_Fault() {
    String payload = "{\"temp\": 0, \"status\": 2, \"fault\": \"E101\"}";
    DeviceStatusReport status = adapter.parseMqttMessage("qianfang/DEV001/telemetry", payload);
    
    assertEquals("FAULT", status.getStatus());
    assertEquals("E101", status.getFaultCode());
}

// DeviceShadowServiceTest
@Test
public void testUpdateReported_Merge() {
    // 初始 reported: {"temperature": 90}
    shadowService.updateReported(1L, Map.of("temperature", 95, "status", "BUSY"));
    
    DeviceShadow shadow = shadowService.getShadow(1L);
    Map<String, Object> reported = parseJson(shadow.getReportedState());
    
    assertEquals(95, reported.get("temperature"));
    assertEquals("BUSY", reported.get("status"));
    assertEquals(1, shadow.getVersion());
}

@Test
public void testUpdateDesired_Overwrite() {
    shadowService.updateDesired(1L, Map.of("targetTemp", 100));
    shadowService.updateDesired(1L, Map.of("targetTemp", 120));  // 覆盖
    
    DeviceShadow shadow = shadowService.getShadow(1L);
    Map<String, Object> desired = parseJson(shadow.getDesiredState());
    
    assertEquals(120, desired.get("targetTemp"));
}

// HeartbeatSchedulerTest
@Test
public void testCheckTimeout() {
    // 创建设备，lastHeartbeatAt = 5分钟前
    EqDevice device = createDevice("DEV001", "IDLE", Date.from(Instant.now().minusSeconds(300)));
    
    scheduler.checkHeartbeatTimeout();
    
    // 验证设备状态被改为 OFFLINE
    verify(equipmentService).updateDeviceStatusReport(device.getId(), "OFFLINE");
}
```

### 6.3 Mock 策略

| 依赖 | Mock 方式 |
|------|----------|
| `EquipmentService` | Mockito `@MockBean` |
| `MqttClient` | 不直接测试，测试 `MqttMessageHandler` 时直接调用 `handle()` |
| `DeviceShadowMapper` | `@MockBean`，验证 SQL 调用 |
| `EqDeviceMapper` | `@MockBean` |

---

## 7. 异常处理设计

| 异常场景 | 处理方式 | 日志级别 |
|---------|---------|---------|
| MQTT payload JSON 解析失败 | 标记设备 FAULT，记录 faultCode="PARSE_ERROR" | ERROR |
| 未知设备编码 | 丢弃消息，记录 warn | WARN |
| 影子数据库写入失败 | 抛异常，事务回滚，缓存不更新 | ERROR |
| 心跳调度器执行异常 | catch 并记录，不影响其他设备 | ERROR |
| 适配器不支持该 Topic | 走兜底 legacyHandle | WARN |

---

## 8. 与现有代码的集成点

### 8.1 修改的文件清单

| 文件 | 修改类型 | 说明 |
|------|---------|------|
| `MqttConfig.java` | 改造 | 抽取 MessageHandler，支持多 Topic 订阅 |
| `EqDevice.java` | 新增字段 | `lastHeartbeatAt`, `heartbeatTimeoutSec`, `manufacturer`, `adapterConfig` |
| `EquipmentService.java` | 可能新增方法 | `getDeviceCode(Long)` 如不存在需补充 |

### 8.2 新增的文件清单

| 文件 | 说明 |
|------|------|
| `DeviceAdapter.java` | 接口（dms-common 或 dms-equipment） |
| `DeviceStatusReport.java` | DTO（dms-common） |
| `DeviceCommand.java` | DTO（dms-common） |
| `AbstractMqttAdapter.java` | 基类 |
| `QianfangAdapter.java` | 仟方适配器 |
| `HoudaAdapter.java` | 厚达适配器 |
| `MqttMessageHandler.java` | 消息处理器 |
| `DeviceShadow.java` | Entity |
| `DeviceShadowMapper.java` | Mapper |
| `DeviceShadowService.java` | 接口 |
| `DeviceShadowServiceImpl.java` | 实现 |
| `DeviceShadowController.java` | Controller |
| `HeartbeatService.java` | 心跳记录服务 |
| `HeartbeatScheduler.java` | 定时检测 |
| `CacheConfig.java` | Caffeine 缓存配置 |
| `V5__mqtt_adapter_shadow_heartbeat.sql` | Flyway 迁移脚本 |

---

## 9. 风险与回退方案

| 风险 | 影响 | 回退方案 |
|------|------|---------|
| Caffeine 缓存引入问题 | 影子数据不一致 | 移除缓存注解，纯数据库访问 |
| 仟方/厚达实际协议不符 | 解析失败 | 通过 `adapter_config` 字段动态配置字段映射，无需改代码 |
| MQTT 消息处理性能瓶颈 | 消息堆积 | 引入线程池异步处理 `MqttMessageHandler.handle()` |

---

*本详细设计完成后，可进入开发阶段。开发完成后由 kimi01 执行单元测试和集成测试。*
