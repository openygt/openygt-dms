package cn.org.openygt.masterdata.controller;

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
@RequestMapping("/api/v1/md/schemes")
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
        BeanUtils.copyProperties(entity, resp);
        return resp;
    }
}
