package cn.org.openygt.equipment.controller;

import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.equipment.EquipmentModule;
import cn.org.openygt.equipment.entity.LabelTemplate;
import cn.org.openygt.equipment.service.LabelTemplateService;
import cn.org.openygt.common.annotation.RequiresPermissions;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(EquipmentModule.API_PREFIX + "/label-templates")
@RequiredArgsConstructor
public class LabelTemplateController {

    private final LabelTemplateService labelTemplateService;

    @GetMapping
    @RequiresPermissions({"ROLE_ADMIN", "ROLE_DIRECTOR", "ROLE_LEADER"})
    public ApiResponse<IPage<LabelTemplate>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String templateType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(labelTemplateService.list(keyword, templateType, page, size));
    }

    @GetMapping("/{id}")
    @RequiresPermissions({"ROLE_ADMIN", "ROLE_DIRECTOR", "ROLE_LEADER"})
    public ApiResponse<LabelTemplate> getById(@PathVariable Long id) {
        return ApiResponse.success(labelTemplateService.getById(id));
    }

    @PostMapping
    @RequiresPermissions({"ROLE_ADMIN"})
    public ApiResponse<LabelTemplate> create(@RequestBody LabelTemplate template) {
        template.setStatus(template.getStatus() != null ? template.getStatus() : 1);
        labelTemplateService.save(template);
        return ApiResponse.success(template);
    }

    @PutMapping("/{id}")
    @RequiresPermissions({"ROLE_ADMIN"})
    public ApiResponse<LabelTemplate> update(@PathVariable Long id, @RequestBody LabelTemplate template) {
        template.setId(id);
        labelTemplateService.updateById(template);
        return ApiResponse.success(template);
    }

    @DeleteMapping("/{id}")
    @RequiresPermissions({"ROLE_ADMIN"})
    public ApiResponse<Void> delete(@PathVariable Long id) {
        labelTemplateService.removeById(id);
        return ApiResponse.success();
    }
}
