package cn.org.openygt.pda.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pda_login_record")
public class PdaLoginRecord extends cn.org.openygt.common.entity.BaseEntity {

    private Long userId;
    private Long deviceId;
    private String deviceCode;
    private String userCode;
    private LocalDateTime loginTime;
    private LocalDateTime logoutTime;
    private String status;
    private Long onlineDuration;
    private String ipAddress;
}
