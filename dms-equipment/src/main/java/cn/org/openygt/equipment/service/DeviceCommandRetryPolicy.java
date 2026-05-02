package cn.org.openygt.equipment.service;

import cn.org.openygt.equipment.entity.DeviceCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 设备指令超时重试策略
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceCommandRetryPolicy {

    private static final int MAX_RETRY = 3;
    private static final long TIMEOUT_MS = 10_000;
    private static final long[] RETRY_INTERVALS = {5_000, 15_000, 30_000};

    private final DeviceCommandService deviceCommandService;

    public void sendWithRetry(DeviceCommand command) {
        for (int i = 0; i <= MAX_RETRY; i++) {
            try {
                sendAndWaitAck(command, TIMEOUT_MS);
                deviceCommandService.handleAck(command.getId(), "SUCCESS");
                return;
            } catch (Exception e) {
                if (i < MAX_RETRY) {
                    log.warn("指令超时，第{}次重试，等待{}ms, commandId={}", i + 1, RETRY_INTERVALS[i], command.getId());
                    try {
                        TimeUnit.MILLISECONDS.sleep(RETRY_INTERVALS[i]);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        // 重试耗尽
        deviceCommandService.handleFailure(command.getId(), "RETRY_EXHAUSTED");
        log.error("设备指令重试耗尽: commandId={}", command.getId());
        // 实际生产环境：创建异常工单 + 任务挂起
    }

    private void sendAndWaitAck(DeviceCommand command, long timeoutMs) throws Exception {
        // 简化实现：实际应通过 MQTT/TCP 发送并等待 ACK
        // 这里模拟可能超时的情况
        DeviceCommand sent = deviceCommandService.sendCommand(command.getId());
        if (sent == null || !"SENT".equals(sent.getStatus())) {
            throw new RuntimeException("Send command timeout");
        }
    }
}
