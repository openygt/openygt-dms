package cn.org.openygt.equipment.service.impl;

import cn.org.openygt.equipment.dto.GatewayDeviceReportRequest;
import cn.org.openygt.equipment.entity.DeviceCommand;
import cn.org.openygt.equipment.entity.EqDevice;
import cn.org.openygt.equipment.entity.EqDeviceStatus;
import cn.org.openygt.equipment.entity.EqTemperatureLog;
import cn.org.openygt.equipment.enums.DeviceDetailStatus;
import cn.org.openygt.equipment.mapper.DeviceCommandMapper;
import cn.org.openygt.equipment.mapper.EqDeviceMapper;
import cn.org.openygt.equipment.mapper.EqTemperatureLogMapper;
import cn.org.openygt.equipment.service.EqDeviceAlarmService;
import cn.org.openygt.equipment.service.EqDeviceStatusService;
import cn.org.openygt.common.event.DeviceStatusChangedEvent;
import cn.org.openygt.equipment.service.GatewayReportService;
import cn.org.openygt.equipment.websocket.DeviceWebSocketController;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GatewayReportServiceImpl implements GatewayReportService {

    private final EqDeviceMapper deviceMapper;
    private final EqDeviceStatusService deviceStatusService;
    private final EqTemperatureLogMapper temperatureLogMapper;
    private final EqDeviceAlarmService alarmService;
    private final DeviceCommandMapper deviceCommandMapper;
    private final DeviceWebSocketController webSocketController;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public void handleReport(GatewayDeviceReportRequest request) {
        if (request == null || request.getDeviceCode() == null || request.getDeviceCode().trim().isEmpty()) {
            throw new IllegalArgumentException("deviceCode 不能为空");
        }

        EqDevice device = getOrCreateDevice(request);
        LocalDateTime reportedAt = request.getReportedAt() != null ? request.getReportedAt() : LocalDateTime.now();
        String messageType = normalize(request.getMessageType());

        if ("COMMAND_ACK".equals(messageType)) {
            handleCommandAck(device, request, reportedAt);
        } else {
            String oldStatus = device.getDetailStatus();
            applyRealtimeState(device, request, reportedAt);
            saveSnapshot(device, request, reportedAt);
            pushRealtimeState(device);
            // BUG-10/BUG-21: 发布设备状态变化事件，由生产模块监听处理任务推进
            // 使用 Objects.equals 确保首次上报（oldStatus=null）也能触发事件
            if (!java.util.Objects.equals(oldStatus, device.getDetailStatus())) {
                eventPublisher.publishEvent(new DeviceStatusChangedEvent(this, device.getId(), device.getDeviceCode(), oldStatus, device.getDetailStatus()));
            }
        }
    }

    private EqDevice getOrCreateDevice(GatewayDeviceReportRequest request) {
        EqDevice device = deviceMapper.findByDeviceCode(request.getDeviceCode());
        if (device != null) {
            return device;
        }
        throw new IllegalArgumentException("设备未登记，不允许接入生产链路: " + request.getDeviceCode());
    }

    private void handleCommandAck(EqDevice device, GatewayDeviceReportRequest request, LocalDateTime reportedAt) {
        DeviceCommand command = null;
        if (request.getCommandId() != null) {
            command = deviceCommandMapper.selectById(request.getCommandId());
        }
        if (command == null && request.getCommandType() != null) {
            command = deviceCommandMapper.findLatestDeliveringCommand(device.getDeviceCode(), request.getCommandType());
        }

        if (command != null) {
            command.setStatus(isSuccessResult(request.getCommandResult()) ? "ACKED" : "FAILED");
            command.setResponsePayload(request.getRawPayload());
            command.setAckTime(reportedAt);
            command.setUpdatedAt(LocalDateTime.now());
            if (!isSuccessResult(request.getCommandResult())) {
                command.setFailReason(defaultString(request.getFaultMessage(), request.getCommandResult()));
            }
            deviceCommandMapper.updateById(command);
        } else {
            log.warn("未找到可匹配的设备指令 ACK: device={}, commandType={}",
                    device.getDeviceCode(), request.getCommandType());
        }

        applyRealtimeState(device, request, reportedAt);
        saveSnapshot(device, request, reportedAt);
        pushRealtimeState(device);
    }

    private void applyRealtimeState(EqDevice device, GatewayDeviceReportRequest request, LocalDateTime reportedAt) {
        String detailStatus = resolveDetailStatus(request, device);
        // BUG-26: 校验 detailStatus 枚举有效性
        if (detailStatus != null) {
            try {
                DeviceDetailStatus.valueOf(detailStatus);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("未知设备状态: " + detailStatus);
            }
        }
        String legacyStatus = DeviceDetailStatus.mapToLegacyStatus(detailStatus);

        device.setTenantId(defaultString(request.getTenantId(), device.getTenantId()));
        device.setProtocolType(defaultString(request.getProtocolType(), device.getProtocolType()));
        device.setManufacturer(defaultString(device.getManufacturer(), resolveManufacturer(request.getProtocolType())));
        device.setDeviceType(resolveDeviceType(request) != null ? resolveDeviceType(request) : device.getDeviceType());
        device.setDetailStatus(detailStatus);
        device.setStatus(legacyStatus);
        device.setLastHeartbeat(reportedAt);
        device.setCurrentTemp(firstNonNull(request.getCurrentTemp(), device.getCurrentTemp()));
        device.setPressure(firstNonNull(request.getPressure(), device.getPressure()));
        device.setWaterLevel(firstNonNull(request.getWaterLevel(), device.getWaterLevel()));
        device.setRemainingTime(firstNonNull(request.getRemainingTime(), device.getRemainingTime()));
        device.setProgressPercent(firstNonNull(request.getProgressPercent(), device.getProgressPercent()));
        // BUG-09: 故障恢复后清除 faultCode
        if (request.getFaultCode() != null) {
            device.setFaultCode(request.getFaultCode());
        } else if (!"FAULT".equals(detailStatus)) {
            device.setFaultCode("");
        }
        device.setUpdatedAt(LocalDateTime.now());
        deviceMapper.updateById(device);

        if (request.getCurrentTemp() != null) {
            saveTemperatureLog(device, request.getCurrentTemp(), reportedAt);
            alarmService.checkTemperatureAlarm(device, request.getCurrentTemp());
        }

        if ("FAULT".equals(detailStatus)) {
            String faultMessage = defaultString(request.getFaultMessage(), request.getFaultCode());
            alarmService.createAlarm(device.getId(), "FAULT", "CRITICAL", faultMessage);
            webSocketController.pushAlarm(defaultString(device.getTenantId(), "default"),
                    device.getDeviceCode(), "FAULT", faultMessage);
        } else if ("OFFLINE".equals(detailStatus)) {
            alarmService.createAlarm(device.getId(), "OFFLINE", "WARNING", "设备离线");
            webSocketController.pushAlarm(defaultString(device.getTenantId(), "default"),
                    device.getDeviceCode(), "OFFLINE", "设备离线");
        }
    }

    private void saveSnapshot(EqDevice device, GatewayDeviceReportRequest request, LocalDateTime reportedAt) {
        EqDeviceStatus snapshot = new EqDeviceStatus();
        snapshot.setTenantId(defaultString(device.getTenantId(), "default"));
        snapshot.setDeviceCode(device.getDeviceCode());
        snapshot.setDeviceType(device.getDeviceType());
        snapshot.setStatus(device.getStatus());
        snapshot.setDetailStatus(device.getDetailStatus());
        snapshot.setCurrentTemp(request.getCurrentTemp());
        snapshot.setTargetTemp(request.getTargetTemp());
        snapshot.setWaterLevel(request.getWaterLevel());
        snapshot.setPressure(request.getPressure());
        snapshot.setProgressPercent(request.getProgressPercent());
        snapshot.setRemainingTime(request.getRemainingTime());
        snapshot.setFaultCode(request.getFaultCode());
        snapshot.setFaultMessage(request.getFaultMessage());
        snapshot.setSnapshotTime(reportedAt);
        deviceStatusService.saveSnapshot(snapshot);
    }

    private void saveTemperatureLog(EqDevice device, BigDecimal currentTemp, LocalDateTime reportedAt) {
        EqTemperatureLog logEntity = new EqTemperatureLog();
        logEntity.setTenantId(defaultString(device.getTenantId(), "default"));
        logEntity.setDeviceId(device.getId());
        logEntity.setDeviceCode(device.getDeviceCode());
        logEntity.setTemperature(currentTemp);
        logEntity.setCreatedAt(LocalDateTime.now());
        logEntity.setRecordedAt(reportedAt);
        temperatureLogMapper.insert(logEntity);
    }

    private void pushRealtimeState(EqDevice device) {
        Map<String, Object> detail = new HashMap<>();
        detail.put("detailStatus", device.getDetailStatus());
        detail.put("status", device.getStatus());
        detail.put("currentTemp", device.getCurrentTemp());
        detail.put("progressPercent", device.getProgressPercent());
        detail.put("remainingTime", device.getRemainingTime());
        detail.put("deviceType", device.getDeviceType());
        detail.put("waterLevel", device.getWaterLevel());
        detail.put("pressure", device.getPressure());
        detail.put("faultCode", device.getFaultCode());

        webSocketController.pushDeviceStatus(device.getDeviceCode(), device.getStatus(), detail);
        webSocketController.pushTenantDevicesSnapshot(defaultString(device.getTenantId(), "default"));
    }

    private String resolveDetailStatus(GatewayDeviceReportRequest request, EqDevice device) {
        String detailStatus = normalize(request.getDetailStatus());
        if (detailStatus != null) {
            // BUG-26: 校验 detailStatus 枚举有效性
            try {
                DeviceDetailStatus.valueOf(detailStatus);
            } catch (IllegalArgumentException e) {
                log.warn("非法 detailStatus: deviceCode={}, detailStatus={}，使用默认值 IDLE",
                        request.getDeviceCode(), detailStatus);
                detailStatus = "IDLE";
            }
            return detailStatus;
        }

        String status = normalize(request.getStatus());
        if (status == null) {
            return defaultString(device.getDetailStatus(), "IDLE");
        }

        switch (status) {
            case "DECOCTING":
                return "FIRST_DECOCTING";
            case "WAIT_DECOCT":
                return "READY";
            case "WAIT_POUR":
                return "DRAINING";
            case "POURING":
                return "DRAINING";
            case "WAIT_WRAP":
                return "PACKAGE_COMPLETE";
            case "WRAPPING":
                return "PACKAGING";
            case "WAIT_LABEL":
                return "LABEL_COMPLETE";
            case "ALARM":
                return "FAULT";
            default:
                return status;
        }
    }

    private Integer resolveDeviceType(GatewayDeviceReportRequest request) {
        if (request.getDeviceType() != null) {
            return request.getDeviceType();
        }

        if (request.getPayload() != null) {
            Object payloadType = request.getPayload().get("deviceType");
            if (payloadType instanceof Number) {
                return ((Number) payloadType).intValue();
            }
            if (payloadType instanceof String) {
                String raw = ((String) payloadType).trim().toUpperCase();
                if ("DECOCTION_MACHINE".equals(raw)) {
                    return 1;
                }
                if ("WRAPPING_MACHINE".equals(raw)) {
                    return 2;
                }
            }
        }

        String protocolType = normalize(request.getProtocolType());
        if (protocolType != null && protocolType.contains("tcp")) {
            return 1;
        }
        return null;
    }

    private String resolveManufacturer(String protocolType) {
        String normalized = normalize(protocolType);
        if (normalized == null) {
            return null;
        }
        if (normalized.startsWith("PENGLIN")) {
            return "朋霖";
        }
        if (normalized.startsWith("XINYAN")) {
            return "新延";
        }
        if (normalized.startsWith("WEIKANG")) {
            return "卫康";
        }
        return normalized;
    }

    private boolean isSuccessResult(String result) {
        String normalized = normalize(result);
        return normalized == null || "OK".equals(normalized) || "SUCCESS".equals(normalized);
    }

    private <T> T firstNonNull(T value, T fallback) {
        return value != null ? value : fallback;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed.toUpperCase();
    }

    private String defaultString(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }
}
