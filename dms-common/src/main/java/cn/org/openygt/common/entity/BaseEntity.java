package cn.org.openygt.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.util.Date;

@Data
public abstract class BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("tenant_id")
    private String tenantId = "default";

    private Date createdAt;

    private Date updatedAt;

    @TableLogic
    private Integer deleted = 0;
}
