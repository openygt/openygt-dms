package cn.org.openygt.production.scheduler;

import cn.org.openygt.production.entity.Task;
import cn.org.openygt.production.mapper.TaskMapper;
import cn.org.openygt.production.service.TaskService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class SoakTimeoutScheduler {

    private final TaskMapper taskMapper;
    private final TaskService taskService;

    @Scheduled(fixedRate = 60000)
    public void checkSoakTimeout() {
        try {
            LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Task::getStatus, "泡药中");
            List<Task> tasks = taskMapper.selectList(wrapper);

            Date now = new Date();
            for (Task task : tasks) {
                if (task.getSoakStartTime() == null || task.getSoakDuration() == null) {
                    continue;
                }
                long elapsedMs = now.getTime() - task.getSoakStartTime().getTime();
                long elapsedMin = TimeUnit.MILLISECONDS.toMinutes(elapsedMs);
                if (elapsedMin >= task.getSoakDuration()) {
                    try {
                        log.info("泡药超时自动推进: taskId={}, elapsedMin={}, soakDuration={}",
                                task.getId(), elapsedMin, task.getSoakDuration());
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
