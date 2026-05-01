package cn.org.openygt.production.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class PutOnRequest {
    @NotNull(message = "处方ID不能为空")
    private Long prescriptionId;
    @NotBlank(message = "包裹条码不能为空")
    private String packageBarcode;
    @NotNull(message = "货架ID不能为空")
    private Long shelfId;
    private Long putOnBy;
}
