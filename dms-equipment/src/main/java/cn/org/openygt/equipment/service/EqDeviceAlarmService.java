package cn.org.openygt.equipment.service;

import cn.org.openygt.equipment.entity.EqDevice;

import java.math.BigDecimal;

/**
 * 设备告警服务接口。
 */
public interface EqDeviceAlarmService {

    /**
     * 检查温度是否超阈值，触发或解除告警。
     * 温度超阈 → 创建告警（5 分钟冷却期抑制重复）
     * 温度正常 → 自动解除未解决告警
     */
    void checkTemperatureAlarm(EqDevice device, BigDecimal currentTemp);

    /**
     * 创建告警（供 HeartbeatCheckScheduler 等其他组件调用）。
     */
    void createAlarm(EqDevice device, String alarmType, String alarmLevel, String message);
}
