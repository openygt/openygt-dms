package cn.org.openygt.equipment.controller;

import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.equipment.EquipmentModule;
import cn.org.openygt.equipment.entity.AlarmConfig;
import cn.org.openygt.equipment.service.AlarmConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

import java.util.List;

@RestController
@RequestMapping(EquipmentModule.API_PREFIX + "/alarm-configs")
@RequiredArgsConstructor
public class AlarmConfigController {

    private final AlarmConfigService alarmConfigService;

    @GetMapping
    public ApiResponse<List<AlarmConfig>> getAllConfigs() {
        return ApiResponse.success(alarmConfigService.getAllConfigs());
    }

    @PostMapping
    public ApiResponse<AlarmConfig> createConfig(@RequestBody @Valid AlarmConfig config) {
        return ApiResponse.success(alarmConfigService.createConfig(config));
    }

    @PutMapping("/{id}")
    public ApiResponse<AlarmConfig> updateConfig(@PathVariable Long id, @RequestBody @Valid AlarmConfig config) {
        return ApiResponse.success(alarmConfigService.updateConfig(id, config));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteConfig(@PathVariable Long id) {
        alarmConfigService.deleteConfig(id);
        return ApiResponse.success(null);
    }
}
