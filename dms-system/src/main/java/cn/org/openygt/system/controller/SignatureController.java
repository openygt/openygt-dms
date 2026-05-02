package cn.org.openygt.system.controller;

import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.system.entity.Signature;
import cn.org.openygt.system.service.SignatureService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sys/signature")
@RequiredArgsConstructor
public class SignatureController {

    private final SignatureService signatureService;

    @PostMapping
    public ApiResponse<Signature> submit(@RequestBody Signature signature) {
        return ApiResponse.success(signatureService.submitSignature(signature));
    }

    @GetMapping
    public ApiResponse<List<Signature>> query(@RequestParam String bizType, @RequestParam Long bizId) {
        return ApiResponse.success(signatureService.querySignatures(bizType, bizId));
    }
}
