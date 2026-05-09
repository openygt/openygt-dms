package cn.org.openygt.production.controller;

import cn.org.openygt.common.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import cn.org.openygt.common.dto.EqDeviceDTO;
import lombok.extern.slf4j.Slf4j;
import cn.org.openygt.common.service.EquipmentService;
import lombok.extern.slf4j.Slf4j;
import cn.org.openygt.production.ProductionModule;
import lombok.extern.slf4j.Slf4j;
import cn.org.openygt.production.dto.DeviceDailyDTO;
import lombok.extern.slf4j.Slf4j;
import cn.org.openygt.production.entity.StepLog;
import lombok.extern.slf4j.Slf4j;
import cn.org.openygt.production.mapper.StepLogMapper;
import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping(ProductionModule.API_PREFIX + "/step-log")
@RequiredArgsConstructor
@Slf4j
public class StepLogController {

    private final StepLogMapper stepLogMapper;
    private final EquipmentService equipmentService;

    private static final int WORKDAY_MINUTES = 480;

    @GetMapping("/device-daily")
    public ApiResponse<List<DeviceDailyDTO>> deviceDaily(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate target = date != null ? date : LocalDate.now();
        LocalDateTime dayStart = target.atStartOfDay();
        LocalDateTime dayEnd = target.plusDays(1).atStartOfDay();

        List<StepLog> logs = stepLogMapper.selectList(
            new LambdaQueryWrapper<StepLog>()
                .isNotNull(StepLog::getDeviceId)
                .ge(StepLog::getStartedAt, dayStart)
                .lt(StepLog::getStartedAt, dayEnd)
        );

        Map<Long, List<StepLog>> byDevice = logs.stream()
            .collect(Collectors.groupingBy(StepLog::getDeviceId));

        List<DeviceDailyDTO> result = new ArrayList<>();
        for (Map.Entry<Long, List<StepLog>> entry : byDevice.entrySet()) {
            DeviceDailyDTO dto = new DeviceDailyDTO();
            dto.setDeviceId(entry.getKey());

            try {
                EqDeviceDTO dev = equipmentService.getDeviceById(entry.getKey());
                if (dev != null) {
                    dto.setDeviceCode(dev.getDeviceCode());
                    dto.setDeviceName(dev.getName());
                }
            } catch (Exception e) { log.error("步骤日志记录异常", e); }

            Set<Long> taskIds = entry.getValue().stream()
                .map(StepLog::getTaskId).filter(Objects::nonNull).collect(Collectors.toSet());
            dto.setTaskCount(taskIds.size());

            int running = entry.getValue().stream().mapToInt(log -> {
                if (log.getStartedAt() == null) return 0;
                LocalDateTime end = log.getEndedAt() != null ? log.getEndedAt() : LocalDateTime.now();
                return (int) Math.max(0, Duration.between(log.getStartedAt(), end).toMinutes());
            }).sum();
            dto.setRunningMinutes(running);
            dto.setIdleMinutes(Math.max(0, WORKDAY_MINUTES - running));
            dto.setUtilization(Math.min(100, running * 100 / WORKDAY_MINUTES));
            result.add(dto);
        }

        result.sort(Comparator.comparingInt(DeviceDailyDTO::getRunningMinutes).reversed());
        return ApiResponse.success(result);
    }
}
