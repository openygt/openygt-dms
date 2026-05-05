package cn.org.openygt.equipment.controller;

import cn.org.openygt.common.exception.GlobalExceptionHandler;
import cn.org.openygt.equipment.service.DecoctionTraceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DecoctionTraceControllerTest {

    private MockMvc mockMvc;

    @Mock
    private DecoctionTraceService traceService;

    @InjectMocks
    private DecoctionTraceController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getTemperatureCurve_withArrayData_shouldReturnStructuredResponse() throws Exception {
        Map<String, Object> curve = new HashMap<>();
        java.util.Map<String, Object> p1 = new HashMap<>();
        p1.put("time", "10:00");
        p1.put("temperature", 85.0);
        java.util.Map<String, Object> p2 = new HashMap<>();
        p2.put("time", "10:05");
        p2.put("temperature", 90.5);
        curve.put("data", Arrays.asList(p1, p2));
        curve.put("maxTemp", 95.0);
        curve.put("avgTemp", 87.75);

        when(traceService.getTemperatureCurve("P2024001")).thenReturn(curve);

        mockMvc.perform(get("/api/v1/eq/traces/P2024001/temperature-curve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.maxTemp").value(95.0))
                .andExpect(jsonPath("$.data.avgTemp").value(87.75))
                .andExpect(jsonPath("$.data.data[0].time").value("10:00"))
                .andExpect(jsonPath("$.data.data[0].temperature").value(85.0));
    }

    @Test
    void getTemperatureCurve_withStringData_shouldReturnStructuredResponse() throws Exception {
        Map<String, Object> curve = new HashMap<>();
        curve.put("data", "[{\"time\":\"10:00\",\"temperature\":85.0}]");
        curve.put("maxTemp", 85.0);
        curve.put("avgTemp", 85.0);

        when(traceService.getTemperatureCurve("P2024002")).thenReturn(curve);

        mockMvc.perform(get("/api/v1/eq/traces/P2024002/temperature-curve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.data").isString())
                .andExpect(jsonPath("$.data.maxTemp").value(85.0));
    }

    @Test
    void getTemperatureCurve_withEmptyData_shouldReturnEmptyArray() throws Exception {
        Map<String, Object> curve = new HashMap<>();
        curve.put("data", new java.util.ArrayList<>());
        curve.put("maxTemp", 0);
        curve.put("avgTemp", 0);

        when(traceService.getTemperatureCurve("P2024003")).thenReturn(curve);

        mockMvc.perform(get("/api/v1/eq/traces/P2024003/temperature-curve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.data").isArray())
                .andExpect(jsonPath("$.data.data").isEmpty());
    }
}
