package cn.org.openygt.rbac.service.impl;

import cn.org.openygt.rbac.entity.SysUserRole;
import cn.org.openygt.rbac.mapper.SysUserRoleMapper;
import cn.org.openygt.rbac.service.SysUserRoleService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SysUserRoleServiceImpl extends ServiceImpl<SysUserRoleMapper, SysUserRole> implements SysUserRoleService {

    @Override
    @Transactional
    public void assignRoles(Long userId, List<Long> roleIds) {
        // 删除旧绑定
        LambdaQueryWrapper<SysUserRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserRole::getUserId, userId);
        remove(wrapper);
        // 插入新绑定
        if (roleIds != null && !roleIds.isEmpty()) {
            for (Long roleId : roleIds) {
                SysUserRole ur = new SysUserRole();
                ur.setUserId(userId);
                ur.setRoleId(roleId);
                save(ur);
            }
        }
    }

    @Override
    public List<Long> getRoleIdsByUserId(Long userId) {
        return baseMapper.selectRoleIdsByUserId(userId);
    }
}
