package cn.org.openygt.equipment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("eq_device_maintenance")
public class DeviceMaintenance {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long deviceId;
    private String maintenanceType;
    private String content;
    private String parts;
    private BigDecimal cost;
    private String operatorId;
    private LocalDate planDate;
    private LocalDate finishDate;
    private Integer status;
    private String tenantId;
    private Integer deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
