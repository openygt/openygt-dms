package cn.org.openygt.production.service.impl;

import cn.org.openygt.production.entity.RollbackReason;
import cn.org.openygt.production.entity.Task;
import cn.org.openygt.production.entity.TaskRollback;
import cn.org.openygt.production.entity.TaskStatusHistory;
import cn.org.openygt.production.mapper.RollbackReasonMapper;
import cn.org.openygt.production.mapper.TaskMapper;
import cn.org.openygt.production.mapper.TaskRollbackMapper;
import cn.org.openygt.production.mapper.TaskStatusHistoryMapper;
import cn.org.openygt.production.service.TaskRollbackService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskRollbackServiceImpl implements TaskRollbackService {

    private final TaskRollbackMapper rollbackMapper;
    private final RollbackReasonMapper reasonMapper;
    private final TaskMapper taskMapper;
    private final TaskStatusHistoryMapper historyMapper;

    @Override
    @Transactional
    public TaskRollback initiateRollback(Long taskId, String rollbackTo, String reasonCode, String remark, Long operatorId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在");
        }

        TaskRollback rollback = new TaskRollback();
        rollback.setOriginalTaskId(taskId);
        rollback.setRollbackFrom(task.getStatus());
        rollback.setRollbackTo(rollbackTo);
        rollback.setRollbackReason(remark);
        rollback.setOperatorId(operatorId);
        rollback.setCreatedAt(LocalDateTime.now());

        boolean needApproval = false;
        if (reasonCode != null && !reasonCode.isEmpty()) {
            RollbackReason reason = reasonMapper.selectOne(
                    new LambdaQueryWrapper<RollbackReason>().eq(RollbackReason::getReasonCode, reasonCode));
            if (reason != null) {
                rollback.setRollbackType(reason.getId() != null ? reason.getId().intValue() : null);
                needApproval = reason.getNeedApproval() != null && reason.getNeedApproval() == 1;
            }
        }

        if (needApproval) {
            rollback.setApprovalStatus(0);
            rollbackMapper.insert(rollback);
            return rollback;
        }

        // 无需审批，直接执行回退
        rollback.setApprovalStatus(1);
        rollback.setApprovedAt(LocalDateTime.now());
        rollbackMapper.insert(rollback);

        String oldStatus = task.getStatus();
        task.setStatus("已返工");
        taskMapper.updateById(task);
        createHistory(task.getId(), oldStatus, "已返工", operatorId, "返工无需审批: " + (remark != null ? remark : ""));

        Task newTask = createReworkTask(task, rollbackTo);
        taskMapper.insert(newTask);
        createHistory(newTask.getId(), null, newTask.getStatus(), operatorId, "返工创建新任务: 回退到 " + rollbackTo);

        rollback.setNewTaskId(newTask.getId());
        rollbackMapper.updateById(rollback);

        log.info("返工无需审批直接执行: rollbackId={}, originalTask={}, newTask={}, rollbackTo={}",
                rollback.getId(), task.getId(), newTask.getId(), rollbackTo);
        return rollback;
    }

    @Override
    @Transactional
    public TaskRollback approveRollback(Long rollbackId, Long approverId, Integer approvalStatus, String comment) {
        TaskRollback rollback = rollbackMapper.selectById(rollbackId);
        if (rollback == null) {
            throw new IllegalArgumentException("回退记录不存在");
        }
        if (rollback.getApprovalStatus() != null && rollback.getApprovalStatus() != 0) {
            throw new IllegalStateException("该回退记录已审批，不可重复操作");
        }

        rollback.setApproverId(approverId);
        rollback.setApprovalStatus(approvalStatus);
        rollback.setApprovalComment(comment);
        rollback.setApprovedAt(LocalDateTime.now());
        rollbackMapper.updateById(rollback);

        if (approvalStatus != null && approvalStatus == 1) {
            Task originalTask = taskMapper.selectById(rollback.getOriginalTaskId());
            if (originalTask != null) {
                // 1. 原任务标记为已返工
                String oldStatus = originalTask.getStatus();
                originalTask.setStatus("已返工");
                taskMapper.updateById(originalTask);
                createHistory(originalTask.getId(), oldStatus, "已返工", approverId, "返工审批通过: " + (comment != null ? comment : ""));

                // 2. 创建新任务（复制原任务处方信息）
                Task newTask = createReworkTask(originalTask, rollback.getRollbackTo());
                taskMapper.insert(newTask);
                createHistory(newTask.getId(), null, newTask.getStatus(), approverId, "返工创建新任务: 回退到 " + rollback.getRollbackTo());

                // 3. 更新回退记录关联新任务
                rollback.setNewTaskId(newTask.getId());
                rollbackMapper.updateById(rollback);

                log.info("返工审批通过: rollbackId={}, originalTask={}, newTask={}, rollbackTo={}",
                        rollbackId, originalTask.getId(), newTask.getId(), rollback.getRollbackTo());
            }
        }
        return rollback;
    }

    @Override
    public IPage<TaskRollback> listRollbacks(Long taskId, Integer approvalStatus, Integer page, Integer size) {
        LambdaQueryWrapper<TaskRollback> wrapper = new LambdaQueryWrapper<>();
        if (taskId != null) {
            wrapper.eq(TaskRollback::getOriginalTaskId, taskId);
        }
        if (approvalStatus != null) {
            wrapper.eq(TaskRollback::getApprovalStatus, approvalStatus);
        }
        wrapper.orderByDesc(TaskRollback::getCreatedAt);
        return rollbackMapper.selectPage(new Page<>(page == null ? 1 : page, size == null ? 20 : size), wrapper);
    }

    @Override
    public List<RollbackReason> listActiveReasons() {
        return reasonMapper.selectList(
                new LambdaQueryWrapper<RollbackReason>()
                        .eq(RollbackReason::getIsActive, 1)
                        .orderByAsc(RollbackReason::getId)
        );
    }

    /**
     * 创建返工新任务：复制原任务处方信息，生成新条码，状态为回退目标节点。
     */
    private Task createReworkTask(Task original, String rollbackTo) {
        Task task = new Task();
        task.setPrescriptionId(original.getPrescriptionId());
        task.setSchemeId(original.getSchemeId());
        task.setBarcode(generateReworkBarcode(original.getBarcode()));
        task.setStatus(rollbackTo);
        task.setPriority(original.getPriority() != null ? original.getPriority() : 3);
        task.setTenantId(original.getTenantId() != null ? original.getTenantId() : "default");
        task.setPrintStatus("PENDING");
        return task;
    }

    private String generateReworkBarcode(String originalBarcode) {
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        if (originalBarcode != null && !originalBarcode.isEmpty()) {
            return originalBarcode + "-RW" + ts;
        }
        return "RW" + ts;
    }

    private void createHistory(Long taskId, String fromStatus, String toStatus, Long operatorId, String remark) {
        TaskStatusHistory history = new TaskStatusHistory();
        history.setTaskId(taskId);
        history.setFromStatus(fromStatus);
        history.setToStatus(toStatus);
        history.setOperatorId(operatorId != null ? String.valueOf(operatorId) : null);
        history.setOperateTime(LocalDateTime.now());
        history.setRemark(remark);
        historyMapper.insert(history);
    }
}
