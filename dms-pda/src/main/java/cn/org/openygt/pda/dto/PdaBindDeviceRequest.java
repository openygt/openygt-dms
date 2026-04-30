package cn.org.openygt.pda.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class PdaBindDeviceRequest {

    @NotBlank(message = "设备编码不能为空")
    private String deviceCode;

    private String bindType;
}
