package cn.org.openygt.production.service;

import cn.org.openygt.production.entity.RollbackReason;
import cn.org.openygt.production.entity.TaskRollback;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;

public interface TaskRollbackService {
    TaskRollback initiateRollback(Long taskId, String rollbackTo, String reasonCode, String remark, Long operatorId);
    TaskRollback approveRollback(Long rollbackId, Long approverId, Integer approvalStatus, String comment);
    IPage<TaskRollback> listRollbacks(Long taskId, Integer approvalStatus, Integer page, Integer size);
    List<RollbackReason> listActiveReasons();
}
