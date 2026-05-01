package cn.org.openygt.system.service.impl;

import cn.org.openygt.common.dto.LoginRequest;
import cn.org.openygt.common.dto.TokenResponse;
import cn.org.openygt.common.util.JwtUtil;
import cn.org.openygt.system.entity.SysUser;
import cn.org.openygt.system.mapper.SysUserMapper;
import cn.org.openygt.system.service.SysUserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SysUserServiceImpl implements SysUserService {

    private final SysUserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    public SysUserServiceImpl(SysUserMapper userMapper, BCryptPasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public SysUser create(SysUser user) {
        user.setStatus("ACTIVE");
        // 密码加密存储
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        userMapper.insert(user);
        return user;
    }

    @Override
    @Transactional
    public SysUser update(Long id, SysUser user) {
        SysUser existing = userMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("用户不存在: " + id);
        }
        user.setId(id);
        // 若更新密码，需重新加密
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        userMapper.updateById(user);
        return userMapper.selectById(id);
    }

    @Override
    public SysUser getById(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在: " + id);
        }
        return user;
    }

    @Override
    public SysUser getByUsername(String username) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, username);
        return userMapper.selectOne(wrapper);
    }

    @Override
    public SysUser getByBarcode(String barcode) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getBarcode, barcode);
        return userMapper.selectOne(wrapper);
    }

    @Override
    public IPage<SysUser> list(String keyword, int page, int size) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(SysUser::getUsername, keyword)
                   .or()
                   .like(SysUser::getRealName, keyword);
        }
        wrapper.orderByDesc(SysUser::getCreatedAt);
        return userMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        userMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new IllegalArgumentException("旧密码错误");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
    }

    @Override
    public TokenResponse login(LoginRequest request) {
        SysUser user = getByUsername(request.getUsername());
        if (user == null) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new IllegalStateException("用户已被禁用");
        }
        List<String> roles = userMapper.selectRoleCodesByUserId(user.getId());
        List<String> permissions = userMapper.selectPermissionCodesByUserId(user.getId());
        String token = JwtUtil.generateToken(user.getId(), user.getUsername(), roles, permissions);
        TokenResponse response = new TokenResponse();
        response.setToken(token);
        response.setTokenType("Bearer");
        response.setExpiresIn(86400L);
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        return response;
    }
}
