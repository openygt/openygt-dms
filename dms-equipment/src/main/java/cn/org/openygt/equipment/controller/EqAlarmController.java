package cn.org.openygt.equipment.controller;
import cn.org.openygt.equipment.EquipmentModule;

import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.equipment.dto.AlarmLogDTO;
import cn.org.openygt.equipment.entity.EqDevice;
import cn.org.openygt.equipment.entity.EqDeviceAlarm;
import cn.org.openygt.rbac.annotation.RequiresPermissions;
import cn.org.openygt.equipment.mapper.EqDeviceAlarmMapper;
import cn.org.openygt.equipment.mapper.EqDeviceMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * 设备告警查询接口。
 */
@RestController
@RequestMapping(EquipmentModule.API_PREFIX + "/alarms")
@RequiredArgsConstructor
public class EqAlarmController {

    private final EqDeviceAlarmMapper alarmMapper;
    private final EqDeviceMapper deviceMapper;

    @GetMapping
    @RequiresPermissions("eq:alarm:view")
    public ApiResponse<Page<AlarmLogDTO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String deviceCode,
            @RequestParam(required = false) String alarmType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {

        // 1. 构建告警查询条件
        QueryWrapper<EqDeviceAlarm> alarmWrapper = new QueryWrapper<>();
        alarmWrapper.orderByDesc("created_at");
        if (alarmType != null && !alarmType.isEmpty()) {
            alarmWrapper.eq("alarm_type", alarmType);
        }
        if (startTime != null && !startTime.isEmpty()) {
            alarmWrapper.ge("created_at", startTime);
        }
        if (endTime != null && !endTime.isEmpty()) {
            alarmWrapper.le("created_at", endTime);
        }
        if ("PENDING".equals(status)) {
            alarmWrapper.eq("is_resolved", 0);
        } else if ("RESOLVED".equals(status)) {
            alarmWrapper.eq("is_resolved", 1);
        } else if ("CANCELLED".equals(status)) {
            alarmWrapper.eq("is_resolved", 2);
        }

        // 2. 如需按 deviceCode 筛选，先查设备ID
        if (deviceCode != null && !deviceCode.isEmpty()) {
            QueryWrapper<EqDevice> deviceWrapper = new QueryWrapper<>();
            deviceWrapper.eq("device_code", deviceCode);
            List<EqDevice> devices = deviceMapper.selectList(deviceWrapper);
            List<Long> deviceIds = devices.stream().map(EqDevice::getId).collect(Collectors.toList());
            if (deviceIds.isEmpty()) {
                return ApiResponse.success(new Page<>(page, size));
            }
            alarmWrapper.in("device_id", deviceIds);
        }

        // 3. 分页查询告警
        Page<EqDeviceAlarm> alarmPage = new Page<>(page, size);
        alarmMapper.selectPage(alarmPage, alarmWrapper);

        // 4. 批量获取设备编码
        List<Long> deviceIds = alarmPage.getRecords().stream()
                .map(EqDeviceAlarm::getDeviceId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> deviceCodeMap = new HashMap<>();
        if (!deviceIds.isEmpty()) {
            for (EqDevice d : deviceMapper.selectBatchIds(deviceIds)) {
                if (d != null && d.getId() != null) {
                    deviceCodeMap.put(d.getId(), d.getDeviceCode());
                }
            }
        }

        // 5. 组装DTO
        List<AlarmLogDTO> dtoList = alarmPage.getRecords().stream().map(alarm -> {
            AlarmLogDTO dto = new AlarmLogDTO();
            dto.setId(alarm.getId());
            dto.setDeviceCode(deviceCodeMap.getOrDefault(alarm.getDeviceId(), "-"));
            dto.setAlarmType(alarm.getAlarmType());
            dto.setAlarmLevel(alarm.getAlarmLevel());
            dto.setContent(alarm.getMessage());
            Integer isResolved = alarm.getIsResolved();
            String statusStr;
            if (isResolved != null && isResolved == 1) {
                statusStr = "RESOLVED";
            } else if (isResolved != null && isResolved == 2) {
                statusStr = "CANCELLED";
            } else {
                statusStr = "PENDING";
            }
            dto.setStatus(statusStr);
            dto.setCreatedAt(alarm.getCreatedAt());
            return dto;
        }).collect(Collectors.toList());

        Page<AlarmLogDTO> resultPage = new Page<>(alarmPage.getCurrent(), alarmPage.getSize(), alarmPage.getTotal());
        resultPage.setRecords(dtoList);
        return ApiResponse.success(resultPage);
    }

    @PutMapping("/{id}/resolve")
    public ApiResponse<Void> resolveAlarm(@PathVariable Long id) {
        EqDeviceAlarm alarm = alarmMapper.selectById(id);
        if (alarm == null) {
            return ApiResponse.error(404, "告警记录不存在");
        }
        alarm.setIsResolved(1);
        alarm.setResolvedAt(java.time.LocalDateTime.now());
        alarmMapper.updateById(alarm);
        return ApiResponse.success();
    }

    @PutMapping("/{id}/cancel")
    public ApiResponse<Void> cancelAlarm(@PathVariable Long id) {
        EqDeviceAlarm alarm = alarmMapper.selectById(id);
        if (alarm == null) {
            return ApiResponse.error(404, "告警记录不存在");
        }
        alarm.setIsResolved(2);
        alarm.setResolvedAt(java.time.LocalDateTime.now());
        alarmMapper.updateById(alarm);
        return ApiResponse.success();
    }
}
