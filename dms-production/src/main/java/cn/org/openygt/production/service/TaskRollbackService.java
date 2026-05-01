package cn.org.openygt.production.service;

import cn.org.openygt.production.entity.TaskRollback;

import java.util.List;

public interface TaskRollbackService {
    TaskRollback initiateRollback(Long taskId, String rollbackTo, String reasonCode, String remark, Long operatorId);
    TaskRollback approveRollback(Long rollbackId, Long approverId, Integer approvalStatus, String comment);
    List<TaskRollback> listRollbacks(Long taskId, Integer approvalStatus);
}
