package cn.org.openygt.equipment.service;

import cn.org.openygt.equipment.entity.DeviceMaintenance;
import com.baomidou.mybatisplus.core.metadata.IPage;

public interface DeviceMaintenanceService {
    IPage<DeviceMaintenance> list(Long deviceId, String maintenanceType, Integer status, int page, int size);
    DeviceMaintenance getById(Long id);
    DeviceMaintenance create(DeviceMaintenance record);
    DeviceMaintenance update(Long id, DeviceMaintenance record);
    void delete(Long id);
}
