package cn.org.openygt.equipment.controller;

import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.equipment.EquipmentModule;
import cn.org.openygt.equipment.entity.DeviceUtilization;
import cn.org.openygt.equipment.service.DeviceUtilizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(EquipmentModule.API_PREFIX + "/device-utilization")
@RequiredArgsConstructor
public class DeviceUtilizationController {

    private final DeviceUtilizationService deviceUtilizationService;

    @GetMapping
    public ApiResponse<List<DeviceUtilization>> queryUtilization(
            @RequestParam(required = false) String deviceCode,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.now().minusDays(7);
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();
        return ApiResponse.success(deviceUtilizationService.queryUtilization(deviceCode, start, end));
    }

    @GetMapping("/trend")
    public ApiResponse<List<Map<String, Object>>> getTrend(
            @RequestParam String deviceCode,
            @RequestParam(defaultValue = "7") int days) {
        return ApiResponse.success(deviceUtilizationService.getTrend(deviceCode, days));
    }

    @GetMapping("/summary")
    public ApiResponse<List<Map<String, Object>>> getSummary(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.now().minusDays(7);
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();
        return ApiResponse.success(deviceUtilizationService.getSummary(start, end));
    }
}
