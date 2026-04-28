package cn.org.openygt.system.controller;
import cn.org.openygt.system.SystemModule;

import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.system.entity.SysUser;
import cn.org.openygt.system.service.SysUserService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(SystemModule.API_PREFIX + "/users")
public class SysUserController {

    private final SysUserService userService;

    public SysUserController(SysUserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ApiResponse<SysUser> create(@RequestBody SysUser user) {
        return ApiResponse.success(userService.create(user));
    }

    @PutMapping("/{id}")
    public ApiResponse<SysUser> update(@PathVariable Long id, @RequestBody SysUser user) {
        return ApiResponse.success(userService.update(id, user));
    }

    @GetMapping("/{id}")
    public ApiResponse<SysUser> getById(@PathVariable Long id) {
        return ApiResponse.success(userService.getById(id));
    }

    @GetMapping
    public ApiResponse<IPage<SysUser>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(userService.list(keyword, page, size));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ApiResponse.success();
    }
}
