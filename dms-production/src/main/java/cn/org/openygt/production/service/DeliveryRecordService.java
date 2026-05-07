package cn.org.openygt.production.service;

import cn.org.openygt.production.dto.ConfirmDeliveryRequest;
import cn.org.openygt.production.entity.DeliveryRecord;
import com.baomidou.mybatisplus.core.metadata.IPage;

public interface DeliveryRecordService {
    IPage<DeliveryRecord> list(String status, String deliveryType, String keyword, int page, int size);
    DeliveryRecord getById(Long id);
    DeliveryRecord create(DeliveryRecord record);
    DeliveryRecord update(Long id, DeliveryRecord record);
    void delete(Long id);
    DeliveryRecord confirmDelivery(Long id, ConfirmDeliveryRequest request);
}
