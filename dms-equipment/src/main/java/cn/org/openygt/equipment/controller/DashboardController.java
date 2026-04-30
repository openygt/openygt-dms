package cn.org.openygt.equipment.controller;

import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.equipment.EquipmentModule;
import cn.org.openygt.equipment.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController("eqDashboardController")
@RequestMapping(EquipmentModule.API_PREFIX + "/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/metrics")
    public ApiResponse<Map<String, Object>> getMetrics() {
        return ApiResponse.success(dashboardService.getMetrics());
    }

    @GetMapping("/stage-distribution")
    public ApiResponse<List<Map<String, Object>>> getStageDistribution() {
        return ApiResponse.success(dashboardService.getStageDistribution());
    }

    @GetMapping("/worker-efficiency")
    public ApiResponse<List<Map<String, Object>>> getWorkerEfficiency(
            @RequestParam(required = false) String date) {
        return ApiResponse.success(dashboardService.getWorkerEfficiency(date));
    }

    @GetMapping("/hourly-trend")
    public ApiResponse<List<Map<String, Object>>> getHourlyTrend(
            @RequestParam(required = false) String date) {
        return ApiResponse.success(dashboardService.getHourlyTrend(date));
    }

    @GetMapping("/device-utilization")
    public ApiResponse<List<Map<String, Object>>> getDeviceUtilization(
            @RequestParam(required = false) String date) {
        return ApiResponse.success(dashboardService.getDeviceUtilization(date));
    }
}
