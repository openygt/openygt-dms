package cn.org.openygt.production.scheduler;

import cn.org.openygt.production.entity.TaskAssignment;
import cn.org.openygt.production.entity.TimeMonitor;
import cn.org.openygt.production.mapper.TaskAssignmentMapper;
import cn.org.openygt.production.mapper.TimeMonitorMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class TimeMonitorInitScheduler {

    private final TaskAssignmentMapper taskAssignmentMapper;
    private final TimeMonitorMapper timeMonitorMapper;

    @Scheduled(fixedRate = 60000)
    @SchedulerLock(name = "timeMonitorInit", lockAtMostFor = "5m", lockAtLeastFor = "30s")
    public void initMonitors() {
        try {
            // 查询所有待执行/执行中的分配记录
            List<TaskAssignment> assignments = taskAssignmentMapper.selectList(
                    new LambdaQueryWrapper<TaskAssignment>()
                            .in(TaskAssignment::getStatus, 1, 2));

            if (assignments.isEmpty()) {
                return;
            }

            // 查询已有的监控记录对应的taskId
            List<TimeMonitor> existing = timeMonitorMapper.selectList(
                    new LambdaQueryWrapper<TimeMonitor>()
                            .select(TimeMonitor::getTaskId));
            Set<Long> monitoredTaskIds = existing.stream()
                    .map(TimeMonitor::getTaskId)
                    .collect(Collectors.toSet());

            LocalDateTime now = LocalDateTime.now();

            for (TaskAssignment a : assignments) {
                if (monitoredTaskIds.contains(a.getTaskId())) {
                    continue;
                }

                LocalDateTime plannedStart = a.getScheduledStartTime();
                LocalDateTime plannedEnd = a.getScheduledEndTime();

                if (plannedStart == null) {
                    plannedStart = now;
                }
                if (plannedEnd == null) {
                    plannedEnd = plannedStart.plusHours(8);
                }

                TimeMonitor monitor = new TimeMonitor();
                monitor.setTaskId(a.getTaskId());
                monitor.setAssignmentId(a.getId());
                monitor.setPrescriptionId(a.getPrescriptionId());
                monitor.setStage("DECOCT");
                monitor.setPlannedStart(plannedStart);
                monitor.setPlannedEnd(plannedEnd);
                monitor.setActualStart(null);
                monitor.setActualEnd(null);
                monitor.setRemainingSeconds((int) Math.max(0, ChronoUnit.SECONDS.between(now, plannedEnd)));
                monitor.setStatus(2);
                monitor.setAlertLevel(0);
                monitor.setWarningCount(0);
                monitor.setCreatedAt(now);
                monitor.setUpdatedAt(now);
                timeMonitorMapper.insert(monitor);
                log.info("创建时效监控: taskId={}, assignmentId={}, plannedStart={}, plannedEnd={}",
                        a.getTaskId(), a.getId(), plannedStart, plannedEnd);
            }
        } catch (Exception e) {
            log.error("时效监控初始化异常", e);
        }
    }
}
