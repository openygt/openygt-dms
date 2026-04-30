package cn.org.openygt.equipment.entity;

import cn.org.openygt.common.entity.BaseAuditEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 设备实时状态快照表（无逻辑删除，物理保留历史）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("eq_device_status")
public class EqDeviceStatus extends BaseAuditEntity {

    /** 设备编码 */
    private String deviceCode;
    /** 设备类型 */
    private Integer deviceType;
    /** 兼容旧状态 */
    private String status;
    /** 精细状态 */
    private String detailStatus;
    /** 当前温度 */
    private BigDecimal currentTemp;
    /** 目标温度 */
    private BigDecimal targetTemp;
    /** 水位 */
    private Integer waterLevel;
    /** 压力 */
    private BigDecimal pressure;
    /** 当前处方编号 */
    private String prescriptionCode;
    /** 当前方案名称 */
    private String schemeName;
    /** 操作人ID */
    private Long operatorId;
    /** 操作人姓名 */
    private String operatorName;
    /** 进度百分比 */
    private Integer progressPercent;
    /** 剩余秒数 */
    private Integer remainingTime;
    /** 故障码 */
    private String faultCode;
    /** 故障描述 */
    private String faultMessage;
    /** 快照时间 */
    private LocalDateTime snapshotTime;
}
