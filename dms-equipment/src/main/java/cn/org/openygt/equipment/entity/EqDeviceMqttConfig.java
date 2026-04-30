package cn.org.openygt.equipment.entity;

import cn.org.openygt.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("eq_device_mqtt_config")
public class EqDeviceMqttConfig extends BaseEntity {

    /** 设备编码 */
    private String deviceCode;
    /** Broker地址 */
    private String brokerUrl;
    /** 端口 */
    private Integer port;
    /** 用户名 */
    private String username;
    /** 加密密码 */
    private String passwordEncrypted;
    /** 设备发布Topic */
    private String publishTopic;
    /** 设备订阅Topic */
    private String subscribeTopic;
    /** Token密钥 */
    private String tokenSecret;
}
