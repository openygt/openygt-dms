package cn.org.openygt.equipment.service.impl;

import cn.org.openygt.common.service.EquipmentService;
import cn.org.openygt.equipment.entity.EqDevice;
import cn.org.openygt.equipment.entity.EqDeviceAlarm;
import cn.org.openygt.equipment.mapper.EqDeviceAlarmMapper;
import cn.org.openygt.equipment.service.EqDeviceAlarmService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 设备告警服务实现。
 *
 * <p>温度告警冷却期：同一设备同一类型告警，5 分钟内不重复触发。
 * 温度恢复正常：自动解除未解决的超温/低温告警。</p>
 */
@Service
public class EqDeviceAlarmServiceImpl implements EqDeviceAlarmService {

    private static final Logger log = LoggerFactory.getLogger(EqDeviceAlarmServiceImpl.class);

    /** 冷却期：同一设备同类告警 5 分钟内不重复触发 */
    private static final long COOLDOWN_MINUTES = 5;

    private final EqDeviceAlarmMapper alarmMapper;
    private final EquipmentService equipmentService;

    public EqDeviceAlarmServiceImpl(EqDeviceAlarmMapper alarmMapper, EquipmentService equipmentService) {
        this.alarmMapper = alarmMapper;
        this.equipmentService = equipmentService;
    }

    @Override
    @Transactional
    public void checkTemperatureAlarm(EqDevice device, BigDecimal currentTemp) {
        cn.org.openygt.common.dto.TemperatureThresholdDTO threshold = equipmentService.getEffectiveThreshold(device.getId());
        BigDecimal high = threshold.getHighTemp();
        BigDecimal low = threshold.getLowTemp();

        if (high != null && currentTemp.compareTo(high) > 0) {
            createAlarm(device, "HIGH_TEMP", "CRITICAL",
                    String.format("设备超温: 当前 %.1f℃ > 阈值 %.1f℃ [来源=%s]",
                            currentTemp, high, threshold.getSource()));
        } else if (low != null && currentTemp.compareTo(low) < 0) {
            createAlarm(device, "LOW_TEMP", "WARNING",
                    String.format("设备低温: 当前 %.1f℃ < 阈值 %.1f℃ [来源=%s]",
                            currentTemp, low, threshold.getSource()));
        } else {
            // 温度恢复正常 → 自动解除未解决告警
            resolveActiveAlarms(device.getId(), "HIGH_TEMP");
            resolveActiveAlarms(device.getId(), "LOW_TEMP");
        }
    }

    @Override
    @Transactional
    public void createAlarm(EqDevice device, String alarmType, String alarmLevel, String message) {
        // 检查冷却期：同一设备同类告警在冷却期内不重复触发
        EqDeviceAlarm latest = alarmMapper.findLatestActiveAlarm(device.getId(), alarmType);
        if (latest != null) {
            LocalDateTime cooldownEnd = latest.getCreatedAt().plusMinutes(COOLDOWN_MINUTES);
            if (cooldownEnd.isAfter(LocalDateTime.now())) {
                log.debug("告警冷却期内跳过: deviceCode={}, type={}", device.getDeviceCode(), alarmType);
                return;
            }
        }

        EqDeviceAlarm alarm = new EqDeviceAlarm();
        alarm.setDeviceId(device.getId());
        alarm.setAlarmType(alarmType);
        alarm.setAlarmLevel(alarmLevel);
        alarm.setMessage(message);
        alarm.setIsResolved(0);
        alarmMapper.insert(alarm);

        log.warn("设备告警: deviceCode={}, type={}, level={}, msg={}",
                device.getDeviceCode(), alarmType, alarmLevel, message);
    }

    private void resolveActiveAlarms(Long deviceId, String alarmType) {
        alarmMapper.findActiveByDeviceIdAndType(deviceId, alarmType)
                .forEach(alarm -> {
                    alarm.setIsResolved(1);
                    alarm.setResolvedAt(LocalDateTime.now());
                    alarmMapper.updateById(alarm);
                    log.info("温度正常，告警自动解除: deviceId={}, type={}", deviceId, alarmType);
                });
    }
}
