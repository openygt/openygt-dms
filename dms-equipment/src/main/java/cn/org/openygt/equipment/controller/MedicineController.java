package cn.org.openygt.equipment.controller;

import cn.org.openygt.equipment.EquipmentModule;
import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.equipment.entity.Medicine;
import cn.org.openygt.equipment.service.MedicineService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 药材目录管理接口。
 */
@RestController
@RequestMapping(EquipmentModule.API_PREFIX + "/medicines")
@RequiredArgsConstructor
public class MedicineController {

    private final MedicineService medicineService;

    @GetMapping
    public ApiResponse<IPage<Medicine>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(medicineService.list(keyword, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<Medicine> getById(@PathVariable Long id) {
        return ApiResponse.success(medicineService.getById(id));
    }

    @PostMapping
    public ApiResponse<Medicine> create(@RequestBody Medicine medicine) {
        return ApiResponse.success(medicineService.create(medicine));
    }

    @PutMapping("/{id}")
    public ApiResponse<Medicine> update(@PathVariable Long id, @RequestBody Medicine medicine) {
        return ApiResponse.success(medicineService.update(id, medicine));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        medicineService.delete(id);
        return ApiResponse.success();
    }
}
