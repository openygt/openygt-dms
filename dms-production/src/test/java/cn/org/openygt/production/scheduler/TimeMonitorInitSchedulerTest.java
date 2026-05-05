package cn.org.openygt.production.scheduler;

import cn.org.openygt.production.entity.Task;
import cn.org.openygt.production.entity.TimeMonitor;
import cn.org.openygt.production.mapper.TaskAssignmentMapper;
import cn.org.openygt.production.mapper.TaskMapper;
import cn.org.openygt.production.mapper.TimeMonitorMapper;
import cn.org.openygt.production.service.impl.TimeMonitorRuleResolver;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimeMonitorInitSchedulerTest {

    @Mock
    private TaskAssignmentMapper taskAssignmentMapper;
    @Mock
    private TimeMonitorMapper timeMonitorMapper;
    @Mock
    private TaskMapper taskMapper;
    @Mock
    private TimeMonitorRuleResolver timeMonitorRuleResolver;

    @InjectMocks
    private TimeMonitorInitScheduler scheduler;

    @Test
    @DisplayName("阶段未实际开始时不创建时效监控")
    void shouldNotCreateMonitorBeforeActualStart() {
        Task task = new Task();
        task.setId(49L);
        task.setStatus("待煎药");
        task.setPrescriptionId(53L);
        task.setSoakEndTime(LocalDateTime.now().minusMinutes(1));

        TimeMonitorRuleResolver.StageContext context = new TimeMonitorRuleResolver.StageContext(
                "DECOCT",
                LocalDateTime.now().minusMinutes(1),
                LocalDateTime.now().plusMinutes(29),
                null,
                1,
                null
        );

        when(taskAssignmentMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());
        when(taskMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.singletonList(task));
        when(timeMonitorMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());
        when(timeMonitorRuleResolver.resolveStageContext(any(Task.class), any(), any(LocalDateTime.class))).thenReturn(context);
        when(timeMonitorRuleResolver.resolveActualStart(any(Task.class), any(String.class))).thenReturn(null);

        scheduler.initMonitors();

        verify(timeMonitorMapper, never()).insert(any(TimeMonitor.class));
        verify(timeMonitorMapper, never()).updateById(any(TimeMonitor.class));
    }
}
