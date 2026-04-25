package cn.org.openygt.common.service;

import cn.org.openygt.common.dto.DeviceFaultStatDTO;
import cn.org.openygt.common.dto.DeviceUtilizationDTO;
import cn.org.openygt.common.dto.EqDeviceDTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 设备物联模块对外服务接口。
 * 定义在 dms-common，由 dms-equipment 实现。
 * 其他模块（如 dms-production）通过注入此接口进行跨模块设备写操作。
 */
public interface EquipmentService {

    EqDeviceDTO getDeviceByCode(String deviceCode);

    EqDeviceDTO getDeviceById(Long deviceId);

    EqDeviceDTO getOrCreateDevice(String deviceCode, int defaultType);

    void updateDeviceStatus(Long deviceId, String status);

    void updateTemperature(Long deviceId, BigDecimal temperature);

    void reportFault(Long deviceId, String faultCode, String message);

    void clearFault(Long deviceId);

    void releaseDevice(Long deviceId);

    void reserveDevice(Long taskId, Long deviceId);

    void checkTemperatureAlarm(Long deviceId, BigDecimal temperature);

    String getDeviceStatus(Long deviceId);

    String getAutoLevel(Long deviceId);

    String getDeviceCode(Long deviceId);

    Long getDeviceId(String deviceCode);

    // ---- 统计扩展（评审03新增，供 analytics 使用） ----

    Integer getOnlineDeviceCount();

    List<DeviceFaultStatDTO> getFaultStats(LocalDateTime start, LocalDateTime end);

    DeviceUtilizationDTO getDeviceUtilization(Long deviceId, LocalDateTime start, LocalDateTime end);
}
