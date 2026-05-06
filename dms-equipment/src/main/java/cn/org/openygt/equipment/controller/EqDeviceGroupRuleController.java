package cn.org.openygt.equipment.controller;

import cn.org.openygt.equipment.EquipmentModule;
import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.equipment.entity.EqDeviceGroupRule;
import cn.org.openygt.equipment.service.EqDeviceGroupRuleService;
import cn.org.openygt.rbac.annotation.RequiresPermissions;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 设备分组联动规则接口。
 */
@RestController
@RequestMapping(EquipmentModule.API_PREFIX + "/group-rules")
@RequiredArgsConstructor
public class EqDeviceGroupRuleController {

    private final EqDeviceGroupRuleService ruleService;

    @PostMapping
    @RequiresPermissions("eq:group:view")
    public ApiResponse<Long> create(@RequestBody EqDeviceGroupRule rule) {
        ruleService.save(rule);
        return ApiResponse.success(rule.getId());
    }

    @PutMapping("/{id}")
    @RequiresPermissions("eq:group:view")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody EqDeviceGroupRule rule) {
        rule.setId(id);
        ruleService.updateById(rule);
        return ApiResponse.success();
    }

    @GetMapping("/{id}")
    public ApiResponse<EqDeviceGroupRule> getById(@PathVariable Long id) {
        return ApiResponse.success(ruleService.getById(id));
    }

    @GetMapping
    public ApiResponse<Page<EqDeviceGroupRule>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) Long groupId) {
        QueryWrapper<EqDeviceGroupRule> wrapper = new QueryWrapper<>();
        if (groupId != null) {
            wrapper.eq("group_id", groupId);
        }
        wrapper.orderByDesc("created_at");
        return ApiResponse.success(ruleService.page(new Page<>(page, size), wrapper));
    }

    @DeleteMapping("/{id}")
    @RequiresPermissions("eq:group:view")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        ruleService.removeById(id);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/toggle")
    public ApiResponse<Void> toggle(@PathVariable Long id) {
        EqDeviceGroupRule rule = ruleService.getById(id);
        if (rule == null) {
            throw new IllegalArgumentException("规则不存在: " + id);
        }
        rule.setEnabled(rule.getEnabled() != null && rule.getEnabled() == 1 ? 0 : 1);
        ruleService.updateById(rule);
        return ApiResponse.success();
    }

    @GetMapping("/group/{groupId}")
    public ApiResponse<List<EqDeviceGroupRule>> listByGroup(@PathVariable Long groupId) {
        QueryWrapper<EqDeviceGroupRule> wrapper = new QueryWrapper<>();
        wrapper.eq("group_id", groupId);
        wrapper.eq("enabled", 1);
        wrapper.orderByDesc("created_at");
        return ApiResponse.success(ruleService.list(wrapper));
    }
}
