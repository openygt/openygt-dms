package cn.org.openygt.system.service;

import cn.org.openygt.system.entity.SysUser;
import com.baomidou.mybatisplus.core.metadata.IPage;

public interface SysUserService {

    SysUser create(SysUser user);

    SysUser update(Long id, SysUser user);

    SysUser getById(Long id);

    SysUser getByUsername(String username);

    IPage<SysUser> list(String keyword, int page, int size);

    void delete(Long id);
}
