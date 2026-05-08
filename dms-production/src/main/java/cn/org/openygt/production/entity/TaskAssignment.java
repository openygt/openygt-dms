package cn.org.openygt.production.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("prod_task_assignment")
public class TaskAssignment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Long prescriptionId;
    private Long deviceId;
    private Long employeeId;
    private Integer assignType;
    private String assignReason;
    private LocalDateTime scheduledStartTime;
    private LocalDateTime scheduledEndTime;
    private LocalDateTime actualStartTime;
    private LocalDateTime actualEndTime;
    private Integer status;
    private String stageBreakdownJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
