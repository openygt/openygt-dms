package cn.org.openygt.print.controller;

import cn.org.openygt.common.dto.PrintTaskDTO;
import cn.org.openygt.common.exception.GlobalExceptionHandler;
import cn.org.openygt.common.service.PrintService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PrintControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PrintService printService;

    @InjectMocks
    private PrintController printController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(printController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void list_shouldReturnPrintTasks() throws Exception {
        when(printService.getPrintTasks(1, 20)).thenReturn(new Page<>());

        mockMvc.perform(get("/api/v1/prt/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void submitPrintTask_shouldSucceed() throws Exception {
        doNothing().when(printService).submitPrintTask(1L, "PRINTER_01", "OP_001");

        mockMvc.perform(post("/api/v1/prt/tasks/1/submit")
                        .param("deviceCode", "PRINTER_01")
                        .param("operatorId", "OP_001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void getPrintQueue_shouldReturnQueue() throws Exception {
        when(printService.getPrintQueue()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/prt/queue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
