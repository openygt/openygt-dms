package cn.org.openygt.system.entity;

import cn.org.openygt.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 电子签名表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_signature")
public class Signature extends BaseEntity {

    private String bizType;
    private Long bizId;
    private Long signerId;
    private String signerName;
    private String signImageUrl;
    private String signHash;
    private LocalDateTime signTime;
    private String signDevice;
}
