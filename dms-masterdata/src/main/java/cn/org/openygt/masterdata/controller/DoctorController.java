package cn.org.openygt.masterdata.controller;

import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.masterdata.MasterdataModule;
import cn.org.openygt.masterdata.entity.Doctor;
import cn.org.openygt.masterdata.service.DoctorService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.web.bind.annotation.*;

@RestController("mdDoctorController")
@RequestMapping(MasterdataModule.API_PREFIX + "/doctors")
public class DoctorController {

    private final DoctorService doctorService;

    public DoctorController(DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    @PostMapping
    public ApiResponse<Doctor> create(@RequestBody Doctor doctor) {
        return ApiResponse.success(doctorService.create(doctor));
    }

    @PutMapping("/{id}")
    public ApiResponse<Doctor> update(@PathVariable Long id, @RequestBody Doctor doctor) {
        return ApiResponse.success(doctorService.update(id, doctor));
    }

    @GetMapping("/{id}")
    public ApiResponse<Doctor> getById(@PathVariable Long id) {
        return ApiResponse.success(doctorService.getById(id));
    }

    @GetMapping
    public ApiResponse<IPage<Doctor>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long hospitalId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(doctorService.list(keyword, hospitalId, page, size));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        doctorService.delete(id);
        return ApiResponse.success();
    }
}
