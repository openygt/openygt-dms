package cn.org.openygt.production.dto;

import lombok.Data;

@Data
public class HerbGroupConfirmRequest {
    /** 操作人ID，不传时由控制器从登录态注入 */
    private Long operatorId;
    /** 设备ID，扫码绑定时传入 */
    private Long deviceId;
}
