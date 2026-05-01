package cn.org.openygt.production.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class TtsRequest {
    @NotBlank(message = "文本不能为空")
    private String text;
    private String deviceId;
}
