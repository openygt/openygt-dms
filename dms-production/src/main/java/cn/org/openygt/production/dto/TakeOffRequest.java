package cn.org.openygt.production.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class TakeOffRequest {
    @NotNull(message = "记录ID不能为空")
    private Long recordId;
    private String takeOffType;
    private Long takeOffBy;
}
