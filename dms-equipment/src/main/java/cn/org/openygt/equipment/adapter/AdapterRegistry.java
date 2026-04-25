package cn.org.openygt.equipment.adapter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 设备适配器注册中心 —— 线程安全，支持运行时动态注册。
 */
@Slf4j
@Component
public class AdapterRegistry {

    // Key: protocolType + ":" + vendor, e.g., "TCP_BINARY:donghuayuan"
    private final Map<String, DeviceAdapter> adapters = new ConcurrentHashMap<>();

    public void register(String protocolType, String vendor, DeviceAdapter adapter) {
        String key = buildKey(protocolType, vendor);
        adapters.put(key, adapter);
        log.info("适配器已注册: protocol={}, vendor={}", protocolType, vendor);
    }

    public Optional<DeviceAdapter> getAdapter(String protocolType, String vendor) {
        return Optional.ofNullable(adapters.get(buildKey(protocolType, vendor)));
    }

    public Collection<DeviceAdapter> getAllAdapters() {
        return Collections.unmodifiableCollection(adapters.values());
    }

    private String buildKey(String protocolType, String vendor) {
        return protocolType.toUpperCase() + ":" + vendor.toLowerCase();
    }
}
