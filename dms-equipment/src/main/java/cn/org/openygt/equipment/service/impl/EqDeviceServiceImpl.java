package cn.org.openygt.equipment.service.impl;

import cn.org.openygt.equipment.dto.TemperatureLogDTO;
import cn.org.openygt.equipment.entity.EqDevice;
import cn.org.openygt.equipment.entity.EqTemperatureLog;
import cn.org.openygt.equipment.mapper.EqDeviceMapper;
import cn.org.openygt.equipment.mapper.EqTemperatureLogMapper;
import cn.org.openygt.equipment.service.EqDeviceService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class EqDeviceServiceImpl implements EqDeviceService {

    private final EqDeviceMapper deviceMapper;
    private final EqTemperatureLogMapper temperatureLogMapper;

    public EqDeviceServiceImpl(EqDeviceMapper deviceMapper, EqTemperatureLogMapper temperatureLogMapper) {
        this.deviceMapper = deviceMapper;
        this.temperatureLogMapper = temperatureLogMapper;
    }

    // ... existing CRUD methods unchanged ...

    @Override
    public EqDevice getById(Long id) {
        EqDevice device = deviceMapper.selectById(id);
        if (device == null) {
            throw new IllegalArgumentException("设备不存在: " + id);
        }
        return device;
    }

    @Override
    public EqDevice getByCode(String deviceCode) {
        LambdaQueryWrapper<EqDevice> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EqDevice::getDeviceCode, deviceCode);
        return deviceMapper.selectOne(wrapper);
    }

    @Override
    @Transactional
    public EqDevice getOrCreate(String deviceCode, int defaultType) {
        EqDevice existing = getByCode(deviceCode);
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
    public IPage<EqDevice> list(String keyword, Integer deviceType, String status, int page, int size) {
        LambdaQueryWrapper<EqDevice> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(EqDevice::getDeviceCode, keyword).or().like(EqDevice::getName, keyword));
        }
        if (deviceType != null) {
            wrapper.eq(EqDevice::getDeviceType, deviceType);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(EqDevice::getStatus, status);
        }
        wrapper.orderByDesc(EqDevice::getCreatedAt);
        return deviceMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    @Transactional
    public EqDevice create(EqDevice device) {
        if (device.getStatus() == null) {
            device.setStatus("IDLE");
        }
        if (device.getEnabled() == null) {
            device.setEnabled(1);
        }
        deviceMapper.insert(device);
        return device;
    }

    @Override
    @Transactional
    public EqDevice update(Long id, EqDevice device) {
        EqDevice existing = deviceMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("设备不存在: " + id);
        }
        device.setId(id);
        deviceMapper.updateById(device);
        return deviceMapper.selectById(id);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        deviceMapper.deleteById(id);
    }

    @Override
    public IPage<TemperatureLogDTO> getTemperatureLogs(Long deviceId, LocalDateTime start, LocalDateTime end, int page, int size) {
        LambdaQueryWrapper<EqTemperatureLog> wrapper = new LambdaQueryWrapper<EqTemperatureLog>()
                .eq(EqTemperatureLog::getDeviceId, deviceId)
                .between(EqTemperatureLog::getRecordedAt, start, end)
                .orderByDesc(EqTemperatureLog::getRecordedAt);
        return temperatureLogMapper.selectPage(new Page<>(page, size), wrapper)
                .convert(this::toTemperatureLogDTO);
    }

    private TemperatureLogDTO toTemperatureLogDTO(EqTemperatureLog log) {
        TemperatureLogDTO dto = new TemperatureLogDTO();
        dto.setId(log.getId());
        dto.setDeviceId(log.getDeviceId());
        dto.setTemperature(log.getTemperature());
        dto.setRecordedAt(log.getRecordedAt());
        return dto;
    }
}
