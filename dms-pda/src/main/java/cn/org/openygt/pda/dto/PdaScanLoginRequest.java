package cn.org.openygt.pda.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class PdaScanLoginRequest {

    @NotBlank(message = "扫码条码不能为空")
    private String scanCode;

    @NotBlank(message = "设备MAC不能为空")
    private String macAddress;
}
