package cn.org.openygt.equipment.websocket;

import cn.org.openygt.equipment.entity.EqDevice;
import cn.org.openygt.equipment.mapper.EqDeviceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 设备 WebSocket 控制器。
 *
 * <p>Phase 3 解耦说明：已移除对 DeviceConnManager（iot 包）的直接依赖，
 * 设备状态改为从数据库 eq_device 表查询。</p>
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class DeviceWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final EqDeviceMapper eqDeviceMapper;

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
        List<EqDevice> onlineDevices = listActiveDevices(String.valueOf(tenantId));
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
        EqDevice device = eqDeviceMapper.findByDeviceCode(deviceCode);
        if (device != null) {
            pushDeviceStatus(deviceCode, device.getStatus(), device);
        }
    }

    @SubscribeMapping("/tenant/devices")
    public List<EqDevice> subscribeTenantDevices() {
        return listActiveDevices(null);
    }

    // ===== Phase 1 新增 =====

    /**
     * 推送租户下所有设备状态快照（60秒低频聚合）
     */
    public void pushTenantDevicesSnapshot(String tenantId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("tenantId", tenantId);
        payload.put("devices", listActiveDevices(tenantId));
        payload.put("timestamp", System.currentTimeMillis());
        messagingTemplate.convertAndSend("/topic/tenant/" + tenantId + "/devices/snapshot", payload);
    }

    /**
     * 推送告警广播
     */
    public void pushAlarm(String tenantId, String deviceCode, String alarmType, String message) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("tenantId", tenantId);
        payload.put("deviceCode", deviceCode);
        payload.put("alarmType", alarmType);
        payload.put("message", message);
        payload.put("timestamp", System.currentTimeMillis());
        messagingTemplate.convertAndSend("/topic/tenant/" + tenantId + "/alarms", payload);
    }

    // ===== Phase 3 解耦新增 =====

    /**
     * 从数据库查询在线设备列表。
     *
     * <p>替代原 DeviceConnManager.listOnline()，避免直接依赖 IoT 连接管理层。</p>
     */
    private List<EqDevice> listActiveDevices(String tenantId) {
        try {
            List<EqDevice> activeDevices = eqDeviceMapper.findAllActive();
            if (tenantId == null || tenantId.trim().isEmpty()) {
                return activeDevices;
            }
            return activeDevices.stream()
                    .filter(device -> tenantId.equals(device.getTenantId()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("查询在线设备列表失败", e);
            return new ArrayList<>();
        }
    }
}
