package cn.org.openygt.production.controller;

import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.production.dto.AlertStatisticsDTO;
import cn.org.openygt.production.dto.TimeMonitorDashboardDTO;
import cn.org.openygt.production.dto.TimeRuleRequest;
import cn.org.openygt.production.entity.AlertLog;
import cn.org.openygt.production.entity.TimeMonitor;
import cn.org.openygt.production.entity.TimeRule;
import cn.org.openygt.production.service.TimeMonitorService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class TimeMonitorController {

    private final TimeMonitorService timeMonitorService;

    @GetMapping("/time-monitor/dashboard")
    public ApiResponse<TimeMonitorDashboardDTO> getDashboard() {
        return ApiResponse.success(timeMonitorService.getDashboard());
    }

    @GetMapping("/time-monitor/{taskId}")
    public ApiResponse<List<TimeMonitor>> getTaskMonitors(@PathVariable Long taskId) {
        return ApiResponse.success(timeMonitorService.getTaskMonitors(taskId));
    }

    @PostMapping("/time-rule")
    public ApiResponse<TimeRule> saveTimeRule(@Validated @RequestBody TimeRuleRequest request) {
        return ApiResponse.success(timeMonitorService.saveTimeRule(request));
    }

    @GetMapping("/time-rule")
    public ApiResponse<List<TimeRule>> listTimeRules() {
        return ApiResponse.success(timeMonitorService.listTimeRules());
    }

    @GetMapping("/alert/active")
    public ApiResponse<IPage<AlertLog>> listActiveAlerts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(timeMonitorService.listActiveAlerts(page, size));
    }

    @PostMapping("/alert/{alertId}/resolve")
    public ApiResponse<AlertLog> resolveAlert(@PathVariable Long alertId,
                                               @RequestParam Long resolvedBy) {
        return ApiResponse.success(timeMonitorService.resolveAlert(alertId, resolvedBy));
    }

    @GetMapping("/alert/statistics")
    public ApiResponse<AlertStatisticsDTO> getAlertStatistics() {
        return ApiResponse.success(timeMonitorService.getAlertStatistics());
    }
}
