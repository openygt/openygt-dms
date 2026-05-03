package cn.org.openygt.iot.adapter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 设备适配器注册中心
 *
 * <p>Spring 容器启动时自动扫描并注册所有 {@link DeviceAdapter} 实现类。</p>
 */
@Slf4j
@Component
public class AdapterRegistry {

    private final Map<String, DeviceAdapter> adapters = new ConcurrentHashMap<>();
    private final List<DeviceAdapter> adapterList;

    public AdapterRegistry(List<DeviceAdapter> adapterList) {
        this.adapterList = adapterList;
    }

    @PostConstruct
    public void init() {
        if (adapterList != null) {
            for (DeviceAdapter adapter : adapterList) {
                adapters.put(adapter.getProtocolType(), adapter);
            }
        }
    }

    public DeviceAdapter getAdapter(String protocolType) {
        return adapters.get(protocolType);
    }

    public Map<String, DeviceAdapter> getAllAdapters() {
        return new ConcurrentHashMap<>(adapters);
    }

    public void startAll() {
        for (DeviceAdapter adapter : adapters.values()) {
            try {
                adapter.start();
            } catch (Exception e) {
                log.error("适配器启动失败: protocolType={}", adapter.getProtocolType(), e);
            }
        }
    }

    public void stopAll() {
        for (DeviceAdapter adapter : adapters.values()) {
            try {
                adapter.stop();
            } catch (Exception e) {
                log.error("适配器停止失败: protocolType={}", adapter.getProtocolType(), e);
            }
        }
    }
}
