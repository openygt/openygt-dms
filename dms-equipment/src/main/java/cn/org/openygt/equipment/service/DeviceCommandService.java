package cn.org.openygt.equipment.service;

import cn.org.openygt.equipment.entity.DeviceCommand;

import java.util.List;

public interface DeviceCommandService {
    DeviceCommand createCommand(String deviceCode, String commandType, String payload);
    DeviceCommand sendCommand(Long commandId);
    DeviceCommand handleAck(Long commandId, String responsePayload);
    DeviceCommand handleFailure(Long commandId, String failReason);
    List<DeviceCommand> getPendingCommands(String deviceCode);
    List<DeviceCommand> getRecentCommands(String deviceCode, int limit);
}
