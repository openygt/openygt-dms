package cn.org.openygt.equipment.entity;

import cn.org.openygt.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 设备分组联动规则
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("eq_device_group_rule")
public class EqDeviceGroupRule extends BaseEntity {

    /** 关联设备分组ID */
    private Long groupId;
    /** 规则名称 */
    private String ruleName;
    /** 触发条件JSON */
    private String triggerCondition;
    /** 动作类型: START/STOP/ALARM/NOTIFY/EMERGENCY_STOP */
    private String actionType;
    /** 目标设备编码列表JSON */
    private String targetDevices;
    /** 是否启用: 0禁用/1启用 */
    private Integer enabled;
}
