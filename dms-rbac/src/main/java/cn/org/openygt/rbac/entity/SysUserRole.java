package cn.org.openygt.rbac.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 用户-角色关联实体。
 */
@Data
@TableName("sys_user_role")
public class SysUserRole {
    private Long id;
    private Long userId;
    private Long roleId;
}
