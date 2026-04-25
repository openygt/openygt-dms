package cn.org.openygt.common.service;

import java.math.BigDecimal;

/**
 * 设备物联模块对外服务接口。
 * 定义在 dms-common，由 dms-equipment 实现。
 * 其他模块（如 dms-production）通过注入此接口进行跨模块设备写操作。
 */
public interface EquipmentService {

    Object getDeviceByCode(String deviceCode);

    Object getDeviceById(Long deviceId);

    Object getOrCreateDevice(String deviceCode, int defaultType);

    void updateDeviceStatus(Long deviceId, String status);

    void updateTemperature(Long deviceId, BigDecimal temperature);

    void reportFault(Long deviceId, String faultCode, String message);

    void clearFault(Long deviceId);

    void releaseDevice(Long deviceId);

    void checkTemperatureAlarm(Long deviceId, BigDecimal temperature);

    String getDeviceStatus(Long deviceId);

    String getAutoLevel(Long deviceId);

    String getDeviceCode(Long deviceId);

    Long getDeviceId(String deviceCode);
}
