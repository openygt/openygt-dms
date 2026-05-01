package cn.org.openygt.equipment.service.impl;

import cn.org.openygt.equipment.entity.DeviceMaintenance;
import cn.org.openygt.equipment.mapper.DeviceMaintenanceMapper;
import cn.org.openygt.equipment.service.DeviceMaintenanceService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeviceMaintenanceServiceImpl implements DeviceMaintenanceService {

    private final DeviceMaintenanceMapper deviceMaintenanceMapper;

    public DeviceMaintenanceServiceImpl(DeviceMaintenanceMapper deviceMaintenanceMapper) {
        this.deviceMaintenanceMapper = deviceMaintenanceMapper;
    }

    @Override
    public IPage<DeviceMaintenance> list(Long deviceId, String maintenanceType, Integer status, int page, int size) {
        QueryWrapper<DeviceMaintenance> wrapper = new QueryWrapper<>();
        if (deviceId != null) {
            wrapper.eq("device_id", deviceId);
        }
        if (maintenanceType != null && !maintenanceType.isEmpty()) {
            wrapper.eq("maintenance_type", maintenanceType);
        }
        if (status != null) {
            wrapper.eq("status", status);
        }
        wrapper.orderByDesc("created_at");
        return deviceMaintenanceMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public DeviceMaintenance getById(Long id) {
        return deviceMaintenanceMapper.selectById(id);
    }

    @Override
    @Transactional
    public DeviceMaintenance create(DeviceMaintenance record) {
        deviceMaintenanceMapper.insert(record);
        return record;
    }

    @Override
    @Transactional
    public DeviceMaintenance update(Long id, DeviceMaintenance record) {
        record.setId(id);
        deviceMaintenanceMapper.updateById(record);
        return deviceMaintenanceMapper.selectById(id);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        deviceMaintenanceMapper.deleteById(id);
    }
}
