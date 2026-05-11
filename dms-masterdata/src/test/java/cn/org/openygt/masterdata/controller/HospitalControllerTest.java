package cn.org.openygt.masterdata.controller;

import cn.org.openygt.common.exception.GlobalExceptionHandler;
import cn.org.openygt.masterdata.entity.Hospital;
import cn.org.openygt.masterdata.service.HospitalService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class HospitalControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private HospitalService hospitalService;

    @InjectMocks
    private HospitalController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void create_shouldReturnHospitalResponse() throws Exception {
        Hospital hospital = new Hospital();
        hospital.setId(1L);
        hospital.setName("测试医院");
        hospital.setCode("H001");

        when(hospitalService.create(any(Hospital.class))).thenReturn(hospital);

        mockMvc.perform(post("/api/v1/md/hospitals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"测试医院\",\"code\":\"H001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("测试医院"));
    }

    @Test
    void update_shouldReturnHospitalResponse() throws Exception {
        Hospital hospital = new Hospital();
        hospital.setId(1L);
        hospital.setName("新医院");

        when(hospitalService.update(anyLong(), any(Hospital.class))).thenReturn(hospital);

        mockMvc.perform(put("/api/v1/md/hospitals/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"新医院\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("新医院"));
    }

    @Test
    void getById_shouldReturnHospital() throws Exception {
        Hospital hospital = new Hospital();
        hospital.setId(1L);
        hospital.setName("人民医院");

        when(hospitalService.getById(1L)).thenReturn(hospital);

        mockMvc.perform(get("/api/v1/md/hospitals/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("人民医院"));
    }

    @Test
    void list_shouldReturnPagedResult() throws Exception {
        Page<Hospital> page = new Page<>(1, 20);
        when(hospitalService.list(any(), anyInt(), anyInt())).thenReturn(page);

        mockMvc.perform(get("/api/v1/md/hospitals")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void delete_shouldSucceed() throws Exception {
        mockMvc.perform(delete("/api/v1/md/hospitals/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
