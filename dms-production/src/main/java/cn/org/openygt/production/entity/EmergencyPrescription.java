package cn.org.openygt.production.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("prod_emergency_prescription")
public class EmergencyPrescription {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long prescriptionId;
    private Integer emergencyLevel;
    private LocalDateTime requestTime;
    private LocalDateTime promisedFinishTime;
    private LocalDateTime actualFinishTime;
    private Integer isOnTime;
    private String delayReason;
    private String deliveryType;
    private String deliveryLocation;
    private String nurseName;
    private LocalDateTime nurseSignTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
