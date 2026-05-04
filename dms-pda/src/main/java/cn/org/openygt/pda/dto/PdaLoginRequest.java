package cn.org.openygt.pda.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class PdaLoginRequest {

    @NotBlank(message = "用户编码不能为空")
    private String userCode;

    @NotBlank(message = "密码不能为空")
    private String password;

    @NotBlank(message = "设备MAC不能为空")
    private String macAddress;
}
