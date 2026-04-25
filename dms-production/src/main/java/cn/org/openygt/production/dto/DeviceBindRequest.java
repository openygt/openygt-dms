package cn.org.openygt.production.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;

@Data
public class DeviceBindRequest {
    @NotBlank(message = "设备编码不能为空")
    private String deviceCode;
}
