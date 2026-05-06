package cn.org.openygt.equipment.controller;

import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.equipment.entity.WashRecord;
import cn.org.openygt.equipment.entity.WashStandard;
import cn.org.openygt.rbac.annotation.RequiresPermissions;
import cn.org.openygt.equipment.service.WashRecordService;
import cn.org.openygt.equipment.service.WashStandardService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/eq")
@RequiredArgsConstructor
public class WashController {

    private final WashRecordService washRecordService;
    private final WashStandardService washStandardService;

    @PostMapping("/wash/start")
    @RequiresPermissions("eq:wash:view")
    public ApiResponse<WashRecord> startWash(@RequestBody StartWashRequest request) {
        return ApiResponse.success(washRecordService.startWash(request.getDeviceId(), request.getTaskId(), request.getOperatorId()));
    }

    @PostMapping("/wash/{id}/complete")
    @RequiresPermissions("eq:wash:view")
    public ApiResponse<WashRecord> completeWash(@PathVariable Long id, @RequestBody CompleteWashRequest request) {
        return ApiResponse.success(washRecordService.completeWash(id, request.getOperatorId(), request.getActualDurationMinutes()));
    }

    @GetMapping("/wash/list")
    public ApiResponse<IPage<WashRecord>> list(
            @RequestParam(required = false) Long deviceId,
            @RequestParam(required = false) Integer washType,
            @RequestParam(required = false) Integer result,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        LambdaQueryWrapper<WashRecord> wrapper = new LambdaQueryWrapper<>();
        if (deviceId != null) wrapper.eq(WashRecord::getDeviceId, deviceId);
        if (washType != null) wrapper.eq(WashRecord::getWashType, washType);
        if (result != null) wrapper.eq(WashRecord::getResult, result);
        wrapper.orderByDesc(WashRecord::getCreatedAt);
        return ApiResponse.success(washRecordService.page(new Page<>(page, size), wrapper));
    }

    @GetMapping("/wash-standard")
    public ApiResponse<IPage<WashStandard>> listStandard(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return ApiResponse.success(washStandardService.page(new Page<>(page, size),
                new LambdaQueryWrapper<WashStandard>().eq(WashStandard::getIsActive, 1)));
    }

    @PostMapping("/wash-standard")
    public ApiResponse<WashStandard> saveStandard(@RequestBody WashStandard standard) {
        washStandardService.saveOrUpdate(standard);
        return ApiResponse.success(standard);
    }

    @lombok.Data
    public static class StartWashRequest {
        private Long deviceId;
        private Long taskId;
        private Long operatorId;
    }

    @lombok.Data
    public static class CompleteWashRequest {
        private Long operatorId;
        private Integer actualDurationMinutes;
    }
}
