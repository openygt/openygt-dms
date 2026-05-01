package cn.org.openygt.production.scheduler;

import cn.org.openygt.production.entity.AlertLog;
import cn.org.openygt.production.entity.TimeMonitor;
import cn.org.openygt.production.mapper.AlertLogMapper;
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

/**
 * 时效监控预警升级定时任务。
 * <p>每分钟扫描一次，根据超时时间分级通知。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TimeMonitorAlertScheduler {

    private final TimeMonitorMapper timeMonitorMapper;
    private final AlertLogMapper alertLogMapper;

    @Scheduled(fixedRate = 60000)
    @SchedulerLock(name = "timeMonitorAlertCheck", lockAtMostFor = "5m", lockAtLeastFor = "30s")
    public void checkAlertLevel() {
        try {
            LambdaQueryWrapper<TimeMonitor> wrapper = new LambdaQueryWrapper<>();
            wrapper.isNull(TimeMonitor::getActualEnd); // 未完成的监控实例
            List<TimeMonitor> monitors = timeMonitorMapper.selectList(wrapper);

            LocalDateTime now = LocalDateTime.now();
            for (TimeMonitor monitor : monitors) {
                if (monitor.getPlannedEnd() == null || monitor.getStatus() == null) {
                    continue;
                }
                // 只处理运行中的任务 (status=2 执行中)
                if (monitor.getStatus() != 2) {
                    continue;
                }

                long minutesOverdue = ChronoUnit.MINUTES.between(monitor.getPlannedEnd(), now);
                int currentLevel = monitor.getAlertLevel() != null ? monitor.getAlertLevel() : 0;

                if (minutesOverdue >= 30 && currentLevel < 4) {
                    // 严重告警：全员通知 + 自动记录异常
                    sendAlert(monitor, 4, "严重告警", "时效严重超时，已超过计划结束时间30分钟，请立即处理！");
                    monitor.setAlertLevel(4);
                    monitor.setStatus(3); // 标记为已超时
                } else if (minutesOverdue >= 10 && currentLevel < 3) {
                    // 告警：通知主任
                    sendAlert(monitor, 3, "告警", "时效超时已超过10分钟，请班组长/主任关注！");
                    monitor.setAlertLevel(3);
                } else if (minutesOverdue >= 0 && currentLevel < 2) {
                    // 超时：通知班组长
                    sendAlert(monitor, 2, "超时", "任务已超时，请尽快处理！");
                    monitor.setAlertLevel(2);
                    monitor.setStatus(3); // 标记为已超时
                } else if (minutesOverdue >= -5 && currentLevel < 1) {
                    // 预警：提前5分钟通知操作员
                    sendAlert(monitor, 1, "预警", "任务即将超时，还剩" + Math.abs(minutesOverdue) + "分钟！");
                    monitor.setAlertLevel(1);
                }

                // 更新告警次数和时间
                if (monitor.getAlertLevel() != null && monitor.getAlertLevel() > currentLevel) {
                    monitor.setWarningCount((monitor.getWarningCount() != null ? monitor.getWarningCount() : 0) + 1);
                    monitor.setLastWarningTime(now);
                    timeMonitorMapper.updateById(monitor);
                }
            }
        } catch (Exception e) {
            log.error("时效预警检查异常", e);
        }
    }

    private void sendAlert(TimeMonitor monitor, int level, String levelName, String content) {
        AlertLog alert = new AlertLog();
        alert.setMonitorId(monitor.getId());
        alert.setTaskId(monitor.getTaskId());
        alert.setAlertLevel(level);
        alert.setAlertType("TIMEOUT");
        alert.setAlertContent(content);
        alert.setIsResolved(0);
        alert.setCreatedAt(LocalDateTime.now());
        alertLogMapper.insert(alert);
        log.info("时效{}: monitorId={}, taskId={}, stage={}, level={}",
                levelName, monitor.getId(), monitor.getTaskId(), monitor.getStage(), level);
    }
}
