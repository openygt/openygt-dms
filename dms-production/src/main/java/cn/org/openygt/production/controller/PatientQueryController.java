package cn.org.openygt.production.controller;

import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.production.dto.PatientProgressDTO;
import cn.org.openygt.production.dto.PrescriptionTraceDTO;
import cn.org.openygt.production.entity.PatientToken;
import cn.org.openygt.production.entity.Prescription;
import cn.org.openygt.production.service.PatientQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import cn.org.openygt.production.ProductionModule;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(ProductionModule.API_PREFIX + "/patient")
@RequiredArgsConstructor
public class PatientQueryController {

    private final PatientQueryService patientQueryService;

    @PostMapping("/query-by-code")
    public ApiResponse<PatientToken> queryByCode(@RequestBody Map<String, String> body) {
        String code = body.get("code");
        return ApiResponse.success(patientQueryService.queryByCode(code));
    }

    @PostMapping("/query-by-phone")
    public ApiResponse<List<Prescription>> queryByPhone(@RequestBody Map<String, String> body) {
        String phone = body.get("phone");
        return ApiResponse.success(patientQueryService.queryByPhone(phone));
    }

    @GetMapping("/progress/{token}")
    public ApiResponse<PatientProgressDTO> progress(@PathVariable String token) {
        return ApiResponse.success(patientQueryService.getProgress(token));
    }

    @GetMapping("/prescription/{prescriptionId}/trace")
    public ApiResponse<PrescriptionTraceDTO> trace(@PathVariable Long prescriptionId) {
        return ApiResponse.success(patientQueryService.getTrace(prescriptionId));
    }
}
