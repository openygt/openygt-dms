package cn.org.openygt.production.service.impl;

import cn.org.openygt.common.dto.EqDeviceDTO;
import cn.org.openygt.common.enums.InspectionResultType;
import cn.org.openygt.common.enums.TaskStatus;
import cn.org.openygt.common.service.EquipmentService;
import cn.org.openygt.common.service.PrintService;
import cn.org.openygt.common.service.QualityService;
import cn.org.openygt.common.service.RetainSampleFacade;
import cn.org.openygt.inventory.dto.ConsumeItemRequest;
import cn.org.openygt.inventory.dto.ConsumeRecordRequest;
import cn.org.openygt.inventory.service.ConsumeRecordService;
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
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final TaskMapper taskMapper;
    private final TaskStatusHistoryMapper historyMapper;
    private final WorkRecordMapper workRecordMapper;
    private final StepLogMapper stepLogMapper;
    private final HandoverDetailMapper handoverDetailMapper;
    private final PrescriptionMedicineMapper prescriptionMedicineMapper;
    private final PrescriptionMapper prescriptionMapper;

    private final EquipmentService equipmentService;
    private final PrintService printService;
    private final ConsumeRecordService consumeRecordService;

    private final HrEmployeeMapper hrEmployeeMapper;

    private final RetainSampleFacade retainSampleFacade;
    private final QualityService qualityService;

    // ==================== 状态机核心 ====================

    @Override
    @Transactional
    public Task startSoak(Long taskId, String operatorId) {
        Task task = getTaskOrThrow(taskId);
        assertStatus(task, TaskStatus.WAIT_SOAK.getLabel());
        transition(task, TaskStatus.SOAKING.getLabel(), operatorId, "开始泡药");
        task.setSoakStartTime(LocalDateTime.now());
        taskMapper.updateById(task);
        recordWork(taskId, operatorId, null, "SOAK", 0);
        createStepLog(taskId, "SOAK", null, operatorId, null);

        // V2.0：泡药开始时尝试记录消耗流水（可选，不阻断任务推进）
        tryRecordConsume(task, operatorId);

        return task;
    }

    @Override
    @Transactional
    public Task endSoak(Long taskId, String operatorId) {
        Task task = getTaskOrThrow(taskId);
        assertStatus(task, TaskStatus.SOAKING.getLabel());
        transition(task, TaskStatus.WAIT_DECOCT.getLabel(), operatorId, "泡药结束");
        task.setSoakEndTime(LocalDateTime.now());
        int duration = calculateDuration(task.getSoakStartTime(), task.getSoakEndTime());
        task.setCurrentStageDuration(duration);
        taskMapper.updateById(task);
        recordWork(taskId, operatorId, null, "SOAK", duration);
        closeLastStepLog(taskId, "SOAK", "正常", null);
        return task;
    }

    @Override
    @Transactional
    public Task startDecoct(Long taskId, String deviceCode, String operatorId) {
        Task task = taskMapper.selectByIdForUpdate(taskId);
        if (task == null) throw new IllegalArgumentException("任务不存在");
        assertStatus(task, TaskStatus.WAIT_DECOCT.getLabel());
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
        transition(task, TaskStatus.DECOCTING.getLabel(), operatorId, "开始煎药，绑定设备: " + deviceCode);
        task.setDecoctDeviceId(deviceId);
        task.setDecoctStartTime(LocalDateTime.now());
        task.setCurrentStageDuration(0);
        taskMapper.updateById(task);
        recordWork(taskId, operatorId, null, "DECOCT", 0);
        createStepLog(taskId, "DECOCT", deviceId, operatorId, null);
        return task;
    }

    @Override
    @Transactional
    public Task endDecoct(Long taskId, String operatorId) {
        Task task = getTaskOrThrow(taskId);
        assertStatus(task, TaskStatus.DECOCTING.getLabel());
        transition(task, TaskStatus.WAIT_POUR.getLabel(), operatorId, "煎药结束");
        task.setDecoctEndTime(LocalDateTime.now());
        int duration = calculateDuration(task.getDecoctStartTime(), task.getDecoctEndTime());
        task.setCurrentStageDuration(duration);
        taskMapper.updateById(task);
        recordWork(taskId, operatorId, null, "DECOCT", duration);
        // 先读取设备自动化级别，再释放设备，避免并发抢占后读到错误状态
        String autoLevel = null;
        if (task.getDecoctDeviceId() != null) {
            autoLevel = equipmentService.getAutoLevel(task.getDecoctDeviceId());
        }
        equipmentService.releaseDevice(task.getDecoctDeviceId());
        closeLastStepLog(taskId, "DECOCT", "正常", null);
        if ("auto".equals(autoLevel)) {
            task = startPour(taskId, operatorId);
        }
        return task;
    }

    @Override
    @Transactional
    public Task startPour(Long taskId, String operatorId) {
        Task task = getTaskOrThrow(taskId);
        assertStatus(task, TaskStatus.WAIT_POUR.getLabel());
        transition(task, TaskStatus.POURING.getLabel(), operatorId, "开始出液");
        task.setPourStartTime(LocalDateTime.now());
        task.setCurrentStageDuration(0);
        taskMapper.updateById(task);
        createStepLog(taskId, "POUR", null, operatorId, null);
        return task;
    }

    @Override
    @Transactional
    public Task endPour(Long taskId, String operatorId) {
        Task task = getTaskOrThrow(taskId);
        assertStatus(task, TaskStatus.POURING.getLabel());
        transition(task, TaskStatus.WAIT_WRAP.getLabel(), operatorId, "出液结束");
        task.setPourEndTime(LocalDateTime.now());
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
        Task task = taskMapper.selectByIdForUpdate(taskId);
        if (task == null) throw new IllegalArgumentException("任务不存在");
        assertStatus(task, TaskStatus.WAIT_WRAP.getLabel());
        Long deviceId = null;
        // 若未传入设备编码（如测试环境或尚未绑定包装机），跳过设备锁定直接推进状态
        if (deviceCode != null && !deviceCode.trim().isEmpty()) {
            // 使用数据库悲观锁（FOR UPDATE）替代单机 synchronized，支持分布式部署
            EqDeviceDTO lockedDevice = equipmentService.lockDeviceByCode(deviceCode);
            if (lockedDevice == null) {
                EqDeviceDTO created = equipmentService.getOrCreateDevice(deviceCode, 2);
                lockedDevice = equipmentService.lockDeviceByCode(deviceCode);
                if (lockedDevice == null) {
                    throw new IllegalStateException("设备创建后锁定失败: " + deviceCode);
                }
            }
            deviceId = lockedDevice.getId();
            if (!("idle".equalsIgnoreCase(lockedDevice.getStatus()) || "IDLE".equals(lockedDevice.getStatus()))) {
                throw new IllegalStateException("包装机不是空闲状态，无法绑定");
            }
            equipmentService.updateDeviceStatus(deviceId, "running");
            transition(task, TaskStatus.WRAPPING.getLabel(), operatorId, "开始包装，绑定包装机: " + deviceCode);
            task.setPackageDeviceId(deviceId);
        } else {
            transition(task, TaskStatus.WRAPPING.getLabel(), operatorId, "开始包装（未绑定设备）");
        }
        task.setWrapStartTime(LocalDateTime.now());
        task.setCurrentStageDuration(0);
        taskMapper.updateById(task);
        recordWork(taskId, operatorId, null, "WRAP", 0);
        createStepLog(taskId, "WRAP", deviceId, operatorId, null);
        return task;
    }

    @Override
    @Transactional
    public Task endWrap(Long taskId, String operatorId) {
        Task task = getTaskOrThrow(taskId);
        assertStatus(task, TaskStatus.WRAPPING.getLabel());
        transition(task, TaskStatus.WAIT_LABEL.getLabel(), operatorId, "包装结束");
        task.setWrapEndTime(LocalDateTime.now());
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
        assertStatus(task, TaskStatus.WAIT_LABEL.getLabel());
        transition(task, TaskStatus.WAIT_QC.getLabel(), operatorId, "贴标完成");
        taskMapper.updateById(task);
        createStepLog(taskId, "LABEL", null, operatorId, null);
        closeLastStepLog(taskId, "LABEL", "正常", null);
        return task;
    }

    @Override
    @Transactional
    public Task qualityInspect(Long taskId, InspectionResultType result, String operatorId, String remark, String reworkNode) {
        Task task = getTaskOrThrow(taskId);
        assertStatus(task, TaskStatus.WAIT_QC.getLabel());
        // 任务状态与质检台账在同一事务内提交；台账写入失败则整单回滚
        qualityService.inspect(taskId, result, operatorId, remark, reworkNode);
        doQualityInspect(task, result, operatorId, remark, reworkNode);
        taskMapper.updateById(task);
        tryCreateRetainSampleAfterPass(task, result, operatorId);
        createStepLog(taskId, "INSPECT", null, operatorId, null);
        closeLastStepLog(taskId, "INSPECT", result != null ? result.getLabel() : null, remark);
        return task;
    }

    @Override
    @Transactional
    public Task qualityInspectWithItems(Long taskId, InspectionResultType result, String operatorId, String remark,
                                        String reworkNode, List<cn.org.openygt.common.dto.InspectionItemDTO> items) {
        Task task = getTaskOrThrow(taskId);
        assertStatus(task, TaskStatus.WAIT_QC.getLabel());
        // 先写完整质检台账（含检查项），失败则整单回滚，避免任务已推进但台账缺失
        qualityService.inspectWithItems(taskId, result, operatorId, remark, reworkNode, items);
        doQualityInspect(task, result, operatorId, remark, reworkNode);
        taskMapper.updateById(task);
        tryCreateRetainSampleAfterPass(task, result, operatorId);
        createStepLog(taskId, "INSPECT", null, operatorId, null);
        closeLastStepLog(taskId, "INSPECT", result != null ? result.getLabel() : null, remark);
        return task;
    }

    @Override
    @Transactional
    public Task handover(Long taskId, Integer bagCount, String handoverType, String handoverUser, String remark, Boolean isFinal) {
        Task task = getTaskOrThrow(taskId);
        if (!TaskStatus.WAIT_HANDOVER.getLabel().equals(task.getStatus()) && !TaskStatus.PARTIAL_COMPLETED.getLabel().equals(task.getStatus()) && !TaskStatus.COMPLETED.getLabel().equals(task.getStatus())) {
            throw new IllegalStateException("任务不在待交接状态，当前状态: " + task.getStatus());
        }
        HandoverDetail detail = new HandoverDetail();
        detail.setTaskId(taskId);
        detail.setBagCount(bagCount);
        detail.setHandoverType(handoverType);
        detail.setHandoverUser(handoverUser);
        detail.setHandoverTime(LocalDateTime.now());
        detail.setRemark(remark);
        handoverDetailMapper.insert(detail);

        task.setHandoverType(handoverType);
        task.setHandoverUser(handoverUser);
        task.setHandoverTime(LocalDateTime.now());

        boolean finalFlag = isFinal != null && isFinal;
        String targetStatus = finalFlag ? TaskStatus.COMPLETED.getLabel() : TaskStatus.PARTIAL_COMPLETED.getLabel();
        transition(task, targetStatus, handoverUser, "扫码交接: " + handoverType + ", 袋数=" + bagCount + (finalFlag ? " (完成)" : " (部分)"));
        if (finalFlag) {
            task.setCompleteTime(LocalDateTime.now());
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
        int pausedMinutes = calculateDuration(step.getUpdatedAt(), LocalDateTime.now());
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
        Task task = taskMapper.selectByIdForUpdate(taskId);
        if (task == null) throw new IllegalArgumentException("任务不存在");
        if (!TaskStatus.WAIT_SOAK.getLabel().equals(task.getStatus()) && !TaskStatus.WAIT_DECOCT.getLabel().equals(task.getStatus())) {
            throw new IllegalStateException("任务状态不允许绑定设备");
        }
        Long deviceId;
        // 使用数据库悲观锁（FOR UPDATE）替代单机 synchronized，支持分布式部署
        EqDeviceDTO lockedDevice = equipmentService.lockDeviceByCode(deviceCode);
        if (lockedDevice == null) {
            equipmentService.getOrCreateDevice(deviceCode, 1);
            lockedDevice = equipmentService.lockDeviceByCode(deviceCode);
            if (lockedDevice == null) {
                throw new IllegalStateException("设备创建后锁定失败: " + deviceCode);
            }
        }
        deviceId = lockedDevice.getId();
        if ("running".equalsIgnoreCase(lockedDevice.getStatus()) || "RUNNING".equals(lockedDevice.getStatus())) {
            throw new IllegalStateException("设备已被占用");
        }
        equipmentService.updateDeviceStatus(deviceId, "running");
        String fromStatus = task.getStatus();
        task.setDecoctDeviceId(deviceId);
        task.setStatus(TaskStatus.WAIT_DECOCT.getLabel().equals(fromStatus) ? TaskStatus.DECOCTING.getLabel() : TaskStatus.WAIT_DECOCT.getLabel());
        taskMapper.updateById(task);
        recordHistory(taskId, fromStatus, task.getStatus(), null, "绑定设备: " + deviceCode);
        return task;
    }

    @Override
    public IPage<Task> queryTasks(String status, Long deviceId, Long id, Long prescriptionId,
                                  String operatorId, String operatorKeyword, String prescriptionNumber,
                                  String startTime, String endTime, int page, int size) {
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            if (status.contains(",")) {
                wrapper.in(Task::getStatus, java.util.Arrays.asList(status.split(",")));
            } else {
                wrapper.eq(Task::getStatus, status);
            }
        }
        if (deviceId != null) {
            wrapper.and(w -> w.eq(Task::getDecoctDeviceId, deviceId).or().eq(Task::getPackageDeviceId, deviceId));
        }
        if (id != null) wrapper.eq(Task::getId, id);
        if (prescriptionId != null) wrapper.eq(Task::getPrescriptionId, prescriptionId);
        if (prescriptionNumber != null && !prescriptionNumber.isEmpty()) {
            List<cn.org.openygt.production.entity.Prescription> matched = prescriptionMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<cn.org.openygt.production.entity.Prescription>()
                            .like(cn.org.openygt.production.entity.Prescription::getPrescriptionNumber, prescriptionNumber));
            if (!matched.isEmpty()) {
                wrapper.in(Task::getPrescriptionId, matched.stream().map(cn.org.openygt.production.entity.Prescription::getId).collect(Collectors.toList()));
            } else {
                wrapper.eq(Task::getId, -1L); // 无匹配返回空
            }
        }
        if (operatorId != null && !operatorId.isEmpty()) {
            wrapper.eq(Task::getOperatorId, operatorId);
        } else if (operatorKeyword != null && !operatorKeyword.trim().isEmpty()) {
            String k = operatorKeyword.trim();
            wrapper.and(w -> w.like(Task::getOperatorName, k)
                    .or().eq(Task::getOperatorId, k));
        }
        if (startTime != null && !startTime.isEmpty()) wrapper.ge(Task::getCreatedAt, startTime);
        if (endTime != null && !endTime.isEmpty()) wrapper.le(Task::getCreatedAt, endTime);
        wrapper.orderByDesc(Task::getCreatedAt);
        IPage<Task> result = taskMapper.selectPage(new Page<>(page, size), wrapper);
        // 批量补充处方号
        List<Task> tasks = result.getRecords();
        if (tasks != null && !tasks.isEmpty()) {
            List<Long> presIds = tasks.stream()
                    .map(Task::getPrescriptionId)
                    .filter(java.util.Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
            if (!presIds.isEmpty()) {
                List<cn.org.openygt.production.entity.Prescription> presList = prescriptionMapper.selectBatchIds(presIds);
                Map<Long, String> presNumMap = presList.stream()
                        .collect(java.util.HashMap::new,
                                (map, p) -> map.put(p.getId(), p.getPrescriptionNumber()),
                                java.util.HashMap::putAll);
                tasks.forEach(t -> t.setPrescriptionNumber(presNumMap.get(t.getPrescriptionId())));
            }
        }
        return result;
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
        if (TaskStatus.WAIT_DECOCT.getLabel().equals(task.getStatus())) {
            task.setStatus(TaskStatus.DECOCTING.getLabel());
            recordHistory(task.getId(), TaskStatus.WAIT_DECOCT.getLabel(), TaskStatus.DECOCTING.getLabel(), null, "温度上报自动推进");
        }
        taskMapper.updateById(task);
        equipmentService.updateTemperature(deviceId, temperature);
        equipmentService.checkTemperatureAlarm(deviceId, temperature);
        return task;
    }

    @Override
    public Task getById(Long taskId) { return taskMapper.selectById(taskId); }

    @Override
    public Task getByBarcode(String barcode) {
        if (barcode == null || barcode.trim().isEmpty()) {
            return null;
        }
        return taskMapper.selectByBarcode(barcode.trim());
    }

    @Override
    public int clearAll() { return taskMapper.delete(null); }

    // ==================== 打印（已抽取到 dms-print，此处仅委托） ====================

    @Override
    public List<Task> queryPrintTasks(String printStatus) {
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Task::getStatus, TaskStatus.WAIT_LABEL.getLabel());
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
        assertStatus(task, TaskStatus.WAIT_LABEL.getLabel());
        printService.submitPrintTask(taskId, deviceCode, operatorId);
        // 打印状态由 dms-print 维护，通过 SPI 反查
        String printStatus = printService.getPrintStatus(taskId);
        task.setPrintStatus(printStatus != null ? printStatus : "PRINTED");
        task.setPrintTime(LocalDateTime.now());
        Long deviceId = equipmentService.getDeviceId(deviceCode);
        task.setPrintDeviceId(deviceId);
        taskMapper.updateById(task);
        recordHistory(taskId, TaskStatus.WAIT_LABEL.getLabel(), "PRINTED", operatorId, "打印标签成功，打印机: " + deviceCode);
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

        // 打印状态由 dms-print 维护，通过 SPI 反查
        String printStatus = printService.getPrintStatus(taskId);
        task.setPrintStatus(printStatus != null ? printStatus : "PRINTED");
        task.setPrintTime(LocalDateTime.now());
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
        LocalDateTime now = LocalDateTime.now();
        if (TaskStatus.DECOCTING.getLabel().equals(targetStatus) && deviceCode != null && !deviceCode.isEmpty()) {
            EqDeviceDTO created = equipmentService.getOrCreateDevice(deviceCode, 1);
            Long devId = created != null ? created.getId() : equipmentService.getDeviceId(deviceCode);
            equipmentService.updateDeviceStatus(devId, "running");
            task.setDecoctDeviceId(devId);
            task.setDecoctStartTime(now);
        } else if (TaskStatus.WRAPPING.getLabel().equals(targetStatus) && deviceCode != null && !deviceCode.isEmpty()) {
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
        if (operatorId != null) {
            task.setOperatorId(operatorId);
            try {
                List<cn.org.openygt.production.entity.HrEmployee> users = hrEmployeeMapper.findByIds(
                        java.util.Collections.singletonList(Long.valueOf(operatorId)));
                if (!users.isEmpty()) {
                    task.setOperatorName(users.get(0).getRealName());
                }
            } catch (Exception ignored) {
            }
        }
        recordHistory(task.getId(), oldStatus, newStatus, operatorId, remark);
    }

    private void recordHistory(Long taskId, String fromStatus, String toStatus, String operatorId, String remark) {
        TaskStatusHistory history = new TaskStatusHistory();
        history.setTaskId(taskId);
        history.setFromStatus(fromStatus);
        history.setToStatus(toStatus);
        history.setOperatorId(operatorId != null ? operatorId : "SYSTEM");
        history.setOperateTime(LocalDateTime.now());
        history.setRemark(remark);
        historyMapper.insert(history);
    }

    private void recordWork(Long taskId, String operatorId, String operatorName, String action, Integer workTime) {
        if (operatorId == null) return;
        WorkRecord record = new WorkRecord();
        record.setTaskId(taskId);
        record.setOperatorId(operatorId);
        record.setOperatorName(operatorName);
        record.setAction(action);
        record.setWorkTime(workTime);
        workRecordMapper.insert(record);
    }

    private int calculateDuration(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) return 0;
        return (int) ChronoUnit.MINUTES.between(start, end);
    }

    /**
     * 留样仅在生产质检接口中创建一次，避免与 qt/inspect 双写重复；失败不阻断任务状态。
     */
    private void tryCreateRetainSampleAfterPass(Task task, InspectionResultType result, String operatorId) {
        if (result != InspectionResultType.PASS && result != InspectionResultType.CONCESSION) {
            return;
        }
        if (task.getPrescriptionId() == null) {
            return;
        }
        try {
            retainSampleFacade.createAfterPass(task.getId(), task.getPrescriptionId(), operatorId);
        } catch (Exception e) {
            log.error("留样创建失败，不阻断质检: taskId={}", task.getId(), e);
        }
    }

    private void doQualityInspect(Task task, InspectionResultType result, String operatorId, String remark, String reworkNode) {
        switch (result) {
            case PASS:
                transition(task, TaskStatus.WAIT_HANDOVER.getLabel(), operatorId, "质检通过" + (remark != null ? ": " + remark : ""));
                break;
            case CONCESSION:
                transition(task, TaskStatus.WAIT_HANDOVER.getLabel(), operatorId, "质检让步放行" + (remark != null ? ": " + remark : ""));
                task.setIsException(1);
                task.setExceptionReason(remark);
                break;
            case REWORK:
                String targetStatus = reworkNode != null && !reworkNode.isEmpty() ? reworkNode : TaskStatus.WAIT_DECOCT.getLabel();
                transition(task, targetStatus, operatorId, "质检返工→" + targetStatus + (remark != null ? ": " + remark : ""));
                // 返工预留设备：防止返工任务无设备可用（设备若已 IDLE 则预留，若仍 RUNNING 则保持）
                if (task.getDecoctDeviceId() != null) {
                    equipmentService.reserveDevice(task.getId(), task.getDecoctDeviceId());
                }
                if (task.getPackageDeviceId() != null) {
                    equipmentService.reserveDevice(task.getId(), task.getPackageDeviceId());
                }
                break;
            case SCRAP:
                transition(task, TaskStatus.SCRAPPED.getLabel(), operatorId, "质检报废" + (remark != null ? ": " + remark : ""));
                task.setIsException(1);
                task.setExceptionReason(remark);
                break;
            default:
                throw new IllegalArgumentException("未知的质检结果: " + result);
        }
    }


    /**
     * 尝试记录任务消耗流水。失败仅记录日志，不回滚任务推进主事务。
     */
    private void tryRecordConsume(Task task, String operatorId) {
        try {
            if (task.getPrescriptionId() == null) {
                log.warn("任务[{}]无关联处方，跳过消耗记录", task.getId());
                return;
            }
            List<PrescriptionMedicine> medicines = prescriptionMedicineMapper.selectByPrescriptionId(task.getPrescriptionId());
            if (medicines == null || medicines.isEmpty()) {
                log.info("任务[{}]处方无药材明细，跳过消耗记录", task.getId());
                return;
            }
            ConsumeRecordRequest request = new ConsumeRecordRequest();
            request.setTaskId(task.getId());
            request.setOperatorId(operatorId);
            request.setItems(new ArrayList<>());
            for (PrescriptionMedicine med : medicines) {
                ConsumeItemRequest item = new ConsumeItemRequest();
                item.setMedicineId(med.getMedicineId());
                item.setMedicineName(med.getMedicineName());
                item.setQuantity(med.getDosage());
                item.setUnit(med.getUnit());
                item.setRemark(med.getMedUsage());
                request.getItems().add(item);
            }
            List<Long> logIds = consumeRecordService.recordConsume(request);
            log.info("任务[{}]消耗记录写入完成，流水数={}", task.getId(), logIds.size());
        } catch (Exception e) {
            log.error("任务[{}]消耗记录写入失败（已忽略，不阻断主流程）", task.getId(), e);
        }
    }

    private void createStepLog(Long taskId, String stepType, Long deviceId, String operatorId, Long parentId) {
        StepLog step = new StepLog();
        step.setTaskId(taskId);
        step.setStepType(stepType);
        step.setDeviceId(deviceId);
        step.setOperatorId(operatorId != null ? operatorId : "SYSTEM");
        step.setParentId(parentId);
        step.setStartedAt(LocalDateTime.now());
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
            step.setEndedAt(LocalDateTime.now());
            step.setResult(result);
            step.setAbortReason(abortReason);
            stepLogMapper.updateById(step);
        }
    }

    // ==================== 挂起/恢复 (V30 新增) ====================

    @Override
    @Transactional
    public Task suspendTask(Long taskId, String operatorId, String reason, Integer suspendType) {
        Task task = taskMapper.selectByIdForUpdate(taskId);
        if (task == null) throw new IllegalArgumentException("任务不存在");
        String currentStatus = task.getStatus();
        cn.org.openygt.common.enums.TaskStatusTransition.validateSuspend(currentStatus);
        
        task.setSuspendedFrom(currentStatus);
        task.setSuspendReason(reason);
        task.setSuspendTime(LocalDateTime.now());
        transition(task, TaskStatus.SUSPENDED.getLabel(), operatorId, "挂起任务: " + reason);
        
        // 释放设备
        if (task.getDecoctDeviceId() != null) {
            equipmentService.releaseDevice(task.getDecoctDeviceId());
            task.setDecoctDeviceId(null);
        }
        if (task.getPackageDeviceId() != null) {
            equipmentService.releaseDevice(task.getPackageDeviceId());
            task.setPackageDeviceId(null);
        }
        taskMapper.updateById(task);
        return task;
    }

    @Override
    @Transactional
    public Task resumeTask(Long taskId, String operatorId) {
        Task task = taskMapper.selectByIdForUpdate(taskId);
        if (task == null) throw new IllegalArgumentException("任务不存在");
        cn.org.openygt.common.enums.TaskStatusTransition.validateResume(task.getStatus(), task.getSuspendedFrom());
        
        String targetStatus = task.getSuspendedFrom();
        transition(task, targetStatus, operatorId, "恢复任务");
        task.setSuspendedFrom(null);
        task.setSuspendReason(null);
        task.setSuspendTime(null);
        task.setExpectedResumeTime(null);
        taskMapper.updateById(task);
        return task;
    }

    @Override
    @Transactional
    public Task assignOperator(Long taskId, String newOperatorId, String newOperatorName, String actingOperatorId) {
        if (newOperatorId == null || newOperatorId.trim().isEmpty()) {
            throw new IllegalArgumentException("操作人不能为空");
        }
        Task task = taskMapper.selectByIdForUpdate(taskId);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在");
        }
        String st = task.getStatus();
        if (TaskStatus.COMPLETED.getLabel().equals(st) || TaskStatus.SCRAPPED.getLabel().equals(st)) {
            throw new IllegalStateException("已完成或已报废任务不能改派操作人");
        }
        String oldOp = task.getOperatorId();
        task.setOperatorId(newOperatorId.trim());
        if (newOperatorName != null && !newOperatorName.trim().isEmpty()) {
            task.setOperatorName(newOperatorName.trim());
        } else {
            try {
                List<HrEmployee> users = hrEmployeeMapper.findByIds(
                        java.util.Collections.singletonList(Long.valueOf(newOperatorId.trim())));
                if (!users.isEmpty()) {
                    task.setOperatorName(users.get(0).getRealName());
                }
            } catch (Exception ignored) {
            }
        }
        taskMapper.updateById(task);
        String remark = "改派操作人: " + (oldOp != null ? oldOp : "-") + " → " + newOperatorId.trim();
        recordHistory(taskId, st, st, actingOperatorId != null ? actingOperatorId : "SYSTEM", remark);
        return task;
    }
}
