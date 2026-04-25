package cn.org.openygt.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审计实体基类（不含逻辑删除）。
 * 适用于审计/遥测表：sys_log、eq_temperature_log、prod_task_status_history。
 */
@Data
public abstract class BaseAuditEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("tenant_id")
    private String tenantId = "default";

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
