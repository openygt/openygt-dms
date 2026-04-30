package cn.org.openygt.equipment.controller;

import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.equipment.EquipmentModule;
import cn.org.openygt.equipment.entity.TimeCheckRule;
import cn.org.openygt.equipment.service.TimeCheckService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(EquipmentModule.API_PREFIX + "/time-check")
@RequiredArgsConstructor
public class TimeCheckController {

    private final TimeCheckService timeCheckService;

    @GetMapping("/rules")
    public ApiResponse<List<TimeCheckRule>> getAllRules() {
        return ApiResponse.success(timeCheckService.getAllRules());
    }

    @PostMapping("/rules")
    public ApiResponse<TimeCheckRule> createRule(@RequestBody TimeCheckRule rule) {
        return ApiResponse.success(timeCheckService.createRule(rule));
    }

    @PutMapping("/rules/{id}")
    public ApiResponse<TimeCheckRule> updateRule(@PathVariable Long id, @RequestBody TimeCheckRule rule) {
        return ApiResponse.success(timeCheckService.updateRule(id, rule));
    }

    @DeleteMapping("/rules/{id}")
    public ApiResponse<Void> deleteRule(@PathVariable Long id) {
        timeCheckService.deleteRule(id);
        return ApiResponse.success(null);
    }

    @PostMapping("/validate")
    public ApiResponse<Map<String, Object>> validate(
            @RequestParam String prescriptionNo,
            @RequestParam String toStep) {
        return ApiResponse.success(timeCheckService.validate(prescriptionNo, toStep));
    }

    @GetMapping("/prescription/{prescriptionNo}/status")
    public ApiResponse<List<Map<String, Object>>> getPrescriptionCheckStatus(@PathVariable String prescriptionNo) {
        return ApiResponse.success(timeCheckService.getPrescriptionCheckStatus(prescriptionNo));
    }
}
