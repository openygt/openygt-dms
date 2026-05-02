package cn.org.openygt.production.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import com.baomidou.mybatisplus.annotation.*;
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
    /** V30: 药材批号 */
    private String batchNo;
    /** V30: 是否毒性药材 0=否 1=是 */
    @TableField("is_toxic")
    private Integer isToxic;
    /** V30: 毒性等级 1小毒 2有毒 3大毒 */
    private Integer toxicityLevel;
    /** V31: 煎法 NORMAL/DECOCT_FIRST/ADD_LATE/WRAP_DECOCT/SEPARATE_DECOCT/DISSOLVE/INFUSE/DECOCT_AS_WATER */
    private String decoctionMethod;
    /** V31: 煎法参数(JSON) */
    private String decoctionParams;
    private String tenantId;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createdAt;
}
