package cn.org.openygt.production.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class PrescriptionRejectRequest {
    @NotBlank(message = "驳回类型不能为空")
    @Size(max = 50, message = "驳回类型长度不能超过50")
    private String rejectType;

    @NotBlank(message = "驳回原因不能为空")
    @Size(max = 500, message = "驳回原因长度不能超过500")
    private String reason;
}
