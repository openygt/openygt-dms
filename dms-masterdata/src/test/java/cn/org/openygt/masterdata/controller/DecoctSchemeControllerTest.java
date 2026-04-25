package cn.org.openygt.masterdata.controller;

import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.common.exception.GlobalExceptionHandler;
import cn.org.openygt.masterdata.dto.SchemeCreateRequest;
import cn.org.openygt.masterdata.dto.SchemeResponse;
import cn.org.openygt.masterdata.dto.SchemeUpdateRequest;
import cn.org.openygt.masterdata.entity.DecoctScheme;
import cn.org.openygt.masterdata.service.DecoctSchemeService;
import com.baomidou.mybatisplus.core.metadata.IPage;
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
class DecoctSchemeControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private DecoctSchemeService schemeService;

    @InjectMocks
    private DecoctSchemeController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void create_shouldReturnSchemeResponse() throws Exception {
        DecoctScheme scheme = new DecoctScheme();
        scheme.setId(1L);
        scheme.setName("测试方案");

        when(schemeService.create(any(DecoctScheme.class))).thenReturn(scheme);

        SchemeCreateRequest request = new SchemeCreateRequest();
        request.setName("测试方案");

        mockMvc.perform(post("/api/v1/md/schemes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("测试方案"));
    }

    @Test
    void update_shouldReturnSchemeResponse() throws Exception {
        DecoctScheme scheme = new DecoctScheme();
        scheme.setId(1L);
        scheme.setName("新方案");

        when(schemeService.update(anyLong(), any(DecoctScheme.class))).thenReturn(scheme);

        SchemeUpdateRequest request = new SchemeUpdateRequest();
        request.setName("新方案");

        mockMvc.perform(put("/api/v1/md/schemes/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("新方案"));
    }

    @Test
    void getById_shouldReturnScheme() throws Exception {
        DecoctScheme scheme = new DecoctScheme();
        scheme.setId(1L);
        scheme.setName("方案A");

        when(schemeService.getById(1L)).thenReturn(scheme);

        mockMvc.perform(get("/api/v1/md/schemes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("方案A"));
    }

    @Test
    void list_shouldReturnPagedResult() throws Exception {
        Page<DecoctScheme> page = new Page<>(1, 20);
        when(schemeService.list(anyString(), anyInt(), anyInt())).thenReturn(page);

        mockMvc.perform(get("/api/v1/md/schemes")
                        .param("keyword", "方案")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void delete_shouldSucceed() throws Exception {
        mockMvc.perform(delete("/api/v1/md/schemes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
