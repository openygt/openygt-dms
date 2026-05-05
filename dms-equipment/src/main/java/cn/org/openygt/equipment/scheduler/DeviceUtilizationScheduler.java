package cn.org.openygt.equipment.scheduler;

import cn.org.openygt.equipment.service.DeviceUtilizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 设备利用率日统计定时任务。
 *
 * <p>每日凌晨 01:00 生成前一天的设备利用率数据。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceUtilizationScheduler {

    private final DeviceUtilizationService utilizationService;

    @Scheduled(cron = "0 0 1 * * ?")
    @SchedulerLock(name = "deviceUtilizationDaily", lockAtMostFor = "30m", lockAtLeastFor = "5m")
    public void generateYesterdayStats() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        log.info("开始生成 {} 设备利用率统计", yesterday);
        try {
            utilizationService.generateDailyStats(yesterday);
            log.info("完成生成 {} 设备利用率统计", yesterday);
        } catch (Exception e) {
            log.error("生成 {} 设备利用率统计失败: {}", yesterday, e.getMessage(), e);
        }
    }
}
