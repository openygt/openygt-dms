package cn.org.openygt.equipment.controller;

import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.equipment.EquipmentModule;
import cn.org.openygt.equipment.entity.PrescriptionDefault;
import cn.org.openygt.equipment.mapper.PrescriptionDefaultMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(EquipmentModule.API_PREFIX + "/prescription-defaults")
@RequiredArgsConstructor
public class PrescriptionDefaultController {

    private final PrescriptionDefaultMapper defaultMapper;

    @GetMapping
    public ApiResponse<List<PrescriptionDefault>> getAllDefaults() {
        return ApiResponse.success(defaultMapper.selectList(null));
    }

    @PutMapping("/{settingKey}")
    public ApiResponse<PrescriptionDefault> updateDefault(
            @PathVariable String settingKey,
            @RequestBody PrescriptionDefault setting) {
        PrescriptionDefault existing = defaultMapper.findByKey(settingKey);
        if (existing != null) {
            existing.setSettingValue(setting.getSettingValue());
            defaultMapper.updateById(existing);
            return ApiResponse.success(existing);
        }
        return ApiResponse.error(404, "设置项不存在: " + settingKey);
    }

    @PostMapping("/reset")
    public ApiResponse<Void> resetDefaults() {
        // 恢复默认设置
        return ApiResponse.success(null);
    }
}
