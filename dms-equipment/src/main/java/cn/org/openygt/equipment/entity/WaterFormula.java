package cn.org.openygt.equipment.entity;

import cn.org.openygt.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("water_formula")
public class WaterFormula extends BaseEntity {

    @NotBlank(message = "公式编码不能为空")
    private String formulaCode;

    @NotBlank(message = "公式名称不能为空")
    private String formulaName;

    @NotBlank(message = "表达式不能为空")
    private String expression;

    private String expressionDesc;
    private String variables;

    @NotNull(message = "是否默认不能为空")
    private Boolean isDefault;

    private Boolean enabled;
    private Integer sortOrder;
}
