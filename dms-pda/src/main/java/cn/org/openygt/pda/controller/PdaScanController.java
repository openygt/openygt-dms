package cn.org.openygt.pda.controller;

import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.pda.service.PdaScanHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/pda")
@RequiredArgsConstructor
public class PdaScanController {

    private final PdaScanHandler pdaScanHandler;

    @PostMapping("/scan")
    public ApiResponse<PdaScanHandler.PdaScanResult> scan(@RequestBody ScanRequest request) {
        return ApiResponse.success(pdaScanHandler.handleScan(request.getBarcode(), request.getOperatorId()));
    }

    @lombok.Data
    public static class ScanRequest {
        private String barcode;
        private Long operatorId;
    }
}
