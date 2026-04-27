package cn.org.openygt.rbac.controller;

import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.common.dto.LoginRequest;
import cn.org.openygt.common.dto.TokenResponse;
import cn.org.openygt.common.util.JwtUtil;
import cn.org.openygt.rbac.annotation.RequiresPermissions;
import cn.org.openygt.rbac.entity.SysMenu;
import cn.org.openygt.rbac.entity.SysRole;
import cn.org.openygt.rbac.service.SysMenuService;
import cn.org.openygt.rbac.service.SysRoleMenuService;
import cn.org.openygt.rbac.service.SysRoleService;
import cn.org.openygt.rbac.service.SysUserRoleService;
import cn.org.openygt.system.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * RBAC 管理接口。
 *
 * <p>提供菜单、角色、用户角色绑定等管理功能，以及增强版登录（返回角色信息）。</p>
 */
@RestController
@RequestMapping("/api/v1/rbac")
@RequiredArgsConstructor
@Validated
public class RbacController {

    private final SysMenuService menuService;
    private final SysRoleService roleService;
    private final SysUserRoleService userRoleService;
    private final SysRoleMenuService roleMenuService;
    private final SysUserService userService;

    // ==================== 菜单管理 ====================

    @GetMapping("/menus/tree")
    public ApiResponse<List<SysMenu>> menuTree() {
        return ApiResponse.success(menuService.getMenuTree());
    }

    @PostMapping("/menus")
    @RequiresPermissions({"ROLE_ADMIN"})
    public ApiResponse<SysMenu> createMenu(@RequestBody SysMenu menu) {
        menuService.save(menu);
        return ApiResponse.success(menu);
    }

    @PutMapping("/menus/{id}")
    @RequiresPermissions({"ROLE_ADMIN"})
    public ApiResponse<SysMenu> updateMenu(@PathVariable Long id, @RequestBody SysMenu menu) {
        menu.setId(id);
        menuService.updateById(menu);
        return ApiResponse.success(menu);
    }

    @DeleteMapping("/menus/{id}")
    @RequiresPermissions({"ROLE_ADMIN"})
    public ApiResponse<Void> deleteMenu(@PathVariable Long id) {
        menuService.removeById(id);
        return ApiResponse.success();
    }

    // ==================== 角色管理 ====================

    @GetMapping("/roles")
    @RequiresPermissions({"ROLE_ADMIN", "ROLE_DIRECTOR"})
    public ApiResponse<List<SysRole>> roleList() {
        return ApiResponse.success(roleService.list());
    }

    @GetMapping("/roles/{id}")
    public ApiResponse<SysRole> getRole(@PathVariable Long id) {
        return ApiResponse.success(roleService.getById(id));
    }

    @PostMapping("/roles")
    @RequiresPermissions({"ROLE_ADMIN"})
    public ApiResponse<SysRole> createRole(@RequestBody SysRole role) {
        roleService.save(role);
        return ApiResponse.success(role);
    }

    @PutMapping("/roles/{id}")
    @RequiresPermissions({"ROLE_ADMIN"})
    public ApiResponse<SysRole> updateRole(@PathVariable Long id, @RequestBody SysRole role) {
        role.setId(id);
        roleService.updateById(role);
        return ApiResponse.success(role);
    }

    @DeleteMapping("/roles/{id}")
    @RequiresPermissions({"ROLE_ADMIN"})
    public ApiResponse<Void> deleteRole(@PathVariable Long id) {
        roleService.removeById(id);
        return ApiResponse.success();
    }

    @PostMapping("/roles/{roleId}/menus")
    @RequiresPermissions({"ROLE_ADMIN"})
    public ApiResponse<Void> assignMenus(@PathVariable Long roleId, @RequestBody List<Long> menuIds) {
        roleService.getById(roleId); // 校验存在性
        roleMenuService.assignMenus(roleId, menuIds);
        return ApiResponse.success();
    }

    // ==================== 用户角色管理 ====================

    @PostMapping("/users/{userId}/roles")
    @RequiresPermissions({"ROLE_ADMIN", "ROLE_DIRECTOR"})
    public ApiResponse<Void> assignUserRoles(@PathVariable Long userId, @RequestBody List<Long> roleIds) {
        userRoleService.assignRoles(userId, roleIds);
        return ApiResponse.success();
    }

    @GetMapping("/users/{userId}/roles")
    public ApiResponse<List<SysRole>> getUserRoles(@PathVariable Long userId) {
        return ApiResponse.success(roleService.getRolesByUserId(userId));
    }

    @GetMapping("/users/{userId}/permissions")
    public ApiResponse<List<SysMenu>> getUserPermissions(@PathVariable Long userId) {
        List<Long> roleIds = userRoleService.getRoleIdsByUserId(userId);
        return ApiResponse.success(menuService.getMenusByRoleIds(roleIds));
    }

    // ==================== 增强登录 ====================

    /**
     * 增强版登录接口（返回带角色信息的 Token）。
     *
     * <p>前端推荐使用此接口替代 /api/v1/auth/login。</p>
     */
    @PostMapping("/auth/login")
    public ApiResponse<TokenResponse> loginWithRoles(@Validated @RequestBody LoginRequest request) {
        TokenResponse base = userService.login(request);
        Long userId = base.getUserId();

        List<SysRole> roles = roleService.getRolesByUserId(userId);
        List<String> roleCodes = roles.stream()
                .map(SysRole::getRoleCode)
                .collect(Collectors.toList());

        String token = JwtUtil.generateToken(userId, base.getUsername(), roleCodes);
        base.setToken(token);
        base.setRoles(roleCodes);
        return ApiResponse.success(base);
    }
}
