package cn.org.openygt.rbac.entity;

import cn.org.openygt.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统角色实体。
 *
 * <p>V2.0 固定 5 角色，不支持动态增删角色代码：</p>
 * <ul>
 *   <li>ROLE_ADMIN     — 系统管理员</li>
 *   <li>ROLE_DIRECTOR  — 车间主任</li>
 *   <li>ROLE_LEADER    — 班组长</li>
 *   <li>ROLE_WORKER    — 操作工</li>
 *   <li>ROLE_INSPECTOR — 质检员</li>
 * </ul>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role")
public class SysRole extends BaseEntity {
    private String roleCode;
    private String roleName;
    private String description;
    private Integer sortOrder;
    private String status;
}
