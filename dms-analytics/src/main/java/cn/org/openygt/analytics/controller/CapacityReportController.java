package cn.org.openygt.analytics.controller;

import cn.org.openygt.analytics.dto.CapacityDailyDTO;
import cn.org.openygt.analytics.service.CapacityReportService;
import cn.org.openygt.common.dto.ApiResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 产能统计报表控制器。
 */
@RestController
@RequestMapping("/api/v1/ops/capacity")
public class CapacityReportController {

    private final CapacityReportService capacityReportService;

    public CapacityReportController(CapacityReportService capacityReportService) {
        this.capacityReportService = capacityReportService;
    }

    @GetMapping("/daily")
    public ApiResponse<List<CapacityDailyDTO>> daily(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long hospitalId) {
        if (startDate == null) startDate = LocalDate.now().minusDays(30);
        if (endDate == null) endDate = LocalDate.now();
        return ApiResponse.success(capacityReportService.getDailyCapacity(startDate, endDate, hospitalId));
    }
}
