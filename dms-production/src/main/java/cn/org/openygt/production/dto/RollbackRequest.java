package cn.org.openygt.production.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class RollbackRequest {
    private Long taskId;
    @NotBlank(message = "回退目标节点不能为空")
    private String rollbackTo;
    private String reasonCode;
    private String remark;
    private Long operatorId;
}
