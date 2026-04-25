package cn.org.openygt.equipment.controller;

import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.equipment.entity.EqDevice;
import cn.org.openygt.equipment.service.EqDeviceService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/eq/devices")
public class EqDeviceController {

    private final EqDeviceService deviceService;

    public EqDeviceController(EqDeviceService deviceService) {
        this.deviceService = deviceService;
    }

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
}
