package cn.org.openygt.equipment.entity;

import cn.org.openygt.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("device_utilization")
public class DeviceUtilization extends BaseEntity {

    private String deviceCode;
    private LocalDate statDate;
    private Integer totalMinutes;
    private Integer runMinutes;
    private Integer idleMinutes;
    private Integer faultMinutes;
    private Integer offlineMinutes;
    private Integer maintenanceMinutes;
    private BigDecimal utilizationRate;
    private BigDecimal availabilityRate;
    private Integer faultCount;
    private Integer taskCount;
}
