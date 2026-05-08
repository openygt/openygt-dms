package cn.org.openygt.equipment.entity;

import cn.org.openygt.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("iot_device_command")
public class DeviceCommand extends BaseEntity {

    /** 目标设备编码 */
    private String deviceCode;
    /** 指令类型: START_SOAK/START_DECOCT/PAUSE/RESUME/EMERGENCY_STOP/START_PACKAGE/SET_TEMP/ADD_LATE_REMIND/CONFIRM_ADD_LATE */
    private String commandType;
    /** 指令参数JSON */
    private String commandPayload;
    /** 状态: PENDING/SENT/ACKED/FAILED/TIMEOUT */
    private String status;
    /** 设备响应JSON */
    private String responsePayload;
    /** 重试次数 */
    private Integer retryCount;
    /** 发送时间 */
    private LocalDateTime sendTime;
    /** 确认时间 */
    private LocalDateTime ackTime;
    /** 失败原因 */
    private String failReason;
    /** 指令等级: NORMAL/IMPORTANT/CRITICAL */
    private String commandLevel;
    /** 风险等级: LOW/MEDIUM/HIGH */
    private String riskLevel;
    /** 是否需要确认: 0否/1是 */
    private Integer requireConfirm;
}
