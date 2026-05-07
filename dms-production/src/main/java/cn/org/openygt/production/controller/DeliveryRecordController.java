package cn.org.openygt.production.controller;

import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.production.ProductionModule;
import cn.org.openygt.production.dto.ConfirmDeliveryRequest;
import cn.org.openygt.production.entity.DeliveryRecord;
import cn.org.openygt.production.service.DeliveryRecordService;
import cn.org.openygt.rbac.annotation.RequiresPermissions;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping(ProductionModule.API_PREFIX + "/delivery-records")
@RequiredArgsConstructor
public class DeliveryRecordController {

    private final DeliveryRecordService deliveryRecordService;

    @GetMapping
    @RequiresPermissions("prod:dispatch:view")
    public ApiResponse<IPage<DeliveryRecord>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String deliveryType,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(deliveryRecordService.list(status, deliveryType, keyword, page, size));
    }

    @GetMapping("/{id}")
    @RequiresPermissions("prod:dispatch:view")
    public ApiResponse<DeliveryRecord> getById(@PathVariable Long id) {
        return ApiResponse.success(deliveryRecordService.getById(id));
    }

    @PostMapping
    @RequiresPermissions("prod:dispatch:view")
    public ApiResponse<DeliveryRecord> create(@RequestBody @Valid DeliveryRecord record) {
        return ApiResponse.success(deliveryRecordService.create(record));
    }

    @PutMapping("/{id}")
    @RequiresPermissions("prod:dispatch:view")
    public ApiResponse<DeliveryRecord> update(@PathVariable Long id, @RequestBody @Valid DeliveryRecord record) {
        return ApiResponse.success(deliveryRecordService.update(id, record));
    }

    @DeleteMapping("/{id}")
    @RequiresPermissions("prod:dispatch:view")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        deliveryRecordService.delete(id);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/confirm")
    @RequiresPermissions("prod:dispatch:view")
    public ApiResponse<DeliveryRecord> confirmDelivery(
            @PathVariable Long id,
            @RequestBody @Valid ConfirmDeliveryRequest request) {
        return ApiResponse.success(deliveryRecordService.confirmDelivery(id, request));
    }
}
