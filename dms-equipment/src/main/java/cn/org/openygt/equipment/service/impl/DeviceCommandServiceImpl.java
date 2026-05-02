package cn.org.openygt.equipment.service.impl;

import cn.org.openygt.common.service.EquipmentService;
import cn.org.openygt.equipment.entity.DeviceCommand;
import cn.org.openygt.equipment.entity.EqDevice;
import cn.org.openygt.equipment.mapper.DeviceCommandMapper;
import cn.org.openygt.equipment.gateway.IoTGatewayClient;
import cn.org.openygt.equipment.mapper.EqDeviceMapper;
import cn.org.openygt.equipment.service.DeviceCommandService;
import cn.org.openygt.equipment.websocket.DeviceWebSocketController;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 设备指令服务实现。
 *
 * <p>Phase 3 解耦说明：已移除对 MqttConfig 的直接依赖，指令下发通过 dms-iot-gateway HTTP API 完成。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceCommandServiceImpl implements DeviceCommandService {

    private final DeviceCommandMapper commandMapper;
    private final EqDeviceMapper deviceMapper;
    private final EquipmentService equipmentService;
    private final DeviceWebSocketController webSocketController;
    private final ObjectMapper objectMapper;
    private final IoTGatewayClient iotGatewayClient;

    @Override
    @Transactional
    public DeviceCommand createCommand(String deviceCode, String commandType, String payload) {
        DeviceCommand command = new DeviceCommand();
        command.setDeviceCode(deviceCode);
        command.setCommandType(commandType);
        command.setCommandPayload(payload);
        command.setStatus("PENDING");
        command.setRetryCount(0);
        command.setCreatedAt(LocalDateTime.now());
        command.setUpdatedAt(LocalDateTime.now());
        commandMapper.insert(command);
        log.info("指令已创建: {} -> {} [id={}]", deviceCode, commandType, command.getId());

        // 1. 更新设备预期状态
        updateDeviceExpectedStatus(deviceCode, commandType);

        // 2. 通过 dms-iot-gateway 下发指令到设备
        // TODO: 集成 IoTGatewayClient 调用 POST /api/v1/iot/command
        sendCommandToDevice(deviceCode, command);

        // 3. 标记为已发送
        command.setStatus("SENT");
        command.setSendTime(LocalDateTime.now());
        command.setUpdatedAt(LocalDateTime.now());
        commandMapper.updateById(command);

        return command;
    }

    private void updateDeviceExpectedStatus(String deviceCode, String commandType) {
        Long deviceId = equipmentService.getDeviceId(deviceCode);
        if (deviceId == null) {
            log.warn("设备不存在，跳过状态更新: {}", deviceCode);
            return;
        }
        String detailStatus = resolveDetailStatus(commandType);
        if (detailStatus != null) {
            equipmentService.updateDeviceStatus(deviceId, detailStatus);
            log.info("设备预期状态更新: {} -> {}", deviceCode, detailStatus);
            // 推送 WebSocket 状态变更
            pushDeviceStatusChange(deviceCode);
        }
    }

    private void pushDeviceStatusChange(String deviceCode) {
        try {
            QueryWrapper<EqDevice> wrapper = new QueryWrapper<>();
            wrapper.eq("device_code", deviceCode);
            EqDevice device = deviceMapper.selectOne(wrapper);
            if (device != null) {
                Map<String, Object> detail = new HashMap<>();
                detail.put("detailStatus", device.getDetailStatus());
                detail.put("status", device.getStatus());
                detail.put("currentTemp", device.getCurrentTemp());
                detail.put("progressPercent", 0);
                detail.put("remainingTime", 0);
                detail.put("deviceType", device.getDeviceType());
                webSocketController.pushDeviceStatus(deviceCode, device.getStatus(), detail);
                log.info("WebSocket状态已推送: {} -> {}", deviceCode, device.getDetailStatus());
            }
        } catch (Exception e) {
            log.error("WebSocket状态推送失败: {}", deviceCode, e);
        }
    }

    private String resolveDetailStatus(String commandType) {
        switch (commandType) {
            case "START_SOAK": return "SOAKING";
            case "START_DECOCT": return "FIRST_DECOCTING";
            case "START_PACKAGE": return "PACKAGING";
            case "PAUSE":
            case "PAUSE_PRINT": return "PAUSED";
            case "RESUME": return "BUSY";
            case "EMERGENCY_STOP": return "FAULT";
            case "START_PRINT":
            case "REPRINT_LABEL": return "PRINTING";
            case "STOP":
            case "END_TASK": return "IDLE";
            default: return null;
        }
    }

    private void sendCommandToDevice(String deviceCode, DeviceCommand command) {
        try {
            // 查询设备协议类型
            QueryWrapper<EqDevice> wrapper = new QueryWrapper<>();
            wrapper.eq("device_code", deviceCode);
            EqDevice device = deviceMapper.selectOne(wrapper);
            String protocolType = device != null ? device.getProtocolType() : "penglin-mqtt";

            Map<String, Object> params = new HashMap<>();
            params.put("commandId", command.getId());
            params.put("payload", command.getCommandPayload());
            params.put("timestamp", System.currentTimeMillis());

            iotGatewayClient.sendCommand(deviceCode, protocolType, command.getCommandType(), params);
            log.info("指令已通过网关下发: {} -> {} [protocol={}]", deviceCode, command.getCommandType(), protocolType);
        } catch (Exception e) {
            log.error("指令下发失败: {} -> {}", deviceCode, command.getCommandType(), e);
        }
    }

    @Override
    @Transactional
    public DeviceCommand sendCommand(Long commandId) {
        DeviceCommand command = commandMapper.selectById(commandId);
        if (command == null) {
            throw new IllegalArgumentException("指令不存在: " + commandId);
        }
        command.setStatus("SENT");
        command.setSendTime(LocalDateTime.now());
        command.setUpdatedAt(LocalDateTime.now());
        commandMapper.updateById(command);
        log.info("指令已发送: {} [id={}]", command.getCommandType(), commandId);
        return command;
    }

    @Override
    @Transactional
    public DeviceCommand handleAck(Long commandId, String responsePayload) {
        DeviceCommand command = commandMapper.selectById(commandId);
        if (command == null) {
            throw new IllegalArgumentException("指令不存在: " + commandId);
        }
        command.setStatus("ACKED");
        command.setResponsePayload(responsePayload);
        command.setAckTime(LocalDateTime.now());
        command.setUpdatedAt(LocalDateTime.now());
        commandMapper.updateById(command);
        log.info("指令已确认: {} [id={}]", command.getCommandType(), commandId);
        return command;
    }

    @Override
    @Transactional
    public DeviceCommand handleFailure(Long commandId, String failReason) {
        DeviceCommand command = commandMapper.selectById(commandId);
        if (command == null) {
            throw new IllegalArgumentException("指令不存在: " + commandId);
        }
        command.setStatus("FAILED");
        command.setFailReason(failReason);
        command.setUpdatedAt(LocalDateTime.now());
        commandMapper.updateById(command);
        log.warn("指令失败: {} [id={}] reason={}", command.getCommandType(), commandId, failReason);
        return command;
    }

    @Override
    public List<DeviceCommand> getPendingCommands(String deviceCode) {
        return commandMapper.findByDeviceCodeAndStatus(deviceCode, "PENDING");
    }

    @Override
    public List<DeviceCommand> getRecentCommands(String deviceCode, int limit) {
        return commandMapper.findRecentByDeviceCode(deviceCode, limit);
    }
}
