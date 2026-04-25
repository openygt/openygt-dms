package cn.org.openygt.print.service;

import cn.org.openygt.common.dto.PrintTaskDTO;
import cn.org.openygt.common.enums.PrintTaskStatus;
import cn.org.openygt.common.service.EquipmentService;
import cn.org.openygt.common.dto.EqDeviceDTO;
import cn.org.openygt.print.entity.PrintRecord;
import cn.org.openygt.print.entity.PrintTask;
import cn.org.openygt.print.mapper.PrintRecordMapper;
import cn.org.openygt.print.mapper.PrintTaskMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrintServiceImplTest {

    @Mock
    private PrintTaskMapper printTaskMapper;

    @Mock
    private PrintRecordMapper printRecordMapper;

    @Mock
    private EquipmentService equipmentService;

    private PrintServiceImpl printService;

    @Captor
    private ArgumentCaptor<PrintTask> printTaskCaptor;

    @Captor
    private ArgumentCaptor<PrintRecord> printRecordCaptor;

    @BeforeEach
    void setUp() {
        printService = new PrintServiceImpl(printTaskMapper, printRecordMapper, equipmentService);
    }

    // ==================== submitPrintTask ====================

    @Test
    @DisplayName("提交打印任务：无进行中任务时，创建新任务并执行")
    void submitPrintTask_shouldCreateAndExecute_whenNoActiveTask() {
        // 模拟：无进行中的打印任务
        when(printTaskMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        // 模拟：打印机可用
        EqDeviceDTO device = new EqDeviceDTO();
        device.setId(10L);
        when(equipmentService.getOrCreateDevice("PRINTER_01", 4)).thenReturn(device);
        when(equipmentService.getDeviceStatus(10L)).thenReturn("IDLE");
        // 模拟：insert 成功后设置 id
        when(printTaskMapper.insert(any(PrintTask.class))).thenAnswer(invocation -> {
            PrintTask pt = invocation.getArgument(0);
            pt.setId(1L);
            return 1;
        });

        printService.submitPrintTask(100L, "PRINTER_01", "OP_001");

        // 验证：PrintTask 已创建（initial status = PENDING, retryCount = 0, maxRetry = 3）
        verify(printTaskMapper).insert(printTaskCaptor.capture());
        PrintTask created = printTaskCaptor.getValue();
        assertThat(created.getTaskId()).isEqualTo(100L);
        assertThat(created.getDeviceCode()).isEqualTo("PRINTER_01");
        assertThat(created.getOperatorId()).isEqualTo("OP_001");
        assertThat(created.getRetryCount()).isEqualTo(0);
        assertThat(created.getMaxRetry()).isEqualTo(3);

        // 验证：执行了打印流程（占用打印机 → 完成 → 释放 → 记录）
        verify(equipmentService).updateDeviceStatus(10L, "RUNNING");
        verify(equipmentService).releaseDevice(10L);
        verify(printRecordMapper).insert(any(PrintRecord.class));
    }

    @Test
    @DisplayName("提交打印任务：已有进行中任务时，幂等返回不重复创建")
    void submitPrintTask_shouldReturn_whenActiveTaskExists() {
        // 模拟：存在进行中的打印任务
        PrintTask existing = new PrintTask();
        existing.setId(1L);
        existing.setTaskId(100L);
        existing.setStatus(PrintTaskStatus.PENDING.name());
        when(printTaskMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

        printService.submitPrintTask(100L, "PRINTER_01", "OP_001");

        // 验证：没有创建新记录
        verify(printTaskMapper, never()).insert(any(PrintTask.class));
        verify(printRecordMapper, never()).insert(any(PrintRecord.class));
        // 验证：没有执行打印相关操作
        verify(equipmentService, never()).updateDeviceStatus(anyLong(), anyString());
        verify(equipmentService, never()).releaseDevice(anyLong());
    }

    @Test
    @DisplayName("提交打印任务：打印机非空闲时抛异常")
    void submitPrintTask_shouldThrow_whenPrinterNotIdle() {
        when(printTaskMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        EqDeviceDTO device = new EqDeviceDTO();
        device.setId(10L);
        when(equipmentService.getOrCreateDevice("PRINTER_01", 4)).thenReturn(device);
        when(equipmentService.getDeviceStatus(10L)).thenReturn("RUNNING");

        assertThatThrownBy(() -> printService.submitPrintTask(100L, "PRINTER_01", "OP_001"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("不是空闲状态");
    }

    @Test
    @DisplayName("提交打印任务：打印机不存在时抛异常")
    void submitPrintTask_shouldThrow_whenPrinterNotFound() {
        when(printTaskMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(equipmentService.getOrCreateDevice("PRINTER_01", 4)).thenReturn(null);

        assertThatThrownBy(() -> printService.submitPrintTask(100L, "PRINTER_01", "OP_001"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("不存在");
    }

    // ==================== retryPrint ====================

    @Test
    @DisplayName("重试打印：未超过 maxRetry 时，递增重试次数并执行")
    void retryPrint_shouldSucceed_whenUnderMaxRetry() {
        // 模拟：存在打印任务，retryCount=1, maxRetry=3
        PrintTask task = new PrintTask();
        task.setId(1L);
        task.setTaskId(100L);
        task.setStatus(PrintTaskStatus.FAILED.name());
        task.setRetryCount(1);
        task.setMaxRetry(3);
        when(printTaskMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(task);
        // 模拟：打印机可用
        EqDeviceDTO device = new EqDeviceDTO();
        device.setId(10L);
        when(equipmentService.getOrCreateDevice("PRINTER_02", 4)).thenReturn(device);
        when(equipmentService.getDeviceStatus(10L)).thenReturn("IDLE");

        printService.retryPrint(100L, "PRINTER_02", "OP_002");

        // 验证：重试计数 +1，执行了打印（最终状态为 COMPLETED）
        assertThat(task.getRetryCount()).isEqualTo(2);
        verify(printTaskMapper, times(2)).updateById(task);
        // 验证：执行了打印
        verify(equipmentService).updateDeviceStatus(10L, "RUNNING");
        verify(equipmentService).releaseDevice(10L);
        verify(printRecordMapper).insert(any(PrintRecord.class));
    }

    @Test
    @DisplayName("重试打印：无打印记录时抛异常")
    void retryPrint_shouldThrow_whenNoPrintTask() {
        when(printTaskMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        assertThatThrownBy(() -> printService.retryPrint(100L, "PRINTER_01", "OP_001"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("无打印记录");
    }

    @Test
    @DisplayName("重试打印：超过 maxRetry 时状态固定为 FAILED 并抛异常")
    void retryPrint_shouldThrowAndSetFailed_whenMaxRetryReached() {
        // 模拟：retryCount=3, maxRetry=3
        PrintTask task = new PrintTask();
        task.setId(1L);
        task.setTaskId(100L);
        task.setStatus(PrintTaskStatus.FAILED.name());
        task.setRetryCount(3);
        task.setMaxRetry(3);
        when(printTaskMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(task);

        assertThatThrownBy(() -> printService.retryPrint(100L, "PRINTER_01", "OP_001"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("已达上限");

        // 验证：状态保持 FAILED
        assertThat(task.getStatus()).isEqualTo(PrintTaskStatus.FAILED.name());
        verify(printTaskMapper).updateById(task);
        // 验证：写入失败记录
        verify(printRecordMapper).insert(printRecordCaptor.capture());
        PrintRecord failedRecord = printRecordCaptor.getValue();
        assertThat(failedRecord.getResult()).isEqualTo("FAILED");
        assertThat(failedRecord.getErrorMessage()).contains("已达上限");
        // 验证：未执行打印
        verify(equipmentService, never()).updateDeviceStatus(anyLong(), anyString());
    }

    // ==================== getPrintQueue ====================

    @Test
    @DisplayName("获取打印队列：返回 DTO 列表")
    void getPrintQueue_shouldReturnDtoList() {
        PrintTask t1 = new PrintTask();
        t1.setId(1L);
        t1.setTaskId(100L);
        t1.setStatus(PrintTaskStatus.PENDING.name());
        t1.setRetryCount(2);
        t1.setMaxRetry(3);

        PrintTask t2 = new PrintTask();
        t2.setId(2L);
        t2.setTaskId(200L);
        t2.setStatus(PrintTaskStatus.FAILED.name());
        t2.setRetryCount(3);
        t2.setMaxRetry(3);

        when(printTaskMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Arrays.asList(t1, t2));

        List<PrintTaskDTO> result = printService.getPrintQueue();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(0).getStatus()).isEqualTo(PrintTaskStatus.PENDING.name());
        assertThat(result.get(0).getRetryCount()).isEqualTo(2);
        assertThat(result.get(0).getMaxRetry()).isEqualTo(3);
        assertThat(result.get(1).getId()).isEqualTo(2L);
        assertThat(result.get(1).getStatus()).isEqualTo(PrintTaskStatus.FAILED.name());
    }

    @Test
    @DisplayName("获取打印队列：空队列返回空列表")
    void getPrintQueue_shouldReturnEmptyList_whenNoTasks() {
        when(printTaskMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        List<PrintTaskDTO> result = printService.getPrintQueue();

        assertThat(result).isEmpty();
    }

    // ==================== getPrintStatus ====================

    @Test
    @DisplayName("获取打印状态：任务存在时返回状态")
    void getPrintStatus_shouldReturnStatus_whenTaskExists() {
        PrintTask task = new PrintTask();
        task.setId(1L);
        task.setTaskId(100L);
        task.setStatus(PrintTaskStatus.COMPLETED.name());
        when(printTaskMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(task);

        String status = printService.getPrintStatus(100L);

        assertThat(status).isEqualTo(PrintTaskStatus.COMPLETED.name());
    }

    @Test
    @DisplayName("获取打印状态：任务不存在时返回 null")
    void getPrintStatus_shouldReturnNull_whenTaskNotFound() {
        when(printTaskMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        String status = printService.getPrintStatus(999L);

        assertThat(status).isNull();
    }
}
