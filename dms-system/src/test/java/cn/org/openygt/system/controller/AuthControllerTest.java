package cn.org.openygt.system.controller;

import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.common.dto.LoginRequest;
import cn.org.openygt.common.dto.TokenResponse;
import cn.org.openygt.common.exception.GlobalExceptionHandler;
import cn.org.openygt.system.service.SysUserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private SysUserService userService;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void login_shouldSucceed_whenCredentialsValid() throws Exception {
        TokenResponse tokenResponse = new TokenResponse();
        tokenResponse.setToken("test-jwt-token");
        tokenResponse.setTokenType("Bearer");
        tokenResponse.setExpiresIn(86400L);
        tokenResponse.setUserId(1L);
        tokenResponse.setUsername("admin");

        when(userService.login(any(LoginRequest.class))).thenReturn(tokenResponse);

        mockMvc.perform(post("/api/v1/rbac/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").value("test-jwt-token"))
                .andExpect(jsonPath("$.data.username").value("admin"));
    }

    @Test
    void login_shouldFail_whenPasswordWrong() throws Exception {
        when(userService.login(any(LoginRequest.class)))
                .thenThrow(new IllegalArgumentException("用户名或密码错误"));

        mockMvc.perform(post("/api/v1/rbac/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"wrong\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }

    @Test
    void login_shouldFail_whenAccountLocked() throws Exception {
        when(userService.login(any(LoginRequest.class)))
                .thenThrow(new IllegalStateException("用户已被禁用"));

        mockMvc.perform(post("/api/v1/rbac/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"locked\",\"password\":\"123456\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409))
                .andExpect(jsonPath("$.message").value("用户已被禁用"));
    }
}
