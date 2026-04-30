package cn.org.openygt.equipment.entity;

import cn.org.openygt.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("prescription_default")
public class PrescriptionDefault extends BaseEntity {

    private String settingKey;
    private String settingName;
    private String settingValue;
    private String settingType;
    private String description;
}
