package cn.org.openygt.system.controller;

import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.system.SystemModule;
import cn.org.openygt.system.entity.InterfaceConfig;
import cn.org.openygt.system.service.InterfaceConfigService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(SystemModule.API_PREFIX + "/interfaces")
@RequiredArgsConstructor
public class InterfaceConfigController {

    private final InterfaceConfigService interfaceConfigService;

    @GetMapping
    public ApiResponse<Page<InterfaceConfig>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String interfaceType) {
        return ApiResponse.success(interfaceConfigService.listConfigs(keyword, interfaceType, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<InterfaceConfig> getById(@PathVariable Long id) {
        InterfaceConfig config = interfaceConfigService.getById(id);
        return config == null ? ApiResponse.error(404, "接口配置不存在") : ApiResponse.success(config);
    }

    @PostMapping
    public ApiResponse<InterfaceConfig> create(@Validated @RequestBody InterfaceConfig config) {
        interfaceConfigService.save(config);
        return ApiResponse.success(config);
    }

    @PutMapping("/{id}")
    public ApiResponse<InterfaceConfig> update(@PathVariable Long id, @Validated @RequestBody InterfaceConfig config) {
        config.setId(id);
        interfaceConfigService.updateById(config);
        return ApiResponse.success(config);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        interfaceConfigService.removeById(id);
        return ApiResponse.success();
    }
}
