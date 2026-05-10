package cn.org.openygt.system.controller;
import cn.org.openygt.system.SystemModule;

import cn.org.openygt.common.annotation.RequiresPermissions;
import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.system.entity.SysConfig;
import cn.org.openygt.system.service.SysConfigService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(SystemModule.API_PREFIX + "/configs")
@RequiredArgsConstructor
public class SysConfigController {

    private final SysConfigService configService;

    @RequiresPermissions({"ROLE_ADMIN"})
    @PostMapping
    public ApiResponse<SysConfig> create(@RequestBody SysConfig config) {
        return ApiResponse.success(configService.create(config));
    }

    @RequiresPermissions({"ROLE_ADMIN"})
    @PutMapping("/{id}")
    public ApiResponse<SysConfig> update(@PathVariable Long id, @RequestBody SysConfig config) {
        return ApiResponse.success(configService.update(id, config));
    }

    @RequiresPermissions({"ROLE_ADMIN", "ROLE_DIRECTOR"})
    @GetMapping("/{id}")
    public ApiResponse<SysConfig> getById(@PathVariable Long id) {
        return ApiResponse.success(configService.getById(id));
    }

    @RequiresPermissions({"ROLE_ADMIN", "ROLE_DIRECTOR"})
    @GetMapping("/key/{configKey}")
    public ApiResponse<SysConfig> getByKey(@PathVariable String configKey) {
        return ApiResponse.success(configService.getByKey(configKey));
    }

    @RequiresPermissions({"ROLE_ADMIN", "ROLE_DIRECTOR"})
    @GetMapping
    public ApiResponse<IPage<SysConfig>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(configService.list(keyword, page, size));
    }

    @RequiresPermissions({"ROLE_ADMIN"})
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        configService.delete(id);
        return ApiResponse.success();
    }
}
