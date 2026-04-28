package cn.org.openygt.pda.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class PdaLoginRequest {

    @NotBlank(message = "用户编码不能为空")
    private String userCode;

    @NotBlank(message = "密码不能为空")
    private String password;

    @NotNull(message = "设备ID不能为空")
    private Long deviceId;

    @NotBlank(message = "设备编码不能为空")
    private String deviceCode;
}
