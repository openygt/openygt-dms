package cn.org.openygt.equipment.controller;

import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.equipment.EquipmentModule;
import cn.org.openygt.equipment.dto.GatewayDeviceReportRequest;
import cn.org.openygt.equipment.service.GatewayReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping(EquipmentModule.API_PREFIX + "/gateway")
@RequiredArgsConstructor
public class GatewayReportController {

    private final GatewayReportService gatewayReportService;

    @PostMapping("/report")
    public ApiResponse<Void> report(@RequestBody GatewayDeviceReportRequest request) {
        gatewayReportService.handleReport(request);
        return ApiResponse.success();
    }
}
