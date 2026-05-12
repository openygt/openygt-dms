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
    /** 分组类型: PRODUCTION_LINE/WORKSHOP/AREA/OTHER */
    private String groupType;
    private Long specId;
    private String decocterIds;
    private Long packageDeviceId;
    private Long printerDeviceId;
    private String status;
    /** 备注 */
    private String remark;
}
