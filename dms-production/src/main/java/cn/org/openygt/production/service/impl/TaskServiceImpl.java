package cn.org.openygt.production.service.impl;

import cn.org.openygt.common.dto.EqDeviceDTO;
import cn.org.openygt.common.enums.InspectionResultType;
import cn.org.openygt.common.service.EquipmentService;
import cn.org.openygt.common.service.PrintService;
import cn.org.openygt.production.entity.*;
import cn.org.openygt.production.mapper.*;
import cn.org.openygt.production.service.TaskService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final TaskMapper taskMapper;
    private final TaskStatusHistoryMapper historyMapper;
    private final WorkRecordMapper workRecordMapper;
    private final StepLogMapper stepLogMapper;
    private final HandoverDetailMapper handoverDetailMapper;

    private final EquipmentService equipmentService;
    private final PrintService printService;

    // ==================== 状态机核心 ====================

    @Override
    @Transactional
    public Task startSoak(Long taskId, String operatorId) {
        Task task = getTaskOrThrow(taskId);
        assertStatus(task, "待泡药");
        transition(task, "泡药中", operatorId, "开始泡药");
        task.setSoakStartTime(new Date());
        taskMapper.updateById(task);
        recordWork(taskId, operatorId, null, "SOAK", 0);
        createStepLog(taskId, "SOAK", null, operatorId, null);
        return task;
    }

    @Override
    @Transactional
    public Task endSoak(Long taskId, String operatorId) {
        Task task = getTaskOrThrow(taskId);
        assertStatus(task, "泡药中");
        transition(task, "待煎药", operatorId, "泡药结束");
        task.setSoakEndTime(new Date());
        int duration = calculateDuration(task.getSoakStartTime(), task.getSoakEndTime());
        task.setCurrentStageDuration(duration);
        taskMapper.updateById(task);
        recordWork(taskId, operatorId, null, "SOAK", duration);
        closeLastStepLog(taskId, "SOAK", "正常", null);
        return task;
    }

    @Override
    @Transactional
    public synchronized Task startDecoct(Long taskId, String deviceCode, String operatorId) {
        Task task = getTaskOrThrow(taskId);
        assertStatus(task, "待煎药");
        Long deviceId = equipmentService.getDeviceId(deviceCode);
        if (deviceId == null) {
            EqDeviceDTO created = equipmentService.getOrCreateDevice(deviceCode, 1);
            deviceId = created != null ? created.getId() : equipmentService.getDeviceId(deviceCode);
        }
        String deviceStatus = equipmentService.getDeviceStatus(deviceId);
        if (!"idle".equalsIgnoreCase(deviceStatus)) {
            throw new IllegalStateException("设备不是空闲状态，无法绑定");
        }
        equipmentService.updateDeviceStatus(deviceId, "running");
        transition(task, "煎药中", operatorId, "开始煎药，绑定设备: " + deviceCode);
        task.setDecoctDeviceId(deviceId);
        task.setDecoctStartTime(new Date());
        task.setCurrentStageDuration(0);
        taskMapper.updateById(task);
        recordWork(taskId, operatorId, null, "DECOCT", 0);
        createStepLog(taskId, "DECOCT", deviceCode, operatorId, null);
        return task;
    }

    @Override
    @Transactional
    public Task endDecoct(Long taskId, String operatorId) {
        Task task = getTaskOrThrow(taskId);
        assertStatus(task, "煎药中");
        transition(task, "待出液", operatorId, "煎药结束");
        task.setDecoctEndTime(new Date());
        int duration = calculateDuration(task.getDecoctStartTime(), task.getDecoctEndTime());
        task.setCurrentStageDuration(duration);
        taskMapper.updateById(task);
        recordWork(taskId, operatorId, null, "DECOCT", duration);
        equipmentService.releaseDevice(task.getDecoctDeviceId());
        closeLastStepLog(taskId, "DECOCT", "正常", null);
        if (task.getDecoctDeviceId() != null) {
            String autoLevel = equipmentService.getAutoLevel(task.getDecoctDeviceId());
            if ("auto".equals(autoLevel)) {
                task = startPour(taskId, operatorId);
            }
        }
        return task;
    }

    @Override
    @Transactional
    public Task startPour(Long taskId, String operatorId) {
        Task task = getTaskOrThrow(taskId);
        assertStatus(task, "待出液");
        transition(task, "出液中", operatorId, "开始出液");
        task.setPourStartTime(new Date());
        task.setCurrentStageDuration(0);
        taskMapper.updateById(task);
        createStepLog(taskId, "POUR", null, operatorId, null);
        return task;
    }

    @Override
    @Transactional
    public Task endPour(Long taskId, String operatorId) {
        Task task = getTaskOrThrow(taskId);
        assertStatus(task, "出液中");
        transition(task, "待包装", operatorId, "出液结束");
        task.setPourEndTime(new Date());
        int duration = calculateDuration(task.getPourStartTime(), task.getPourEndTime());
        task.setCurrentStageDuration(duration);
        taskMapper.updateById(task);
        closeLastStepLog(taskId, "POUR", "正常", null);
        if (task.getDecoctDeviceId() != null) {
            String autoLevel = equipmentService.getAutoLevel(task.getDecoctDeviceId());
            if ("auto".equals(autoLevel)) {
                String code = equipmentService.getDeviceCode(task.getDecoctDeviceId());
                task = startWrap(taskId, code, operatorId);
            }
        }
        return task;
    }

    @Override
    @Transactional
    public Task startWrap(Long taskId, String deviceCode, String operatorId) {
        Task task = getTaskOrThrow(taskId);
        assertStatus(task, "待包装");
        Long deviceId;
        synchronized (deviceCode.intern()) {
            deviceId = equipmentService.getDeviceId(deviceCode);
            if (deviceId == null) {
                EqDeviceDTO created = equipmentService.getOrCreateDevice(deviceCode, 2);
                deviceId = created != null ? created.getId() : equipmentService.getDeviceId(deviceCode);
            }
            String deviceStatus = equipmentService.getDeviceStatus(deviceId);
            if (!"idle".equalsIgnoreCase(deviceStatus)) {
                throw new IllegalStateException("包装机不是空闲状态，无法绑定");
            }
            equipmentService.updateDeviceStatus(deviceId, "running");
        }
        transition(task, "包装中", operatorId, "开始包装，绑定包装机: " + deviceCode);
        task.setPackageDeviceId(deviceId);
        task.setWrapStartTime(new Date());
        task.setCurrentStageDuration(0);
        taskMapper.updateById(task);
        recordWork(taskId, operatorId, null, "WRAP", 0);
        createStepLog(taskId, "WRAP", deviceCode, operatorId, null);
        return task;
    }

    @Override
    @Transactional
    public Task endWrap(Long taskId, String operatorId) {
        Task task = getTaskOrThrow(taskId);
        assertStatus(task, "包装中");
        transition(task, "待贴标", operatorId, "包装结束");
        task.setWrapEndTime(new Date());
        int duration = calculateDuration(task.getWrapStartTime(), task.getWrapEndTime());
        task.setCurrentStageDuration(duration);
        taskMapper.updateById(task);
        recordWork(taskId, operatorId, null, "WRAP", duration);
        equipmentService.releaseDevice(task.getPackageDeviceId());
        closeLastStepLog(taskId, "WRAP", "正常", null);
        return task;
    }

    // ==================== V3：贴标、质检、交接 ====================

    @Override
    @Transactional
    public Task confirmLabel(Long taskId, String operatorId) {
        Task task = getTaskOrThrow(taskId);
        assertStatus(task, "待贴标");
        transition(task, "待质检", operatorId, "贴标完成");
        taskMapper.updateById(task);
        createStepLog(taskId, "LABEL", null, operatorId, null);
        closeLastStepLog(taskId, "LABEL", "正常", null);
        return task;
    }

    @Override
    @Transactional
    public Task qualityInspect(Long taskId, InspectionResultType result, String operatorId, String remark) {
        Task task = getTaskOrThrow(taskId);
        assertStatus(task, "待质检");
        doQualityInspect(task, result, operatorId, remark);
        taskMapper.updateById(task);
        createStepLog(taskId, "INSPECT", null, operatorId, null);
        closeLastStepLog(taskId, "INSPECT", result != null ? result.getLabel() : null, remark);
        return task;
    }

    @Override
    @Transactional
    public Task handover(Long taskId, Integer bagCount, String handoverType, String handoverUser, String remark, Boolean isFinal) {
        Task task = getTaskOrThrow(taskId);
        if (!"待交接".equals(task.getStatus()) && !"已部分完成".equals(task.getStatus()) && !"已完成".equals(task.getStatus())) {
            throw new IllegalStateException("任务不在待交接状态，当前状态: " + task.getStatus());
        }
        HandoverDetail detail = new HandoverDetail();
        detail.setTaskId(taskId);
        detail.setBagCount(bagCount);
        detail.setHandoverType(handoverType);
        detail.setHandoverUser(handoverUser);
        detail.setHandoverTime(new Date());
        detail.setRemark(remark);
        handoverDetailMapper.insert(detail);

        task.setHandoverType(handoverType);
        task.setHandoverUser(handoverUser);
        task.setHandoverTime(new Date());

        boolean finalFlag = isFinal != null && isFinal;
        String targetStatus = finalFlag ? "已完成" : "已部分完成";
        transition(task, targetStatus, handoverUser, "扫码交接: " + handoverType + ", 袋数=" + bagCount + (finalFlag ? " (完成)" : " (部分)"));
        if (finalFlag) {
            task.setCompleteTime(new Date());
        }
        taskMapper.updateById(task);
        createStepLog(taskId, "HANDOVER", null, handoverUser, null);
        closeLastStepLog(taskId, "HANDOVER", "正常", remark);
        return task;
    }

    // ==================== V3 工序记录 ====================

    @Override
    @Transactional
    public StepLog pauseStep(Long stepLogId, String reason) {
        StepLog step = stepLogMapper.selectById(stepLogId);
        if (step == null) throw new IllegalArgumentException("工序记录不存在");
        step.setIsPaused(1);
        step.setPauseReason(reason);
        stepLogMapper.updateById(step);
        return step;
    }

    @Override
    @Transactional
    public StepLog resumeStep(Long stepLogId) {
        StepLog step = stepLogMapper.selectById(stepLogId);
        if (step == null) throw new IllegalArgumentException("工序记录不存在");
        int pausedMinutes = calculateDuration(step.getUpdatedAt(), new Date());
        step.setPauseDuration((step.getPauseDuration() != null ? step.getPauseDuration() : 0) + pausedMinutes);
        step.setIsPaused(0);
        stepLogMapper.updateById(step);
        return step;
    }

    @Override
    public List<StepLog> queryStepLogs(Long taskId) {
        LambdaQueryWrapper<StepLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StepLog::getTaskId, taskId).orderByAsc(StepLog::getStartedAt);
        return stepLogMapper.selectList(wrapper);
    }

    @Override
    public List<HandoverDetail> queryHandoverDetails(Long taskId) {
        LambdaQueryWrapper<HandoverDetail> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HandoverDetail::getTaskId, taskId).orderByDesc(HandoverDetail::getHandoverTime);
        return handoverDetailMapper.selectList(wrapper);
    }

    // ==================== 绑定、查询、温度 ====================

    @Override
    @Transactional
    public Task bindDevice(Long taskId, String deviceCode) {
        Task task = getTaskOrThrow(taskId);
        if (!"待泡药".equals(task.getStatus()) && !"待煎药".equals(task.getStatus())) {
            throw new IllegalStateException("任务状态不允许绑定设备");
        }
        Long deviceId;
        synchronized (deviceCode.intern()) {
            deviceId = equipmentService.getDeviceId(deviceCode);
            if (deviceId == null) {
                equipmentService.getOrCreateDevice(deviceCode, 1);
                deviceId = equipmentService.getDeviceId(deviceCode);
            }
            String deviceStatus = equipmentService.getDeviceStatus(deviceId);
            if ("running".equals(deviceStatus)) {
                throw new IllegalStateException("设备已被占用");
            }
            equipmentService.updateDeviceStatus(deviceId, "running");
        }
        String fromStatus = task.getStatus();
        task.setDecoctDeviceId(deviceId);
        task.setStatus("待煎药".equals(fromStatus) ? "煎药中" : "待煎药");
        taskMapper.updateById(task);
        recordHistory(taskId, fromStatus, task.getStatus(), null, "绑定设备: " + deviceCode);
        return task;
    }

    @Override
    public IPage<Task> queryTasks(String status, Long deviceId, int page, int size) {
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) wrapper.eq(Task::getStatus, status);
        if (deviceId != null) {
            wrapper.and(w -> w.eq(Task::getDecoctDeviceId, deviceId).or().eq(Task::getPackageDeviceId, deviceId));
        }
        wrapper.orderByDesc(Task::getCreatedAt);
        return taskMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    @Transactional
    public Task updateTemperature(String deviceCode, BigDecimal temperature) {
        Long deviceId = equipmentService.getDeviceId(deviceCode);
        if (deviceId == null) return null;
        Task task = taskMapper.selectOne(
                new LambdaQueryWrapper<Task>()
                        .eq(Task::getDecoctDeviceId, deviceId)
                        .orderByDesc(Task::getCreatedAt)
                        .last("LIMIT 1"));
        if (task == null) return null;
        task.setCurrentTemp(temperature);
        if ("待煎药".equals(task.getStatus())) {
            task.setStatus("煎药中");
            recordHistory(task.getId(), "待煎药", "煎药中", null, "温度上报自动推进");
        }
        taskMapper.updateById(task);
        equipmentService.updateTemperature(deviceId, temperature);
        equipmentService.checkTemperatureAlarm(deviceId, temperature);
        return task;
    }

    @Override
    public Task getById(Long taskId) { return taskMapper.selectById(taskId); }

    @Override
    public int clearAll() { return taskMapper.delete(null); }

    // ==================== 打印（已抽取到 dms-print，此处仅委托） ====================

    @Override
    public List<Task> queryPrintTasks(String printStatus) {
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Task::getStatus, "待贴标");
        if (printStatus != null && !printStatus.isEmpty()) {
            wrapper.eq(Task::getPrintStatus, printStatus);
        } else {
            wrapper.and(w -> w.eq(Task::getPrintStatus, "PENDING").or().eq(Task::getPrintStatus, "FAILED"));
        }
        wrapper.orderByDesc(Task::getCompleteTime);
        return taskMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public Task printLabel(Long taskId, String deviceCode, String operatorId) {
        Task task = getTaskOrThrow(taskId);
        assertStatus(task, "待贴标");
        printService.submitPrintTask(taskId, deviceCode, operatorId);
        task.setPrintStatus("PRINTED");
        task.setPrintTime(new Date());
        Long deviceId = equipmentService.getDeviceId(deviceCode);
        task.setPrintDeviceId(deviceId);
        taskMapper.updateById(task);
        recordHistory(taskId, "待贴标", "PRINTED", operatorId, "打印标签成功，打印机: " + deviceCode);
        return task;
    }

    @Override
    @Transactional
    public Task retryPrint(Long taskId, String deviceCode, String operatorId) {
        Task task = getTaskOrThrow(taskId);
        if (!"FAILED".equals(task.getPrintStatus()) && !"PENDING".equals(task.getPrintStatus()) && !"PRINTED".equals(task.getPrintStatus())) {
            throw new IllegalStateException("当前任务不可重试打印");
        }
        task.setPrintStatus("PENDING");
        task.setPrintDeviceId(null);
        task.setPrintTime(null);
        taskMapper.updateById(task);

        printService.retryPrint(taskId, deviceCode, operatorId);

        task.setPrintStatus("PRINTED");
        task.setPrintTime(new Date());
        Long deviceId = equipmentService.getDeviceId(deviceCode);
        task.setPrintDeviceId(deviceId);
        taskMapper.updateById(task);
        recordHistory(taskId, "PENDING", "PRINTED", operatorId, "重试打印标签成功，打印机: " + deviceCode);
        return task;
    }

    // ==================== 强制操作 ====================

    @Override
    @Transactional
    public Task forceStatus(Long taskId, String targetStatus, String operatorId, String deviceCode, String remark) {
        Task task = getTaskOrThrow(taskId);
        String oldStatus = task.getStatus();
        Date now = new Date();
        if ("煎药中".equals(targetStatus) && deviceCode != null && !deviceCode.isEmpty()) {
            EqDeviceDTO created = equipmentService.getOrCreateDevice(deviceCode, 1);
            Long devId = created != null ? created.getId() : equipmentService.getDeviceId(deviceCode);
            equipmentService.updateDeviceStatus(devId, "running");
            task.setDecoctDeviceId(devId);
            task.setDecoctStartTime(now);
        } else if ("包装中".equals(targetStatus) && deviceCode != null && !deviceCode.isEmpty()) {
            EqDeviceDTO created = equipmentService.getOrCreateDevice(deviceCode, 2);
            Long devId = created != null ? created.getId() : equipmentService.getDeviceId(deviceCode);
            equipmentService.updateDeviceStatus(devId, "running");
            task.setPackageDeviceId(devId);
            task.setWrapStartTime(now);
        }
        task.setStatus(targetStatus);
        task.setOperatorId(operatorId);
        taskMapper.updateById(task);
        recordHistory(taskId, oldStatus, targetStatus, operatorId, "【强制操作】" + oldStatus + " → " + targetStatus + (remark != null ? " | " + remark : ""));
        return task;
    }

    // ==================== 私有辅助方法 ====================

    private Task getTaskOrThrow(Long taskId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) throw new IllegalArgumentException("任务不存在");
        return task;
    }

    private void assertStatus(Task task, String expected) {
        if (!expected.equals(task.getStatus())) {
            throw new IllegalStateException("任务状态不正确，期望: " + expected + "，实际: " + task.getStatus());
        }
    }

    private void transition(Task task, String newStatus, String operatorId, String remark) {
        String oldStatus = task.getStatus();
        task.setStatus(newStatus);
        task.setOperatorId(operatorId);
        recordHistory(task.getId(), oldStatus, newStatus, operatorId, remark);
    }

    private void recordHistory(Long taskId, String fromStatus, String toStatus, String operatorId, String remark) {
        TaskStatusHistory history = new TaskStatusHistory();
        history.setTaskId(taskId);
        history.setFromStatus(fromStatus);
        history.setToStatus(toStatus);
        history.setOperatorId(operatorId);
        history.setOperateTime(new Date());
        history.setRemark(remark);
        historyMapper.insert(history);
    }

    private void recordWork(Long taskId, String operatorId, String operatorName, String action, Integer workTime) {
        WorkRecord record = new WorkRecord();
        record.setTaskId(taskId);
        record.setOperatorId(operatorId);
        record.setOperatorName(operatorName);
        record.setAction(action);
        record.setWorkTime(workTime);
        workRecordMapper.insert(record);
    }

    private int calculateDuration(Date start, Date end) {
        if (start == null || end == null) return 0;
        return (int) TimeUnit.MILLISECONDS.toMinutes(end.getTime() - start.getTime());
    }

    private void doQualityInspect(Task task, InspectionResultType result, String operatorId, String remark) {
        switch (result) {
            case PASS:
                transition(task, "待交接", operatorId, "质检通过" + (remark != null ? ": " + remark : ""));
                break;
            case CONCESSION:
                transition(task, "待交接", operatorId, "质检让步放行" + (remark != null ? ": " + remark : ""));
                task.setIsException(1);
                task.setExceptionReason(remark);
                break;
            case REWORK:
                transition(task, "待煎药", operatorId, "质检返工" + (remark != null ? ": " + remark : ""));
                // 释放设备，防止返工任务被占用
                if (task.getDecoctDeviceId() != null) {
                    equipmentService.releaseDevice(task.getDecoctDeviceId());
                }
                if (task.getPackageDeviceId() != null) {
                    equipmentService.releaseDevice(task.getPackageDeviceId());
                }
                break;
            case SCRAP:
                transition(task, "已报废", operatorId, "质检报废" + (remark != null ? ": " + remark : ""));
                task.setIsException(1);
                task.setExceptionReason(remark);
                break;
            default:
                throw new IllegalArgumentException("未知的质检结果: " + result);
        }
    }


    private void createStepLog(Long taskId, String stepType, String deviceId, String operatorId, Long parentId) {
        StepLog step = new StepLog();
        step.setTaskId(taskId);
        step.setStepType(stepType);
        step.setDeviceId(deviceId);
        step.setOperatorId(operatorId);
        step.setParentId(parentId);
        step.setStartedAt(new Date());
        step.setResult("正常");
        stepLogMapper.insert(step);
    }

    private void closeLastStepLog(Long taskId, String stepType, String result, String abortReason) {
        LambdaQueryWrapper<StepLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StepLog::getTaskId, taskId)
               .eq(StepLog::getStepType, stepType)
               .isNull(StepLog::getEndedAt)
               .orderByDesc(StepLog::getStartedAt)
               .last("LIMIT 1");
        StepLog step = stepLogMapper.selectOne(wrapper);
        if (step != null) {
            step.setEndedAt(new Date());
            step.setResult(result);
            step.setAbortReason(abortReason);
            stepLogMapper.updateById(step);
        }
    }
}
