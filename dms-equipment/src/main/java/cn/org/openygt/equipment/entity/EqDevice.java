package cn.org.openygt.equipment.entity;

import cn.org.openygt.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("eq_device")
public class EqDevice extends BaseEntity {

    private String deviceCode;
    private String name;
    private Integer deviceType;
    private String ipAddress;
    private Integer port;
    private String protocolType;
    private Integer locationX;
    private Integer locationY;
    private Integer decoctMode;
    private Integer pressureMode;
    private Integer slowFireTime;
    private Integer packageNum;
    private Integer packageCapacity;
    private BigDecimal alarmMinTemp;
    private BigDecimal alarmMaxTemp;
    private String faultCode;
    private String version;
    private String status;
    private BigDecimal currentTemp;
    private Integer enabled;
    private String autoLevel;
    private String labelMode;
    private Long currentSchemeId;
    private Long groupId;
    private LocalDateTime lastHeartbeat;

    // ===== Phase 1 扩展字段 =====
    /** 精细状态: 24种设备状态 */
    private String detailStatus;
    /** 厂商 */
    private String manufacturer;
    /** 型号 */
    private String modelNum;
    /** 序列号 */
    private String serialNumber;
    /** 通信ID */
    private String communicationId;
    /** 条码数据 */
    private String barcodeData;
    /** 安装日期 */
    private LocalDate installDate;
    /** 保修到期日 */
    private LocalDate warrantyExpire;
    /** MQTT配置ID(外键) */
    private Long configId;
    /** 当前处方编号 */
    private String currentPrescriptionCode;
    /** 当前操作人ID */
    private Long currentOperatorId;
    /** 当前操作人姓名 */
    private String currentOperatorName;
    /** 预计完成时间 */
    private LocalDateTime estimatedFinishTime;
    /** 剩余时间(秒) */
    private Integer remainingTime;
    /** 当前工序进度百分比 */
    private Integer progressPercent;
    /** 水位百分比(0-100) */
    private Integer waterLevel;
    /** 压力值(MPa) */
    private BigDecimal pressure;

    /** MQTT配置对象（关联查询，不持久化到设备表） */
    @TableField(exist = false)
    private EqDeviceMqttConfig mqttConfig;

    /** 当前操作人信息（关联查询，不持久化） */
    @TableField(exist = false)
    private EqDeviceOperator currentOperator;
}
