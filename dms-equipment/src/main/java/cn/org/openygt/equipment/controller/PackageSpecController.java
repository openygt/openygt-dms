package cn.org.openygt.equipment.controller;

import cn.org.openygt.equipment.EquipmentModule;
import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.equipment.entity.PackageSpec;
import cn.org.openygt.equipment.service.PackageSpecService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 包装规格管理接口。
 */
@RestController
@RequestMapping(EquipmentModule.API_PREFIX + "/package-specs")
@RequiredArgsConstructor
public class PackageSpecController {

    private final PackageSpecService packageSpecService;

    @GetMapping
    public ApiResponse<IPage<PackageSpec>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(packageSpecService.list(keyword, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<PackageSpec> getById(@PathVariable Long id) {
        return ApiResponse.success(packageSpecService.getById(id));
    }

    @PostMapping
    public ApiResponse<PackageSpec> create(@RequestBody PackageSpec packageSpec) {
        return ApiResponse.success(packageSpecService.create(packageSpec));
    }

    @PutMapping("/{id}")
    public ApiResponse<PackageSpec> update(@PathVariable Long id, @RequestBody PackageSpec packageSpec) {
        return ApiResponse.success(packageSpecService.update(id, packageSpec));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        packageSpecService.delete(id);
        return ApiResponse.success();
    }
}
