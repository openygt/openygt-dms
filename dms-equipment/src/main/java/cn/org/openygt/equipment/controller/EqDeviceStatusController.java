package cn.org.openygt.equipment.controller;

import cn.org.openygt.equipment.EquipmentModule;
import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.equipment.entity.EqDeviceStatus;
import cn.org.openygt.equipment.service.EqDeviceStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping(EquipmentModule.API_PREFIX + "/devices/{deviceCode}/status-snapshot")
@RequiredArgsConstructor
public class EqDeviceStatusController {

    private final EqDeviceStatusService statusService;

    @GetMapping("/latest")
    public ApiResponse<EqDeviceStatus> getLatestStatus(@PathVariable String deviceCode) {
        return ApiResponse.success(statusService.getLatestByDeviceCode(deviceCode));
    }

    @GetMapping("/history")
    public ApiResponse<List<EqDeviceStatus>> getStatusHistory(
            @PathVariable String deviceCode,
            @RequestParam String startTime,
            @RequestParam String endTime) {
        return ApiResponse.success(statusService.getHistory(deviceCode, startTime, endTime));
    }

    @PostMapping
    public ApiResponse<Void> saveSnapshot(@PathVariable String deviceCode,
                                           @RequestBody EqDeviceStatus snapshot) {
        snapshot.setDeviceCode(deviceCode);
        statusService.saveSnapshot(snapshot);
        return ApiResponse.success(null);
    }
}
