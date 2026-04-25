package cn.org.openygt.masterdata.controller;

import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.masterdata.entity.Hospital;
import cn.org.openygt.masterdata.service.HospitalService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/md/hospitals")
public class HospitalController {

    private final HospitalService hospitalService;

    public HospitalController(HospitalService hospitalService) {
        this.hospitalService = hospitalService;
    }

    @PostMapping
    public ApiResponse<Hospital> create(@RequestBody Hospital hospital) {
        return ApiResponse.success(hospitalService.create(hospital));
    }

    @PutMapping("/{id}")
    public ApiResponse<Hospital> update(@PathVariable Long id, @RequestBody Hospital hospital) {
        return ApiResponse.success(hospitalService.update(id, hospital));
    }

    @GetMapping("/{id}")
    public ApiResponse<Hospital> getById(@PathVariable Long id) {
        return ApiResponse.success(hospitalService.getById(id));
    }

    @GetMapping
    public ApiResponse<IPage<Hospital>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(hospitalService.list(page, size));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        hospitalService.delete(id);
        return ApiResponse.success();
    }
}
