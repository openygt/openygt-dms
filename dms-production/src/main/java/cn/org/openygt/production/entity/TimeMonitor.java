package cn.org.openygt.production.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("dms_time_monitor")
public class TimeMonitor {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Long assignmentId;
    private Long prescriptionId;
    private String stage;
    private LocalDateTime plannedStart;
    private LocalDateTime plannedEnd;
    private LocalDateTime actualStart;
    private LocalDateTime actualEnd;
    private Integer remainingSeconds;
    private Integer status;
    private Integer alertLevel;
    private Integer warningCount;
    private LocalDateTime lastWarningTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
