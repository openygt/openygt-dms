package cn.org.openygt.iot.gateway.session;

import cn.org.openygt.iot.config.GatewayProperties;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RestController
@RequestMapping("/api/v1/iot/device-sessions")
public class GatewayDeviceSessionController {

    private final GatewayProperties properties;
    private final Map<String, DeviceSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, OnlineDevice> onlineDevices = new ConcurrentHashMap<>();

    public GatewayDeviceSessionController(GatewayProperties properties) {
        this.properties = properties;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody DeviceSessionRequest request) {
        if (request == null || isBlank(request.getDeviceCode()) || isBlank(request.getMacAddress())) {
            return ResponseEntity.badRequest().body(error("deviceCode 和 macAddress 不能为空"));
        }
        DeviceSession session = new DeviceSession();
        session.setDeviceCode(request.getDeviceCode());
        session.setMacAddress(normalize(request.getMacAddress()));
        session.setUserId(request.getUserId());
        session.setUserCode(request.getUserCode());
        session.setUserName(request.getUserName());
        session.setRegisteredAt(LocalDateTime.now());
        sessions.put(session.getMacAddress(), session);
        log.info("设备会话已登记: mac={}, deviceCode={}, userCode={}",
                session.getMacAddress(), session.getDeviceCode(), session.getUserCode());
        return ResponseEntity.ok(success(session));
    }

    @PostMapping("/unregister")
    public ResponseEntity<Map<String, Object>> unregister(@RequestBody DeviceSessionRequest request) {
        if (request == null || isBlank(request.getMacAddress())) {
            return ResponseEntity.badRequest().body(error("macAddress 不能为空"));
        }
        DeviceSession removed = sessions.remove(normalize(request.getMacAddress()));
        log.info("设备会话已注销: mac={}, removed={}", request.getMacAddress(), removed != null);
        return ResponseEntity.ok(success(removed));
    }

    @GetMapping("/current")
    public ResponseEntity<Map<String, Object>> current() {
        return ResponseEntity.ok(success(new ArrayList<>(sessions.values())));
    }

    @GetMapping("/online-devices")
    public ResponseEntity<Map<String, Object>> onlineDevices() {
        return ResponseEntity.ok(success(new ArrayList<>(onlineDevices.values())));
    }

    @GetMapping("/online-managed")
    public ResponseEntity<Map<String, Object>> onlineManaged() {
        List<ManagedOnlineDevice> result = new ArrayList<>();
        for (OnlineDevice onlineDevice : onlineDevices.values()) {
            ManagedOnlineDevice item = new ManagedOnlineDevice();
            item.setDeviceCode(onlineDevice.getDeviceCode());
            item.setProtocolType(onlineDevice.getProtocolType());
            item.setSourceIp(onlineDevice.getSourceIp());
            item.setLastSeenAt(onlineDevice.getLastSeenAt());

            DeviceSession matchedSession = findSessionByDeviceCode(onlineDevice.getDeviceCode());
            if (matchedSession != null) {
                item.setMacAddress(matchedSession.getMacAddress());
                item.setUserId(matchedSession.getUserId());
                item.setUserCode(matchedSession.getUserCode());
                item.setUserName(matchedSession.getUserName());
                item.setMatchedAt(matchedSession.getRegisteredAt());
            }
            result.add(item);
        }
        return ResponseEntity.ok(success(result));
    }

    public void touchOnlineDevice(String deviceCode, String protocolType, String sourceIp) {
        if (isBlank(deviceCode)) {
            return;
        }
        OnlineDevice device = onlineDevices.computeIfAbsent(deviceCode, key -> new OnlineDevice());
        device.setDeviceCode(deviceCode);
        device.setProtocolType(protocolType);
        device.setSourceIp(sourceIp);
        device.setLastSeenAt(LocalDateTime.now());
    }

    public int getOnlineDeviceCount() {
        return onlineDevices.size();
    }

    public int getMatchedSessionCount() {
        return sessions.size();
    }

    @Scheduled(fixedDelay = 30000L)
    public void evictOfflineDevices() {
        long timeoutSeconds = Math.max(30L, properties.getDeviceSession().getOfflineTimeoutSeconds());
        LocalDateTime cutoff = LocalDateTime.now().minusSeconds(timeoutSeconds);
        for (Map.Entry<String, OnlineDevice> entry : onlineDevices.entrySet()) {
            OnlineDevice device = entry.getValue();
            if (device.getLastSeenAt() != null && device.getLastSeenAt().isBefore(cutoff)) {
                onlineDevices.remove(entry.getKey());
                log.info("设备离线超时移除: deviceCode={}, lastSeenAt={}",
                        device.getDeviceCode(), device.getLastSeenAt());
            }
        }
    }

    @GetMapping("/current/map")
    public ResponseEntity<Map<String, Object>> currentMap() {
        Map<String, Object> result = new ConcurrentHashMap<>();
        for (Map.Entry<String, DeviceSession> entry : sessions.entrySet()) {
            result.put(entry.getKey(), entry.getValue());
        }
        return ResponseEntity.ok(success(result));
    }

    private Map<String, Object> success(Object data) {
        Map<String, Object> result = new ConcurrentHashMap<>();
        result.put("success", true);
        result.put("data", data);
        return result;
    }

    private Map<String, Object> error(String message) {
        Map<String, Object> result = new ConcurrentHashMap<>();
        result.put("success", false);
        result.put("error", message);
        return result;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }

    private DeviceSession findSessionByDeviceCode(String deviceCode) {
        if (isBlank(deviceCode)) {
            return null;
        }
        for (DeviceSession session : sessions.values()) {
            if (deviceCode.equals(session.getDeviceCode())) {
                return session;
            }
        }
        return null;
    }

    @Data
    public static class DeviceSessionRequest {
        private String deviceCode;
        private String macAddress;
        private Long userId;
        private String userCode;
        private String userName;
    }

    @Data
    public static class DeviceSession {
        private String deviceCode;
        private String macAddress;
        private Long userId;
        private String userCode;
        private String userName;
        private LocalDateTime registeredAt;
    }

    @Data
    public static class OnlineDevice {
        private String deviceCode;
        private String protocolType;
        private String sourceIp;
        private LocalDateTime lastSeenAt;
    }

    @Data
    public static class ManagedOnlineDevice {
        private String deviceCode;
        private String protocolType;
        private String sourceIp;
        private LocalDateTime lastSeenAt;
        private String macAddress;
        private Long userId;
        private String userCode;
        private String userName;
        private LocalDateTime matchedAt;
    }
}
