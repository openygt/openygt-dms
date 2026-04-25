package cn.org.openygt.production.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("prod_step_log")
public class StepLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Long parentId;
    private String stepType;
    private Long deviceId;
    private String operatorId;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private Integer isPaused;
    private String pauseReason;
    private Integer pauseDuration;
    private Integer delayMinutes;
    private String delayReason;
    private String result;
    private String abortReason;
    private BigDecimal wasteAmount;
    private String wasteUnit;
    private Integer isRetry;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
