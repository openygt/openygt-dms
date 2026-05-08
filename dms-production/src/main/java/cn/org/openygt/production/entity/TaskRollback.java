package cn.org.openygt.production.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("prod_task_rollback")
public class TaskRollback {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long originalTaskId;
    private Long newTaskId;
    private String rollbackFrom;
    private String rollbackTo;
    private String rollbackReason;
    private Integer rollbackType;
    private Long operatorId;
    private Long reviewerId;
    private Long approverId;
    private Integer approvalStatus;
    private String approvalComment;
    private Integer inventoryReverted;
    private Integer deviceReset;
    private Integer patientNotified;
    private LocalDateTime createdAt;
    private LocalDateTime approvedAt;
}
