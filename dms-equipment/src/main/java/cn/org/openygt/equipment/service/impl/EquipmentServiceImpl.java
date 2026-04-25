package cn.org.openygt.equipment.service.impl;

import cn.org.openygt.common.service.EquipmentService;
import cn.org.openygt.equipment.entity.EqDevice;
import cn.org.openygt.equipment.mapper.EqDeviceMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;

@Slf4j
@Service
public class EquipmentServiceImpl implements EquipmentService {

    private final EqDeviceMapper deviceMapper;

    public EquipmentServiceImpl(EqDeviceMapper deviceMapper) {
        this.deviceMapper = deviceMapper;
    }

    @Override
    public EqDevice getDeviceByCode(String deviceCode) {
        LambdaQueryWrapper<EqDevice> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EqDevice::getDeviceCode, deviceCode);
        return deviceMapper.selectOne(wrapper);
    }

    @Override
    public EqDevice getDeviceById(Long deviceId) {
        return deviceMapper.selectById(deviceId);
    }

    @Override
    @Transactional
    public EqDevice getOrCreateDevice(String deviceCode, int defaultType) {
        EqDevice existing = getDeviceByCode(deviceCode);
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
        return device;
    }

    @Override
    @Transactional
    public void updateDeviceStatus(Long deviceId, String status) {
        LambdaUpdateWrapper<EqDevice> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(EqDevice::getId, deviceId)
               .set(EqDevice::getStatus, status)
               .set(EqDevice::getUpdatedAt, new Date());
        deviceMapper.update(null, wrapper);
        log.info("设备状态更新: deviceId={}, status={}", deviceId, status);
    }

    @Override
    @Transactional
    public void updateTemperature(Long deviceId, BigDecimal temperature) {
        LambdaUpdateWrapper<EqDevice> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(EqDevice::getId, deviceId)
               .set(EqDevice::getCurrentTemp, temperature)
               .set(EqDevice::getUpdatedAt, new Date());
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
               .set(EqDevice::getAlertTime, new Date())
               .set(EqDevice::getUpdatedAt, new Date());
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
               .set(EqDevice::getResolvedAt, new Date())
               .set(EqDevice::getUpdatedAt, new Date());
        deviceMapper.update(null, wrapper);
        log.info("设备故障清除: deviceId={}", deviceId);
    }

    @Override
    @Transactional
    public void releaseDevice(Long deviceId) {
        LambdaUpdateWrapper<EqDevice> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(EqDevice::getId, deviceId)
               .set(EqDevice::getStatus, "IDLE")
               .set(EqDevice::getUpdatedAt, new Date());
        deviceMapper.update(null, wrapper);
        log.info("设备释放: deviceId={}", deviceId);
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
        EqDevice device = getDeviceByCode(deviceCode);
        return device != null ? device.getId() : null;
    }
}
