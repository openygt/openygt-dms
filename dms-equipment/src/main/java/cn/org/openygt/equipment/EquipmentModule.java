package cn.org.openygt.equipment;

/**
 * 设备物联模块 (Equipment IoT)
 *
 * 表前缀: eq_
 * API前缀: /api/v1/eq/
 *
 * 职责:
 * - eq_device: 设备注册/管理
 * - eq_device_connection: 设备配对
 * - eq_device_alarm: 设备告警
 * - eq_temperature_log: 温度记录
 * - MQTT适配器、TCP适配器
 * - 数字孪生模拟器
 */
public final class EquipmentModule {
    private EquipmentModule() {}
    public static final String TABLE_PREFIX = "eq_";
    public static final String API_PREFIX = "/api/v1/eq";
}
