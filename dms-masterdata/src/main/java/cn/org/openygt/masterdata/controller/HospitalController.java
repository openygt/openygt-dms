package cn.org.openygt.masterdata.controller;
import cn.org.openygt.masterdata.MasterdataModule;

import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.masterdata.dto.HospitalResponse;
import cn.org.openygt.masterdata.entity.Hospital;
import cn.org.openygt.masterdata.service.HospitalService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(MasterdataModule.API_PREFIX + "/hospitals")
public class HospitalController {

    private final HospitalService hospitalService;

    public HospitalController(HospitalService hospitalService) {
        this.hospitalService = hospitalService;
    }

    @PostMapping
    public ApiResponse<HospitalResponse> create(@RequestBody Hospital hospital) {
        return ApiResponse.success(toResponse(hospitalService.create(hospital)));
    }

    @PutMapping("/{id}")
    public ApiResponse<HospitalResponse> update(@PathVariable Long id, @RequestBody Hospital hospital) {
        return ApiResponse.success(toResponse(hospitalService.update(id, hospital)));
    }

    @GetMapping("/{id}")
    public ApiResponse<HospitalResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(toResponse(hospitalService.getById(id)));
    }

    @GetMapping
    public ApiResponse<IPage<HospitalResponse>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        IPage<Hospital> entityPage = hospitalService.list(keyword, page, size);
        // 使用 MyBatis-Plus 的 convert 方法转换记录类型
        IPage<HospitalResponse> respPage = entityPage.convert(this::toResponse);
        return ApiResponse.success(respPage);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        hospitalService.delete(id);
        return ApiResponse.success();
    }

    private HospitalResponse toResponse(Hospital entity) {
        if (entity == null) return null;
        HospitalResponse resp = new HospitalResponse();
        resp.setId(entity.getId());
        resp.setName(entity.getName());
        resp.setCode(entity.getCode());
        resp.setContactPerson(entity.getContactPerson());
        resp.setPhone(entity.getPhone());
        resp.setAddress(entity.getAddress());
        resp.setStatus(entity.getStatus());
        resp.setCreatedAt(entity.getCreatedAt());
        resp.setUpdatedAt(entity.getUpdatedAt());
        return resp;
    }
}
