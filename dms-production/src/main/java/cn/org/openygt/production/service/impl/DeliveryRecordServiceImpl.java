package cn.org.openygt.production.service.impl;

import cn.org.openygt.production.dto.ConfirmDeliveryRequest;
import cn.org.openygt.production.entity.DeliveryRecord;
import cn.org.openygt.production.entity.Prescription;
import cn.org.openygt.production.mapper.DeliveryRecordMapper;
import cn.org.openygt.production.mapper.PrescriptionMapper;
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
    private final PrescriptionMapper prescriptionMapper;

    public DeliveryRecordServiceImpl(DeliveryRecordMapper deliveryRecordMapper,
                                      PrescriptionMapper prescriptionMapper) {
        this.deliveryRecordMapper = deliveryRecordMapper;
        this.prescriptionMapper = prescriptionMapper;
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
        // P0-5: 校验处方号存在性
        if (StringUtils.hasText(record.getPrescriptionNo())) {
            LambdaQueryWrapper<Prescription> pw = new LambdaQueryWrapper<>();
            pw.eq(Prescription::getPrescriptionNumber, record.getPrescriptionNo());
            Long count = prescriptionMapper.selectCount(pw);
            if (count == null || count == 0) {
                throw new IllegalArgumentException("处方号不存在: " + record.getPrescriptionNo());
            }
        }

        if (record.getStatus() == null) {
            record.setStatus("PENDING");
        }
        deliveryRecordMapper.insert(record);
        return record;
    }

    @Override
    @Transactional
    public DeliveryRecord update(Long id, DeliveryRecord record) {
        // P0-2: 先查出原记录，合并非空字段，避免覆盖已有数据
        DeliveryRecord existing = deliveryRecordMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("交付记录不存在: " + id);
        }

        record.setId(id);
        // 对可能为空的字段做保护：如果传入值为 null 或空字符串，保留原值
        if (!StringUtils.hasText(record.getPrescriptionNo())) {
            record.setPrescriptionNo(existing.getPrescriptionNo());
        }
        if (!StringUtils.hasText(record.getPatientName())) {
            record.setPatientName(existing.getPatientName());
        }
        if (!StringUtils.hasText(record.getDeliveryType())) {
            record.setDeliveryType(existing.getDeliveryType());
        }
        if (!StringUtils.hasText(record.getReceiverName())) {
            record.setReceiverName(existing.getReceiverName());
        }
        if (!StringUtils.hasText(record.getReceiverPhone())) {
            record.setReceiverPhone(existing.getReceiverPhone());
        }
        if (!StringUtils.hasText(record.getReceiverAddress())) {
            record.setReceiverAddress(existing.getReceiverAddress());
        }
        if (!StringUtils.hasText(record.getCourierCompany())) {
            record.setCourierCompany(existing.getCourierCompany());
        }
        if (!StringUtils.hasText(record.getCourierNo())) {
            record.setCourierNo(existing.getCourierNo());
        }
        if (record.getBagCount() == null) {
            record.setBagCount(existing.getBagCount());
        }
        if (!StringUtils.hasText(record.getRemark())) {
            record.setRemark(existing.getRemark());
        }
        // 状态、交付时间等不允许通过编辑修改，保持原值
        record.setStatus(existing.getStatus());
        record.setDeliveredAt(existing.getDeliveredAt());
        record.setOperatorId(existing.getOperatorId());
        record.setCreatedAt(existing.getCreatedAt());
        record.setDeleted(existing.getDeleted());
        record.setTenantId(existing.getTenantId());

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
        // P0-1: 全局逻辑删除配置 + @TableLogic 确保为逻辑删除
        deliveryRecordMapper.deleteById(id);
    }

    @Override
    @Transactional
    public DeliveryRecord confirmDelivery(Long id, ConfirmDeliveryRequest request) {
        DeliveryRecord record = deliveryRecordMapper.selectById(id);
        if (record == null) {
            throw new IllegalArgumentException("交付记录不存在: " + id);
        }
        if ("DELIVERED".equals(record.getStatus())) {
            throw new IllegalStateException("该记录已交付，请勿重复确认");
        }
        record.setStatus("DELIVERED");
        record.setOperatorId(String.valueOf(request.getOperatorId()));
        record.setReceiverName(request.getReceiverName());
        record.setReceiverPhone(request.getReceiverPhone());
        record.setRemark(request.getRemark());
        record.setDeliveredAt(LocalDateTime.now());
        deliveryRecordMapper.updateById(record);
        return record;
    }
}
