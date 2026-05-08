package cn.org.openygt.equipment.entity;

import cn.org.openygt.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("eq_department")
public class Department extends BaseEntity {

    private String deptCode;
    private String deptName;
    private Long hospitalId;
    private String description;
    private Integer sortOrder;
    private Integer status;
}
