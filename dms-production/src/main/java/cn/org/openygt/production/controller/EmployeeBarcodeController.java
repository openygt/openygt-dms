package cn.org.openygt.production.controller;

import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.production.entity.EmployeeBarcode;
import cn.org.openygt.production.service.EmployeeBarcodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/employee")
@RequiredArgsConstructor
public class EmployeeBarcodeController {

    private final EmployeeBarcodeService employeeBarcodeService;

    @GetMapping("/{employeeId}/barcode")
    public ApiResponse<EmployeeBarcode> getBarcode(@PathVariable Long employeeId) {
        return ApiResponse.success(employeeBarcodeService.getOrCreateBarcode(employeeId));
    }

    @PostMapping("/{employeeId}/barcode/print")
    public ApiResponse<EmployeeBarcode> printBarcode(@PathVariable Long employeeId) {
        return ApiResponse.success(employeeBarcodeService.recordPrint(employeeId));
    }
}
