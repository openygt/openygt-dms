package cn.org.openygt.equipment.controller;

import cn.org.openygt.equipment.EquipmentModule;
import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.equipment.entity.DeviceCommand;
import cn.org.openygt.equipment.service.DeviceCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping(EquipmentModule.API_PREFIX + "/commands")
@RequiredArgsConstructor
public class DeviceCommandController {

    private final DeviceCommandService commandService;

    @PostMapping
    public ApiResponse<DeviceCommand> createCommand(@RequestBody Map<String, Object> request) {
        String deviceCode = (String) request.get("deviceCode");
        String commandType = (String) request.get("commandType");
        String payload = request.get("payload") != null ? request.get("payload").toString() : null;
        return ApiResponse.success(commandService.createCommand(deviceCode, commandType, payload));
    }

    @PostMapping("/{commandId}/send")
    public ApiResponse<DeviceCommand> sendCommand(@PathVariable Long commandId) {
        return ApiResponse.success(commandService.sendCommand(commandId));
    }

    @PostMapping("/{commandId}/ack")
    public ApiResponse<DeviceCommand> ackCommand(@PathVariable Long commandId,
                                                  @RequestBody Map<String, Object> request) {
        String responsePayload = request.get("responsePayload") != null ? request.get("responsePayload").toString() : null;
        return ApiResponse.success(commandService.handleAck(commandId, responsePayload));
    }

    @PostMapping("/{commandId}/fail")
    public ApiResponse<DeviceCommand> failCommand(@PathVariable Long commandId,
                                                   @RequestBody Map<String, Object> request) {
        String failReason = (String) request.get("failReason");
        return ApiResponse.success(commandService.handleFailure(commandId, failReason));
    }

    @GetMapping("/device/{deviceCode}/pending")
    public ApiResponse<List<DeviceCommand>> getPendingCommands(@PathVariable String deviceCode) {
        return ApiResponse.success(commandService.getPendingCommands(deviceCode));
    }

    @GetMapping("/device/{deviceCode}/recent")
    public ApiResponse<List<DeviceCommand>> getRecentCommands(@PathVariable String deviceCode,
                                                               @RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.success(commandService.getRecentCommands(deviceCode, limit));
    }
}
