package cn.org.openygt.equipment.controller;
import cn.org.openygt.equipment.EquipmentModule;

import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.equipment.entity.EqDeviceGroup;
import cn.org.openygt.equipment.entity.EqPairingSpec;
import cn.org.openygt.equipment.mapper.EqPairingSpecMapper;
import cn.org.openygt.rbac.annotation.RequiresPermissions;
import cn.org.openygt.equipment.service.EqDeviceGroupService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 设备分组管理接口。
 */
@RestController
@RequestMapping(EquipmentModule.API_PREFIX + "/groups")
@RequiredArgsConstructor
public class EqDeviceGroupController {

    private final EqDeviceGroupService groupService;
    private final EqPairingSpecMapper specMapper;

    // ---- 列表与特殊路由（必须在 /{id} 前） ----

    @GetMapping
    @RequiresPermissions("eq:group:view")
    public ApiResponse<Page<EqDeviceGroup>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String keyword) {
        QueryWrapper<EqDeviceGroup> wrapper = new QueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like("group_name", keyword).or().like("group_code", keyword);
        }
        return ApiResponse.success(groupService.page(new Page<>(page, size), wrapper));
    }

    @GetMapping("/all")
    @RequiresPermissions("eq:group:view")
    public ApiResponse<List<EqDeviceGroup>> all() {
        return ApiResponse.success(groupService.list());
    }

    @GetMapping("/specs")
    @RequiresPermissions("eq:group:view")
    public ApiResponse<List<EqPairingSpec>> specs() {
        return ApiResponse.success(specMapper.selectList(null));
    }

    // ---- 单个资源 CRUD ----

    @PostMapping
    public ApiResponse<Long> create(@RequestBody EqDeviceGroup group) {
        groupService.save(group);
        return ApiResponse.success(group.getId());
    }

    @GetMapping("/{id}")
    public ApiResponse<EqDeviceGroup> getById(@PathVariable Long id) {
        return ApiResponse.success(groupService.getById(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody EqDeviceGroup group) {
        group.setId(id);
        groupService.updateById(group);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        groupService.removeById(id);
        return ApiResponse.success();
    }
}
