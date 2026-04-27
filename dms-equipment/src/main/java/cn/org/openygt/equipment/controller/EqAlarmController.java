package cn.org.openygt.equipment.controller;

import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.equipment.dto.AlarmLogDTO;
import cn.org.openygt.equipment.entity.EqDevice;
import cn.org.openygt.equipment.entity.EqDeviceAlarm;
import cn.org.openygt.equipment.mapper.EqDeviceAlarmMapper;
import cn.org.openygt.equipment.mapper.EqDeviceMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 设备告警查询接口。
 */
@RestController
@RequestMapping("/api/v1/eq/alarms")
@RequiredArgsConstructor
public class EqAlarmController {

    private final EqDeviceAlarmMapper alarmMapper;
    private final EqDeviceMapper deviceMapper;

    @GetMapping
    public ApiResponse<Page<AlarmLogDTO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String deviceCode,
            @RequestParam(required = false) String alarmType) {

        // 1. 构建告警查询条件
        QueryWrapper<EqDeviceAlarm> alarmWrapper = new QueryWrapper<>();
        alarmWrapper.orderByDesc("created_at");
        if (alarmType != null && !alarmType.isEmpty()) {
            alarmWrapper.eq("alarm_type", alarmType);
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
        Map<Long, String> deviceCodeMap = deviceIds.isEmpty() ? Collections.emptyMap() :
                deviceMapper.selectBatchIds(deviceIds).stream()
                        .collect(Collectors.toMap(EqDevice::getId, EqDevice::getDeviceCode));

        // 5. 组装DTO
        List<AlarmLogDTO> dtoList = alarmPage.getRecords().stream().map(alarm -> {
            AlarmLogDTO dto = new AlarmLogDTO();
            dto.setId(alarm.getId());
            dto.setDeviceCode(deviceCodeMap.getOrDefault(alarm.getDeviceId(), "-"));
            dto.setAlarmType(alarm.getAlarmType());
            dto.setAlarmLevel(alarm.getAlarmLevel());
            dto.setContent(alarm.getMessage());
            dto.setStatus(alarm.getIsResolved() != null && alarm.getIsResolved() == 1 ? "RESOLVED" : "PENDING");
            dto.setCreatedAt(alarm.getCreatedAt());
            return dto;
        }).collect(Collectors.toList());

        Page<AlarmLogDTO> resultPage = new Page<>(alarmPage.getCurrent(), alarmPage.getSize(), alarmPage.getTotal());
        resultPage.setRecords(dtoList);
        return ApiResponse.success(resultPage);
    }
}
