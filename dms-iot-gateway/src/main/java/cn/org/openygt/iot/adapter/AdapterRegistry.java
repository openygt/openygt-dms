package cn.org.openygt.iot.adapter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
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

    public AdapterRegistry(@Autowired(required = false) List<DeviceAdapter> adapterList) {
        this.adapterList = adapterList != null ? adapterList : Collections.<DeviceAdapter>emptyList();
    }

    @PostConstruct
    public void init() {
        for (DeviceAdapter adapter : adapterList) {
            adapters.put(adapter.getProtocolType(), adapter);
        }
    }

    public DeviceAdapter getAdapter(String protocolType) {
        return adapters.get(protocolType);
    }

    public Map<String, DeviceAdapter> getAllAdapters() {
        return new ConcurrentHashMap<>(adapters);
    }

    public StartupReport startAll() {
        StartupReport report = new StartupReport();
        for (DeviceAdapter adapter : adapters.values()) {
            try {
                adapter.start();
                report.started.add(adapter.getProtocolType());
            } catch (Exception e) {
                log.error("适配器启动失败: protocolType={}", adapter.getProtocolType(), e);
                report.failed.put(adapter.getProtocolType(), e.getMessage());
            }
        }
        return report;
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

    public boolean restart(String protocolType) {
        DeviceAdapter adapter = adapters.get(protocolType);
        if (adapter == null) {
            return false;
        }

        try {
            adapter.stop();
        } catch (Exception e) {
            log.warn("适配器停止失败，继续尝试重启: protocolType={}", protocolType, e);
        }

        try {
            adapter.start();
            return true;
        } catch (Exception e) {
            log.error("适配器重启失败: protocolType={}", protocolType, e);
            return false;
        }
    }

    public static class StartupReport {
        private final List<String> started = new ArrayList<>();
        private final Map<String, String> failed = new LinkedHashMap<>();

        public List<String> getStarted() {
            return started;
        }

        public Map<String, String> getFailed() {
            return failed;
        }

        public boolean hasFailures() {
            return !failed.isEmpty();
        }
    }
}
