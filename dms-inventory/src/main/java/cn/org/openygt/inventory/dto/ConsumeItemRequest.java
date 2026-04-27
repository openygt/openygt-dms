package cn.org.openygt.inventory.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class ConsumeItemRequest {
    private Long medicineId;
    private String medicineCode;
    @NotBlank(message = "药材名称不能为空")
    private String medicineName;
    @NotNull(message = "消耗数量不能为空")
    private BigDecimal quantity;
    private String unit;
    private String remark;
}
