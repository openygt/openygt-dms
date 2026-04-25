package cn.org.openygt.equipment.service.impl;

import cn.org.openygt.common.dto.DeviceFaultStatDTO;
import cn.org.openygt.common.dto.DeviceUtilizationDTO;
import cn.org.openygt.common.dto.EqDeviceDTO;
import cn.org.openygt.common.service.EquipmentService;
import cn.org.openygt.equipment.entity.EqDevice;
import cn.org.openygt.equipment.mapper.EqDeviceMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class EquipmentServiceImpl implements EquipmentService {

    private final EqDeviceMapper deviceMapper;

    public EquipmentServiceImpl(EqDeviceMapper deviceMapper) {
        this.deviceMapper = deviceMapper;
    }

    @Override
    public EqDeviceDTO getDeviceByCode(String deviceCode) {
        LambdaQueryWrapper<EqDevice> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EqDevice::getDeviceCode, deviceCode);
        EqDevice entity = deviceMapper.selectOne(wrapper);
        return toDTO(entity);
    }

    @Override
    public EqDeviceDTO getDeviceById(Long deviceId) {
        return toDTO(deviceMapper.selectById(deviceId));
    }

    @Override
    @Transactional
    public EqDeviceDTO getOrCreateDevice(String deviceCode, int defaultType) {
        EqDeviceDTO existing = getDeviceByCode(deviceCode);
        if (existing != null) {
            return existing;
        }
        EqDevice device = new EqDevice();
        device.setDeviceCode(deviceCode);
        device.setName(deviceCode);
        device.setDeviceType(defaultType);
        device.setStatus("IDLE");
        device.setEnabled(1);
        deviceMapper.insert(device);
        return toDTO(device);
    }

    @Override
    @Transactional
    public void updateDeviceStatus(Long deviceId, String status) {
        LambdaUpdateWrapper<EqDevice> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(EqDevice::getId, deviceId)
               .set(EqDevice::getStatus, status)
               .set(EqDevice::getUpdatedAt, LocalDateTime.now());
        deviceMapper.update(null, wrapper);
        log.info("设备状态更新: deviceId={}, status={}", deviceId, status);
    }

    @Override
    @Transactional
    public void updateTemperature(Long deviceId, BigDecimal temperature) {
        LambdaUpdateWrapper<EqDevice> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(EqDevice::getId, deviceId)
               .set(EqDevice::getCurrentTemp, temperature)
               .set(EqDevice::getUpdatedAt, LocalDateTime.now());
        deviceMapper.update(null, wrapper);
        checkTemperatureAlarm(deviceId, temperature);
    }

    @Override
    @Transactional
    public void reportFault(Long deviceId, String faultCode, String message) {
        LambdaUpdateWrapper<EqDevice> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(EqDevice::getId, deviceId)
               .set(EqDevice::getStatus, "FAULT")
               .set(EqDevice::getFaultCode, faultCode)
               .set(EqDevice::getAlertTime, LocalDateTime.now())
               .set(EqDevice::getUpdatedAt, LocalDateTime.now());
        deviceMapper.update(null, wrapper);
        log.warn("设备故障上报: deviceId={}, faultCode={}, message={}", deviceId, faultCode, message);
    }

    @Override
    @Transactional
    public void clearFault(Long deviceId) {
        LambdaUpdateWrapper<EqDevice> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(EqDevice::getId, deviceId)
               .set(EqDevice::getStatus, "IDLE")
               .set(EqDevice::getFaultCode, null)
               .set(EqDevice::getResolvedAt, LocalDateTime.now())
               .set(EqDevice::getUpdatedAt, LocalDateTime.now());
        deviceMapper.update(null, wrapper);
        log.info("设备故障清除: deviceId={}", deviceId);
    }

    @Override
    @Transactional
    public void releaseDevice(Long deviceId) {
        LambdaUpdateWrapper<EqDevice> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(EqDevice::getId, deviceId)
               .set(EqDevice::getStatus, "IDLE")
               .set(EqDevice::getUpdatedAt, LocalDateTime.now());
        deviceMapper.update(null, wrapper);
        log.info("设备释放: deviceId={}", deviceId);
    }

    @Override
    @Transactional
    public void reserveDevice(Long taskId, Long deviceId) {
        // MVP 阶段：预留即将返工的设备，防止被其他任务抢占
        EqDevice device = deviceMapper.selectById(deviceId);
        if (device == null) {
            throw new IllegalArgumentException("设备不存在: " + deviceId);
        }
        if (!"IDLE".equalsIgnoreCase(device.getStatus())) {
            throw new IllegalStateException("设备非空闲，无法预留: " + deviceId);
        }
        LambdaUpdateWrapper<EqDevice> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(EqDevice::getId, deviceId)
               .set(EqDevice::getStatus, "RESERVED")
               .set(EqDevice::getUpdatedAt, LocalDateTime.now());
        deviceMapper.update(null, wrapper);
        log.info("设备预留: deviceId={}, taskId={}", deviceId, taskId);
    }

    @Override
    public void checkTemperatureAlarm(Long deviceId, BigDecimal temperature) {
        EqDevice device = deviceMapper.selectById(deviceId);
        if (device == null) {
            return;
        }
        boolean alarm = false;
        if (device.getAlarmMinTemp() != null && temperature.compareTo(device.getAlarmMinTemp()) < 0) {
            alarm = true;
        }
        if (device.getAlarmMaxTemp() != null && temperature.compareTo(device.getAlarmMaxTemp()) > 0) {
            alarm = true;
        }
        if (alarm) {
            log.warn("温度告警: deviceId={}, temp={}, range=[{}, {}]",
                    deviceId, temperature, device.getAlarmMinTemp(), device.getAlarmMaxTemp());
        }
    }

    @Override
    public String getDeviceStatus(Long deviceId) {
        EqDevice device = deviceMapper.selectById(deviceId);
        return device != null ? device.getStatus() : null;
    }

    @Override
    public String getAutoLevel(Long deviceId) {
        EqDevice device = deviceMapper.selectById(deviceId);
        return device != null ? device.getAutoLevel() : null;
    }

    @Override
    public String getDeviceCode(Long deviceId) {
        EqDevice device = deviceMapper.selectById(deviceId);
        return device != null ? device.getDeviceCode() : null;
    }

    @Override
    public Long getDeviceId(String deviceCode) {
        EqDevice device = deviceMapper.selectOne(
                new LambdaQueryWrapper<EqDevice>().eq(EqDevice::getDeviceCode, deviceCode));
        return device != null ? device.getId() : null;
    }

    @Override
    public Integer getOnlineDeviceCount() {
        // TODO: 实现在线设备统计（Wave 2）
        log.warn("getOnlineDeviceCount 尚未实现");
        return 0;
    }

    @Override
    public List<DeviceFaultStatDTO> getFaultStats(LocalDateTime from, LocalDateTime to) {
        // TODO: 实现故障统计（Wave 2）
        log.warn("getFaultStats 尚未实现");
        return Collections.emptyList();
    }

    @Override
    public DeviceUtilizationDTO getDeviceUtilization(Long deviceId, LocalDateTime from, LocalDateTime to) {
        // TODO: 实现利用率统计（Wave 2）
        log.warn("getDeviceUtilization 尚未实现");
        return new DeviceUtilizationDTO();
    }

    private EqDeviceDTO toDTO(EqDevice entity) {
        if (entity == null) return null;
        EqDeviceDTO dto = new EqDeviceDTO();
        dto.setId(entity.getId());
        dto.setDeviceCode(entity.getDeviceCode());
        dto.setName(entity.getName());
        dto.setDeviceType(entity.getDeviceType() != null ? String.valueOf(entity.getDeviceType()) : null);
        dto.setStatus(entity.getStatus());
        dto.setCurrentTemp(entity.getCurrentTemp());
        dto.setFaultCode(entity.getFaultCode());
        dto.setAutoLevel(entity.getAutoLevel());
        dto.setProtocolType(entity.getProtocolType());
        dto.setAlarmHighTemp(entity.getAlarmMaxTemp());
        dto.setAlarmLowTemp(entity.getAlarmMinTemp());
        dto.setLastHeartbeat(entity.getUpdatedAt());
        return dto;
    }
}
