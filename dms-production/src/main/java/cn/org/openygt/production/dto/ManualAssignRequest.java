package cn.org.openygt.production.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class ManualAssignRequest {
    @NotNull(message = "任务ID不能为空")
    private Long taskId;
    private Long deviceId;
    private Long employeeId;
    private String reason;
}
