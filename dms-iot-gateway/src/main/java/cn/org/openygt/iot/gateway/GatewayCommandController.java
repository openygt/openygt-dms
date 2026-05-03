package cn.org.openygt.iot.gateway;

import cn.org.openygt.iot.adapter.AdapterRegistry;
import cn.org.openygt.iot.adapter.DeviceAdapter;
import cn.org.openygt.iot.adapter.DeviceCommandDTO;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 网关指令下发 REST 接口
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/iot")
public class GatewayCommandController {

    private final AdapterRegistry registry;

    public GatewayCommandController(AdapterRegistry registry) {
        this.registry = registry;
    }

    @PostMapping("/command")
    public ResponseEntity<Map<String, Object>> sendCommand(@RequestBody CommandRequest request) {
        Map<String, Object> result = new HashMap<>();

        String protocolType = request.getProtocolType();
        String deviceCode = request.getDeviceCode();

        if (protocolType == null || protocolType.isEmpty()) {
            result.put("success", false);
            result.put("error", "protocolType 不能为空");
            return ResponseEntity.badRequest().body(result);
        }

        DeviceAdapter adapter = registry.getAdapter(protocolType);
        if (adapter == null) {
            result.put("success", false);
            result.put("error", "未找到协议适配器: " + protocolType);
            return ResponseEntity.badRequest().body(result);
        }

        DeviceCommandDTO command = new DeviceCommandDTO();
        command.setCommandType(request.getCommandType());
        Map<String, Object> params = request.getParams() != null ? new HashMap<>(request.getParams()) : new HashMap<String, Object>();
        params.put("deviceCode", deviceCode);
        params.put("protocolType", protocolType);
        command.setParams(params);

        try {
            adapter.sendCommand(deviceCode, command);
            result.put("success", true);
            result.put("message", "指令已下发");
            log.info("指令下发成功: device={}, protocol={}, type={}",
                    deviceCode, protocolType, request.getCommandType());
        } catch (Exception e) {
            log.error("指令下发失败: device={}, protocol={}", deviceCode, protocolType, e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.status(500).body(result);
        }

        return ResponseEntity.ok(result);
    }

    @Data
    public static class CommandRequest {
        private String deviceCode;
        private String protocolType;
        private String commandType;
        private Map<String, Object> params;
    }
}
