package cn.org.openygt.equipment.entity;

import cn.org.openygt.common.entity.BaseAuditEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 设备温度日志实体，对应表 {@code eq_temperature_log}。
 *
 * <p>温度数据为审计追踪数据，物理保留不逻辑删除（继承 BaseAuditEntity，无 deleted 字段）。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("eq_temperature_log")
public class EqTemperatureLog extends BaseAuditEntity {

    private Long deviceId;
    private BigDecimal temperature;
    private java.time.LocalDateTime recordedAt;   // 设备上报时间
    private Integer isAlarm;
    private String alarmReason;
}
