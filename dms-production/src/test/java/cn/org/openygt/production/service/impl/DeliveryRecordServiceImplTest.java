package cn.org.openygt.production.service.impl;

import cn.org.openygt.production.dto.ConfirmDeliveryRequest;
import cn.org.openygt.production.entity.DeliveryRecord;
import cn.org.openygt.production.entity.Prescription;
import cn.org.openygt.production.mapper.DeliveryRecordMapper;
import cn.org.openygt.production.mapper.PrescriptionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DeliveryRecordServiceImplTest {

    @Mock
    private DeliveryRecordMapper deliveryRecordMapper;

    @Mock
    private PrescriptionMapper prescriptionMapper;

    @Captor
    private ArgumentCaptor<DeliveryRecord> recordCaptor;

    @Captor
    private ArgumentCaptor<LambdaQueryWrapper<Prescription>> prescriptionWrapperCaptor;

    private DeliveryRecordServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DeliveryRecordServiceImpl(deliveryRecordMapper, prescriptionMapper);
    }

    @Nested
    @DisplayName("业务规则: 新增交付记录")
    class CreateTests {

        @Test
        @DisplayName("正常场景: 处方号存在时创建成功")
        void create_whenPrescriptionExists_shouldSucceed() {
            DeliveryRecord record = new DeliveryRecord();
            record.setPrescriptionNo("P20250428001");
            record.setPatientName("张三");

            when(prescriptionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
            when(deliveryRecordMapper.insert((DeliveryRecord) any())).thenReturn(1);

            DeliveryRecord result = service.create(record);

            assertEquals("PENDING", result.getStatus());
            verify(prescriptionMapper).selectCount(any(LambdaQueryWrapper.class));
            verify(deliveryRecordMapper).insert(recordCaptor.capture());
            assertEquals("PENDING", recordCaptor.getValue().getStatus());
            assertEquals("PENDING", recordCaptor.getValue().getStatus());
        }

        @Test
        @DisplayName("边界条件: 处方号为空时跳过校验并创建")
        void create_whenPrescriptionNoEmpty_shouldSkipValidation() {
            DeliveryRecord record = new DeliveryRecord();
            record.setPatientName("张三");

            when(deliveryRecordMapper.insert(any(DeliveryRecord.class))).thenReturn(1);

            DeliveryRecord result = service.create(record);

            verify(prescriptionMapper, never()).selectCount(any());
            verify(deliveryRecordMapper).insert(recordCaptor.capture());
            assertEquals("PENDING", recordCaptor.getValue().getStatus());
        }

        @Test
        @DisplayName("规则冲突: 处方号不存在时应拦截")
        void create_whenPrescriptionNotExists_shouldThrow() {
            DeliveryRecord record = new DeliveryRecord();
            record.setPrescriptionNo("P99999999");
            record.setPatientName("张三");

            when(prescriptionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.create(record));
            assertEquals("处方号不存在: P99999999", ex.getMessage());
            verify(deliveryRecordMapper, never()).insert((DeliveryRecord) any());
        }
    }

    @Nested
    @DisplayName("业务规则: 更新交付记录")
    class UpdateTests {

        @Test
        @DisplayName("正常场景: 传入部分字段时保留原值")
        void update_withPartialFields_shouldPreserveExistingValues() {
            Long id = 1L;
            DeliveryRecord existing = new DeliveryRecord();
            existing.setId(id);
            existing.setPrescriptionNo("P001");
            existing.setPatientName("张三");
            existing.setReceiverName("张三");
            existing.setReceiverPhone("13800138001");
            existing.setStatus("PENDING");
            existing.setBagCount(7);

            DeliveryRecord update = new DeliveryRecord();
            update.setPatientName("张三（改）");
            // receiverPhone 为 null，应保留原值
            // status 不应被修改

            when(deliveryRecordMapper.selectById(id)).thenReturn(existing);
            when(deliveryRecordMapper.updateById((DeliveryRecord) any())).thenReturn(1);
            when(deliveryRecordMapper.selectById(id)).thenReturn(existing).thenReturn(existing);

            service.update(id, update);

            verify(deliveryRecordMapper).updateById(recordCaptor.capture());
            DeliveryRecord captured = recordCaptor.getValue();
            assertEquals("张三（改）", captured.getPatientName());
            assertEquals("P001", captured.getPrescriptionNo()); // 保留原值
            assertEquals("13800138001", captured.getReceiverPhone()); // 保留原值
            assertEquals("PENDING", captured.getStatus()); // 不允许修改
            assertEquals(7, captured.getBagCount()); // 保留原值
        }

        @Test
        @DisplayName("规则冲突: 记录不存在时应拦截")
        void update_whenRecordNotExists_shouldThrow() {
            Long id = 999L;
            DeliveryRecord update = new DeliveryRecord();
            update.setPatientName("张三");

            when(deliveryRecordMapper.selectById(id)).thenReturn(null);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.update(id, update));
            assertEquals("交付记录不存在: 999", ex.getMessage());
            verify(deliveryRecordMapper, never()).updateById((DeliveryRecord) any());
        }
    }

    @Nested
    @DisplayName("业务规则: 删除交付记录")
    class DeleteTests {

        @Test
        @DisplayName("正常场景: 待交付记录可删除（逻辑删除）")
        void delete_pendingRecord_shouldSucceed() {
            Long id = 1L;
            DeliveryRecord record = new DeliveryRecord();
            record.setId(id);
            record.setStatus("PENDING");

            when(deliveryRecordMapper.selectById(id)).thenReturn(record);
            doReturn(1).when(deliveryRecordMapper).deleteById(eq(id));

            service.delete(id);

            verify(deliveryRecordMapper).deleteById(eq(id));
        }

        @Test
        @DisplayName("规则冲突: 已交付记录不允许删除")
        void delete_deliveredRecord_shouldThrow() {
            Long id = 1L;
            DeliveryRecord record = new DeliveryRecord();
            record.setId(id);
            record.setStatus("DELIVERED");

            when(deliveryRecordMapper.selectById(id)).thenReturn(record);

            IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service.delete(id));
            assertEquals("已交付记录不允许删除", ex.getMessage());
            verify(deliveryRecordMapper, never()).deleteById((DeliveryRecord) any());
        }

        @Test
        @DisplayName("规则冲突: 记录不存在时应拦截")
        void delete_whenRecordNotExists_shouldThrow() {
            Long id = 999L;
            when(deliveryRecordMapper.selectById(id)).thenReturn(null);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.delete(id));
            assertEquals("交付记录不存在: 999", ex.getMessage());
        }
    }

    @Nested
    @DisplayName("业务规则: 确认交付")
    class ConfirmDeliveryTests {

        @Test
        @DisplayName("正常场景: 待交付记录确认后状态变为已交付")
        void confirmDelivery_pendingRecord_shouldSucceed() {
            Long id = 1L;
            DeliveryRecord record = new DeliveryRecord();
            record.setId(id);
            record.setStatus("PENDING");
            record.setPrescriptionNo("P001");

            ConfirmDeliveryRequest request = new ConfirmDeliveryRequest();
            request.setOperatorId(100L);
            request.setReceiverName("李四");
            request.setReceiverPhone("13900139001");
            request.setRemark("已签收");

            when(deliveryRecordMapper.selectById(id)).thenReturn(record);
            when(deliveryRecordMapper.updateById((DeliveryRecord) any())).thenReturn(1);
            when(deliveryRecordMapper.selectById(id)).thenReturn(record).thenReturn(record);

            DeliveryRecord result = service.confirmDelivery(id, request);

            assertEquals("DELIVERED", result.getStatus());
            assertEquals("100", result.getOperatorId());
            assertEquals("李四", result.getReceiverName());
            assertEquals("13900139001", result.getReceiverPhone());
            assertEquals("已签收", result.getRemark());
            assertNotNull(result.getDeliveredAt());
        }

        @Test
        @DisplayName("规则冲突: 已交付记录重复确认应拦截")
        void confirmDelivery_alreadyDelivered_shouldThrow() {
            Long id = 1L;
            DeliveryRecord record = new DeliveryRecord();
            record.setId(id);
            record.setStatus("DELIVERED");

            ConfirmDeliveryRequest request = new ConfirmDeliveryRequest();
            request.setOperatorId(100L);
            request.setReceiverName("李四");

            when(deliveryRecordMapper.selectById(id)).thenReturn(record);

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> service.confirmDelivery(id, request));
            assertEquals("该记录已交付，请勿重复确认", ex.getMessage());
            verify(deliveryRecordMapper, never()).updateById((DeliveryRecord) any());
        }

        @Test
        @DisplayName("规则冲突: 记录不存在时应拦截")
        void confirmDelivery_whenRecordNotExists_shouldThrow() {
            Long id = 999L;
            ConfirmDeliveryRequest request = new ConfirmDeliveryRequest();
            request.setOperatorId(100L);
            request.setReceiverName("李四");

            when(deliveryRecordMapper.selectById(id)).thenReturn(null);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.confirmDelivery(id, request));
            assertEquals("交付记录不存在: 999", ex.getMessage());
        }
    }
}
