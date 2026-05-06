package cn.org.openygt.system.entity;

import cn.org.openygt.common.entity.BaseAuditEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统操作日志实体，对应表 {@code sys_log}。
 *
 * <p>操作日志为审计追踪数据，物理保留不逻辑删除（继承 BaseAuditEntity，无 deleted 字段）。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_log")
public class SysLog extends BaseAuditEntity {

    private String userId;
    private String action;
    private String module;
    private String detail;
    private String ipAddress;

    private String result;
    private String targetId;
    private String targetName;
}
