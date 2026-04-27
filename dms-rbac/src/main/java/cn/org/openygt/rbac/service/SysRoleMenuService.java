package cn.org.openygt.rbac.service;

import com.baomidou.mybatisplus.extension.service.IService;
import cn.org.openygt.rbac.entity.SysRoleMenu;

import java.util.List;

public interface SysRoleMenuService extends IService<SysRoleMenu> {
    void assignMenus(Long roleId, List<Long> menuIds);
}
