package cn.org.openygt.controller.inventory;

import cn.org.openygt.common.exception.GlobalExceptionHandler;
import cn.org.openygt.common.interceptor.AuthInterceptor;
import cn.org.openygt.common.util.JwtUtil;
import cn.org.openygt.inventory.dto.ConsumeRecordRequest;
import cn.org.openygt.inventory.service.ConsumeRecordService;
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

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ConsumeRecordControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ConsumeRecordService consumeRecordService;

    @InjectMocks
    private ConsumeRecordController controller;

    private String validToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .addInterceptors(new AuthInterceptor())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        validToken = JwtUtil.generateToken(1L, "admin",
                Collections.singletonList("ROLE_ADMIN"),
                Collections.singletonList("inv:log:list"));
    }

    @Test
    void shouldReject_whenNoToken() throws Exception {
        mockMvc.perform(get("/api/v1/inv/consume/list"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void shouldReject_whenInvalidToken() throws Exception {
        mockMvc.perform(get("/api/v1/inv/consume/list")
                        .header("Authorization", "Bearer invalid_token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void shouldPass_whenValidToken() throws Exception {
        when(consumeRecordService.pageQuery(any(), any(), any(), any(), any(), any(Integer.class), any(Integer.class)))
                .thenReturn(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>());

        mockMvc.perform(get("/api/v1/inv/consume/list")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void shouldPass_whenRecordConsumeWithValidToken() throws Exception {
        when(consumeRecordService.recordConsume(any(ConsumeRecordRequest.class)))
                .thenReturn(Collections.singletonList(1L));

        mockMvc.perform(post("/api/v1/inv/consume/record")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"taskId\":1,\"operatorId\":\"OP001\",\"items\":[]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
