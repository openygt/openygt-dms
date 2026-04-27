package cn.org.openygt.equipment.service.impl;

import cn.org.openygt.common.service.EquipmentService;
import cn.org.openygt.equipment.entity.EqAlarmNotification;
import cn.org.openygt.equipment.entity.EqDevice;
import cn.org.openygt.equipment.entity.EqDeviceAlarm;
import cn.org.openygt.equipment.mapper.EqAlarmNotificationMapper;
import cn.org.openygt.equipment.mapper.EqDeviceAlarmMapper;
import cn.org.openygt.equipment.service.EqDeviceAlarmService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 设备告警服务实现。
 *
 * <p>温度告警冷却期：同一设备同一类型告警，5 分钟内不重复触发。
 * 温度恢复正常：自动解除未解决的超温/低温告警。</p>
 *
 * <p>V2.0 变更：
 * <ul>
 *   <li>告警创建时同步写入站内通知（eq_alarm_notification，类型 IN_APP）</li>
 *   <li>语音拨号（VOICE）通道已下线，外部调用 sendNotification 若传入 VOICE 会被强制降级为 IN_APP</li>
 * </ul></p>
 */
@Service
public class EqDeviceAlarmServiceImpl implements EqDeviceAlarmService {

    private static final Logger log = LoggerFactory.getLogger(EqDeviceAlarmServiceImpl.class);

    /** 冷却期：同一设备同类告警 5 分钟内不重复触发 */
    private static final long COOLDOWN_MINUTES = 5;

    private final EqDeviceAlarmMapper alarmMapper;
    private final EqAlarmNotificationMapper notificationMapper;
    private final EquipmentService equipmentService;

    public EqDeviceAlarmServiceImpl(EqDeviceAlarmMapper alarmMapper,
                                    EqAlarmNotificationMapper notificationMapper,
                                    @Lazy EquipmentService equipmentService) {
        this.alarmMapper = alarmMapper;
        this.notificationMapper = notificationMapper;
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
        doCreateAlarm(device.getId(), alarmType, alarmLevel, message);
    }

    @Override
    @Transactional
    public void createAlarm(Long deviceId, String alarmType, String alarmLevel, String message) {
        doCreateAlarm(deviceId, alarmType, alarmLevel, message);
    }

    private void doCreateAlarm(Long deviceId, String alarmType, String alarmLevel, String message) {
        // 检查冷却期：同一设备同类告警在冷却期内不重复触发
        EqDeviceAlarm latest = alarmMapper.findLatestActiveAlarm(deviceId, alarmType);
        if (latest != null) {
            LocalDateTime cooldownEnd = latest.getCreatedAt().plusMinutes(COOLDOWN_MINUTES);
            if (cooldownEnd.isAfter(LocalDateTime.now())) {
                log.debug("告警冷却期内跳过: deviceId={}, type={}", deviceId, alarmType);
                return;
            }
        }

        EqDeviceAlarm alarm = new EqDeviceAlarm();
        alarm.setDeviceId(deviceId);
        alarm.setAlarmType(alarmType);
        alarm.setAlarmLevel(alarmLevel);
        alarm.setMessage(message);
        alarm.setIsResolved(0);
        alarmMapper.insert(alarm);

        // V2.0：同步写入站内通知（IN_APP），语音拨号已下线
        createInAppNotification(alarm);

        log.warn("设备告警: deviceId={}, type={}, level={}, msg={}",
                deviceId, alarmType, alarmLevel, message);
    }

    @Override
    @Transactional
    public void sendNotification(EqAlarmNotification notification) {
        // V2.0 拦截并过滤语音拨号调用路径
        String filteredType = filterVoiceNotifyType(notification.getNotifyType());
        notification.setNotifyType(filteredType);
        notification.setSendTime(LocalDateTime.now());
        notification.setSendStatus("SENT");
        notificationMapper.insert(notification);
    }

    private void createInAppNotification(EqDeviceAlarm alarm) {
        EqAlarmNotification n = new EqAlarmNotification();
        n.setAlarmId(alarm.getId());
        n.setNotifyType("IN_APP");
        n.setNotifyContent(alarm.getMessage());
        n.setSendStatus("SENT");
        n.setSendTime(LocalDateTime.now());
        notificationMapper.insert(n);
    }

    /**
     * 过滤语音拨号类型。V2.0 不再支持 VOICE 通道，强制降级为 IN_APP。
     */
    private String filterVoiceNotifyType(String type) {
        if (type != null && type.toUpperCase().contains("VOICE")) {
            log.warn("语音拨号通知已被过滤并降级为系统内通知。原始类型={}", type);
            return "IN_APP";
        }
        return type;
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
