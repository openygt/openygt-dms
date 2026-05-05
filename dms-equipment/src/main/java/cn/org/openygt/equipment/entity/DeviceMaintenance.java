package cn.org.openygt.equipment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("eq_device_maintenance")
public class DeviceMaintenance {

    @TableId(type = IdType.AUTO)
    private Long id;

    @NotNull(message = "设备ID不能为空")
    private Long deviceId;

    @NotBlank(message = "维保类型不能为空")
    private String maintenanceType;

    @NotBlank(message = "维保内容不能为空")
    private String content;

    private String parts;
    private BigDecimal cost;
    private String operatorId;

    @NotNull(message = "计划日期不能为空")
    private LocalDate planDate;

    private LocalDate finishDate;

    @NotNull(message = "状态不能为空")
    private Integer status;

    private String tenantId;
    private Integer deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
