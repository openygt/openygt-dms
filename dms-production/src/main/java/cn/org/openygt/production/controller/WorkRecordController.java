package cn.org.openygt.production.controller;

import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.production.ProductionModule;
import cn.org.openygt.production.dto.EmployeeWorkDailyDTO;
import cn.org.openygt.production.service.WorkRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping(ProductionModule.API_PREFIX + "/work-record")
@RequiredArgsConstructor
public class WorkRecordController {

    private final WorkRecordService workRecordService;

    @GetMapping("/daily")
    public ApiResponse<List<EmployeeWorkDailyDTO>> daily(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.success(workRecordService.getDailyReport(date));
    }
}
