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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
        } else {
            rollback.setApprovalStatus(1);
            rollback.setApprovedAt(LocalDateTime.now());
            doRollbackTask(task, rollbackTo, operatorId, remark);
        }

        rollbackMapper.insert(rollback);
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
            Task task = taskMapper.selectById(rollback.getOriginalTaskId());
            if (task != null) {
                doRollbackTask(task, rollback.getRollbackTo(), approverId, comment);
            }
        }
        return rollback;
    }

    @Override
    public List<TaskRollback> listRollbacks(Long taskId, Integer approvalStatus) {
        LambdaQueryWrapper<TaskRollback> wrapper = new LambdaQueryWrapper<>();
        if (taskId != null) {
            wrapper.eq(TaskRollback::getOriginalTaskId, taskId);
        }
        if (approvalStatus != null) {
            wrapper.eq(TaskRollback::getApprovalStatus, approvalStatus);
        }
        wrapper.orderByDesc(TaskRollback::getCreatedAt);
        return rollbackMapper.selectList(wrapper);
    }

    private void doRollbackTask(Task task, String rollbackTo, Long operatorId, String remark) {
        String oldStatus = task.getStatus();
        task.setStatus(rollbackTo);
        taskMapper.updateById(task);

        TaskStatusHistory history = new TaskStatusHistory();
        history.setTaskId(task.getId());
        history.setFromStatus(oldStatus);
        history.setToStatus(rollbackTo);
        history.setOperatorId(operatorId != null ? String.valueOf(operatorId) : null);
        history.setOperateTime(LocalDateTime.now());
        history.setRemark("回退操作: " + (remark != null ? remark : ""));
        historyMapper.insert(history);
    }
}
