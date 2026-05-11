package cn.org.openygt.equipment.controller;

import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.equipment.EquipmentModule;
import cn.org.openygt.equipment.dto.GatewayDeviceReportRequest;
import cn.org.openygt.equipment.service.GatewayReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping(EquipmentModule.API_PREFIX + "/gateway")
@RequiredArgsConstructor
public class GatewayReportController {

    private final GatewayReportService gatewayReportService;

    @Value("${device.gateway.api-key:}")
    private String deviceApiKey;

    @PostMapping("/report")
    public ApiResponse<Void> report(
            @RequestBody GatewayDeviceReportRequest request,
            @RequestHeader(value = "X-Device-Api-Key", required = false) String requestApiKey) {
        // BUG-33: 若配置了设备网关密钥，则必须校验请求头
        if (deviceApiKey != null && !deviceApiKey.isEmpty()) {
            if (!deviceApiKey.equals(requestApiKey)) {
                log.warn("设备上报 API Key 校验失败: deviceCode={}, remoteKey={}", request.getDeviceCode(), requestApiKey);
                return ApiResponse.error(401, "设备认证失败");
            }
        }
        gatewayReportService.handleReport(request);
        return ApiResponse.success();
    }
}
