package cn.org.openygt.equipment.entity;

import cn.org.openygt.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_package_spec")
public class PackageSpec extends BaseEntity {

    private String specCode;
    private String specName;
    private Integer volumeMl;
    private String bagType;
    private String description;
    private Integer sortOrder;
    private Integer status;
}
