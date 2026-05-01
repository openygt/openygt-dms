package cn.org.openygt.system.service;

import cn.org.openygt.common.dto.LoginRequest;
import cn.org.openygt.common.dto.TokenResponse;
import cn.org.openygt.system.entity.SysUser;
import com.baomidou.mybatisplus.core.metadata.IPage;

public interface SysUserService {

    SysUser create(SysUser user);

    SysUser update(Long id, SysUser user);

    SysUser getById(Long id);

    SysUser getByUsername(String username);

    SysUser getByBarcode(String barcode);

    IPage<SysUser> list(String keyword, int page, int size);

    void delete(Long id);

    /**
     * 用户登录。
     *
     * @param request 登录请求
     * @return Token 响应
     */
    TokenResponse login(LoginRequest request);

    /**
     * 修改密码
     *
     * @param userId      用户ID
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     */
    void changePassword(Long userId, String oldPassword, String newPassword);
}
