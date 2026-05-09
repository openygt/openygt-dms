package cn.org.openygt.equipment.entity;

import cn.org.openygt.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_package_spec")
public class PackageSpec extends BaseEntity {

    @NotBlank(message = "规格编码不能为空")
    private String specCode;

    @NotBlank(message = "规格名称不能为空")
    private String specName;

    @NotNull(message = "容量不能为空")
    private Integer volumeMl;

    @NotBlank(message = "袋型不能为空")
    private String bagType;

    private String description;
    private Integer sortOrder;

    @NotNull(message = "状态不能为空")
    private Integer status;
}
