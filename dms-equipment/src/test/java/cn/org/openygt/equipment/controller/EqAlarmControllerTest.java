package cn.org.openygt.equipment.controller;

import cn.org.openygt.common.exception.GlobalExceptionHandler;
import cn.org.openygt.equipment.entity.EqDevice;
import cn.org.openygt.equipment.entity.EqDeviceAlarm;
import cn.org.openygt.equipment.mapper.EqDeviceAlarmMapper;
import cn.org.openygt.equipment.mapper.EqDeviceMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class EqAlarmControllerTest {

    private MockMvc mockMvc;

    @Mock
    private EqDeviceAlarmMapper alarmMapper;

    @Mock
    private EqDeviceMapper deviceMapper;

    @InjectMocks
    private EqAlarmController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void list_shouldReturnAlarms() throws Exception {
        EqDeviceAlarm alarm = new EqDeviceAlarm();
        alarm.setId(1L);
        alarm.setDeviceId(10L);
        alarm.setAlarmType("HIGH_TEMP");
        alarm.setAlarmLevel("CRITICAL");
        alarm.setMessage("超温");
        alarm.setIsResolved(0);
        alarm.setCreatedAt(LocalDateTime.now());

        EqDevice device = new EqDevice();
        device.setId(10L);
        device.setDeviceCode("DEC001");

        when(alarmMapper.selectPage(any(), any())).thenAnswer(invocation -> {
            Page<EqDeviceAlarm> page = invocation.getArgument(0);
            page.setRecords(Collections.singletonList(alarm));
            page.setTotal(1);
            return page;
        });
        when(deviceMapper.selectBatchIds(any())).thenReturn(Collections.singletonList(device));

        mockMvc.perform(get("/api/v1/eq/alarms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records[0].deviceCode").value("DEC001"))
                .andExpect(jsonPath("$.data.records[0].status").value("PENDING"));
    }

    @Test
    void list_withEmptyResult_shouldReturnEmpty() throws Exception {
        when(alarmMapper.selectPage(any(), any())).thenReturn(new Page<EqDeviceAlarm>(1, 10, 0));

        mockMvc.perform(get("/api/v1/eq/alarms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isEmpty());
    }
}
