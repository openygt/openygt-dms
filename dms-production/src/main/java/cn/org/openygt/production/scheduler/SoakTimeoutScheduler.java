package cn.org.openygt.production.scheduler;

import cn.org.openygt.common.service.SysConfigService;
import cn.org.openygt.production.entity.Task;
import cn.org.openygt.production.mapper.TaskMapper;
import cn.org.openygt.production.service.TaskService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class SoakTimeoutScheduler {

    private final TaskMapper taskMapper;
    private final TaskService taskService;
    private final SysConfigService sysConfigService;

    private static final String CFG_SOAK_TIMEOUT = "soak.timeout.minutes";
    private static final int DEFAULT_SOAK_TIMEOUT_MINUTES = 30;

    @Scheduled(fixedRate = 60000)
    @SchedulerLock(name = "soakTimeoutCheck", lockAtMostFor = "5m", lockAtLeastFor = "30s")
    public void checkSoakTimeout() {
        try {
            int timeoutMinutes = sysConfigService.getIntValue(CFG_SOAK_TIMEOUT, DEFAULT_SOAK_TIMEOUT_MINUTES);

            LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Task::getStatus, cn.org.openygt.common.enums.TaskStatus.SOAKING.getLabel());
            List<Task> tasks = taskMapper.selectList(wrapper);

            LocalDateTime now = LocalDateTime.now();
            for (Task task : tasks) {
                if (task.getSoakStartTime() == null || task.getSoakDuration() == null) {
                    continue;
                }
                long elapsedMin = ChronoUnit.MINUTES.between(task.getSoakStartTime(), now);
                if (elapsedMin >= timeoutMinutes || elapsedMin >= task.getSoakDuration()) {
                    try {
                        log.info("泡药超时自动推进: taskId={}, elapsedMin={}, threshold={}",
                                task.getId(), elapsedMin, Math.min(timeoutMinutes, task.getSoakDuration()));
                        taskService.endSoak(task.getId(), "SYSTEM");
                    } catch (Exception e) {
                        log.error("泡药超时自动推进失败: taskId={}", task.getId(), e);
                    }
                }
            }
        } catch (Exception e) {
            log.debug("泡药超时检查跳过: {}", e.getMessage());
        }
    }
}
