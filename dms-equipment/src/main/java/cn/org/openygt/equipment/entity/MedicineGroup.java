package cn.org.openygt.equipment.entity;

import cn.org.openygt.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("md_medicine_group")
public class MedicineGroup extends BaseEntity {

    private Long prescriptionId;
    private String prescriptionNo;
    private String medicineName;
    private String groupType;
    private BigDecimal dosage;
    private String unit;
    private Integer sortOrder;
    private String remark;
}
