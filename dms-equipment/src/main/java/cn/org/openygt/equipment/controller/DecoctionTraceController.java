package cn.org.openygt.equipment.controller;

import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.equipment.EquipmentModule;
import cn.org.openygt.equipment.entity.DecoctionTrace;
import cn.org.openygt.equipment.entity.DecoctionTraceEvent;
import cn.org.openygt.equipment.service.DecoctionTraceService;
import cn.org.openygt.rbac.annotation.RequiresPermissions;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(EquipmentModule.API_PREFIX + "/traces")
@RequiredArgsConstructor
public class DecoctionTraceController {

    private final DecoctionTraceService traceService;

    @PostMapping
    public ApiResponse<DecoctionTrace> createTrace(@RequestBody DecoctionTrace trace) {
        return ApiResponse.success(traceService.createTrace(trace));
    }

    @GetMapping
    public ApiResponse<Page<DecoctionTrace>> queryTraces(
            @RequestParam(required = false) String prescriptionNo,
            @RequestParam(required = false) String patientName,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false) String deviceCode,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        return ApiResponse.success(traceService.queryTraces(prescriptionNo, patientName, status, startTime, endTime, deviceCode, page, size));
    }

    @GetMapping("/{prescriptionNo}")
    public ApiResponse<DecoctionTrace> getTrace(@PathVariable String prescriptionNo) {
        return ApiResponse.success(traceService.getByPrescriptionNo(prescriptionNo));
    }

    @GetMapping("/{prescriptionNo}/events")
    public ApiResponse<List<DecoctionTraceEvent>> getTraceEvents(@PathVariable String prescriptionNo) {
        return ApiResponse.success(traceService.getTraceEvents(prescriptionNo));
    }

    @GetMapping("/{prescriptionNo}/temperature-curve")
    @RequiresPermissions({"eq:temp:view", "ROLE_WORKER", "ROLE_LEADER", "ROLE_INSPECTOR", "ROLE_DIRECTOR", "ROLE_ADMIN"})
    public ApiResponse<Map<String, Object>> getTemperatureCurve(@PathVariable String prescriptionNo) {
        return ApiResponse.success(traceService.getTemperatureCurve(prescriptionNo));
    }

    @PutMapping("/{prescriptionNo}/step/{stepCode}")
    public ApiResponse<DecoctionTrace> updateStep(
            @PathVariable String prescriptionNo,
            @PathVariable String stepCode,
            @RequestBody Map<String, Object> data) {
        return ApiResponse.success(traceService.updateStep(prescriptionNo, stepCode, data));
    }

    @PostMapping("/{prescriptionNo}/print")
    public ApiResponse<Map<String, Object>> printTrace(
            @PathVariable String prescriptionNo,
            @RequestParam(defaultValue = "TRACE") String printType) {
        return ApiResponse.success(new java.util.HashMap<String, Object>() {{ put("prescriptionNo", prescriptionNo); put("printType", printType); put("message", "打印任务已创建"); }});
    }
}
