package cn.org.openygt.equipment.controller;

import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.equipment.EquipmentModule;
import cn.org.openygt.equipment.entity.DeviceMaintenance;
import cn.org.openygt.equipment.service.DeviceMaintenanceService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping(EquipmentModule.API_PREFIX + "/device-maintenances")
@RequiredArgsConstructor
public class DeviceMaintenanceController {

    private final DeviceMaintenanceService deviceMaintenanceService;

    @GetMapping
    public ApiResponse<IPage<DeviceMaintenance>> list(
            @RequestParam(required = false) Long deviceId,
            @RequestParam(required = false) String maintenanceType,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(deviceMaintenanceService.list(deviceId, maintenanceType, status, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<DeviceMaintenance> getById(@PathVariable Long id) {
        return ApiResponse.success(deviceMaintenanceService.getById(id));
    }

    @PostMapping
    public ApiResponse<DeviceMaintenance> create(@RequestBody @Valid DeviceMaintenance record) {
        return ApiResponse.success(deviceMaintenanceService.create(record));
    }

    @PutMapping("/{id}")
    public ApiResponse<DeviceMaintenance> update(@PathVariable Long id, @RequestBody @Valid DeviceMaintenance record) {
        return ApiResponse.success(deviceMaintenanceService.update(id, record));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        deviceMaintenanceService.delete(id);
        return ApiResponse.success();
    }
}
