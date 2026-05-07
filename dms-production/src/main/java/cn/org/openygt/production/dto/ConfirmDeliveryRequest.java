package cn.org.openygt.production.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 确认交付请求 DTO。
 */
@Data
public class ConfirmDeliveryRequest {

    @NotNull(message = "操作人ID不能为空")
    private Long operatorId;

    @NotBlank(message = "接收人姓名不能为空")
    private String receiverName;

    private String receiverPhone;

    private String remark;
}
