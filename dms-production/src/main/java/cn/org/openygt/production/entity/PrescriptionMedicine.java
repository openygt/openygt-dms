package cn.org.openygt.production.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("prod_prescription_medicine")
public class PrescriptionMedicine {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long prescriptionId;
    private Long medicineId;
    private String medicineName;
    private BigDecimal dosage;
    private String unit;
    private String medUsage;
    private Integer sortOrder;
    private String tenantId;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createdAt;
}
