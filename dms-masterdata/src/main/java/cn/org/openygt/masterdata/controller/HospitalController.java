package cn.org.openygt.masterdata.controller;
import cn.org.openygt.masterdata.MasterdataModule;

import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.masterdata.dto.HospitalResponse;
import cn.org.openygt.masterdata.entity.Hospital;
import cn.org.openygt.masterdata.service.HospitalService;
import cn.org.openygt.common.annotation.RequiresPermissions;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping(MasterdataModule.API_PREFIX + "/hospitals")
public class HospitalController {

    private final HospitalService hospitalService;

    public HospitalController(HospitalService hospitalService) {
        this.hospitalService = hospitalService;
    }

    @PostMapping
    @RequiresPermissions({"ROLE_ADMIN"})
    public ApiResponse<HospitalResponse> create(@RequestBody @Valid Hospital hospital) {
        return ApiResponse.success(toResponse(hospitalService.create(hospital)));
    }

    @PutMapping("/{id}")
    @RequiresPermissions({"ROLE_ADMIN"})
    public ApiResponse<HospitalResponse> update(@PathVariable Long id, @RequestBody Hospital hospital) {
        return ApiResponse.success(toResponse(hospitalService.update(id, hospital)));
    }

    @GetMapping("/{id}")
    @RequiresPermissions({"ROLE_ADMIN", "ROLE_DIRECTOR", "ROLE_LEADER"})
    public ApiResponse<HospitalResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(toResponse(hospitalService.getById(id)));
    }

    @GetMapping
    @RequiresPermissions({"ROLE_ADMIN", "ROLE_DIRECTOR", "ROLE_LEADER"})
    public ApiResponse<IPage<HospitalResponse>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        IPage<Hospital> entityPage = hospitalService.list(keyword, page, size);
        // 使用 MyBatis-Plus 的 convert 方法转换记录类型
        IPage<HospitalResponse> respPage = entityPage.convert(this::toResponse);
        return ApiResponse.success(respPage);
    }

    @GetMapping("/all")
    @RequiresPermissions({"ROLE_ADMIN", "ROLE_DIRECTOR", "ROLE_LEADER"})
    public ApiResponse<List<HospitalResponse>> all() {
        IPage<Hospital> entityPage = hospitalService.list(null, 1, 9999);
        return ApiResponse.success(entityPage.getRecords().stream().map(this::toResponse).collect(Collectors.toList()));
    }

    @DeleteMapping("/{id}")
    @RequiresPermissions({"ROLE_ADMIN"})
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
