package cn.org.openygt.pda.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class PdaTaskConfirmRequest {

    @NotNull(message = "任务ID不能为空")
    private Long taskId;

    @NotBlank(message = "工序类型不能为空")
    private String stepType;

    private String remark;

    private Long deviceId;
}
