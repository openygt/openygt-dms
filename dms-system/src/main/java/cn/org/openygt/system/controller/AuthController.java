package cn.org.openygt.system.controller;

import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.common.dto.LoginRequest;
import cn.org.openygt.common.dto.TokenResponse;
import cn.org.openygt.system.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口。
 *
 * <p>API 前缀: /api/v1/auth</p>
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Validated
public class AuthController {

    private final SysUserService userService;

    /**
     * 用户登录。
     *
     * @param request 登录请求
     * @return Token 响应
     */
    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@Validated @RequestBody LoginRequest request) {
        return ApiResponse.success(userService.login(request));
    }
}
