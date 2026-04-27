package cn.org.openygt.rbac.service;

import cn.org.openygt.rbac.entity.SysUserRole;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface SysUserRoleService extends IService<SysUserRole> {
    void assignRoles(Long userId, List<Long> roleIds);
    List<Long> getRoleIdsByUserId(Long userId);
}
