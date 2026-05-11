package cn.org.openygt.masterdata.controller;

import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.masterdata.MasterdataModule;
import cn.org.openygt.masterdata.entity.Doctor;
import cn.org.openygt.masterdata.service.DoctorService;
import cn.org.openygt.common.annotation.RequiresPermissions;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;

@RestController("mdDoctorController")
@RequestMapping(MasterdataModule.API_PREFIX + "/doctors")
public class DoctorController {

    private final DoctorService doctorService;

    public DoctorController(DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    @PostMapping
    @RequiresPermissions({"ROLE_ADMIN"})
    public ApiResponse<Doctor> create(@RequestBody @Valid Doctor doctor) {
        return ApiResponse.success(doctorService.create(doctor));
    }

    @PutMapping("/{id}")
    @RequiresPermissions({"ROLE_ADMIN"})
    public ApiResponse<Doctor> update(@PathVariable Long id, @RequestBody Doctor doctor) {
        return ApiResponse.success(doctorService.update(id, doctor));
    }

    @GetMapping("/{id}")
    @RequiresPermissions({"ROLE_ADMIN", "ROLE_DIRECTOR", "ROLE_LEADER"})
    public ApiResponse<Doctor> getById(@PathVariable Long id) {
        return ApiResponse.success(doctorService.getById(id));
    }

    @GetMapping
    @RequiresPermissions({"ROLE_ADMIN", "ROLE_DIRECTOR", "ROLE_LEADER"})
    public ApiResponse<IPage<Doctor>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long hospitalId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(doctorService.list(keyword, hospitalId, page, size));
    }

    @DeleteMapping("/{id}")
    @RequiresPermissions({"ROLE_ADMIN"})
    public ApiResponse<Void> delete(@PathVariable Long id) {
        doctorService.delete(id);
        return ApiResponse.success();
    }
}
