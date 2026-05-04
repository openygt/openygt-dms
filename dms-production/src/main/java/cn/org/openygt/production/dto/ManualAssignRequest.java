package cn.org.openygt.production.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;

@Data
public class ManualAssignRequest {
    @NotNull(message = "任务ID不能为空")
    private Long taskId;
    @NotNull(message = "设备ID不能为空")
    private Long deviceId;
    @NotNull(message = "员工ID不能为空")
    private Long employeeId;
    private String reason;
    private LocalDate scheduledDate;
}
