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

import java.util.List;

@RestController
@RequestMapping("/api/v1/patient")
@RequiredArgsConstructor
public class PatientQueryController {

    private final PatientQueryService patientQueryService;

    @GetMapping("/query-by-code")
    public ApiResponse<PatientToken> queryByCode(@RequestParam String token) {
        return ApiResponse.success(patientQueryService.queryByCode(token));
    }

    @GetMapping("/query-by-phone")
    public ApiResponse<List<Prescription>> queryByPhone(@RequestParam String phone) {
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
