package cn.org.openygt.iot.gateway;

import cn.org.openygt.iot.adapter.AdapterRegistry;
import cn.org.openygt.iot.adapter.AdapterStatus;
import cn.org.openygt.iot.adapter.DeviceAdapter;
import cn.org.openygt.iot.config.GatewayProperties;
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
    private final GatewayProperties properties;

    public AdapterLifecycleManager(AdapterRegistry registry, GatewayProperties properties) {
        this.registry = registry;
        this.properties = properties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (!properties.getLifecycle().isAutoStartAdapters()) {
            log.info("适配器自动启动已禁用，跳过启动流程");
            return;
        }
        log.info("启动所有设备适配器...");
        AdapterRegistry.StartupReport report = registry.startAll();
        if (report.hasFailures()) {
            log.warn("适配器启动完成，但存在失败项: started={}, failed={}",
                    report.getStarted().size(), report.getFailed());
        } else {
            log.info("适配器启动完成: started={}", report.getStarted().size());
        }
    }

    /** 每 30 秒检查一次适配器健康状态 */
    @Scheduled(fixedRate = 30000)
    public void healthCheck() {
        Map<String, DeviceAdapter> adapters = registry.getAllAdapters();
        for (Map.Entry<String, DeviceAdapter> entry : adapters.entrySet()) {
            AdapterStatus status = entry.getValue().getStatus();
            if (status == AdapterStatus.ERROR) {
                log.warn("适配器 {} 状态异常，尝试自动重启: {}", entry.getKey(), status);
                boolean restarted = registry.restart(entry.getKey());
                if (restarted) {
                    log.info("适配器 {} 自动重启成功", entry.getKey());
                } else {
                    log.error("适配器 {} 自动重启失败", entry.getKey());
                }
            } else if (status != AdapterStatus.RUNNING) {
                log.warn("适配器 {} 状态异常: {}", entry.getKey(), status);
            }
        }
    }
}
