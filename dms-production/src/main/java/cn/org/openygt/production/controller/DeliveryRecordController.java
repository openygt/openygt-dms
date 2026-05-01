package cn.org.openygt.production.controller;

import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.production.ProductionModule;
import cn.org.openygt.production.entity.DeliveryRecord;
import cn.org.openygt.production.service.DeliveryRecordService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ProductionModule.API_PREFIX + "/delivery-records")
@RequiredArgsConstructor
public class DeliveryRecordController {

    private final DeliveryRecordService deliveryRecordService;

    @GetMapping
    public ApiResponse<IPage<DeliveryRecord>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String deliveryType,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(deliveryRecordService.list(status, deliveryType, keyword, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<DeliveryRecord> getById(@PathVariable Long id) {
        return ApiResponse.success(deliveryRecordService.getById(id));
    }

    @PostMapping
    public ApiResponse<DeliveryRecord> create(@RequestBody DeliveryRecord record) {
        return ApiResponse.success(deliveryRecordService.create(record));
    }

    @PutMapping("/{id}")
    public ApiResponse<DeliveryRecord> update(@PathVariable Long id, @RequestBody DeliveryRecord record) {
        return ApiResponse.success(deliveryRecordService.update(id, record));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        deliveryRecordService.delete(id);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/confirm")
    public ApiResponse<DeliveryRecord> confirmDelivery(
            @PathVariable Long id,
            @RequestParam String operatorId,
            @RequestParam String receiverName,
            @RequestParam(required = false) String receiverPhone,
            @RequestParam(required = false) String remark) {
        return ApiResponse.success(deliveryRecordService.confirmDelivery(id, operatorId, receiverName, receiverPhone, remark));
    }
}
