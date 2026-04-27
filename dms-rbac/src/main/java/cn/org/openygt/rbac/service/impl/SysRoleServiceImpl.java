package cn.org.openygt.rbac.service.impl;

import cn.org.openygt.rbac.entity.SysRole;
import cn.org.openygt.rbac.mapper.SysRoleMapper;
import cn.org.openygt.rbac.service.SysRoleService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements SysRoleService {

    @Override
    public List<SysRole> getRolesByUserId(Long userId) {
        return baseMapper.selectRolesByUserId(userId);
    }
}
