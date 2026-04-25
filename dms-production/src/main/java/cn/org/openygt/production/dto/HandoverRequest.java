package cn.org.openygt.production.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class HandoverRequest {
    @NotNull
    private Integer bagCount;
    @NotBlank
    private String handoverType;
    @NotBlank
    private String handoverUser;
    private Boolean isFinal;
    private String remark;
}
