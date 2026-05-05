package cn.org.openygt.production.service.impl;

import cn.org.openygt.production.entity.DeliveryRecord;
import cn.org.openygt.production.mapper.DeliveryRecordMapper;
import cn.org.openygt.production.service.DeliveryRecordService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class DeliveryRecordServiceImpl implements DeliveryRecordService {

    private final DeliveryRecordMapper deliveryRecordMapper;

    public DeliveryRecordServiceImpl(DeliveryRecordMapper deliveryRecordMapper) {
        this.deliveryRecordMapper = deliveryRecordMapper;
    }

    @Override
    public IPage<DeliveryRecord> list(String status, String deliveryType, String keyword, int page, int size) {
        LambdaQueryWrapper<DeliveryRecord> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status)) {
            wrapper.eq(DeliveryRecord::getStatus, status);
        }
        if (StringUtils.hasText(deliveryType)) {
            wrapper.eq(DeliveryRecord::getDeliveryType, deliveryType);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(DeliveryRecord::getPrescriptionNo, keyword)
                    .or().like(DeliveryRecord::getPatientName, keyword)
                    .or().like(DeliveryRecord::getReceiverName, keyword));
        }
        wrapper.orderByDesc(DeliveryRecord::getCreatedAt);
        return deliveryRecordMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public DeliveryRecord getById(Long id) {
        return deliveryRecordMapper.selectById(id);
    }

    @Override
    @Transactional
    public DeliveryRecord create(DeliveryRecord record) {
        if (record.getStatus() == null) {
            record.setStatus("PENDING");
        }
        deliveryRecordMapper.insert(record);
        return record;
    }

    @Override
    @Transactional
    public DeliveryRecord update(Long id, DeliveryRecord record) {
        record.setId(id);
        deliveryRecordMapper.updateById(record);
        return deliveryRecordMapper.selectById(id);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        DeliveryRecord record = deliveryRecordMapper.selectById(id);
        if (record == null) {
            throw new IllegalArgumentException("交付记录不存在: " + id);
        }
        if ("DELIVERED".equals(record.getStatus())) {
            throw new IllegalStateException("已交付记录不允许删除");
        }
        deliveryRecordMapper.deleteById(id);
    }

    @Override
    @Transactional
    public DeliveryRecord confirmDelivery(Long id, String operatorId, String receiverName, String receiverPhone, String remark) {
        DeliveryRecord record = deliveryRecordMapper.selectById(id);
        if (record == null) {
            throw new IllegalArgumentException("交付记录不存在: " + id);
        }
        if ("DELIVERED".equals(record.getStatus())) {
            throw new IllegalStateException("该记录已交付，请勿重复确认");
        }
        record.setStatus("DELIVERED");
        record.setOperatorId(operatorId);
        record.setReceiverName(receiverName);
        record.setReceiverPhone(receiverPhone);
        record.setRemark(remark);
        record.setDeliveredAt(LocalDateTime.now());
        deliveryRecordMapper.updateById(record);
        return record;
    }
}
