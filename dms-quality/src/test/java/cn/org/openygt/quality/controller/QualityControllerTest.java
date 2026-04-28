package cn.org.openygt.quality.controller;

import cn.org.openygt.common.dto.InspectionResult;
import cn.org.openygt.common.enums.InspectionResultType;
import cn.org.openygt.common.exception.GlobalExceptionHandler;
import cn.org.openygt.common.service.QualityService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class QualityControllerTest {

    private MockMvc mockMvc;

    @Mock
    private QualityService qualityService;

    @InjectMocks
    private QualityController qualityController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(qualityController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void inspect_shouldSucceed() throws Exception {
        InspectionResult result = new InspectionResult();
        result.setTaskId(1L);
        result.setResult(InspectionResultType.PASS);
        when(qualityService.inspect(anyLong(), any(InspectionResultType.class), any(), any()))
                .thenReturn(result);

        mockMvc.perform(post("/api/v1/qt/inspect")
                        .param("taskId", "1")
                        .param("result", "PASS")
                        .param("operatorId", "QC001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.result").value("PASS"));
    }
}
