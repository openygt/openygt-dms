package cn.org.openygt.equipment.controller;

import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.equipment.EquipmentModule;
import cn.org.openygt.equipment.service.GatewayDeviceSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController("eqGatewayDeviceSessionController")
@RequestMapping(EquipmentModule.API_PREFIX + "/gateway")
@RequiredArgsConstructor
public class GatewayDeviceSessionController {

    private final GatewayDeviceSessionService gatewayDeviceSessionService;

    @GetMapping("/online-managed")
    public ApiResponse<List<Map<String, Object>>> listOnlineManagedDevices() {
        return ApiResponse.success(gatewayDeviceSessionService.listOnlineManagedDevices());
    }
}
