package cn.org.openygt.equipment.controller;

import cn.org.openygt.equipment.EquipmentModule;
import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.equipment.entity.EqDeviceOperator;
import cn.org.openygt.equipment.service.EqDeviceOperatorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping(EquipmentModule.API_PREFIX + "/devices/{deviceCode}/operator")
@RequiredArgsConstructor
public class EqDeviceOperatorController {

    private final EqDeviceOperatorService operatorService;

    @GetMapping("/current")
    public ApiResponse<EqDeviceOperator> getCurrentOperator(@PathVariable String deviceCode) {
        return ApiResponse.success(operatorService.getCurrentByDeviceCode(deviceCode));
    }

    @GetMapping("/history")
    public ApiResponse<List<EqDeviceOperator>> getOperatorHistory(@PathVariable String deviceCode) {
        return ApiResponse.success(operatorService.getHistoryByDeviceCode(deviceCode));
    }

    @PostMapping("/shift-handover")
    public ApiResponse<EqDeviceOperator> shiftHandover(@PathVariable String deviceCode,
                                                        @RequestBody Map<String, Object> request) {
        Long operatorId = Long.valueOf(request.get("operatorId").toString());
        String operatorName = (String) request.get("operatorName");
        return ApiResponse.success(operatorService.shiftHandover(deviceCode, operatorId, operatorName));
    }

    @PostMapping("/end-shift")
    public ApiResponse<Void> endShift(@PathVariable String deviceCode) {
        operatorService.endShift(deviceCode);
        return ApiResponse.success(null);
    }
}
