package cn.org.openygt.masterdata.controller;
import cn.org.openygt.masterdata.MasterdataModule;

import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.masterdata.dto.SchemeCreateRequest;
import cn.org.openygt.masterdata.dto.SchemeResponse;
import cn.org.openygt.masterdata.dto.SchemeUpdateRequest;
import cn.org.openygt.masterdata.entity.DecoctScheme;
import cn.org.openygt.masterdata.service.DecoctSchemeService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping(MasterdataModule.API_PREFIX + "/schemes")
public class DecoctSchemeController {

    private final DecoctSchemeService schemeService;

    public DecoctSchemeController(DecoctSchemeService schemeService) {
        this.schemeService = schemeService;
    }

    @PostMapping
    public ApiResponse<SchemeResponse> create(@RequestBody @Valid SchemeCreateRequest request) {
        DecoctScheme scheme = new DecoctScheme();
        BeanUtils.copyProperties(request, scheme);
        return ApiResponse.success(toResponse(schemeService.create(scheme)));
    }

    @PutMapping("/{id}")
    public ApiResponse<SchemeResponse> update(@PathVariable Long id, @RequestBody @Valid SchemeUpdateRequest request) {
        DecoctScheme scheme = new DecoctScheme();
        BeanUtils.copyProperties(request, scheme);
        return ApiResponse.success(toResponse(schemeService.update(id, scheme)));
    }

    @GetMapping("/{id}")
    public ApiResponse<SchemeResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(toResponse(schemeService.getById(id)));
    }

    @GetMapping
    public ApiResponse<IPage<SchemeResponse>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        IPage<DecoctScheme> entityPage = schemeService.list(keyword, page, size);
        IPage<SchemeResponse> respPage = entityPage.convert(this::toResponse);
        return ApiResponse.success(respPage);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        schemeService.delete(id);
        return ApiResponse.success();
    }

    private SchemeResponse toResponse(DecoctScheme entity) {
        if (entity == null) return null;
        SchemeResponse resp = new SchemeResponse();
        resp.setSchemeName(entity.getName());
        resp.setSchemeCode(entity.getCode());
        resp.setDecoctTime(entity.getHeatingTime());
        resp.setSoakTime(entity.getPreHeatingTime());
        resp.setSchemeType(entity.getSchemeType());
        resp.setDecoctTimes(entity.getDecoctTimes());
        resp.setPressure(entity.getPressure());
        resp.setUpperWater(entity.getUpperWater());
        resp.setPostHeatingTime(entity.getPostHeatingTime());
        resp.setRemark(entity.getDescription());
        resp.setFirstDecoctTime(entity.getFirstDecoctTime());
        resp.setSecondDecoctTime(entity.getSecondDecoctTime());
        resp.setDrainTime(entity.getDrainTime());
        resp.setPackageTime(entity.getPackageTime());
        resp.setLateAddRemindTime(entity.getLateAddRemindTime());
        resp.setTempRiseRate(entity.getTempRiseRate());
        resp.setIsDefault(entity.getIsDefault());
        resp.setAlarmHighTemp(entity.getAlarmHighTemp());
        resp.setAlarmLowTemp(entity.getAlarmLowTemp());
        if (entity.getAlarmLowTemp() != null && entity.getAlarmHighTemp() != null) {
            resp.setTempRange(entity.getAlarmLowTemp() + "~" + entity.getAlarmHighTemp() + "°C");
        } else {
            resp.setTempRange("-");
        }
        resp.setStatus(entity.getStatus() != null ? entity.getStatus() : 1);
        resp.setCreatedAt(entity.getCreatedAt());
        resp.setUpdatedAt(entity.getUpdatedAt());
        resp.setId(entity.getId());
        return resp;
    }
}
