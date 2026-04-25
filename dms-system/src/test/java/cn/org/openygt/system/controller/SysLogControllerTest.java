package cn.org.openygt.system.controller;

import cn.org.openygt.common.exception.GlobalExceptionHandler;
import cn.org.openygt.system.entity.SysLog;
import cn.org.openygt.system.service.SysLogService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SysLogControllerTest {

    private MockMvc mockMvc;

    @Mock
    private SysLogService logService;

    @InjectMocks
    private SysLogController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void list_shouldReturnPagedResult() throws Exception {
        Page<SysLog> page = new Page<>(1, 20);
        when(logService.list(anyString(), anyInt(), anyInt())).thenReturn(page);

        mockMvc.perform(get("/api/v1/sys/logs")
                        .param("keyword", "test")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void list_shouldReturnAll_whenNoKeyword() throws Exception {
        Page<SysLog> page = new Page<>(1, 20);
        when(logService.list(isNull(), anyInt(), anyInt())).thenReturn(page);

        mockMvc.perform(get("/api/v1/sys/logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
