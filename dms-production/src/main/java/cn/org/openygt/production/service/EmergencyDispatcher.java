package cn.org.openygt.production.service;

import cn.org.openygt.production.entity.Task;
import cn.org.openygt.production.mapper.TaskMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmergencyDispatcher {

    private final TaskMapper taskMapper;
    private final TaskService taskService;

    public DispatchResult dispatch(Long emergencyPrescriptionId, Integer preferredDeviceType) {
        log.info("紧急处方调度: prescriptionId={}", emergencyPrescriptionId);

        Task interruptible = findInterruptibleTask();
        if (interruptible != null) {
            taskService.suspendTask(interruptible.getId(), "SYSTEM", "紧急处方插队", 5);
            return new DispatchResult("INTERRUPT", interruptible.getDecoctDeviceId(),
                    "JY-" + interruptible.getDecoctDeviceId(), interruptible.getId(), null, 0);
        }

        return new DispatchResult("QUEUE", null, null, null, 1, 15);
    }

    private Task findInterruptibleTask() {
        return taskMapper.selectOne(
                new LambdaQueryWrapper<Task>()
                        .eq(Task::getStatus, "泡药中")
                        .isNotNull(Task::getDecoctDeviceId)
                        .apply("TIMESTAMPDIFF(MINUTE, soak_start_time, NOW()) < soak_duration * 0.5")
                        .orderByAsc(Task::getSoakStartTime)
                        .last("LIMIT 1")
        );
    }

    @Data
    public static class DispatchResult {
        private String dispatchResult;
        private Long assignedDeviceId;
        private String deviceCode;
        private Long interruptedTaskId;
        private Integer queuePosition;
        private Integer estimatedWaitMinutes;

        public DispatchResult(String dispatchResult, Long assignedDeviceId, String deviceCode,
                               Long interruptedTaskId, Integer queuePosition, Integer estimatedWaitMinutes) {
            this.dispatchResult = dispatchResult;
            this.assignedDeviceId = assignedDeviceId;
            this.deviceCode = deviceCode;
            this.interruptedTaskId = interruptedTaskId;
            this.queuePosition = queuePosition;
            this.estimatedWaitMinutes = estimatedWaitMinutes;
        }
    }
}
