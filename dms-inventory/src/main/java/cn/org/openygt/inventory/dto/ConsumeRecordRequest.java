package cn.org.openygt.inventory.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class ConsumeRecordRequest {
    @NotNull(message = "任务ID不能为空")
    private Long taskId;
    @NotBlank(message = "操作人ID不能为空")
    private String operatorId;
    private List<ConsumeItemRequest> items;
}
