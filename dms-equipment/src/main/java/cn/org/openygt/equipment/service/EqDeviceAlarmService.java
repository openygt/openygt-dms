package cn.org.openygt.equipment.service;

import cn.org.openygt.equipment.entity.EqAlarmNotification;
import cn.org.openygt.equipment.entity.EqDevice;

import java.math.BigDecimal;

/**
 * 设备告警服务接口。
 *
 * <p>V2.0 变更：告警通知仅保留系统内渠道（IN_APP / 日志），语音拨号（VOICE）已下线。</p>
 */
public interface EqDeviceAlarmService {

    /**
     * 检查温度是否超阈值，触发或解除告警。
     * 温度超阈 → 创建告警（5 分钟冷却期抑制重复）
     * 温度正常 → 自动解除未解决告警
     */
    void checkTemperatureAlarm(EqDevice device, BigDecimal currentTemp);

    /**
     * 创建告警（供温度告警使用）。
     */
    void createAlarm(EqDevice device, String alarmType, String alarmLevel, String message);

    /**
     * 创建告警（供离线/故障等其他组件使用）。
     */
    void createAlarm(Long deviceId, String alarmType, String alarmLevel, String message);

    /**
     * 发送告警通知（V2.0 拦截并过滤 VOICE 类型）。
     */
    void sendNotification(EqAlarmNotification notification);
}
