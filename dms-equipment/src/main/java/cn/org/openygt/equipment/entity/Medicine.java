package cn.org.openygt.equipment.entity;

import cn.org.openygt.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_medicine")
public class Medicine extends BaseEntity {

    private String medicineCode;
    private String medicineName;
    private String aliases;
    private String hisCode;
    private String nationalCode;
    private String spec;
    private String unit;
    private Integer stockWarning;
    private Integer status;
}
