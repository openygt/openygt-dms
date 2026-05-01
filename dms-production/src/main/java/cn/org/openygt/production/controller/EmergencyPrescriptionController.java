package cn.org.openygt.production.controller;

import cn.org.openygt.production.ProductionModule;

import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.production.dto.EmergencyPrescriptionDTO;
import cn.org.openygt.production.dto.EmergencySignRequest;
import cn.org.openygt.production.service.EmergencyPrescriptionService;
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

@RestController
@RequestMapping(ProductionModule.API_PREFIX)
@RequiredArgsConstructor
public class EmergencyPrescriptionController {

    private final EmergencyPrescriptionService emergencyPrescriptionService;

    @GetMapping("/prescription/emergency")
    public ApiResponse<IPage<EmergencyPrescriptionDTO>> listEmergencyPrescriptions(
            @RequestParam(required = false) Integer emergencyLevel,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(emergencyPrescriptionService.listEmergencyPrescriptions(emergencyLevel, status, page, size));
    }

    @PostMapping("/prescription/{prescriptionId}/emergency")
    public ApiResponse<EmergencyPrescriptionDTO> markEmergency(
            @PathVariable Long prescriptionId,
            @RequestParam Integer emergencyLevel) {
        return ApiResponse.success(emergencyPrescriptionService.markEmergency(prescriptionId, emergencyLevel));
    }

    @PostMapping("/emergency/{emergencyId}/sign")
    public ApiResponse<EmergencyPrescriptionDTO> signEmergency(
            @PathVariable Long emergencyId,
            @Validated @RequestBody EmergencySignRequest request) {
        return ApiResponse.success(emergencyPrescriptionService.signEmergency(emergencyId, request.getNurseName()));
    }
}
