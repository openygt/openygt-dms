package cn.org.openygt.iot.gateway;

import cn.org.openygt.iot.adapter.AdapterRegistry;
import cn.org.openygt.iot.adapter.AdapterStatus;
import cn.org.openygt.iot.adapter.DeviceAdapter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 网关健康检查 REST 接口
 */
@RestController
@RequestMapping("/api/v1/iot")
public class GatewayHealthController {

    private final AdapterRegistry registry;

    public GatewayHealthController(AdapterRegistry registry) {
        this.registry = registry;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "UP");
        result.put("adapters", registry.getAllAdapters().size());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/adapters")
    public ResponseEntity<Map<String, String>> adapters() {
        Map<String, String> statusMap = new HashMap<>();
        for (Map.Entry<String, DeviceAdapter> entry : registry.getAllAdapters().entrySet()) {
            statusMap.put(entry.getKey(), entry.getValue().getStatus().name());
        }
        return ResponseEntity.ok(statusMap);
    }
}
