package cn.org.openygt.system.controller;

import cn.org.openygt.common.annotation.RequiresPermissions;
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

    @RequiresPermissions({"ROLE_ADMIN", "ROLE_DIRECTOR"})
    @PostMapping
    public ApiResponse<Signature> submit(@RequestBody Signature signature) {
        return ApiResponse.success(signatureService.submitSignature(signature));
    }

    @RequiresPermissions({"ROLE_ADMIN", "ROLE_DIRECTOR"})
    @GetMapping
    public ApiResponse<List<Signature>> query(
            @RequestParam(required = false) String bizType,
            @RequestParam(required = false) Long bizId) {
        if (bizType != null && bizId != null) {
            return ApiResponse.success(signatureService.querySignatures(bizType, bizId));
        }
        // 不带参数时返回全部列表
        return ApiResponse.success(signatureService.list());
    }
}
