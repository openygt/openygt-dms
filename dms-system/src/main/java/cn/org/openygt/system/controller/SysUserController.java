package cn.org.openygt.system.controller;
import cn.org.openygt.system.SystemModule;

import cn.org.openygt.common.annotation.RequiresPermissions;
import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.common.dto.ChangePasswordRequest;
import cn.org.openygt.system.entity.SysUser;
import cn.org.openygt.system.service.SysUserService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping(SystemModule.API_PREFIX + "/users")
public class SysUserController {

    private final SysUserService userService;

    public SysUserController(SysUserService userService) {
        this.userService = userService;
    }

    @RequiresPermissions({"ROLE_ADMIN"})
    @PostMapping
    public ApiResponse<SysUser> create(@RequestBody @Valid SysUser user) {
        return ApiResponse.success(userService.create(user));
    }

    @RequiresPermissions({"ROLE_ADMIN"})
    @PutMapping("/{id}")
    public ApiResponse<SysUser> update(@PathVariable Long id, @RequestBody @Valid SysUser user) {
        return ApiResponse.success(userService.update(id, user));
    }

    @RequiresPermissions({"ROLE_ADMIN", "ROLE_DIRECTOR"})
    @GetMapping("/{id}")
    public ApiResponse<SysUser> getById(@PathVariable Long id) {
        return ApiResponse.success(userService.getById(id));
    }

    @RequiresPermissions({"ROLE_ADMIN", "ROLE_DIRECTOR"})
    @GetMapping
    public ApiResponse<IPage<SysUser>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(userService.list(keyword, page, size));
    }

    @RequiresPermissions({"ROLE_ADMIN"})
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ApiResponse.success();
    }

    @RequiresPermissions({"ROLE_ADMIN", "ROLE_DIRECTOR", "ROLE_LEADER", "ROLE_WORKER", "ROLE_INSPECTOR", "ROLE_SHIPPER"})
    @PostMapping("/change-password")
    public ApiResponse<Void> changePassword(@RequestAttribute("userId") Long userId,
                                             @RequestBody @Valid ChangePasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            return ApiResponse.error(400, "两次输入的新密码不一致");
        }
        userService.changePassword(userId, request.getOldPassword(), request.getNewPassword());
        return ApiResponse.success();
    }
}
