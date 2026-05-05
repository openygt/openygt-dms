package cn.org.openygt.production.scheduler;

import cn.org.openygt.production.entity.Task;
import cn.org.openygt.production.entity.TaskAssignment;
import cn.org.openygt.production.entity.TimeMonitor;
import cn.org.openygt.production.mapper.TaskMapper;
import cn.org.openygt.production.mapper.TaskAssignmentMapper;
import cn.org.openygt.production.mapper.TimeMonitorMapper;
import cn.org.openygt.production.service.impl.TimeMonitorRuleResolver;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class TimeMonitorInitScheduler {

    private static final List<String> ACTIVE_TASK_STATUSES = Arrays.asList(
            "待泡药", "泡药中",
            "待煎药", "煎药中",
            "待出液", "出液中",
            "待包装", "包装中"
    );

    private final TaskAssignmentMapper taskAssignmentMapper;
    private final TimeMonitorMapper timeMonitorMapper;
    private final TaskMapper taskMapper;
    private final TimeMonitorRuleResolver timeMonitorRuleResolver;

    @Scheduled(fixedRate = 60000)
    @SchedulerLock(name = "timeMonitorInit", lockAtMostFor = "5m", lockAtLeastFor = "30s")
    public void initMonitors() {
        try {
            List<TaskAssignment> assignments = taskAssignmentMapper.selectList(
                    new LambdaQueryWrapper<TaskAssignment>()
                            .in(TaskAssignment::getStatus, 1, 2));
            LocalDateTime now = LocalDateTime.now();
            Map<Long, TaskAssignment> activeAssignmentMap = assignments.stream()
                    .filter(a -> a.getTaskId() != null)
                    .collect(Collectors.toMap(TaskAssignment::getTaskId, a -> a, this::pickNewerAssignment, HashMap::new));

            List<Task> activeTasks = taskMapper.selectList(new LambdaQueryWrapper<Task>()
                    .in(Task::getStatus, ACTIVE_TASK_STATUSES));
            Map<Long, Task> activeTaskMap = activeTasks.stream()
                    .filter(t -> t.getId() != null)
                    .collect(Collectors.toMap(Task::getId, t -> t, (left, right) -> right, HashMap::new));

            List<TimeMonitor> openMonitors = timeMonitorMapper.selectList(
                    new LambdaQueryWrapper<TimeMonitor>().isNull(TimeMonitor::getActualEnd));
            Map<Long, List<TimeMonitor>> openMonitorMap = openMonitors.stream()
                    .filter(m -> m.getTaskId() != null)
                    .collect(Collectors.groupingBy(TimeMonitor::getTaskId));

            Set<Long> taskIds = new HashSet<>(openMonitorMap.keySet());
            taskIds.addAll(activeAssignmentMap.keySet());
            taskIds.addAll(activeTaskMap.keySet());

            for (Long taskId : taskIds) {
                TaskAssignment assignment = activeAssignmentMap.get(taskId);
                Task task = activeTaskMap.get(taskId);
                if (task == null && openMonitorMap.containsKey(taskId)) {
                    task = taskMapper.selectById(taskId);
                }
                if (task == null) {
                    closeActiveMonitors(openMonitorMap.get(taskId), null, now);
                    continue;
                }

                TimeMonitorRuleResolver.StageContext context = timeMonitorRuleResolver.resolveStageContext(task, assignment, now);
                List<TimeMonitor> taskOpenMonitors = openMonitorMap.get(taskId);
                TimeMonitor activeMonitor = pickActiveMonitor(taskOpenMonitors);
                boolean stageStarted = hasStageStarted(task, context);

                if (context == null) {
                    closeActiveMonitors(taskOpenMonitors, task, now);
                    continue;
                }

                if (taskOpenMonitors != null && taskOpenMonitors.size() > 1) {
                    closeDuplicateMonitors(taskOpenMonitors, activeMonitor, task, now);
                }

                if (!stageStarted) {
                    closeActiveMonitors(taskOpenMonitors, task, now);
                    continue;
                }

                if (activeMonitor != null && !context.getStage().equals(activeMonitor.getStage())) {
                    completeMonitor(activeMonitor, task, now);
                    activeMonitor = null;
                }

                if (activeMonitor == null) {
                    createMonitor(task, assignment, context, now);
                    continue;
                }

                refreshMonitor(activeMonitor, assignment, context, now);
            }
        } catch (Exception e) {
            log.error("时效监控初始化异常", e);
        }
    }

    private boolean hasStageStarted(Task task, TimeMonitorRuleResolver.StageContext context) {
        if (task == null || context == null) {
            return false;
        }
        return timeMonitorRuleResolver.resolveActualStart(task, context.getStage()) != null;
    }

    private TaskAssignment pickNewerAssignment(TaskAssignment left, TaskAssignment right) {
        LocalDateTime leftTime = left.getUpdatedAt() != null ? left.getUpdatedAt() : left.getCreatedAt();
        LocalDateTime rightTime = right.getUpdatedAt() != null ? right.getUpdatedAt() : right.getCreatedAt();
        if (leftTime == null) {
            return right;
        }
        if (rightTime == null) {
            return left;
        }
        return rightTime.isAfter(leftTime) ? right : left;
    }

    private TimeMonitor pickActiveMonitor(List<TimeMonitor> monitors) {
        if (monitors == null || monitors.isEmpty()) {
            return null;
        }
        return monitors.stream()
                .max(Comparator
                        .comparing((TimeMonitor m) -> m.getUpdatedAt() != null ? m.getUpdatedAt() : m.getCreatedAt(),
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(TimeMonitor::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);
    }

    private void closeActiveMonitors(List<TimeMonitor> monitors, Task task, LocalDateTime now) {
        if (monitors == null || monitors.isEmpty()) {
            return;
        }
        for (TimeMonitor monitor : monitors) {
            completeMonitor(monitor, task, now);
        }
    }

    private void closeDuplicateMonitors(List<TimeMonitor> monitors, TimeMonitor keep, Task task, LocalDateTime now) {
        for (TimeMonitor monitor : monitors) {
            if (keep != null && keep.getId() != null && keep.getId().equals(monitor.getId())) {
                continue;
            }
            completeMonitor(monitor, task, now);
        }
    }

    private void completeMonitor(TimeMonitor monitor, Task task, LocalDateTime now) {
        if (monitor == null || monitor.getActualEnd() != null) {
            return;
        }
        monitor.setActualEnd(resolveActualEnd(task, monitor.getStage(), now));
        monitor.setRemainingSeconds(0);
        monitor.setStatus(4);
        monitor.setUpdatedAt(now);
        timeMonitorMapper.updateById(monitor);
    }

    private LocalDateTime resolveActualEnd(Task task, String stage, LocalDateTime now) {
        LocalDateTime actualEnd = task != null ? timeMonitorRuleResolver.resolveActualEnd(task, stage) : null;
        return actualEnd != null ? actualEnd : now;
    }

    private void createMonitor(Task task,
                               TaskAssignment assignment,
                               TimeMonitorRuleResolver.StageContext context,
                               LocalDateTime now) {
        TimeMonitor monitor = new TimeMonitor();
        monitor.setTaskId(task.getId());
        monitor.setAssignmentId(assignment != null ? assignment.getId() : null);
        monitor.setPrescriptionId(task.getPrescriptionId());
        monitor.setStage(context.getStage());
        monitor.setPlannedStart(context.getPlannedStart());
        monitor.setPlannedEnd(context.getPlannedEnd());
        monitor.setActualStart(context.getActualStart());
        monitor.setActualEnd(null);
        monitor.setRemainingSeconds(calculateRemainingSeconds(now, context.getPlannedEnd()));
        monitor.setStatus(context.getStatus());
        monitor.setAlertLevel(0);
        monitor.setWarningCount(0);
        monitor.setCreatedAt(now);
        monitor.setUpdatedAt(now);
        timeMonitorMapper.insert(monitor);
        log.info("创建时效监控: taskId={}, assignmentId={}, stage={}, plannedStart={}, plannedEnd={}",
                task.getId(), assignment != null ? assignment.getId() : null, context.getStage(), context.getPlannedStart(), context.getPlannedEnd());
    }

    private void refreshMonitor(TimeMonitor monitor,
                                TaskAssignment assignment,
                                TimeMonitorRuleResolver.StageContext context,
                                LocalDateTime now) {
        monitor.setAssignmentId(assignment != null ? assignment.getId() : monitor.getAssignmentId());
        monitor.setStage(context.getStage());
        monitor.setPlannedStart(context.getPlannedStart());
        monitor.setPlannedEnd(context.getPlannedEnd());
        monitor.setActualStart(context.getActualStart());
        monitor.setRemainingSeconds(calculateRemainingSeconds(now, context.getPlannedEnd()));
        if (monitor.getStatus() == null || monitor.getStatus() == 1 || monitor.getStatus() == 2) {
            monitor.setStatus(context.getStatus());
        }
        monitor.setUpdatedAt(now);
        timeMonitorMapper.updateById(monitor);
    }

    private int calculateRemainingSeconds(LocalDateTime now, LocalDateTime plannedEnd) {
        if (plannedEnd == null) {
            return 0;
        }
        return (int) ChronoUnit.SECONDS.between(now, plannedEnd);
    }
}
