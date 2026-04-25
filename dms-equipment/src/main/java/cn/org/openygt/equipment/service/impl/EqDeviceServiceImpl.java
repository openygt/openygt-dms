package cn.org.openygt.equipment.service.impl;

import cn.org.openygt.equipment.entity.EqDevice;
import cn.org.openygt.equipment.mapper.EqDeviceMapper;
import cn.org.openygt.equipment.service.EqDeviceService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EqDeviceServiceImpl implements EqDeviceService {

    private final EqDeviceMapper deviceMapper;

    public EqDeviceServiceImpl(EqDeviceMapper deviceMapper) {
        this.deviceMapper = deviceMapper;
    }

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
    public IPage<EqDevice> list(String keyword, int page, int size) {
        LambdaQueryWrapper<EqDevice> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(EqDevice::getDeviceCode, keyword)
                   .or()
                   .like(EqDevice::getName, keyword);
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
}
