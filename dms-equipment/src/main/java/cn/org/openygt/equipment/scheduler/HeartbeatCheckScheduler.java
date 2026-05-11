package cn.org.openygt.equipment.scheduler;

import cn.org.openygt.common.service.SysConfigService;
import cn.org.openygt.equipment.entity.EqDevice;
import cn.org.openygt.equipment.mapper.EqDeviceMapper;
import cn.org.openygt.equipment.service.EqDeviceAlarmService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 设备心跳超时检测定时任务。
 *
 * <p>每 30 秒扫描一次，根据设备状态取不同超时阈值：</p>
 * <ul>
 *   <li>IDLE / FAULT：120 秒（快速发现离线）</li>
 *   <li>RUNNING：600 秒（运行中允许更长超时）</li>
 *   <li>MAINTENANCE：不检测</li>
 * </ul>
 *
 * <p>阈值可从 sys_config 读取，配置键为 {@code device.heartbeat.idle.timeout}
 * 和 {@code device.heartbeat.running.timeout}。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HeartbeatCheckScheduler {

    private final EqDeviceMapper eqDeviceMapper;
    private final SysConfigService sysConfigService;
    private final EqDeviceAlarmService alarmService;

    // 默认超时秒数
    private static final long DEFAULT_IDLE_TIMEOUT_SECONDS = 120;
    private static final long DEFAULT_RUNNING_TIMEOUT_SECONDS = 600;

    private static final String CFG_IDLE_TIMEOUT = "device.heartbeat.idle.timeout";
    private static final String CFG_RUNNING_TIMEOUT = "device.heartbeat.running.timeout";

    @Scheduled(fixedRate = 30000)
    @SchedulerLock(name = "heartbeatCheck", lockAtMostFor = "2m", lockAtLeastFor = "10s")
    @Transactional
    public void checkHeartbeatTimeout() {
        LocalDateTime now = LocalDateTime.now();

        long idleTimeout = sysConfigService.getIntValue(CFG_IDLE_TIMEOUT, (int) DEFAULT_IDLE_TIMEOUT_SECONDS);
        long runningTimeout = sysConfigService.getIntValue(CFG_RUNNING_TIMEOUT, (int) DEFAULT_RUNNING_TIMEOUT_SECONDS);

        LambdaQueryWrapper<EqDevice> wrapper = new LambdaQueryWrapper<>();
        wrapper.ne(EqDevice::getStatus, "OFFLINE")
               .ne(EqDevice::getStatus, "MAINTENANCE");
        List<EqDevice> devices = eqDeviceMapper.selectList(wrapper);

        int offlineCount = 0;
        for (EqDevice device : devices) {
            long timeout = getTimeoutSeconds(device.getStatus(), idleTimeout, runningTimeout);
            LocalDateTime lastHb = device.getLastHeartbeat();

            if (lastHb == null) {
                // 从未上报过心跳，视为离线
                markOffline(device, now);
                offlineCount++;
                continue;
            }

            if (lastHb.plusSeconds(timeout).isBefore(now)) {
                markOffline(device, now);
                offlineCount++;
            }
        }

        if (offlineCount > 0) {
            log.warn("心跳检测完成，标记 {} 台设备离线", offlineCount);
        }
    }

    private long getTimeoutSeconds(String status, long idleTimeout, long runningTimeout) {
        if (status == null) return idleTimeout;
        switch (status.toUpperCase()) {
            case "IDLE":
            case "FAULT":
                return idleTimeout;
            case "RUNNING":
            case "BUSY":
                return runningTimeout;
            default:
                return idleTimeout;
        }
    }

    private void markOffline(EqDevice device, LocalDateTime now) {
        log.warn("设备心跳超时离线: deviceCode={}, status={}, lastHeartbeat={}",
                device.getDeviceCode(), device.getStatus(), device.getLastHeartbeat());

        device.setStatus("OFFLINE");
        device.setCurrentTemp(null);
        device.setUpdatedAt(now);
        eqDeviceMapper.updateById(device);

        // V2.0：创建离线告警（系统内通知，不触发语音拨号）
        alarmService.createAlarm(device.getId(), "OFFLINE", "WARNING",
                String.format("设备 %s 心跳超时离线", device.getDeviceCode()));
    }
}
