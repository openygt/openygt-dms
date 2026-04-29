package cn.org.openygt.inventory.service.impl;

import cn.org.openygt.inventory.dto.ConsumeItemRequest;
import cn.org.openygt.inventory.dto.ConsumeRecordDTO;
import cn.org.openygt.inventory.dto.ConsumeRecordRequest;
import cn.org.openygt.inventory.entity.InvStockLog;
import cn.org.openygt.inventory.mapper.InvStockLogMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConsumeRecordServiceImplTest {

    @Mock
    private InvStockLogMapper invStockLogMapper;

    @InjectMocks
    private ConsumeRecordServiceImpl consumeRecordService;

    @Test
    @DisplayName("recordConsume: 请求为 null 时返回空列表")
    void testRecordConsumeNullRequest() {
        assertTrue(consumeRecordService.recordConsume(null).isEmpty());
        verify(invStockLogMapper, never()).insert((InvStockLog) any());
    }

    @Test
    @DisplayName("recordConsume: items 为 null 时返回空列表")
    void testRecordConsumeNullItems() {
        ConsumeRecordRequest request = new ConsumeRecordRequest();
        request.setTaskId(1L);
        request.setItems(null);
        assertTrue(consumeRecordService.recordConsume(request).isEmpty());
        verify(invStockLogMapper, never()).insert((InvStockLog) any());
    }

    @Test
    @DisplayName("recordConsume: items 为空列表时返回空列表")
    void testRecordConsumeEmptyItems() {
        ConsumeRecordRequest request = new ConsumeRecordRequest();
        request.setTaskId(1L);
        request.setItems(Collections.emptyList());
        assertTrue(consumeRecordService.recordConsume(request).isEmpty());
        verify(invStockLogMapper, never()).insert((InvStockLog) any());
    }

    @Test
    @DisplayName("recordConsume: 正常明细写入流水")
    void testRecordConsumeSuccess() {
        ConsumeItemRequest item1 = new ConsumeItemRequest();
        item1.setMedicineId(10L);
        item1.setMedicineName("黄芪");
        item1.setQuantity(BigDecimal.valueOf(15));
        item1.setUnit("g");
        ConsumeItemRequest item2 = new ConsumeItemRequest();
        item2.setMedicineName("当归");
        item2.setQuantity(BigDecimal.valueOf(10));
        ConsumeRecordRequest request = new ConsumeRecordRequest();
        request.setTaskId(1L);
        request.setOperatorId("OP01");
        request.setItems(Arrays.asList(item1, item2));
        when(invStockLogMapper.insert((InvStockLog) any())).thenAnswer(inv -> {
            InvStockLog log = inv.getArgument(0);
            log.setId(System.nanoTime());
            return 1;
        });
        List<Long> ids = consumeRecordService.recordConsume(request);
        assertEquals(2, ids.size());
        verify(invStockLogMapper, times(2)).insert((InvStockLog) any());
        ArgumentCaptor<InvStockLog> captor = ArgumentCaptor.forClass(InvStockLog.class);
        verify(invStockLogMapper, times(2)).insert(captor.capture());
        List<InvStockLog> logs = captor.getAllValues();
        assertEquals("CONSUME", logs.get(0).getChangeType());
        assertEquals(Long.valueOf(1L), logs.get(0).getTaskId());
        assertEquals("黄芪", logs.get(0).getMedicineName());
        assertEquals(0, BigDecimal.valueOf(15).compareTo(logs.get(0).getChangeQuantity()));
        assertNull(logs.get(0).getBeforeQuantity());
    }

    @Test
    @DisplayName("recordConsume: 单条明细异常不影响其他明细写入")
    void testRecordConsumePartialFailure() {
        ConsumeItemRequest item1 = new ConsumeItemRequest();
        item1.setMedicineName("黄芪");
        item1.setQuantity(BigDecimal.valueOf(15));
        ConsumeItemRequest item2 = new ConsumeItemRequest();
        item2.setMedicineName("当归");
        item2.setQuantity(BigDecimal.valueOf(10));
        ConsumeRecordRequest request = new ConsumeRecordRequest();
        request.setTaskId(1L);
        request.setItems(Arrays.asList(item1, item2));
        when(invStockLogMapper.insert((InvStockLog) any()))
                .thenReturn(1)
                .thenThrow(new RuntimeException("模拟DB异常"));
        List<Long> ids = consumeRecordService.recordConsume(request);
        assertEquals(1, ids.size());
        verify(invStockLogMapper, times(2)).insert((InvStockLog) any());
    }

    @Test
    @DisplayName("recordConsume: 全部明细异常返回空列表")
    void testRecordConsumeAllFailure() {
        ConsumeItemRequest item = new ConsumeItemRequest();
        item.setMedicineName("黄芪");
        item.setQuantity(BigDecimal.valueOf(15));
        ConsumeRecordRequest request = new ConsumeRecordRequest();
        request.setTaskId(1L);
        request.setItems(Collections.singletonList(item));
        when(invStockLogMapper.insert((InvStockLog) any())).thenThrow(new RuntimeException("模拟DB异常"));
        List<Long> ids = consumeRecordService.recordConsume(request);
        assertTrue(ids.isEmpty());
    }

    @Test
    @DisplayName("recordConsume: 重复提交幂等——每次均生成新流水")
    void testRecordConsumeIdempotent() {
        ConsumeItemRequest item = new ConsumeItemRequest();
        item.setMedicineName("黄芪");
        item.setQuantity(BigDecimal.valueOf(15));
        ConsumeRecordRequest request = new ConsumeRecordRequest();
        request.setTaskId(1L);
        request.setItems(Collections.singletonList(item));
        when(invStockLogMapper.insert((InvStockLog) any())).thenAnswer(inv -> {
            InvStockLog log = inv.getArgument(0);
            log.setId(System.nanoTime());
            return 1;
        });
        List<Long> ids1 = consumeRecordService.recordConsume(request);
        List<Long> ids2 = consumeRecordService.recordConsume(request);
        assertEquals(1, ids1.size());
        assertEquals(1, ids2.size());
        assertNotEquals(ids1.get(0), ids2.get(0));
        verify(invStockLogMapper, times(2)).insert((InvStockLog) any());
    }

    @Test
    @DisplayName("listByTaskId: 按任务ID查询流水")
    void testListByTaskId() {
        consumeRecordService.listByTaskId(1L);
        verify(invStockLogMapper).selectList(any());
    }

    @Test
    @DisplayName("pageQuery: 分页查询流水")
    void testPageQuery() {
        Page<InvStockLog> pageResult = new Page<>();
        pageResult.setRecords(Collections.emptyList());
        when(invStockLogMapper.selectPage(any(Page.class), any())).thenReturn(pageResult);
        IPage<ConsumeRecordDTO> result = consumeRecordService.pageQuery(null, null, null, null, null, null, 1, 10);
        assertNotNull(result);
        verify(invStockLogMapper).selectPage(any(Page.class), any());
    }
}
