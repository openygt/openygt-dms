package cn.org.openygt.rbac.service.impl;

import cn.org.openygt.rbac.entity.SysMenu;
import cn.org.openygt.rbac.mapper.SysMenuMapper;
import cn.org.openygt.rbac.service.SysMenuService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SysMenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu> implements SysMenuService {

    @Override
    public List<SysMenu> getMenuTree() {
        List<SysMenu> all = list();
        return buildTree(all, 0L);
    }

    @Override
    public List<SysMenu> getMenusByRoleIds(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return new ArrayList<>();
        }
        String ids = roleIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        List<SysMenu> menus = baseMapper.selectMenusByRoleIds(ids);
        // 去重并构建树
        List<SysMenu> distinct = menus.stream()
                .collect(Collectors.toMap(SysMenu::getId, m -> m, (a, b) -> a))
                .values().stream()
                .collect(Collectors.toList());
        return buildTree(distinct, 0L);
    }

    @Override
    public List<SysMenu> getMenusByRoleId(Long roleId) {
        return baseMapper.selectMenusByRoleId(roleId);
    }

    private List<SysMenu> buildTree(List<SysMenu> menus, Long parentId) {
        List<SysMenu> tree = new ArrayList<>();
        for (SysMenu menu : menus) {
            if (parentId.equals(menu.getParentId())) {
                menu.setChildren(buildTree(menus, menu.getId()));
                tree.add(menu);
            }
        }
        return tree;
    }
}
