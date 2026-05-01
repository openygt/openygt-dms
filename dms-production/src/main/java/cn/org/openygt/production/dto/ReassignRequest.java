package cn.org.openygt.production.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class ReassignRequest {
    @NotNull(message = "新设备ID不能为空")
    private Long newDeviceId;
    @NotNull(message = "新员工ID不能为空")
    private Long newEmployeeId;
    private String reason;
}
