package cn.org.openygt.equipment.controller;

import cn.org.openygt.equipment.EquipmentModule;
import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.equipment.entity.EqDeviceMqttConfig;
import cn.org.openygt.equipment.service.EqDeviceMqttConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping(EquipmentModule.API_PREFIX + "/devices/{deviceCode}/mqtt-config")
@RequiredArgsConstructor
public class EqDeviceMqttConfigController {

    private final EqDeviceMqttConfigService mqttConfigService;

    @GetMapping
    public ApiResponse<EqDeviceMqttConfig> getConfig(@PathVariable String deviceCode) {
        return ApiResponse.success(mqttConfigService.getByDeviceCode(deviceCode));
    }

    @PostMapping
    public ApiResponse<EqDeviceMqttConfig> saveConfig(@PathVariable String deviceCode,
                                                       @RequestBody EqDeviceMqttConfig config) {
        return ApiResponse.success(mqttConfigService.createOrUpdate(deviceCode, config));
    }

    @DeleteMapping
    public ApiResponse<Void> deleteConfig(@PathVariable String deviceCode) {
        mqttConfigService.deleteByDeviceCode(deviceCode);
        return ApiResponse.success(null);
    }
}
