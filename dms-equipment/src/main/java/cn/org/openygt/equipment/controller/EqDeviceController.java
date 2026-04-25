package cn.org.openygt.equipment.controller;

import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.common.dto.EqDeviceDTO;
import cn.org.openygt.common.dto.TemperatureThresholdDTO;
import cn.org.openygt.common.service.EquipmentService;
import cn.org.openygt.equipment.dto.TemperatureLogDTO;
import cn.org.openygt.equipment.entity.EqDevice;
import cn.org.openygt.equipment.service.EqDeviceService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/eq/devices")
public class EqDeviceController {

    private final EqDeviceService deviceService;
    private final EquipmentService equipmentService;

    public EqDeviceController(EqDeviceService deviceService, EquipmentService equipmentService) {
        this.deviceService = deviceService;
        this.equipmentService = equipmentService;
    }

    // ========== 基础 CRUD ==========

    @PostMapping
    public ApiResponse<EqDevice> create(@RequestBody EqDevice device) {
        return ApiResponse.success(deviceService.create(device));
    }

    @PutMapping("/{id}")
    public ApiResponse<EqDevice> update(@PathVariable Long id, @RequestBody EqDevice device) {
        return ApiResponse.success(deviceService.update(id, device));
    }

    @GetMapping("/{id}")
    public ApiResponse<EqDevice> getById(@PathVariable Long id) {
        return ApiResponse.success(deviceService.getById(id));
    }

    @GetMapping("/code/{deviceCode}")
    public ApiResponse<EqDevice> getByCode(@PathVariable String deviceCode) {
        return ApiResponse.success(deviceService.getByCode(deviceCode));
    }

    @GetMapping
    public ApiResponse<IPage<EqDevice>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(deviceService.list(keyword, page, size));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        deviceService.delete(id);
        return ApiResponse.success();
    }

    // ========== 设备预留/释放 ==========

    @PostMapping("/{id}/reserve")
    public ApiResponse<Void> reserveDevice(@PathVariable Long id,
                                           @RequestParam Long taskId) {
        equipmentService.reserveDevice(taskId, id);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/release")
    public ApiResponse<Void> releaseDevice(@PathVariable Long id) {
        equipmentService.releaseDevice(id);
        return ApiResponse.success();
    }

    // ========== 温度阈值查询 ==========

    @GetMapping("/{id}/threshold")
    public ApiResponse<TemperatureThresholdDTO> getEffectiveThreshold(@PathVariable Long id) {
        return ApiResponse.success(equipmentService.getEffectiveThreshold(id));
    }

    // ========== 温度日志查询 ==========

    @GetMapping("/{id}/temperature-logs")
    public ApiResponse<IPage<TemperatureLogDTO>> getTemperatureLogs(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (start == null) {
            start = LocalDateTime.now().minusHours(24);
        }
        if (end == null) {
            end = LocalDateTime.now();
        }
        // 直接从温度日志表查询
        return ApiResponse.success(deviceService.getTemperatureLogs(id, start, end, page, size));
    }
}
