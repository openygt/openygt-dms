package cn.org.openygt.common.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 任务状态历史传输对象。
 */
@Data
public class TaskStatusHistoryDTO implements Serializable {

    private Long id;
    private Long taskId;
    private String fromStatus;
    private String toStatus;
    private String operatorId;
    private LocalDateTime operateTime;
    private String remark;
}
