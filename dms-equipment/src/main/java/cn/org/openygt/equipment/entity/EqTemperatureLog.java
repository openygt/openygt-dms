package cn.org.openygt.equipment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 设备温度日志实体，对应表 {@code eq_temperature_log}。
 *
 * <p>该表在线上库中仅包含遥测相关字段，不包含 updated_at / alarm_reason / is_alarm 等扩展列。</p>
 */
@Data
@TableName("eq_temperature_log")
public class EqTemperatureLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("tenant_id")
    private String tenantId = "default";

    private Long deviceId;
    private String deviceCode;
    private BigDecimal temperature;
    private LocalDateTime createdAt;
    private LocalDateTime recordedAt;
}
