package cn.org.openygt.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_interface_log")
public class InterfaceLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long interfaceId;
    private String interfaceCode;
    private String direction;
    private String method;
    private String url;
    private String requestBody;
    private String responseBody;
    private String statusCode;
    private String result;
    private Integer durationMs;
    private String errorMsg;
    private String tenantId;
    private Integer deleted;
    private LocalDateTime createdAt;
}
