package cn.org.openygt.equipment.controller;

import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.equipment.EquipmentModule;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * WebSocket 能力探测接口。
 *
 * <p>前端在建立 WebSocket 连接前先调用此接口，探测服务端支持的通配符订阅、
 * 告警推送、批量快照等能力，实现优雅降级。</p>
 */
@RestController
@RequestMapping(EquipmentModule.API_PREFIX + "/ws")
public class WebSocketCapabilityController {

    @GetMapping("/capability")
    public ApiResponse<Map<String, Object>> getCapability() {
        Map<String, Object> caps = new HashMap<>();
        // 通配符订阅支持（Spring SimpleBroker 原生支持 /topic/device/+/status）
        caps.put("wildcardSubscription", true);
        // 租户级设备快照推送
        caps.put("tenantSnapshot", true);
        // 告警广播推送
        caps.put("alarmPush", true);
        // 支持的心跳间隔（毫秒）
        caps.put("heartbeatIntervalMs", 10000);
        // 推荐重连间隔（毫秒）
        caps.put("reconnectIntervalMs", 5000);
        // 协议版本
        caps.put("protocolVersion", "1.0");
        return ApiResponse.success(caps);
    }
}
