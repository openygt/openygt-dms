package cn.org.openygt.pda.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class PdaScanLoginRequest {

    @NotBlank(message = "扫码条码不能为空")
    private String scanCode;

    private String deviceCode;
}
