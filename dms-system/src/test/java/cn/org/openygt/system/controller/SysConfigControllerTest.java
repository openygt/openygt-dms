package cn.org.openygt.system.controller;

import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.common.exception.GlobalExceptionHandler;
import cn.org.openygt.system.entity.SysConfig;
import cn.org.openygt.system.service.SysConfigService;
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
class SysConfigControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private SysConfigService configService;

    @InjectMocks
    private SysConfigController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void create_shouldReturnConfig() throws Exception {
        SysConfig config = new SysConfig();
        config.setId(1L);
        config.setConfigKey("my.key");
        config.setConfigValue("my-value");

        when(configService.create(any(SysConfig.class))).thenReturn(config);

        mockMvc.perform(post("/api/v1/sys/configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"configKey\":\"my.key\",\"configValue\":\"my-value\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.configKey").value("my.key"));
    }

    @Test
    void update_shouldReturnConfig() throws Exception {
        SysConfig config = new SysConfig();
        config.setId(1L);
        config.setConfigKey("new.key");

        when(configService.update(anyLong(), any(SysConfig.class))).thenReturn(config);

        mockMvc.perform(put("/api/v1/sys/configs/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"configKey\":\"new.key\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.configKey").value("new.key"));
    }

    @Test
    void getById_shouldReturnConfig() throws Exception {
        SysConfig config = new SysConfig();
        config.setId(1L);
        config.setConfigKey("my.key");

        when(configService.getById(1L)).thenReturn(config);

        mockMvc.perform(get("/api/v1/sys/configs/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.configKey").value("my.key"));
    }

    @Test
    void getByKey_shouldReturnConfig() throws Exception {
        SysConfig config = new SysConfig();
        config.setConfigKey("my.key");
        config.setConfigValue("my-value");

        when(configService.getByKey("my.key")).thenReturn(config);

        mockMvc.perform(get("/api/v1/sys/configs/key/my.key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.configValue").value("my-value"));
    }

    @Test
    void list_shouldReturnPagedResult() throws Exception {
        Page<SysConfig> page = new Page<>(1, 20);
        when(configService.list(anyString(), anyInt(), anyInt())).thenReturn(page);

        mockMvc.perform(get("/api/v1/sys/configs")
                        .param("keyword", "key")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void delete_shouldSucceed() throws Exception {
        mockMvc.perform(delete("/api/v1/sys/configs/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
