package cn.org.openygt.masterdata.controller;

import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.masterdata.MasterdataModule;
import cn.org.openygt.masterdata.entity.Department;
import cn.org.openygt.masterdata.service.DepartmentService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping(MasterdataModule.API_PREFIX + "/departments")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @PostMapping
    public ApiResponse<Department> create(@RequestBody Department department) {
        return ApiResponse.success(departmentService.create(department));
    }

    @PutMapping("/{id}")
    public ApiResponse<Department> update(@PathVariable Long id, @RequestBody Department department) {
        return ApiResponse.success(departmentService.update(id, department));
    }

    @GetMapping("/{id}")
    public ApiResponse<Department> getById(@PathVariable Long id) {
        return ApiResponse.success(departmentService.getById(id));
    }

    @GetMapping
    public ApiResponse<IPage<Department>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(departmentService.list(keyword, page, size));
    }

    @GetMapping("/all")
    public ApiResponse<List<Department>> all() {
        IPage<Department> entityPage = departmentService.list(null, 1, 9999);
        return ApiResponse.success(entityPage.getRecords().stream().collect(Collectors.toList()));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        departmentService.delete(id);
        return ApiResponse.success();
    }
}
