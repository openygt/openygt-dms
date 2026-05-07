package cn.org.openygt.system.service.impl;

import cn.org.openygt.common.dto.LoginRequest;
import cn.org.openygt.common.dto.TokenResponse;
import cn.org.openygt.common.util.JwtUtil;
import cn.org.openygt.system.entity.SysUser;
import cn.org.openygt.system.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SysUserServiceImplTest {

    @Mock
    private SysUserMapper userMapper;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    private SysUserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new SysUserServiceImpl(userMapper, passwordEncoder);
    }

    @Test
    void create_shouldSetActiveAndEncryptPassword() {
        SysUser user = new SysUser();
        user.setUsername("testuser");
        user.setPassword("raw-password");
        user.setRealName("测试用户");

        when(passwordEncoder.encode("raw-password")).thenReturn("encoded-password");
        when(userMapper.insert(any(SysUser.class))).thenReturn(1);

        SysUser result = userService.create(user);

        assertEquals("ACTIVE", result.getStatus());
        assertEquals("encoded-password", result.getPassword());
        verify(userMapper).insert(user);
    }

    @Test
    void create_shouldThrow_whenPasswordNull() {
        SysUser user = new SysUser();
        user.setUsername("nopass");

        assertThrows(IllegalArgumentException.class, () -> userService.create(user));
    }

    @Test
    void create_shouldSetInactive_whenStatusZero() {
        SysUser user = new SysUser();
        user.setUsername("testuser");
        user.setPassword("raw-password");
        user.setStatus("0");

        when(passwordEncoder.encode("raw-password")).thenReturn("encoded-password");
        when(userMapper.insert(any(SysUser.class))).thenReturn(1);

        SysUser result = userService.create(user);

        assertEquals("INACTIVE", result.getStatus());
    }

    @Test
    void update_shouldSucceed_whenExists() {
        SysUser existing = new SysUser();
        existing.setId(1L);
        existing.setUsername("olduser");

        SysUser update = new SysUser();
        update.setRealName("新名字");

        when(userMapper.selectById(anyLong())).thenReturn(existing).thenReturn(update);
        when(userMapper.updateById(any(SysUser.class))).thenReturn(1);

        SysUser result = userService.update(1L, update);

        assertNotNull(result);
        verify(userMapper).updateById(any(SysUser.class));
    }

    @Test
    void update_shouldReEncryptPassword_whenChanged() {
        SysUser existing = new SysUser();
        existing.setId(1L);
        existing.setPassword("old-encoded");

        SysUser update = new SysUser();
        update.setPassword("new-password");

        when(userMapper.selectById(anyLong())).thenReturn(existing).thenReturn(update);
        when(userMapper.updateById(any(SysUser.class))).thenReturn(1);
        when(passwordEncoder.encode("new-password")).thenReturn("new-encoded");

        userService.update(1L, update);

        verify(passwordEncoder).encode("new-password");
    }

    @Test
    void update_shouldThrow_whenNotExists() {
        when(userMapper.selectById(1L)).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> userService.update(1L, new SysUser()));
    }

    @Test
    void getById_shouldReturnUser_whenExists() {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername("admin");

        when(userMapper.selectById(1L)).thenReturn(user);

        SysUser result = userService.getById(1L);

        assertNotNull(result);
        assertEquals("admin", result.getUsername());
    }

    @Test
    void getById_shouldThrow_whenNotExists() {
        when(userMapper.selectById(1L)).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> userService.getById(1L));
    }

    @Test
    void getByUsername_shouldReturnUser() {
        SysUser user = new SysUser();
        user.setUsername("admin");

        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);

        SysUser result = userService.getByUsername("admin");

        assertNotNull(result);
        assertEquals("admin", result.getUsername());
    }

    @Test
    void list_shouldApplyKeywordFilter() {
        Page<SysUser> page = new Page<>(1, 10);
        when(userMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        IPage<SysUser> result = userService.list("admin", 1, 10);

        assertNotNull(result);
        verify(userMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    void delete_shouldCallDeleteById() {
        userService.delete(1L);

        verify(userMapper).deleteById(1L);
    }

    @Test
    void login_shouldSucceed_whenCredentialsValid() {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername("admin");
        user.setPassword("encoded-pass");
        user.setStatus("ACTIVE");

        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);
        when(passwordEncoder.matches("correct-password", "encoded-pass")).thenReturn(true);
        when(userMapper.selectRoleCodesByUserId(1L)).thenReturn(java.util.Collections.singletonList("ROLE_ADMIN"));
        when(userMapper.selectPermissionCodesByUserId(1L)).thenReturn(java.util.Collections.singletonList("ROLE_ADMIN"));

        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("correct-password");

        TokenResponse result = userService.login(request);

        assertNotNull(result);
        assertEquals("admin", result.getUsername());
        assertEquals(1L, result.getUserId().longValue());
        assertEquals("Bearer", result.getTokenType());
        assertEquals(86400L, result.getExpiresIn().longValue());
        assertNotNull(result.getToken());
    }

    @Test
    void login_shouldFallbackToLegacyRole_whenUserRoleBindingMissing() {
        SysUser user = new SysUser();
        user.setId(2L);
        user.setUsername("jgy001");
        user.setPassword("encoded-pass");
        user.setStatus("ACTIVE");
        user.setRole("煎药工");

        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);
        when(passwordEncoder.matches("correct-password", "encoded-pass")).thenReturn(true);
        when(userMapper.selectRoleCodesByUserId(2L)).thenReturn(java.util.Collections.emptyList());
        when(userMapper.selectPermissionCodesByUserId(2L)).thenReturn(java.util.Collections.emptyList());

        LoginRequest request = new LoginRequest();
        request.setUsername("jgy001");
        request.setPassword("correct-password");

        TokenResponse result = userService.login(request);

        assertEquals(java.util.Collections.singletonList("ROLE_WORKER"), JwtUtil.getRoles(result.getToken()));
        assertEquals(java.util.Collections.singletonList("ROLE_WORKER"), JwtUtil.getPermissions(result.getToken()));
    }

    @Test
    void login_shouldThrow_whenUserNotFound() {
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        LoginRequest request = new LoginRequest();
        request.setUsername("unknown");
        request.setPassword("pw");

        assertThrows(IllegalArgumentException.class, () -> userService.login(request));
    }

    @Test
    void login_shouldThrow_whenPasswordWrong() {
        SysUser user = new SysUser();
        user.setPassword("encoded-pass");
        user.setStatus("ACTIVE");

        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);
        when(passwordEncoder.matches("wrong", "encoded-pass")).thenReturn(false);

        LoginRequest request = new LoginRequest();
        request.setUsername("u1");
        request.setPassword("wrong");

        assertThrows(IllegalArgumentException.class, () -> userService.login(request));
    }

    @Test
    void login_shouldThrow_whenUserDisabled() {
        SysUser user = new SysUser();
        user.setPassword("encoded");
        user.setStatus("INACTIVE");

        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);

        LoginRequest request = new LoginRequest();
        request.setUsername("u1");
        request.setPassword("pw");

        assertThrows(IllegalStateException.class, () -> userService.login(request));
    }
}
