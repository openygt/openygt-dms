package cn.org.openygt.production.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("prod_step_log")
public class StepLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Long parentId;
    private String stepType;
    private String deviceId;
    private String operatorId;
    private Date startedAt;
    private Date endedAt;
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
    private Date createdAt;
    private Date updatedAt;
}
