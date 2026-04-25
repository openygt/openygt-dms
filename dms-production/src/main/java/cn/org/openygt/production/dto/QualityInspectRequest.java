package cn.org.openygt.production.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;

@Data
public class QualityInspectRequest {
    @NotBlank
    private String result;
    private String operatorId;
    private String remark;
}
