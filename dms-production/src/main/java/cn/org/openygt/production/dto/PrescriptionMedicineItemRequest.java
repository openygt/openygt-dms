package cn.org.openygt.production.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;

@Data
public class PrescriptionMedicineItemRequest {
    /** 系统药品ID（FLEXIBLE模式可为null，STRICT模式必填） */
    private Long medicineId;

    @NotBlank(message = "药材名称不能为空")
    private String medicineName;

    @Positive(message = "用量必须大于0")
    private BigDecimal dosage;

    private String unit;

    /** 用法：先煎/后下/包煎/另煎/烊化/冲服/煎汤代水 */
    private String medUsage;

    private Integer sortOrder;

    /** 煎法：NORMAL/DECOCT_FIRST/ADD_LATE/WRAP_DECOCT/SEPARATE_DECOCT/DISSOLVE/INFUSE/DECOCT_AS_WATER */
    private String decoctionMethod;

    private String batchNo;
}
