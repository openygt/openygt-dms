package cn.org.openygt.quality.controller;

import cn.org.openygt.common.dto.InspectionResult;
import cn.org.openygt.common.dto.InspectionSummaryDTO;
import cn.org.openygt.common.dto.InspectionTrendDTO;
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

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
        when(qualityService.inspect(anyLong(), any(InspectionResultType.class), any(), any(), any()))
                .thenReturn(result);

        mockMvc.perform(post("/api/v1/qt/inspect")
                        .param("taskId", "1")
                        .param("result", "PASS")
                        .param("operatorId", "QC001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.result").value("PASS"));
    }

    @Test
    void summary_shouldReturnCorrectStructure() throws Exception {
        InspectionSummaryDTO dto = new InspectionSummaryDTO();
        dto.setTotalCount(100);
        dto.setPassCount(90);
        dto.setConcessionCount(5);
        dto.setReworkCount(3);
        dto.setScrapCount(2);
        dto.setPassRate(90.0);

        when(qualityService.getInspectionSummary(any(), any())).thenReturn(dto);

        mockMvc.perform(get("/api/v1/qt/report/summary")
                        .param("dateStart", "2026-05-01T00:00:00")
                        .param("dateEnd", "2026-05-31T23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalCount").value(100))
                .andExpect(jsonPath("$.data.passCount").value(90))
                .andExpect(jsonPath("$.data.passRate").value(90.0));
    }

    @Test
    void trend_shouldReturnTotalAlias() throws Exception {
        InspectionTrendDTO dto = new InspectionTrendDTO();
        dto.setPeriod("2026-05-05");
        dto.setTotalCount(10);
        dto.setPassCount(8);

        when(qualityService.getInspectionTrend(any(), any(), any())).thenReturn(Collections.singletonList(dto));

        mockMvc.perform(get("/api/v1/qt/report/trend")
                        .param("dateStart", "2026-05-01T00:00:00")
                        .param("dateEnd", "2026-05-31T23:59:59")
                        .param("groupBy", "day"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].totalCount").value(10))
                .andExpect(jsonPath("$.data[0].total").value(10))
                .andExpect(jsonPath("$.data[0].passCount").value(8));
    }

    @Test
    void reason_shouldReturnList() throws Exception {
        java.util.Map<String, Object> row = new java.util.HashMap<>();
        row.put("item", "温度不达标");
        row.put("pass", 0);
        row.put("fail", 3);

        when(qualityService.getInspectionReasonStat(any(), any())).thenReturn(Collections.singletonList(row));

        mockMvc.perform(get("/api/v1/qt/report/reason")
                        .param("dateStart", "2026-05-01T00:00:00")
                        .param("dateEnd", "2026-05-31T23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].item").value("温度不达标"))
                .andExpect(jsonPath("$.data[0].fail").value(3));
    }
}
