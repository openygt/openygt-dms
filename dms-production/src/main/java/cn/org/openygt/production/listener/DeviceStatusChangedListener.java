package cn.org.openygt.production.listener;

import cn.org.openygt.common.event.DeviceStatusChangedEvent;
import cn.org.openygt.production.entity.Task;
import cn.org.openygt.production.mapper.TaskMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * BUG-10 修复：监听设备状态变化事件，同步推进绑定任务的步骤。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceStatusChangedListener {

    private final TaskMapper taskMapper;

    @Async
    @EventListener
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
            if (!newStep.equals(task.getCurrentStep())) {
                task.setCurrentStep(newStep);
                taskMapper.updateById(task);
                log.info("任务步骤自动推进: taskId={}, deviceCode={}, detailStatus={}, currentStep={}",
                        task.getId(), event.getDeviceCode(), newStatus, newStep);
            }
        }
    }

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
}
