package cn.org.openygt.production.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;

@Data
public class StageRequest {
    @NotBlank(message = "操作员工号不能为空")
    private String operatorId;
}
