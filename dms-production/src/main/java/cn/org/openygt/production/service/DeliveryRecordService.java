package cn.org.openygt.production.service;

import cn.org.openygt.production.entity.DeliveryRecord;
import com.baomidou.mybatisplus.core.metadata.IPage;

public interface DeliveryRecordService {
    IPage<DeliveryRecord> list(String status, String deliveryType, String keyword, int page, int size);
    DeliveryRecord getById(Long id);
    DeliveryRecord create(DeliveryRecord record);
    DeliveryRecord update(Long id, DeliveryRecord record);
    void delete(Long id);
    DeliveryRecord confirmDelivery(Long id, String operatorId, String receiverName, String receiverPhone, String remark);
}
