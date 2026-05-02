package cn.org.openygt.iot.gateway;

import cn.org.openygt.iot.adapter.AdapterRegistry;
import cn.org.openygt.iot.adapter.AdapterStatus;
import cn.org.openygt.iot.adapter.DeviceAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 适配器生命周期管理器
 *
 * <p>Spring 启动完成后自动启动所有适配器；定期健康检查。</p>
 */
@Slf4j
@Component
public class AdapterLifecycleManager {

    private final AdapterRegistry registry;

    public AdapterLifecycleManager(AdapterRegistry registry) {
        this.registry = registry;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("启动所有设备适配器...");
        registry.startAll();
        log.info("适配器启动完成");
    }

    /** 每 30 秒检查一次适配器健康状态 */
    @Scheduled(fixedRate = 30000)
    public void healthCheck() {
        Map<String, DeviceAdapter> adapters = registry.getAllAdapters();
        for (Map.Entry<String, DeviceAdapter> entry : adapters.entrySet()) {
            AdapterStatus status = entry.getValue().getStatus();
            if (status != AdapterStatus.RUNNING) {
                log.warn("适配器 {} 状态异常: {}", entry.getKey(), status);
            }
        }
    }
}
