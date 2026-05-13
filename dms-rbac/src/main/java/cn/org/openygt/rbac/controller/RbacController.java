package cn.org.openygt.rbac.controller;
import cn.org.openygt.rbac.RbacModule;

import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.common.dto.LoginRequest;
import cn.org.openygt.common.dto.TokenResponse;
import cn.org.openygt.common.util.JwtUtil;
import cn.org.openygt.common.annotation.RequiresPermissions;
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

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * RBAC 管理接口。
 *
 * <p>提供菜单、角色、用户角色绑定等管理功能，以及增强版登录（返回角色信息）。</p>
 */
@RestController
@RequestMapping(RbacModule.API_PREFIX)
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
    public ApiResponse<List<SysMenu>> menuTree(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.success(Collections.emptyList());
        }
        List<Long> roleIds = userRoleService.getRoleIdsByUserId(userId);
        if (roleIds == null || roleIds.isEmpty()) {
            return ApiResponse.success(Collections.emptyList());
        }
        // ROLE_ADMIN 看全部菜单
        List<SysRole> roles = roleService.getRolesByUserId(userId);
        boolean isAdmin = roles != null && roles.stream()
                .anyMatch(r -> "ROLE_ADMIN".equals(r.getRoleCode()));
        if (isAdmin) {
            return ApiResponse.success(menuService.getMenuTree());
        }
        return ApiResponse.success(menuService.getMenusByRoleIds(roleIds));
    }

    @PostMapping("/menus")
    @RequiresPermissions({"ROLE_ADMIN"})
    public ApiResponse<SysMenu> createMenu(@RequestBody @Valid SysMenu menu) {
        menuService.save(menu);
        return ApiResponse.success(menu);
    }

    @PutMapping("/menus/{id}")
    @RequiresPermissions({"ROLE_ADMIN"})
    public ApiResponse<SysMenu> updateMenu(@PathVariable Long id, @RequestBody @Valid SysMenu menu) {
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

    @RequiresPermissions({"ROLE_ADMIN", "ROLE_DIRECTOR"})
    @GetMapping("/roles")
    public ApiResponse<List<SysRole>> roleList() {
        return ApiResponse.success(roleService.list());
    }

    @RequiresPermissions({"ROLE_ADMIN", "ROLE_DIRECTOR"})
    @GetMapping("/roles/{id}")
    public ApiResponse<SysRole> getRole(@PathVariable Long id) {
        return ApiResponse.success(roleService.getById(id));
    }

    @PostMapping("/roles")
    @RequiresPermissions({"ROLE_ADMIN"})
    public ApiResponse<SysRole> createRole(@RequestBody @Valid SysRole role) {
        roleService.save(role);
        return ApiResponse.success(role);
    }

    @PutMapping("/roles/{id}")
    @RequiresPermissions({"ROLE_ADMIN"})
    public ApiResponse<SysRole> updateRole(@PathVariable Long id, @RequestBody @Valid SysRole role) {
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

    @RequiresPermissions({"ROLE_ADMIN", "ROLE_DIRECTOR"})
    @GetMapping("/users/{userId}/roles")
    public ApiResponse<List<SysRole>> getUserRoles(@PathVariable Long userId) {
        return ApiResponse.success(roleService.getRolesByUserId(userId));
    }

    @RequiresPermissions({"ROLE_ADMIN", "ROLE_DIRECTOR"})
    @GetMapping("/users/{userId}/permissions")
    public ApiResponse<List<SysMenu>> getUserPermissions(@PathVariable Long userId) {
        List<Long> roleIds = userRoleService.getRoleIdsByUserId(userId);
        return ApiResponse.success(menuService.getMenusByRoleIds(roleIds));
    }

}
