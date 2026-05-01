package cn.org.openygt.production.dto;

import javax.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EmergencySignRequest {
    @NotBlank
    private String nurseName;
}
