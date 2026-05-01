package cn.org.openygt.production.scheduler;

import cn.org.openygt.production.entity.AlertLog;
import cn.org.openygt.production.entity.TimeMonitor;
import cn.org.openygt.production.mapper.AlertLogMapper;
import cn.org.openygt.production.mapper.TimeMonitorMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 时效监控预警升级定时任务单元测试。
 */
@ExtendWith(MockitoExtension.class)
class TimeMonitorAlertSchedulerTest {

    @Mock
    private TimeMonitorMapper timeMonitorMapper;
    @Mock
    private AlertLogMapper alertLogMapper;

    @InjectMocks
    private TimeMonitorAlertScheduler scheduler;

    private TimeMonitor createMonitor(Long id, LocalDateTime plannedEnd, Integer alertLevel, Integer status) {
        TimeMonitor m = new TimeMonitor();
        m.setId(id);
        m.setTaskId(id);
        m.setStage("DECOCT");
        m.setPlannedEnd(plannedEnd);
        m.setActualEnd(null);
        m.setAlertLevel(alertLevel);
        m.setWarningCount(0);
        m.setStatus(status);
        return m;
    }

    @Test
    @DisplayName("提前5分钟：触发预警（alertLevel=1）")
    void testWarningFiveMinutesBefore() {
        LocalDateTime now = LocalDateTime.now();
        TimeMonitor monitor = createMonitor(1L, now.plusMinutes(3), 0, 2);
        when(timeMonitorMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.singletonList(monitor));

        scheduler.checkAlertLevel();

        ArgumentCaptor<AlertLog> captor = ArgumentCaptor.forClass(AlertLog.class);
        verify(alertLogMapper).insert(captor.capture());
        AlertLog log = captor.getValue();
        assertEquals(1, log.getAlertLevel());
        assertEquals("TIMEOUT", log.getAlertType());
        assertTrue(log.getAlertContent().contains("即将超时"));
        assertEquals(2, monitor.getStatus().intValue()); // 预警不改变 status
    }

    @Test
    @DisplayName("已超时：触发超时（alertLevel=2）并标记状态为已超时")
    void testTimeoutTrigger() {
        LocalDateTime now = LocalDateTime.now();
        TimeMonitor monitor = createMonitor(1L, now.minusMinutes(2), 0, 2);
        when(timeMonitorMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.singletonList(monitor));

        scheduler.checkAlertLevel();

        ArgumentCaptor<AlertLog> captor = ArgumentCaptor.forClass(AlertLog.class);
        verify(alertLogMapper).insert(captor.capture());
        assertEquals(2, captor.getValue().getAlertLevel());
        assertEquals(3, monitor.getStatus().intValue()); // 标记为已超时
        verify(timeMonitorMapper).updateById(monitor);
    }

    @Test
    @DisplayName("超时10分钟：触发告警（alertLevel=3）")
    void testAlertAfterTenMinutes() {
        LocalDateTime now = LocalDateTime.now();
        TimeMonitor monitor = createMonitor(1L, now.minusMinutes(12), 2, 2);
        when(timeMonitorMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.singletonList(monitor));

        scheduler.checkAlertLevel();

        ArgumentCaptor<AlertLog> captor = ArgumentCaptor.forClass(AlertLog.class);
        verify(alertLogMapper).insert(captor.capture());
        assertEquals(3, captor.getValue().getAlertLevel());
    }

    @Test
    @DisplayName("超时30分钟：触发严重告警（alertLevel=4）")
    void testCriticalAfterThirtyMinutes() {
        LocalDateTime now = LocalDateTime.now();
        TimeMonitor monitor = createMonitor(1L, now.minusMinutes(40), 3, 2);
        when(timeMonitorMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.singletonList(monitor));

        scheduler.checkAlertLevel();

        ArgumentCaptor<AlertLog> captor = ArgumentCaptor.forClass(AlertLog.class);
        verify(alertLogMapper).insert(captor.capture());
        assertEquals(4, captor.getValue().getAlertLevel());
        assertTrue(captor.getValue().getAlertContent().contains("严重超时"));
        assertEquals(3, monitor.getStatus().intValue()); // 保持已超时
    }

    @Test
    @DisplayName("已完成的监控实例不扫描")
    void testCompletedMonitorSkipped() {
        LocalDateTime now = LocalDateTime.now();
        TimeMonitor monitor = createMonitor(1L, now.minusMinutes(60), 0, 4); // status=4 已完成
        when(timeMonitorMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.singletonList(monitor));

        scheduler.checkAlertLevel();

        verify(alertLogMapper, never()).insert(any(AlertLog.class));
        verify(timeMonitorMapper, never()).updateById(any(TimeMonitor.class));
    }

    @Test
    @DisplayName("无计划结束时间跳过")
    void testNullPlannedEndSkipped() {
        TimeMonitor monitor = createMonitor(1L, null, 0, 2);
        when(timeMonitorMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.singletonList(monitor));

        scheduler.checkAlertLevel();

        verify(alertLogMapper, never()).insert(any(AlertLog.class));
    }

    @Test
    @DisplayName("多实例同时扫描：分别触发不同级别")
    void testMultipleMonitorsDifferentLevels() {
        LocalDateTime now = LocalDateTime.now();
        TimeMonitor m1 = createMonitor(1L, now.plusMinutes(3), 0, 2);   // 预警
        TimeMonitor m2 = createMonitor(2L, now.minusMinutes(15), 0, 2); // 告警
        when(timeMonitorMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Arrays.asList(m1, m2));

        scheduler.checkAlertLevel();

        verify(alertLogMapper, times(2)).insert(any(AlertLog.class));
    }

    @Test
    @DisplayName("异常防护：mapper 异常不抛出")
    void testExceptionSwallowed() {
        when(timeMonitorMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenThrow(new RuntimeException("数据库异常"));

        assertDoesNotThrow(() -> scheduler.checkAlertLevel());
    }
}
