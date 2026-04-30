package cn.org.openygt.equipment.entity;

import cn.org.openygt.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("eq_device_operator")
public class EqDeviceOperator extends BaseEntity {

    /** 设备编码 */
    private String deviceCode;
    /** 操作人ID */
    private Long operatorId;
    /** 操作人姓名 */
    private String operatorName;
    /** 班次开始时间 */
    private LocalDateTime shiftStartTime;
    /** 班次结束时间 */
    private LocalDateTime shiftEndTime;
    /** 是否当前班次:1=是,0=否 */
    private Integer isCurrent;
}
