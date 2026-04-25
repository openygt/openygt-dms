package cn.org.openygt.production.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;

@Data
public class ForceRequest {
    @NotBlank(message = "目标状态不能为空")
    private String targetStatus;
    @NotBlank(message = "操作员工号不能为空")
    private String operatorId;
    private String deviceCode;
    private String remark;
}
