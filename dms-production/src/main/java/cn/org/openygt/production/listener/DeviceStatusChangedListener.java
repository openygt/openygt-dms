package cn.org.openygt.production.listener;

import cn.org.openygt.common.event.DeviceStatusChangedEvent;
import cn.org.openygt.production.entity.Task;
import cn.org.openygt.production.entity.TaskStatusHistory;
import cn.org.openygt.production.mapper.TaskMapper;
import cn.org.openygt.production.mapper.TaskStatusHistoryMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * BUG-10/BUG-17 修复：监听设备状态变化事件，同步推进绑定任务的步骤和状态。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceStatusChangedListener {

    private final TaskMapper taskMapper;
    private final TaskStatusHistoryMapper historyMapper;

    @Async
    @EventListener
    @Transactional
    public void onDeviceStatusChanged(DeviceStatusChangedEvent event) {
        Long deviceId = event.getDeviceId();
        String newStatus = event.getNewStatus();
        if (deviceId == null || newStatus == null) return;

        String newStep = mapDeviceStatusToTaskStep(newStatus);
        if (newStep == null) return;

        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Task::getDecoctDeviceId, deviceId)
               .in(Task::getStatus, "WAIT_SOAK", "SOAKING", "WAIT_DECOCT", "DECOCTING",
                   "WAIT_POUR", "POURING", "WAIT_WRAP", "WRAPPING", "WAIT_LABEL");
        List<Task> tasks = taskMapper.selectList(wrapper);
        if (tasks == null || tasks.isEmpty()) return;

        for (Task task : tasks) {
            boolean updated = false;

            // 1. 推进 currentStep
            if (!newStep.equals(task.getCurrentStep())) {
                task.setCurrentStep(newStep);
                updated = true;
                log.info("任务步骤自动推进: taskId={}, deviceCode={}, detailStatus={}, currentStep={}",
                        task.getId(), event.getDeviceCode(), newStatus, newStep);
            }

            // 2. BUG-17: 同步推进 task.status 状态机
            String targetStatus = mapDeviceStatusToTaskStatus(newStatus, task.getStatus());
            if (targetStatus != null && !targetStatus.equals(task.getStatus())) {
                String oldStatus = task.getStatus();
                task.setStatus(targetStatus);
                fillStartTimeIfNeeded(task, targetStatus);
                updated = true;
                recordHistory(task.getId(), oldStatus, targetStatus,
                        "设备状态变化自动推进: " + event.getDeviceCode() + " " + event.getOldStatus() + " → " + newStatus);
                log.info("任务状态自动推进: taskId={}, {} → {}, deviceCode={}",
                        task.getId(), oldStatus, targetStatus, event.getDeviceCode());
            }

            if (updated) {
                taskMapper.updateById(task);
            }
        }
    }

    /**
     * 设备 detailStatus → 任务 currentStep 映射
     */
    private String mapDeviceStatusToTaskStep(String detailStatus) {
        switch (detailStatus) {
            case "SOAKING": return "SOAK";
            case "FIRST_DECOCTING":
            case "SECOND_DECOCTING": return "DECOCT";
            case "DRAINING": return "POUR";
            case "PACKAGING": return "WRAP";
            case "IDLE": return "COMPLETE";
            default: return null;
        }
    }

    /**
     * BUG-17: 设备 detailStatus + 当前任务 status → 目标任务 status
     */
    private String mapDeviceStatusToTaskStatus(String deviceStatus, String currentTaskStatus) {
        switch (deviceStatus) {
            case "SOAKING":
                if ("WAIT_SOAK".equals(currentTaskStatus)) return "SOAKING";
                return null;
            case "FIRST_DECOCTING":
            case "SECOND_DECOCTING":
                if ("WAIT_DECOCT".equals(currentTaskStatus) || "SOAKING".equals(currentTaskStatus)) {
                    return "DECOCTING";
                }
                return null;
            case "DRAINING":
                if ("DECOCTING".equals(currentTaskStatus)) return "WAIT_POUR";
                if ("WAIT_POUR".equals(currentTaskStatus)) return "POURING";
                return null;
            case "PACKAGING":
                if ("POURING".equals(currentTaskStatus)) return "WAIT_WRAP";
                if ("WAIT_WRAP".equals(currentTaskStatus)) return "WRAPPING";
                return null;
            case "IDLE":
                // IDLE 不自动推进任务状态，避免误推（可能是设备空闲、故障恢复等）
                return null;
            default:
                return null;
        }
    }

    /**
     * 自动推进时补充开始时间（若为空），避免后续 duration 计算异常
     */
    private void fillStartTimeIfNeeded(Task task, String targetStatus) {
        LocalDateTime now = LocalDateTime.now();
        switch (targetStatus) {
            case "SOAKING":
                if (task.getSoakStartTime() == null) task.setSoakStartTime(now);
                break;
            case "DECOCTING":
                if (task.getDecoctStartTime() == null) task.setDecoctStartTime(now);
                if (task.getCurrentStageDuration() == null) task.setCurrentStageDuration(0);
                break;
            case "POURING":
                if (task.getPourStartTime() == null) task.setPourStartTime(now);
                if (task.getCurrentStageDuration() == null) task.setCurrentStageDuration(0);
                break;
            case "WRAPPING":
                if (task.getWrapStartTime() == null) task.setWrapStartTime(now);
                if (task.getCurrentStageDuration() == null) task.setCurrentStageDuration(0);
                break;
            default:
                break;
        }
    }

    private void recordHistory(Long taskId, String fromStatus, String toStatus, String remark) {
        try {
            TaskStatusHistory history = new TaskStatusHistory();
            history.setTaskId(taskId);
            history.setFromStatus(fromStatus);
            history.setToStatus(toStatus);
            history.setOperateTime(LocalDateTime.now());
            history.setRemark(remark);
            historyMapper.insert(history);
        } catch (Exception e) {
            log.error("记录任务状态历史失败: taskId={}", taskId, e);
        }
    }
}
