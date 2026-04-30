package cn.org.openygt.equipment.controller;

import cn.org.openygt.equipment.EquipmentModule;
import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.equipment.entity.Doctor;
import cn.org.openygt.equipment.service.DoctorService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 医师管理接口。
 */
@RestController
@RequestMapping(EquipmentModule.API_PREFIX + "/doctors")
@RequiredArgsConstructor
public class DoctorController {

    private final DoctorService doctorService;

    @GetMapping
    public ApiResponse<IPage<Doctor>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long hospitalId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(doctorService.list(keyword, departmentId, hospitalId, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<Doctor> getById(@PathVariable Long id) {
        return ApiResponse.success(doctorService.getById(id));
    }

    @PostMapping
    public ApiResponse<Doctor> create(@RequestBody Doctor doctor) {
        return ApiResponse.success(doctorService.create(doctor));
    }

    @PutMapping("/{id}")
    public ApiResponse<Doctor> update(@PathVariable Long id, @RequestBody Doctor doctor) {
        return ApiResponse.success(doctorService.update(id, doctor));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        doctorService.delete(id);
        return ApiResponse.success();
    }
}
