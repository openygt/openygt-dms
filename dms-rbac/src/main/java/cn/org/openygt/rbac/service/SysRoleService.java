package cn.org.openygt.rbac.service;

import cn.org.openygt.rbac.entity.SysRole;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface SysRoleService extends IService<SysRole> {
    List<SysRole> getRolesByUserId(Long userId);
}
