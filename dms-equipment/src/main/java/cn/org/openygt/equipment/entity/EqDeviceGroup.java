package cn.org.openygt.equipment.entity;

import cn.org.openygt.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("eq_device_group")
public class EqDeviceGroup extends BaseEntity {

    private String groupCode;
    private String groupName;
    private Long packageDeviceId;
    private Long printerDeviceId;
    private String status;
}
