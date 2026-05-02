package cn.org.openygt.quality.controller;

import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.quality.entity.RetainSample;
import cn.org.openygt.quality.service.RetainSampleService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/qt/retain-sample")
@RequiredArgsConstructor
public class RetainSampleController {

    private final RetainSampleService retainSampleService;

    @GetMapping("/list")
    public ApiResponse<IPage<RetainSample>> list(
            @RequestParam(required = false) Long taskId,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        LambdaQueryWrapper<RetainSample> wrapper = new LambdaQueryWrapper<>();
        if (taskId != null) wrapper.eq(RetainSample::getTaskId, taskId);
        if (status != null) wrapper.eq(RetainSample::getStatus, status);
        wrapper.orderByDesc(RetainSample::getCreatedAt);
        return ApiResponse.success(retainSampleService.page(new Page<>(page, size), wrapper));
    }

    @PostMapping("/{id}/destroy")
    public ApiResponse<RetainSample> destroy(@PathVariable Long id, @RequestBody DestroyRequest request) {
        return ApiResponse.success(retainSampleService.destroySample(id, request.getDestroyBy(), request.getRemark()));
    }

    @GetMapping("/expiring")
    public ApiResponse<List<RetainSample>> expiring(@RequestParam(defaultValue = "2") Integer withinHours) {
        return ApiResponse.success(retainSampleService.findExpiringSamples(withinHours));
    }

    @lombok.Data
    public static class DestroyRequest {
        private Long destroyBy;
        private String remark;
    }
}
