package cn.org.openygt.equipment.websocket;

import cn.org.openygt.equipment.iot.DeviceConnInfo;
import cn.org.openygt.equipment.iot.DeviceConnManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class DeviceWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final DeviceConnManager deviceConnManager;

    /**
     * 推送设备状态变更到订阅频道。
     */
    public void pushDeviceStatus(String deviceCode, String status, Object detail) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("deviceCode", deviceCode);
        payload.put("status", status);
        payload.put("detail", detail);
        payload.put("timestamp", System.currentTimeMillis());

        messagingTemplate.convertAndSend("/topic/device/" + deviceCode + "/status", payload);
        log.debug("Pushed device status: deviceCode={}, status={}", deviceCode, status);
    }

    /**
     * 推送任务进度到订阅频道。
     */
    public void pushTaskProgress(Long taskId, String step, String progress) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("taskId", taskId);
        payload.put("step", step);
        payload.put("progress", progress);
        payload.put("timestamp", System.currentTimeMillis());

        messagingTemplate.convertAndSend("/topic/task/" + taskId + "/progress", payload);
        log.debug("Pushed task progress: taskId={}, step={}", taskId, step);
    }

    /**
     * 推送租户全部设备状态快照。
     */
    public void pushTenantDevicesSnapshot(Long tenantId) {
        List<DeviceConnInfo> onlineDevices = deviceConnManager.listOnline();
        Map<String, Object> payload = new HashMap<>();
        payload.put("tenantId", tenantId);
        payload.put("onlineCount", onlineDevices.size());
        payload.put("devices", onlineDevices);
        payload.put("timestamp", System.currentTimeMillis());

        messagingTemplate.convertAndSend("/topic/tenant/" + tenantId + "/devices", payload);
        log.debug("Pushed tenant devices snapshot: tenantId={}, onlineCount={}", tenantId, onlineDevices.size());
    }

    @MessageMapping("/device/status/query")
    public void handleStatusQuery(Map<String, Object> request) {
        String deviceCode = (String) request.get("deviceCode");
        DeviceConnInfo info = deviceConnManager.get(deviceCode);
        if (info != null) {
            pushDeviceStatus(deviceCode, info.getStatus(), info);
        }
    }

    @SubscribeMapping("/tenant/devices")
    public List<DeviceConnInfo> subscribeTenantDevices() {
        return deviceConnManager.listOnline();
    }
}
