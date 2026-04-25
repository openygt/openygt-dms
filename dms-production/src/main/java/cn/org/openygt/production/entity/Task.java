package cn.org.openygt.production.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("prod_task")
public class Task {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String tenantId;
    private Long prescriptionId;
    private Long decoctDeviceId;
    private Long packageDeviceId;
    private Long schemeId;
    private String operatorId;
    private String status;
    private BigDecimal currentTemp;
    private BigDecimal targetTemp;
    private Integer soakDuration;
    private LocalDateTime soakStartTime;
    private LocalDateTime soakEndTime;
    private LocalDateTime decoctStartTime;
    private LocalDateTime decoctEndTime;
    private LocalDateTime pourStartTime;
    private LocalDateTime pourEndTime;
    private LocalDateTime wrapStartTime;
    private LocalDateTime wrapEndTime;
    private LocalDateTime completeTime;
    private Integer currentStageDuration;
    private Long printDeviceId;
    private String printStatus;
    private LocalDateTime printTime;
    private String currentStep;
    private String poolId;
    private Integer printCopies;
    private Integer isException;
    private String exceptionReason;
    private String patientAgreement;
    private BigDecimal standardCost;
    private BigDecimal actualCost;
    private String handoverType;
    private String handoverUser;
    private LocalDateTime handoverTime;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
