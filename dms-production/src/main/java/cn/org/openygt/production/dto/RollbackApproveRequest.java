package cn.org.openygt.production.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class RollbackApproveRequest {
    @NotNull(message = "审批人ID不能为空")
    private Long approverId;
    @NotNull(message = "审批状态不能为空")
    private Integer approvalStatus;
    private String approvalComment;
}
