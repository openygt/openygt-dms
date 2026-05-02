package cn.org.openygt.iot.adapter;

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
            adapter.start();
        }
    }

    public void stopAll() {
        for (DeviceAdapter adapter : adapters.values()) {
            adapter.stop();
        }
    }
}
