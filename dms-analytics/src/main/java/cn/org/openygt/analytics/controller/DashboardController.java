package cn.org.openygt.analytics.controller;

import cn.org.openygt.analytics.dto.DashboardRealtimeDTO;
import cn.org.openygt.analytics.service.DashboardService;
import cn.org.openygt.common.dto.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 监控大屏控制器。
 */
@RestController
@RequestMapping("/api/v1/ops/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/realtime")
    public ApiResponse<DashboardRealtimeDTO> realtime() {
        return ApiResponse.success(dashboardService.getRealtime());
    }
}
