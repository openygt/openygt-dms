package cn.org.openygt.equipment.entity;

import cn.org.openygt.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("water_formula")
public class WaterFormula extends BaseEntity {

    private String formulaCode;
    private String formulaName;
    private String expression;
    private String expressionDesc;
    private String variables;
    private Boolean isDefault;
    private Boolean enabled;
    private Integer sortOrder;
}
