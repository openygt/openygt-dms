package cn.org.openygt.analytics.service.impl;

import cn.org.openygt.analytics.dto.DashboardRealtimeDTO;
import cn.org.openygt.analytics.mapper.DashboardStatMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    @Mock
    private DashboardStatMapper statMapper;

    @InjectMocks
    private DashboardServiceImpl dashboardService;

    @Test
    void getRealtime_shouldReturnDashboardData() {
        Map<String, Object> taskStats = new HashMap<>();
        taskStats.put("total", 26L);
        taskStats.put("ended", 4L);
        taskStats.put("inProgress", 22L);
        taskStats.put("alertingNow", 2L);
        taskStats.put("everAlerted", 3L);

        when(statMapper.selectTodayTaskStats()).thenReturn(taskStats);
        when(statMapper.selectYesterdayTaskStats()).thenReturn(new HashMap<>());
        when(statMapper.selectOnlineDeviceCount()).thenReturn(8L);
        when(statMapper.selectOfflineDeviceCount()).thenReturn(2L);
        when(statMapper.selectTodayInspectionCount()).thenReturn(4L);
        when(statMapper.selectTaskStatusDistribution()).thenReturn(Collections.emptyList());
        when(statMapper.selectDeviceTypeDistribution()).thenReturn(Collections.emptyList());

        DashboardRealtimeDTO dto = dashboardService.getRealtime();

        assertThat(dto.getTodayTotalTasks()).isEqualTo(26L);
        assertThat(dto.getTodayEndedTasks()).isEqualTo(4L);
        assertThat(dto.getTodayInProgressTasks()).isEqualTo(22L);
        assertThat(dto.getTodayAlertingTasks()).isEqualTo(2L);
        assertThat(dto.getTodayAlertedTasks()).isEqualTo(3L);
        assertThat(dto.getOnlineDeviceCount()).isEqualTo(8L);
        assertThat(dto.getOfflineDeviceCount()).isEqualTo(2L);
        assertThat(dto.getTodayInspectionCount()).isEqualTo(4L);
    }

    @Test
    void getRealtime_withNullStats_shouldReturnZeros() {
        Map<String, Object> taskStats = new HashMap<>();
        when(statMapper.selectTodayTaskStats()).thenReturn(taskStats);
        when(statMapper.selectYesterdayTaskStats()).thenReturn(null);
        when(statMapper.selectOnlineDeviceCount()).thenReturn(null);
        when(statMapper.selectOfflineDeviceCount()).thenReturn(null);
        when(statMapper.selectTodayInspectionCount()).thenReturn(null);
        when(statMapper.selectTaskStatusDistribution()).thenReturn(Collections.emptyList());
        when(statMapper.selectDeviceTypeDistribution()).thenReturn(Collections.emptyList());

        DashboardRealtimeDTO dto = dashboardService.getRealtime();

        assertThat(dto.getTodayTotalTasks()).isEqualTo(0L);
        assertThat(dto.getTodayEndedTasks()).isEqualTo(0L);
        assertThat(dto.getTodayInProgressTasks()).isEqualTo(0L);
        assertThat(dto.getTodayAlertingTasks()).isEqualTo(0L);
        assertThat(dto.getTodayAlertedTasks()).isEqualTo(0L);
        assertThat(dto.getOnlineDeviceCount()).isNull();
    }
}
