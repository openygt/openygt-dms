package cn.org.openygt.production.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;

@Data
public class SoakRequest {
    @NotBlank(message = "操作员工号不能为空")
    private String operatorId;
    private Integer soakDuration;
}
