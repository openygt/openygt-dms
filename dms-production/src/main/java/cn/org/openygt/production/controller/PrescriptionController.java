package cn.org.openygt.production.controller;
import cn.org.openygt.production.ProductionModule;

import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.production.dto.PrescriptionCreateRequest;
import cn.org.openygt.production.dto.PrescriptionRejectRequest;
import cn.org.openygt.production.entity.Prescription;
import cn.org.openygt.production.service.PrescriptionService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ProductionModule.API_PREFIX + "/prescriptions")
@RequiredArgsConstructor
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    @PostMapping
    public ApiResponse<Prescription> create(@Validated @RequestBody PrescriptionCreateRequest request) {
        return ApiResponse.success(prescriptionService.create(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<Prescription> getById(@PathVariable Long id) {
        Prescription p = prescriptionService.getById(id);
        return p == null ? ApiResponse.error(404, "处方不存在") : ApiResponse.success(p);
    }

    @GetMapping
    public ApiResponse<IPage<Prescription>> list(
            @RequestParam(required = false) Long hospitalId,
            @RequestParam(required = false) Integer patientType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(prescriptionService.list(hospitalId, patientType, status, keyword, startTime, endTime, page, size));
    }

    @GetMapping("/receive-list")
    public ApiResponse<IPage<Prescription>> receiveList(
            @RequestParam(required = false) Long hospitalId,
            @RequestParam(required = false) Integer patientType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(prescriptionService.list(hospitalId, patientType, status, keyword, startTime, endTime, page, size));
    }

    @PostMapping("/{id}/receive")
    public ApiResponse<Prescription> receive(@PathVariable Long id,
                                              @RequestParam Long operatorId,
                                              @RequestParam String operatorName) {
        try {
            return ApiResponse.success(prescriptionService.receive(id, operatorId, operatorName));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<Prescription> reject(@PathVariable Long id,
                                             @Validated @RequestBody PrescriptionRejectRequest request,
                                             @RequestParam Long operatorId,
                                             @RequestParam String operatorName) {
        try {
            return ApiResponse.success(prescriptionService.reject(id, request.getRejectType(), request.getReason(), operatorId, operatorName));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @GetMapping("/{id}/detail")
    public ApiResponse<Prescription> detail(@PathVariable Long id) {
        Prescription p = prescriptionService.getDetail(id);
        return p == null ? ApiResponse.error(404, "处方不存在") : ApiResponse.success(p);
    }
}
