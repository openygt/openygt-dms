package cn.org.openygt.pda.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class PdaPhotoUploadRequest {

    @NotNull(message = "任务ID不能为空")
    private Long taskId;

    private Long prescriptionId;

    @NotBlank(message = "照片URL不能为空")
    private String photoUrl;

    @NotBlank(message = "照片类型不能为空")
    @javax.validation.constraints.Pattern(regexp = "REVIEW|WEIGHING|EXCEPTION", message = "照片类型必须是 REVIEW、WEIGHING 或 EXCEPTION")
    private String photoType;

    private Long fileSize;

    private String remark;
}
