package cn.org.openygt.rbac.service;

import cn.org.openygt.rbac.entity.SysMenu;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface SysMenuService extends IService<SysMenu> {
    List<SysMenu> getMenuTree();
    List<SysMenu> getMenusByRoleIds(List<Long> roleIds);
    List<SysMenu> getMenusByRoleId(Long roleId);
}
