package cn.org.openygt.production.service.impl;

import cn.org.openygt.production.entity.DeliveryRecord;
import cn.org.openygt.production.mapper.DeliveryRecordMapper;
import cn.org.openygt.production.service.DeliveryRecordService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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
        QueryWrapper<DeliveryRecord> wrapper = new QueryWrapper<>();
        if (StringUtils.hasText(status)) {
            wrapper.eq("status", status);
        }
        if (StringUtils.hasText(deliveryType)) {
            wrapper.eq("delivery_type", deliveryType);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like("prescription_no", keyword)
                    .or().like("patient_name", keyword)
                    .or().like("receiver_name", keyword));
        }
        wrapper.orderByDesc("created_at");
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
        deliveryRecordMapper.deleteById(id);
    }

    @Override
    @Transactional
    public DeliveryRecord confirmDelivery(Long id, String operatorId, String receiverName, String receiverPhone, String remark) {
        DeliveryRecord record = deliveryRecordMapper.selectById(id);
        if (record == null) {
            throw new IllegalArgumentException("交付记录不存在: " + id);
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
