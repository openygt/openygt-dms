package cn.org.openygt.production.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class PrescriptionCreateRequest {
    @NotBlank(message = "患者姓名不能为空")
    @Size(max = 100, message = "患者姓名长度不能超过100")
    private String patientName;

    @NotBlank(message = "药品清单不能为空")
    @Size(max = 500, message = "药品清单长度不能超过500")
    private String medicineList;

    @Size(max = 500, message = "备注长度不能超过500")
    private String remark;
}
