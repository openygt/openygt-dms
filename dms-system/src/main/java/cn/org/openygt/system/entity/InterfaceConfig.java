package cn.org.openygt.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_interface_config")
public class InterfaceConfig {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String interfaceCode;
    private String interfaceName;
    private String interfaceType;
    private String protocol;
    private String baseUrl;
    private String authType;
    private String authConfig;
    private Integer status;
    private String remark;
    private String tenantId;
    private Integer deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
