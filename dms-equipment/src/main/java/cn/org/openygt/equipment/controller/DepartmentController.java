package cn.org.openygt.equipment.controller;

import cn.org.openygt.equipment.EquipmentModule;
import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.equipment.entity.Department;
import cn.org.openygt.equipment.service.DepartmentService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 科室管理接口。
 */
@RestController
@RequestMapping(EquipmentModule.API_PREFIX + "/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    @GetMapping
    public ApiResponse<IPage<Department>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long hospitalId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(departmentService.list(keyword, hospitalId, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<Department> getById(@PathVariable Long id) {
        return ApiResponse.success(departmentService.getById(id));
    }

    @PostMapping
    public ApiResponse<Department> create(@RequestBody Department department) {
        return ApiResponse.success(departmentService.create(department));
    }

    @PutMapping("/{id}")
    public ApiResponse<Department> update(@PathVariable Long id, @RequestBody Department department) {
        return ApiResponse.success(departmentService.update(id, department));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        departmentService.delete(id);
        return ApiResponse.success();
    }

    @GetMapping("/all")
    public ApiResponse<List<Department>> all() {
        return ApiResponse.success(departmentService.listAll());
    }
}
