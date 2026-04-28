package cn.org.openygt.equipment.iot;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class RedisDeviceConnManager implements DeviceConnManager {

    private static final String KEY_PREFIX = "openygt:device:conn:";
    private static final long TTL_HOURS = 24;

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private HashOperations<String, String, String> hashOps;

    public RedisDeviceConnManager(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @PostConstruct
    public void init() {
        this.hashOps = redisTemplate.opsForHash();
    }

    private String key(String deviceCode) {
        return KEY_PREFIX + deviceCode;
    }

    @Override
    public void register(DeviceConnInfo info) {
        try {
            String json = objectMapper.writeValueAsString(info);
            hashOps.put(key(info.getDeviceCode()), "info", json);
            redisTemplate.expire(key(info.getDeviceCode()), TTL_HOURS, TimeUnit.HOURS);
            log.info("Device registered: code={}, protocol={}, status={}",
                    info.getDeviceCode(), info.getProtocol(), info.getStatus());
        } catch (JsonProcessingException e) {
            log.error("Failed to register device: {}", info.getDeviceCode(), e);
        }
    }

    @Override
    public void heartbeat(String deviceCode) {
        DeviceConnInfo info = get(deviceCode);
        if (info != null) {
            info.setLastHeartbeat(LocalDateTime.now());
            info.setStatus("online");
            register(info);
        } else {
            log.warn("Heartbeat from unregistered device: {}", deviceCode);
        }
    }

    @Override
    public void unregister(String deviceCode) {
        DeviceConnInfo info = get(deviceCode);
        if (info != null) {
            info.setStatus("offline");
            register(info);
        }
        log.info("Device unregistered: {}", deviceCode);
    }

    @Override
    public DeviceConnInfo get(String deviceCode) {
        String json = hashOps.get(key(deviceCode), "info");
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, DeviceConnInfo.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse device info: {}", deviceCode, e);
            return null;
        }
    }

    @Override
    public List<DeviceConnInfo> listOnline() {
        List<DeviceConnInfo> result = new ArrayList<>();
        // 扫描所有设备连接 key
        String pattern = KEY_PREFIX + "*";
        redisTemplate.keys(pattern).forEach(key -> {
            String deviceCode = key.substring(KEY_PREFIX.length());
            DeviceConnInfo info = get(deviceCode);
            if (info != null && "online".equals(info.getStatus())) {
                result.add(info);
            }
        });
        return result;
    }

    @Override
    public void updateStatus(String deviceCode, String status) {
        DeviceConnInfo info = get(deviceCode);
        if (info != null) {
            info.setStatus(status);
            register(info);
        }
    }

    @Override
    public void updateLastHeartbeat(String deviceCode) {
        heartbeat(deviceCode);
    }
}
