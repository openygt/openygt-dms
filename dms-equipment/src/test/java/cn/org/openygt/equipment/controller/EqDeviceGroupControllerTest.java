package cn.org.openygt.equipment.controller;

import cn.org.openygt.common.exception.GlobalExceptionHandler;
import cn.org.openygt.equipment.entity.EqDeviceGroup;
import cn.org.openygt.equipment.service.EqDeviceGroupService;
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

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class EqDeviceGroupControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private EqDeviceGroupService groupService;

    @InjectMocks
    private EqDeviceGroupController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void create_shouldReturnId() throws Exception {
        EqDeviceGroup group = new EqDeviceGroup();
        group.setGroupCode("G001");
        group.setGroupName("煎药一组");

        when(groupService.save(any())).thenReturn(true);

        mockMvc.perform(post("/api/v1/eq/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(group)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void list_shouldReturnPage() throws Exception {
        EqDeviceGroup group = new EqDeviceGroup();
        group.setId(1L);
        group.setGroupCode("G001");
        group.setGroupName("煎药一组");

        when(groupService.page(any(), any())).thenReturn(
                new Page<EqDeviceGroup>(1, 10, 1).setRecords(Collections.singletonList(group)));

        mockMvc.perform(get("/api/v1/eq/groups"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records[0].groupName").value("煎药一组"));
    }

    @Test
    void all_shouldReturnList() throws Exception {
        EqDeviceGroup group = new EqDeviceGroup();
        group.setId(1L);
        group.setGroupName("煎药一组");

        when(groupService.list()).thenReturn(Collections.singletonList(group));

        mockMvc.perform(get("/api/v1/eq/groups/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].groupName").value("煎药一组"));
    }
}
