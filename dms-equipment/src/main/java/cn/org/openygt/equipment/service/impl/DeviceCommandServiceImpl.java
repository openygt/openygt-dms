package cn.org.openygt.equipment.service.impl;

import cn.org.openygt.equipment.entity.DeviceCommand;
import cn.org.openygt.equipment.mapper.DeviceCommandMapper;
import cn.org.openygt.equipment.service.DeviceCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceCommandServiceImpl implements DeviceCommandService {

    private final DeviceCommandMapper commandMapper;

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
        return command;
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
