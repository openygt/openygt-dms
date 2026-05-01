package cn.org.openygt.production.dto;

import javax.validation.constraints.NotNull;
import lombok.Data;

@Data
public class HerbGroupConfirmRequest {
    @NotNull
    private Long operatorId;
    private Long deviceId;
}
