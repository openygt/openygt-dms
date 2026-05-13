package cn.org.openygt.equipment.service.impl;

import cn.org.openygt.common.dto.DeviceFaultStatDTO;
import cn.org.openygt.common.dto.DeviceUtilizationDTO;
import cn.org.openygt.common.dto.EqDeviceDTO;
import cn.org.openygt.common.dto.TemperatureThresholdDTO;
import cn.org.openygt.common.service.EquipmentService;
import cn.org.openygt.common.service.SysConfigService;
import cn.org.openygt.equipment.entity.DeviceUtilization;
import cn.org.openygt.equipment.entity.EqDevice;
import cn.org.openygt.equipment.entity.EqDeviceAlarm;
import cn.org.openygt.equipment.mapper.DeviceUtilizationMapper;
import cn.org.openygt.equipment.mapper.EqDeviceAlarmMapper;
import cn.org.openygt.equipment.mapper.EqDeviceMapper;
import cn.org.openygt.equipment.service.EqDeviceAlarmService;
import cn.org.openygt.masterdata.entity.DecoctScheme;
import cn.org.openygt.masterdata.service.DecoctSchemeService;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * EquipmentService SPI 实现。
 *
 * <p>使用 UpdateWrapper/QueryWrapper（非 Lambda 版本）以保证在纯 Mockito 单元测试中可运行，
 * 避免 MyBatis-Plus LambdaProxy 在无 Spring 上下文时解析失败。</p>
 */
@Slf4j
@Service
public class EquipmentServiceImpl implements EquipmentService {

    private final EqDeviceMapper deviceMapper;
    private final SysConfigService sysConfigService;
    private final DecoctSchemeService decoctSchemeService;
    private final EqDeviceAlarmMapper alarmMapper;
    private final EqDeviceAlarmService alarmService;
    private final DeviceUtilizationMapper utilizationMapper;

    public EquipmentServiceImpl(EqDeviceMapper deviceMapper,
                                SysConfigService sysConfigService,
                                DecoctSchemeService decoctSchemeService,
                                EqDeviceAlarmMapper alarmMapper,
                                EqDeviceAlarmService alarmService,
                                DeviceUtilizationMapper utilizationMapper) {
        this.deviceMapper = deviceMapper;
        this.sysConfigService = sysConfigService;
        this.decoctSchemeService = decoctSchemeService;
        this.alarmMapper = alarmMapper;
        this.alarmService = alarmService;
        this.utilizationMapper = utilizationMapper;
    }

    // ========== 常量：系统默认阈值配置键 ==========
    private static final String CFG_DEFAULT_HIGH_TEMP = "temp.alarm.high";
    private static final String CFG_DEFAULT_LOW_TEMP  = "temp.alarm.low";
    private static final String DEFAULT_HIGH_TEMP_STR = "110.0";
    private static final String DEFAULT_LOW_TEMP_STR  = "80.0";

    @Override
    public EqDeviceDTO getDeviceByCode(String deviceCode) {
        QueryWrapper<EqDevice> wrapper = new QueryWrapper<>();
        wrapper.eq("device_code", deviceCode);
        EqDevice entity = deviceMapper.selectOne(wrapper);
        return toDTO(entity);
    }

    @Override
    public EqDeviceDTO getDeviceByCommunicationId(String communicationId) {
        if (communicationId == null || communicationId.trim().isEmpty()) {
            return null;
        }
        return toDTO(deviceMapper.findByCommunicationId(communicationId.trim().toUpperCase()));
    }

    @Override
    public EqDeviceDTO getDeviceById(Long deviceId) {
        return toDTO(deviceMapper.selectById(deviceId));
    }

    @Override
    @Transactional
    public EqDeviceDTO getOrCreateDevice(String deviceCode, int defaultType) {
        EqDeviceDTO existing = getDeviceByCode(deviceCode);
        if (existing != null) {
            return existing;
        }
        EqDevice device = new EqDevice();
        device.setDeviceCode(deviceCode);
        device.setName(deviceCode);
        device.setDeviceType(defaultType);
        device.setStatus("IDLE");
        device.setEnabled(1);
        deviceMapper.insert(device);
        return toDTO(device);
    }

    @Override
    @Transactional
    public void updateDeviceStatus(Long deviceId, String status) {
        UpdateWrapper<EqDevice> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", deviceId)
                .set("last_heartbeat", LocalDateTime.now())
                .set("updated_at", LocalDateTime.now());

        // 粗粒度状态 vs 精细状态区分
        if ("OFFLINE".equals(status) || "FAULT".equals(status) || "IDLE".equals(status)) {
            wrapper.set("status", status);
            wrapper.set("detail_status", status);
        } else {
            // 精细状态（SOAKING/FIRST_DECOCTING 等）只更新 detail_status
            wrapper.set("detail_status", status);
            wrapper.set("status", "BUSY");
        }
        deviceMapper.update(null, wrapper);
        log.info("设备状态更新: deviceId={}, status={}", deviceId, status);
    }

    @Override
    @Transactional
    public void updateTemperature(Long deviceId, BigDecimal temperature) {
        UpdateWrapper<EqDevice> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", deviceId)
                .set("current_temp", temperature)
                .set("last_heartbeat", LocalDateTime.now())
                .set("updated_at", LocalDateTime.now());
        deviceMapper.update(null, wrapper);
        checkTemperatureAlarm(deviceId, temperature);
    }

    @Override
    @Transactional
    public void reportFault(Long deviceId, String faultCode, String message) {
        UpdateWrapper<EqDevice> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", deviceId)
                .set("status", "FAULT")
                .set("fault_code", faultCode)
                .set("alert_time", java.util.Date.from(java.time.Instant.now()))
                .set("updated_at", LocalDateTime.now());
        deviceMapper.update(null, wrapper);
        log.warn("设备故障上报: deviceId={}, faultCode={}, message={}", deviceId, faultCode, message);
    }

    @Override
    @Transactional
    public void clearFault(Long deviceId) {
        UpdateWrapper<EqDevice> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", deviceId)
                .set("status", "IDLE")
                .set("fault_code", null)
                .set("resolved_at", java.util.Date.from(java.time.Instant.now()))
                .set("updated_at", LocalDateTime.now());
        deviceMapper.update(null, wrapper);
        log.info("设备故障清除: deviceId={}", deviceId);
    }

    @Override
    @Transactional
    public void releaseDevice(Long deviceId) {
        EqDevice device = deviceMapper.selectForUpdate(deviceId);
        if (device == null) {
            log.warn("释放设备不存在: deviceId={}", deviceId);
            return;
        }

        UpdateWrapper<EqDevice> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", deviceId)
                .set("status", "IDLE")
                .set("updated_at", LocalDateTime.now());
        deviceMapper.update(null, wrapper);
        log.info("设备释放: deviceId={}", deviceId);
    }

    @Override
    @Transactional
    public void reserveDevice(Long taskId, Long deviceId) {
        EqDevice device = deviceMapper.selectForUpdate(deviceId);
        if (device == null) {
            throw new IllegalArgumentException("设备不存在: " + deviceId);
        }

        String status = device.getStatus();
        if (!"IDLE".equalsIgnoreCase(status) && !"RESERVED".equalsIgnoreCase(status)) {
            throw new IllegalStateException("设备无法预留，当前状态: " + status);
        }

        UpdateWrapper<EqDevice> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", deviceId)
                .set("status", "RESERVED")
                .set("updated_at", LocalDateTime.now());
        deviceMapper.update(null, wrapper);
        log.info("设备预留: deviceId={}, taskId={}", deviceId, taskId);
    }

    @Override
    public void checkTemperatureAlarm(Long deviceId, BigDecimal temperature) {
        EqDevice device = deviceMapper.selectById(deviceId);
        if (device == null) {
            return;
        }

        EqDeviceAlarm latest = alarmMapper.findLatestActiveAlarm(deviceId, "HIGH_TEMP");
        TemperatureThresholdDTO threshold = getEffectiveThreshold(deviceId);

        boolean alarm = false;
        if (threshold.getHighTemp() != null && temperature.compareTo(threshold.getHighTemp()) > 0) {
            alarm = true;
            if (latest == null || latest.getCreatedAt().plusMinutes(5).isBefore(LocalDateTime.now())) {
                String msg = String.format("设备超温: 当前 %.1f℃ > 阈值 %.1f℃ [来源=%s]",
                        temperature, threshold.getHighTemp(), threshold.getSource());
                alarmService.createAlarm(device, "HIGH_TEMP", "CRITICAL", msg);
                log.warn("温度告警: deviceId={}, temp={}, highThreshold={}", deviceId, temperature, threshold.getHighTemp());
            }
        }
        if (threshold.getLowTemp() != null && temperature.compareTo(threshold.getLowTemp()) < 0) {
            alarm = true;
            EqDeviceAlarm latestLow = alarmMapper.findLatestActiveAlarm(deviceId, "LOW_TEMP");
            if (latestLow == null || latestLow.getCreatedAt().plusMinutes(5).isBefore(LocalDateTime.now())) {
                String msg = String.format("设备低温: 当前 %.1f℃ < 阈值 %.1f℃ [来源=%s]",
                        temperature, threshold.getLowTemp(), threshold.getSource());
                alarmService.createAlarm(device, "LOW_TEMP", "WARNING", msg);
                log.warn("低温告警: deviceId={}, temp={}, lowThreshold={}", deviceId, temperature, threshold.getLowTemp());
            }
        }
        if (!alarm) {
            resolveActiveAlarms(deviceId, "HIGH_TEMP");
            resolveActiveAlarms(deviceId, "LOW_TEMP");
        }
    }

    private void resolveActiveAlarms(Long deviceId, String alarmType) {
        alarmMapper.findActiveByDeviceIdAndType(deviceId, alarmType)
                .forEach(alarm -> {
                    alarm.setIsResolved(1);
                    alarm.setResolvedAt(LocalDateTime.now());
                    alarmMapper.updateById(alarm);
                });
    }

    @Override
    public String getDeviceStatus(Long deviceId) {
        EqDevice device = deviceMapper.selectById(deviceId);
        return device != null ? device.getStatus() : null;
    }

    @Override
    public String getAutoLevel(Long deviceId) {
        EqDevice device = deviceMapper.selectById(deviceId);
        return device != null ? device.getAutoLevel() : null;
    }

    @Override
    public String getDeviceCode(Long deviceId) {
        EqDevice device = deviceMapper.selectById(deviceId);
        return device != null ? device.getDeviceCode() : null;
    }

    @Override
    public Long getDeviceId(String deviceCode) {
        QueryWrapper<EqDevice> wrapper = new QueryWrapper<>();
        wrapper.eq("device_code", deviceCode);
        EqDevice device = deviceMapper.selectOne(wrapper);
        return device != null ? device.getId() : null;
    }

    @Override
    @Transactional
    public EqDeviceDTO lockDeviceByCode(String deviceCode) {
        EqDevice device = deviceMapper.findByDeviceCodeForUpdate(deviceCode);
        return toDTO(device);
    }

    @Override
    public TemperatureThresholdDTO getEffectiveThreshold(Long deviceId) {
        EqDevice device = deviceMapper.selectById(deviceId);
        if (device == null) {
            throw new IllegalArgumentException("设备不存在: " + deviceId);
        }

        String deviceCode = device.getDeviceCode();

        // 第 1 层：方案级覆盖（最高优先）
        if (device.getCurrentSchemeId() != null) {
            try {
                DecoctScheme scheme = decoctSchemeService.getById(device.getCurrentSchemeId());
                if (scheme != null
                        && scheme.getAlarmHighTemp() != null
                        && scheme.getAlarmLowTemp() != null) {
                    log.debug("温度阈值来源: SCHEME, deviceCode={}, schemeId={}", deviceCode, scheme.getId());
                    return new TemperatureThresholdDTO(deviceCode,
                            scheme.getAlarmHighTemp(),
                            scheme.getAlarmLowTemp(),
                            "SCHEME");
                }
            } catch (Exception e) {
                log.warn("查询方案阈值异常: deviceCode={}, schemeId={}", deviceCode, device.getCurrentSchemeId(), e);
            }
        }

        // 第 2 层：设备级阈值
        if (device.getAlarmMaxTemp() != null && device.getAlarmMinTemp() != null) {
            log.debug("温度阈值来源: DEVICE, deviceCode={}", deviceCode);
            return new TemperatureThresholdDTO(deviceCode,
                    device.getAlarmMaxTemp(),
                    device.getAlarmMinTemp(),
                    "DEVICE");
        }

        // 第 3 层：系统默认（sys_config）
        BigDecimal highTemp = parseBigDecimal(
                sysConfigService.getStringValue(CFG_DEFAULT_HIGH_TEMP, DEFAULT_HIGH_TEMP_STR),
                new BigDecimal(DEFAULT_HIGH_TEMP_STR));
        BigDecimal lowTemp = parseBigDecimal(
                sysConfigService.getStringValue(CFG_DEFAULT_LOW_TEMP, DEFAULT_LOW_TEMP_STR),
                new BigDecimal(DEFAULT_LOW_TEMP_STR));
        log.debug("温度阈值来源: SYSTEM, deviceCode={}", deviceCode);
        return new TemperatureThresholdDTO(deviceCode, highTemp, lowTemp, "SYSTEM");
    }

    private BigDecimal parseBigDecimal(String value, BigDecimal defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            log.warn("配置值解析失败，使用默认值: value={}, default={}", value, defaultValue);
            return defaultValue;
        }
    }

    @Override
    public Integer getOnlineDeviceCount() {
        Long count = deviceMapper.selectCount(
                new QueryWrapper<EqDevice>().eq("deleted", 0).ne("status", "OFFLINE"));
        return count != null ? count.intValue() : 0;
    }

    @Override
    public List<DeviceFaultStatDTO> getFaultStats(LocalDateTime from, LocalDateTime to) {
        QueryWrapper<EqDeviceAlarm> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted", 0);
        if (from != null) {
            wrapper.ge("created_at", from);
        }
        if (to != null) {
            wrapper.le("created_at", to);
        }
        List<EqDeviceAlarm> alarms = alarmMapper.selectList(wrapper);

        Map<Long, List<EqDeviceAlarm>> grouped = alarms.stream()
                .collect(Collectors.groupingBy(EqDeviceAlarm::getDeviceId));

        List<DeviceFaultStatDTO> result = new ArrayList<>();
        for (Map.Entry<Long, List<EqDeviceAlarm>> entry : grouped.entrySet()) {
            DeviceFaultStatDTO dto = new DeviceFaultStatDTO();
            dto.setDeviceId(entry.getKey());
            EqDevice device = deviceMapper.selectById(entry.getKey());
            if (device != null) {
                dto.setDeviceCode(device.getDeviceCode());
                dto.setDeviceName(device.getName());
            }
            dto.setFaultCount(entry.getValue().size());
            entry.getValue().stream()
                    .max(Comparator.comparing(EqDeviceAlarm::getCreatedAt))
                    .ifPresent(latest -> {
                        dto.setLatestFaultCode(latest.getAlarmType());
                        dto.setLatestFaultMessage(latest.getMessage());
                    });
            result.add(dto);
        }
        return result;
    }

    @Override
    public DeviceUtilizationDTO getDeviceUtilization(Long deviceId, LocalDateTime from, LocalDateTime to) {
        EqDevice device = deviceMapper.selectById(deviceId);
        if (device == null) {
            return new DeviceUtilizationDTO();
        }

        LocalDate startDate = from != null ? from.toLocalDate() : LocalDate.now().minusDays(7);
        LocalDate endDate = to != null ? to.toLocalDate() : LocalDate.now();

        List<DeviceUtilization> list = utilizationMapper.findByDeviceAndDateRange(
                device.getDeviceCode(), startDate, endDate);

        DeviceUtilizationDTO dto = new DeviceUtilizationDTO();
        dto.setDeviceId(deviceId);
        dto.setDeviceCode(device.getDeviceCode());
        dto.setDeviceName(device.getName());

        if (!list.isEmpty()) {
            dto.setUtilizationRate(list.stream()
                    .map(DeviceUtilization::getUtilizationRate)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(list.size()), 2, RoundingMode.HALF_UP));
            dto.setTotalMinutes(list.stream().mapToInt(u -> u.getTotalMinutes() != null ? u.getTotalMinutes() : 0).sum());
            dto.setRunningMinutes(list.stream().mapToInt(u -> u.getRunMinutes() != null ? u.getRunMinutes() : 0).sum());
            dto.setIdleMinutes(list.stream().mapToInt(u -> u.getIdleMinutes() != null ? u.getIdleMinutes() : 0).sum());
            dto.setFaultMinutes(list.stream().mapToInt(u -> u.getFaultMinutes() != null ? u.getFaultMinutes() : 0).sum());
        }

        return dto;
    }

    private EqDeviceDTO toDTO(EqDevice entity) {
        if (entity == null) return null;
        EqDeviceDTO dto = new EqDeviceDTO();
        dto.setId(entity.getId());
        dto.setDeviceCode(entity.getDeviceCode());
        dto.setName(entity.getName());
        dto.setDeviceType(entity.getDeviceType() != null ? String.valueOf(entity.getDeviceType()) : null);
        dto.setStatus(entity.getStatus());
        dto.setCurrentTemp(entity.getCurrentTemp());
        dto.setFaultCode(entity.getFaultCode());
        dto.setAutoLevel(entity.getAutoLevel());
        dto.setProtocolType(entity.getProtocolType());
        dto.setCommunicationId(entity.getCommunicationId());
        dto.setAlarmHighTemp(entity.getAlarmMaxTemp());
        dto.setAlarmLowTemp(entity.getAlarmMinTemp());
        dto.setLastHeartbeat(entity.getLastHeartbeat());
        return dto;
    }
}
