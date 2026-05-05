package cn.org.openygt.production.service.impl;

import cn.org.openygt.common.dto.EqDeviceDTO;
import cn.org.openygt.common.enums.InspectionResultType;
import cn.org.openygt.common.service.EquipmentService;
import cn.org.openygt.common.service.PrintService;
import cn.org.openygt.common.dto.InspectionResult;
import cn.org.openygt.common.service.QualityService;
import cn.org.openygt.common.service.RetainSampleFacade;
import cn.org.openygt.inventory.dto.ConsumeRecordRequest;
import cn.org.openygt.inventory.service.ConsumeRecordService;
import cn.org.openygt.production.entity.*;
import cn.org.openygt.production.mapper.*;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TaskServiceImpl 状态机核心单元测试。
 *
 * <p>⚠️ 注意：架构师完成 Task.java 实体重构和 TaskServiceImpl 状态机方法改造前，
 * 部分测试可能因字段/方法签名变更而编译失败。
 * 架构师提交后需立即填充所有骨架方法的断言逻辑并修复编译错误。
 */
@ExtendWith(MockitoExtension.class)
class TaskServiceImplTest {

    @Mock
    private TaskMapper taskMapper;
    @Mock
    private TaskStatusHistoryMapper historyMapper;
    @Mock
    private WorkRecordMapper workRecordMapper;
    @Mock
    private StepLogMapper stepLogMapper;
    @Mock
    private HandoverDetailMapper handoverDetailMapper;
    @Mock
    private PrescriptionMedicineMapper prescriptionMedicineMapper;
    @Mock
    private PrescriptionMapper prescriptionMapper;
    @Mock
    private EquipmentService equipmentService;
    @Mock
    private PrintService printService;
    @Mock
    private ConsumeRecordService consumeRecordService;
    @Mock
    private cn.org.openygt.production.mapper.HrEmployeeMapper hrEmployeeMapper;
    @Mock
    private RetainSampleFacade retainSampleFacade;
    @Mock
    private QualityService qualityService;

    private TaskServiceImpl taskService;

    @BeforeEach
    void setUp() {
        lenient().when(qualityService.inspect(anyLong(), any(), any(), any(), any()))
                .thenReturn(new InspectionResult());
        lenient().doNothing().when(retainSampleFacade).createAfterPass(anyLong(), anyLong(), any());
        taskService = new TaskServiceImpl(
                taskMapper, historyMapper, workRecordMapper, stepLogMapper,
                handoverDetailMapper, prescriptionMedicineMapper, prescriptionMapper,
                equipmentService, printService, consumeRecordService, hrEmployeeMapper,
                retainSampleFacade, qualityService
        );
    }

    private Task mockTask(Long id, String status) {
        Task t = new Task();
        t.setId(id);
        t.setStatus(status);
        return t;
    }

    // ========================================================================
    //  已实现的测试（架构师重构后需验证字段引用正确性）
    // ========================================================================

    // ==================== startDecoct ====================

    @Test
    @DisplayName("startDecoct: 正常开始煎药，状态流转 + 设备绑定 + 记录创建")
    void testStartDecoctSuccess() {
        Task task = mockTask(1L, "待煎药");
        when(taskMapper.selectByIdForUpdate(1L)).thenReturn(task);
        when(equipmentService.getDeviceId("D001")).thenReturn(10L);
        when(equipmentService.getDeviceStatus(10L)).thenReturn("idle");

        Task result = taskService.startDecoct(1L, "D001", "OP01");

        assertEquals("煎药中", result.getStatus());
        assertEquals(Long.valueOf(10L), result.getDecoctDeviceId());
        verify(equipmentService).updateDeviceStatus(10L, "running");
        verify(taskMapper).updateById(task);
        verify(stepLogMapper).insert(any(StepLog.class));
        verify(workRecordMapper).insert(any(WorkRecord.class));
        verify(historyMapper).insert(any(TaskStatusHistory.class));
    }

    @Test
    @DisplayName("startDecoct: 设备非空闲抛 IllegalStateException")
    void testStartDecoctDeviceNotIdle() {
        Task task = mockTask(1L, "待煎药");
        when(taskMapper.selectByIdForUpdate(1L)).thenReturn(task);
        when(equipmentService.getDeviceId("D001")).thenReturn(10L);
        when(equipmentService.getDeviceStatus(10L)).thenReturn("running");

        assertThrows(IllegalStateException.class, () ->
                taskService.startDecoct(1L, "D001", "OP01"));
    }

    @Test
    @DisplayName("startDecoct: 状态不正确抛 IllegalStateException")
    void testStartDecoctWrongStatus() {
        Task task = mockTask(1L, "待泡药");
        when(taskMapper.selectByIdForUpdate(1L)).thenReturn(task);

        assertThrows(IllegalStateException.class, () ->
                taskService.startDecoct(1L, "D001", "OP01"));
    }

    // ==================== endDecoct ====================

    @Test
    @DisplayName("endDecoct: 释放设备前先读取 autoLevel（时序验证）")
    void testEndDecoctReadsAutoLevelBeforeRelease() {
        Task task = mockTask(1L, "煎药中");
        task.setDecoctDeviceId(10L);
        when(taskMapper.selectById(1L)).thenReturn(task);
        when(equipmentService.getAutoLevel(10L)).thenReturn("manual");

        taskService.endDecoct(1L, "OP01");

        // 验证时序：先读 autoLevel，再 releaseDevice
        org.mockito.InOrder inOrder = inOrder(equipmentService);
        inOrder.verify(equipmentService).getAutoLevel(10L);
        inOrder.verify(equipmentService).releaseDevice(10L);
    }

    @Test
    @DisplayName("endDecoct: auto 级别自动触发 startPour")
    void testEndDecoctAutoTriggersPour() {
        Task task = mockTask(1L, "煎药中");
        task.setDecoctDeviceId(10L);
        Task pourTask = mockTask(1L, "待出液");
        when(taskMapper.selectById(1L)).thenReturn(task, pourTask);
        when(equipmentService.getAutoLevel(10L)).thenReturn("auto");

        Task result = taskService.endDecoct(1L, "OP01");

        assertEquals("出液中", result.getStatus());
        verify(equipmentService).getAutoLevel(10L);
        verify(equipmentService).releaseDevice(10L);
    }

    // ==================== qualityInspect ====================

    @Test
    @DisplayName("qualityInspect: REWORK 返工——预留设备 + 状态回退待煎药")
    void testQualityInspectReworkReservesDevice() {
        Task task = mockTask(1L, "待质检");
        task.setDecoctDeviceId(10L);
        task.setPackageDeviceId(20L);
        when(taskMapper.selectById(1L)).thenReturn(task);

        taskService.qualityInspect(1L, InspectionResultType.REWORK, "OP01", "温度不足", null);

        verify(equipmentService).reserveDevice(1L, 10L);
        verify(equipmentService).reserveDevice(1L, 20L);
        assertEquals("待煎药", task.getStatus());
        verify(taskMapper).updateById(task);
    }

    @Test
    @DisplayName("qualityInspect: PASS 通过——状态流转至待交接")
    void testQualityInspectPass() {
        Task task = mockTask(1L, "待质检");
        when(taskMapper.selectById(1L)).thenReturn(task);

        taskService.qualityInspect(1L, InspectionResultType.PASS, "OP01", null, null);

        assertEquals("待交接", task.getStatus());
        verify(equipmentService, never()).reserveDevice(any(), any());
    }

    @Test
    @DisplayName("qualityInspect: SCRAP 报废——状态流转 + 异常标记")
    void testQualityInspectScrap() {
        Task task = mockTask(1L, "待质检");
        when(taskMapper.selectById(1L)).thenReturn(task);

        taskService.qualityInspect(1L, InspectionResultType.SCRAP, "OP01", "污染", null);

        assertEquals("已报废", task.getStatus());
        assertEquals(Integer.valueOf(1), task.getIsException());
        assertEquals("污染", task.getExceptionReason());
    }

    // ==================== bindDevice ====================

    @Test
    @DisplayName("bindDevice: 正常绑定设备，状态推进")
    void testBindDeviceSuccess() {
        Task task = mockTask(1L, "待泡药");
        when(taskMapper.selectByIdForUpdate(1L)).thenReturn(task);
        EqDeviceDTO lockedDevice = new EqDeviceDTO();
        lockedDevice.setId(10L);
        lockedDevice.setStatus("idle");
        when(equipmentService.lockDeviceByCode("D001")).thenReturn(lockedDevice);

        Task result = taskService.bindDevice(1L, "D001");

        assertEquals(Long.valueOf(10L), result.getDecoctDeviceId());
        assertEquals("待煎药", result.getStatus());
        verify(equipmentService).updateDeviceStatus(10L, "running");
    }

    @Test
    @DisplayName("bindDevice: 设备已被占用抛 IllegalStateException")
    void testBindDeviceAlreadyRunning() {
        Task task = mockTask(1L, "待泡药");
        when(taskMapper.selectByIdForUpdate(1L)).thenReturn(task);
        EqDeviceDTO lockedDevice = new EqDeviceDTO();
        lockedDevice.setId(10L);
        lockedDevice.setStatus("running");
        when(equipmentService.lockDeviceByCode("D001")).thenReturn(lockedDevice);

        assertThrows(IllegalStateException.class, () ->
                taskService.bindDevice(1L, "D001"));
    }

    // ==================== printLabel ====================

    @Test
    @DisplayName("printLabel: 委托打印服务 + 回填打印状态")
    void testPrintLabelDelegatesToPrintService() {
        Task task = mockTask(1L, "待贴标");
        when(taskMapper.selectById(1L)).thenReturn(task);
        when(printService.getPrintStatus(1L)).thenReturn("PRINTED");
        when(equipmentService.getDeviceId("P001")).thenReturn(30L);

        Task result = taskService.printLabel(1L, "P001", "OP01");

        verify(printService).submitPrintTask(1L, "P001", "OP01");
        verify(printService).getPrintStatus(1L);
        assertEquals("PRINTED", result.getPrintStatus());
        assertEquals(Long.valueOf(30L), result.getPrintDeviceId());
    }

    // ==================== forceStatus ====================

    @Test
    @DisplayName("forceStatus: 强制推进到煎药中——绑定设备 + 状态覆盖")
    void testForceStatusToDecoct() {
        Task task = mockTask(1L, "待煎药");
        when(taskMapper.selectById(1L)).thenReturn(task);
        EqDeviceDTO dto = new EqDeviceDTO();
        dto.setId(10L);
        when(equipmentService.getOrCreateDevice("D001", 1)).thenReturn(dto);

        Task result = taskService.forceStatus(1L, "煎药中", "OP01", "D001", "强制开始");

        assertEquals("煎药中", result.getStatus());
        assertEquals(Long.valueOf(10L), result.getDecoctDeviceId());
        verify(equipmentService).updateDeviceStatus(10L, "running");
    }

    // ========================================================================
    //  填充的测试方法（适配架构师重构后的实体和状态机）
    // ========================================================================

    // ==================== startSoak ====================

    @Test
    @DisplayName("startSoak: 待泡药 → 泡药中，记录开始时间、工时记录、工序日志")
    void testStartSoakSuccess() {
        Task task = mockTask(1L, "待泡药");
        when(taskMapper.selectById(1L)).thenReturn(task);

        Task result = taskService.startSoak(1L, "OP01");

        assertEquals("泡药中", result.getStatus());
        assertNotNull(result.getSoakStartTime());
        verify(taskMapper).updateById(task);
        verify(workRecordMapper).insert(argThat((WorkRecord r) -> "SOAK".equals(r.getAction())));
        verify(stepLogMapper).insert(argThat((StepLog s) -> "SOAK".equals(s.getStepType())));
        verify(historyMapper).insert(any(TaskStatusHistory.class));
    }

    @Test
    @DisplayName("startSoak: 状态非待泡药抛 IllegalStateException")
    void testStartSoakWrongStatus() {
        Task task = mockTask(1L, "煎药中");
        when(taskMapper.selectById(1L)).thenReturn(task);

        assertThrows(IllegalStateException.class, () -> taskService.startSoak(1L, "OP01"));
    }

    // ==================== endSoak ====================

    @Test
    @DisplayName("endSoak: 泡药中 → 待煎药，计算耗时，关闭工序日志")
    void testEndSoakSuccess() {
        Task task = mockTask(1L, "泡药中");
        task.setSoakStartTime(LocalDateTime.now().minusMinutes(30));
        when(taskMapper.selectById(1L)).thenReturn(task);

        Task result = taskService.endSoak(1L, "OP01");

        assertEquals("待煎药", result.getStatus());
        assertEquals(30, result.getCurrentStageDuration());
        verify(workRecordMapper).insert(argThat((WorkRecord r) -> "SOAK".equals(r.getAction()) && r.getWorkTime() == 30));
        verify(historyMapper).insert(any(TaskStatusHistory.class));
    }

    @Test
    @DisplayName("endSoak: 状态非泡药中抛 IllegalStateException")
    void testEndSoakWrongStatus() {
        Task task = mockTask(1L, "待泡药");
        when(taskMapper.selectById(1L)).thenReturn(task);

        assertThrows(IllegalStateException.class, () -> taskService.endSoak(1L, "OP01"));
    }

    // ==================== startPour ====================

    @Test
    @DisplayName("startPour: 待出液 → 出液中，记录开始时间和工序日志")
    void testStartPourSuccess() {
        Task task = mockTask(1L, "待出液");
        when(taskMapper.selectById(1L)).thenReturn(task);

        Task result = taskService.startPour(1L, "OP01");

        assertEquals("出液中", result.getStatus());
        assertEquals(Integer.valueOf(0), result.getCurrentStageDuration());
        verify(stepLogMapper).insert(argThat((StepLog s) -> "POUR".equals(s.getStepType())));
        verify(historyMapper).insert(any(TaskStatusHistory.class));
    }

    @Test
    @DisplayName("startPour: 状态非待出液抛 IllegalStateException")
    void testStartPourWrongStatus() {
        Task task = mockTask(1L, "泡药中");
        when(taskMapper.selectById(1L)).thenReturn(task);

        assertThrows(IllegalStateException.class, () -> taskService.startPour(1L, "OP01"));
    }

    // ==================== endPour ====================

    @Test
    @DisplayName("endPour: 出液中 → 待包装，计算耗时，关闭工序日志")
    void testEndPourSuccess() {
        Task task = mockTask(1L, "出液中");
        when(taskMapper.selectById(1L)).thenReturn(task);

        Task result = taskService.endPour(1L, "OP01");

        assertEquals("待包装", result.getStatus());
        verify(historyMapper).insert(any(TaskStatusHistory.class));
    }

    @Test
    @DisplayName("endPour: auto 级别自动触发 startWrap（绑定包装机）")
    void testEndPourAutoTriggersWrap() {
        Task task = mockTask(1L, "出液中");
        task.setDecoctDeviceId(10L);
        when(taskMapper.selectById(1L)).thenReturn(task);
        when(equipmentService.getAutoLevel(10L)).thenReturn("auto");
        when(equipmentService.getDeviceCode(10L)).thenReturn("D001");
        // startWrap 需要 selectByIdForUpdate + 设备绑定
        Task wrapReady = mockTask(1L, "待包装");
        when(taskMapper.selectByIdForUpdate(1L)).thenReturn(wrapReady);
        EqDeviceDTO lockedDevice = new EqDeviceDTO();
        lockedDevice.setId(20L);
        lockedDevice.setStatus("idle");
        when(equipmentService.lockDeviceByCode("D001")).thenReturn(lockedDevice);

        Task result = taskService.endPour(1L, "OP01");

        assertEquals("包装中", result.getStatus());
        assertEquals(Long.valueOf(20L), result.getPackageDeviceId());
        verify(equipmentService).updateDeviceStatus(20L, "running");
    }

    @Test
    @DisplayName("endPour: 状态非出液中抛 IllegalStateException")
    void testEndPourWrongStatus() {
        Task task = mockTask(1L, "出液");
        when(taskMapper.selectById(1L)).thenReturn(task);

        assertThrows(IllegalStateException.class, () -> taskService.endPour(1L, "OP01"));
    }

    // ==================== startWrap ====================

    @Test
    @DisplayName("startWrap: 待包装 → 包装中，绑定包装机 + 记录创建")
    void testStartWrapSuccess() {
        Task task = mockTask(1L, "待包装");
        when(taskMapper.selectByIdForUpdate(1L)).thenReturn(task);
        EqDeviceDTO lockedDevice = new EqDeviceDTO();
        lockedDevice.setId(20L);
        lockedDevice.setStatus("idle");
        when(equipmentService.lockDeviceByCode("W001")).thenReturn(lockedDevice);

        Task result = taskService.startWrap(1L, "W001", "OP01");

        assertEquals("包装中", result.getStatus());
        assertEquals(Long.valueOf(20L), result.getPackageDeviceId());
        verify(equipmentService).updateDeviceStatus(20L, "running");
        verify(workRecordMapper).insert(argThat((WorkRecord r) -> "WRAP".equals(r.getAction())));
        verify(stepLogMapper).insert(argThat((StepLog s) -> "WRAP".equals(s.getStepType())));
        verify(historyMapper).insert(any(TaskStatusHistory.class));
    }

    @Test
    @DisplayName("startWrap: 包装机非空闲抛 IllegalStateException")
    void testStartWrapDeviceNotIdle() {
        Task task = mockTask(1L, "待包装");
        when(taskMapper.selectByIdForUpdate(1L)).thenReturn(task);
        EqDeviceDTO lockedDevice = new EqDeviceDTO();
        lockedDevice.setId(20L);
        lockedDevice.setStatus("running");
        when(equipmentService.lockDeviceByCode("W001")).thenReturn(lockedDevice);

        assertThrows(IllegalStateException.class, () -> taskService.startWrap(1L, "W001", "OP01"));
    }

    @Test
    @DisplayName("startWrap: 状态非待包装抛 IllegalStateException")
    void testStartWrapWrongStatus() {
        Task task = mockTask(1L, "出液中");
        when(taskMapper.selectByIdForUpdate(1L)).thenReturn(task);

        assertThrows(IllegalStateException.class, () -> taskService.startWrap(1L, "W001", "OP01"));
    }

    // ==================== endWrap ====================

    @Test
    @DisplayName("endWrap: 包装中 → 待贴标，释放包装机 + 记录工时")
    void testEndWrapSuccess() {
        Task task = mockTask(1L, "包装中");
        task.setPackageDeviceId(20L);
        when(taskMapper.selectById(1L)).thenReturn(task);

        Task result = taskService.endWrap(1L, "OP01");

        assertEquals("待贴标", result.getStatus());
        verify(equipmentService).releaseDevice(20L);
        verify(workRecordMapper).insert(argThat((WorkRecord r) -> "WRAP".equals(r.getAction())));
        verify(historyMapper).insert(any(TaskStatusHistory.class));
    }

    @Test
    @DisplayName("endWrap: 状态非包装中抛 IllegalStateException")
    void testEndWrapWrongStatus() {
        Task task = mockTask(1L, "待包装");
        when(taskMapper.selectById(1L)).thenReturn(task);

        assertThrows(IllegalStateException.class, () -> taskService.endWrap(1L, "OP01"));
    }

    // ==================== confirmLabel ====================

    @Test
    @DisplayName("confirmLabel: 待贴标 → 待质检，创建并关闭 LABEL 工序日志")
    void testConfirmLabelSuccess() {
        Task task = mockTask(1L, "待贴标");
        when(taskMapper.selectById(1L)).thenReturn(task);

        Task result = taskService.confirmLabel(1L, "OP01");

        assertEquals("待质检", result.getStatus());
        verify(stepLogMapper).insert(argThat((StepLog s) -> "LABEL".equals(s.getStepType())));
        verify(historyMapper).insert(any(TaskStatusHistory.class));
    }

    @Test
    @DisplayName("confirmLabel: 状态非待贴标抛 IllegalStateException")
    void testConfirmLabelWrongStatus() {
        Task task = mockTask(1L, "待质检");
        when(taskMapper.selectById(1L)).thenReturn(task);

        assertThrows(IllegalStateException.class, () -> taskService.confirmLabel(1L, "OP01"));
    }

    // ==================== qualityInspect 补充 ====================

    @Test
    @DisplayName("qualityInspect: CONCESSION 让步放行——待交接 + 异常标记")
    void testQualityInspectConcession() {
        Task task = mockTask(1L, "待质检");
        when(taskMapper.selectById(1L)).thenReturn(task);

        taskService.qualityInspect(1L, InspectionResultType.CONCESSION, "OP01", "轻微糊味，允许放行", null);

        assertEquals("待交接", task.getStatus());
        assertEquals(Integer.valueOf(1), task.getIsException());
        verify(taskMapper).updateById(task);
        verify(historyMapper).insert(any(TaskStatusHistory.class));
    }

    // ==================== handover ====================

    @Test
    @DisplayName("handover: 最终交接——已完成 + 记录交接时间")
    void testHandoverFinal() {
        Task task = mockTask(1L, "待交接");
        when(taskMapper.selectById(1L)).thenReturn(task);

        Task result = taskService.handover(1L, 10, "自取", "张三", "10袋", true);

        assertEquals("已完成", result.getStatus());
        assertNotNull(result.getCompleteTime());
        verify(handoverDetailMapper).insert(any(HandoverDetail.class));
        verify(historyMapper).insert(argThat((TaskStatusHistory h) -> "已完成".equals(h.getToStatus())));
    }

    @Test
    @DisplayName("handover: 非最终交接——已部分完成")
    void testHandoverPartial() {
        Task task = mockTask(1L, "待交接");
        when(taskMapper.selectById(1L)).thenReturn(task);

        Task result = taskService.handover(1L, 5, "快递", "李四", "5袋", false);

        assertEquals("已部分完成", result.getStatus());
        assertNull(result.getCompleteTime());
        verify(historyMapper).insert(argThat((TaskStatusHistory h) -> "已部分完成".equals(h.getToStatus())));
    }

    @Test
    @DisplayName("handover: 状态不在 [待交接, 已部分完成] 抛 IllegalStateException")
    void testHandoverWrongStatus() {
        Task task = mockTask(1L, "煎药中");
        when(taskMapper.selectById(1L)).thenReturn(task);

        assertThrows(IllegalStateException.class, () ->
                taskService.handover(1L, 10, "自取", "王五", null, true));
    }

    // ==================== pauseStep / resumeStep ====================

    @Test
    @DisplayName("pauseStep: 正常暂停工序记录")
    void testPauseStepSuccess() {
        StepLog step = new StepLog();
        step.setId(1L);
        when(stepLogMapper.selectById(1L)).thenReturn(step);

        StepLog result = taskService.pauseStep(1L, "设备故障");

        assertEquals(Integer.valueOf(1), result.getIsPaused());
        assertEquals("设备故障", result.getPauseReason());
        verify(stepLogMapper).updateById(step);
    }

    @Test
    @DisplayName("pauseStep: 工序记录不存在抛 IllegalArgumentException")
    void testPauseStepNotFound() {
        when(stepLogMapper.selectById(99L)).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> taskService.pauseStep(99L, "原因"));
    }

    @Test
    @DisplayName("resumeStep: 正常恢复工序，累加暂停时长")
    void testResumeStepSuccess() {
        StepLog step = new StepLog();
        step.setId(1L);
        step.setPauseDuration(5);
        when(stepLogMapper.selectById(1L)).thenReturn(step);

        StepLog result = taskService.resumeStep(1L);

        assertEquals(Integer.valueOf(0), result.getIsPaused());
        // pauseDuration 应在原基础 5 上增加 (now - updatedAt) 分钟
        assertTrue(result.getPauseDuration() >= 5);
        verify(stepLogMapper).updateById(step);
    }

    @Test
    @DisplayName("resumeStep: 工序记录不存在抛 IllegalArgumentException")
    void testResumeStepNotFound() {
        when(stepLogMapper.selectById(99L)).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> taskService.resumeStep(99L));
    }

    // ==================== queryStepLogs / queryHandoverDetails ====================

    @Test
    @DisplayName("queryStepLogs: 按 taskId 查询工序记录，按 startedAt 升序")
    void testQueryStepLogs() {
        taskService.queryStepLogs(1L);

        verify(stepLogMapper).selectList(any());
    }

    @Test
    @DisplayName("queryHandoverDetails: 按 taskId 查询交接明细，按 handoverTime 降序")
    void testQueryHandoverDetails() {
        taskService.queryHandoverDetails(1L);

        verify(handoverDetailMapper).selectList(any());
    }

    // ==================== updateTemperature ====================

    @Test
    @DisplayName("updateTemperature: 正常温度上报，更新当前温度 + 设备温度 + 告警检查")
    void testUpdateTemperatureSuccess() {
        Task task = mockTask(1L, "煎药中");
        when(equipmentService.getDeviceId("D001")).thenReturn(10L);
        when(taskMapper.selectOne(any())).thenReturn(task);

        Task result = taskService.updateTemperature("D001", BigDecimal.valueOf(98.5));

        assertEquals(0, BigDecimal.valueOf(98.5).compareTo(result.getCurrentTemp()));
        verify(equipmentService).updateTemperature(10L, BigDecimal.valueOf(98.5));
        verify(equipmentService).checkTemperatureAlarm(10L, BigDecimal.valueOf(98.5));
    }

    @Test
    @DisplayName("updateTemperature: 设备编码不存在返回 null")
    void testUpdateTemperatureDeviceNotFound() {
        when(equipmentService.getDeviceId("UNKNOWN")).thenReturn(null);

        assertNull(taskService.updateTemperature("UNKNOWN", BigDecimal.valueOf(100)));
    }

    @Test
    @DisplayName("updateTemperature: 待煎药状态—温度上报自动推进到煎药中")
    void testUpdateTemperatureAutoAdvance() {
        Task task = mockTask(1L, "待煎药");
        when(equipmentService.getDeviceId("D001")).thenReturn(10L);
        when(taskMapper.selectOne(any())).thenReturn(task);

        Task result = taskService.updateTemperature("D001", BigDecimal.valueOf(95));

        assertEquals("煎药中", result.getStatus());
        verify(historyMapper).insert(argThat((TaskStatusHistory h) -> "待煎药".equals(h.getFromStatus()) && "煎药中".equals(h.getToStatus())));
    }

    // ==================== getById / clearAll ====================

    @Test
    @DisplayName("getById: 返回 taskMapper.selectById 结果")
    void testGetById() {
        Task task = mockTask(1L, "待泡药");
        when(taskMapper.selectById(1L)).thenReturn(task);

        assertEquals(task, taskService.getById(1L));
    }

    @Test
    @DisplayName("clearAll: 委托 taskMapper.delete(null) 执行")
    void testClearAll() {
        when(taskMapper.delete(any())).thenReturn(5);

        assertEquals(5, taskService.clearAll());
    }

    // ==================== queryTasks ====================

    @Test
    @DisplayName("queryTasks: 按状态和设备 ID 分页查询")
    void testQueryTasksWithFilters() {
        when(taskMapper.selectPage(any(), any())).thenReturn(new Page<>(1, 20, 0));

        taskService.queryTasks("煎药中", 10L, null, null, null, null, null, null, null, 1, 20);

        verify(taskMapper).selectPage(any(), any());
    }

    // ==================== queryPrintTasks ====================

    @Test
    @DisplayName("queryPrintTasks: 查询待贴标且打印状态为 PENDING/FAILED 的任务")
    void testQueryPrintTasksDefault() {
        taskService.queryPrintTasks(null);

        verify(taskMapper).selectList(any());
    }

    @Test
    @DisplayName("queryPrintTasks: 按指定 printStatus 过滤")
    void testQueryPrintTasksWithStatus() {
        taskService.queryPrintTasks("FAILED");

        verify(taskMapper).selectList(any());
    }

    // ==================== retryPrint ====================

    @Test
    @DisplayName("retryPrint: 重试打印成功——重置状态 + 委托打印服务")
    void testRetryPrintSuccess() {
        Task task = mockTask(1L, "待贴标");
        task.setPrintStatus("FAILED");
        when(taskMapper.selectById(1L)).thenReturn(task);
        when(printService.getPrintStatus(1L)).thenReturn("PRINTED");
        when(equipmentService.getDeviceId("P001")).thenReturn(30L);

        Task result = taskService.retryPrint(1L, "P001", "OP01");

        assertEquals("PRINTED", result.getPrintStatus());
        assertEquals(Long.valueOf(30L), result.getPrintDeviceId());
        verify(printService).retryPrint(1L, "P001", "OP01");
        verify(historyMapper).insert(argThat((TaskStatusHistory h) -> "PRINTED".equals(h.getToStatus())));
    }

    @Test
    @DisplayName("retryPrint: 打印状态不允许重试抛 IllegalStateException")
    void testRetryPrintWrongStatus() {
        Task task = mockTask(1L, "待贴标");
        task.setPrintStatus("PRINTING");
        when(taskMapper.selectById(1L)).thenReturn(task);

        assertThrows(IllegalStateException.class, () ->
                taskService.retryPrint(1L, "P001", "OP01"));
    }

    // ==================== forceStatus 补充 ====================

    @Test
    @DisplayName("forceStatus: 强制推进到包装中——绑定包装机 + 状态覆盖")
    void testForceStatusToWrap() {
        Task task = mockTask(1L, "待包装");
        when(taskMapper.selectById(1L)).thenReturn(task);
        EqDeviceDTO dto = new EqDeviceDTO();
        dto.setId(20L);
        when(equipmentService.getOrCreateDevice("W001", 2)).thenReturn(dto);

        Task result = taskService.forceStatus(1L, "包装中", "OP01", "W001", "强制包装");

        assertEquals("包装中", result.getStatus());
        assertEquals(Long.valueOf(20L), result.getPackageDeviceId());
        verify(equipmentService).updateDeviceStatus(20L, "running");
        verify(historyMapper).insert(argThat((TaskStatusHistory h) -> "包装中".equals(h.getToStatus())));
    }

    @Test
    @DisplayName("forceStatus: 无设备编码的纯状态强制推进")
    void testForceStatusNoDevice() {
        Task task = mockTask(1L, "待质检");
        when(taskMapper.selectById(1L)).thenReturn(task);

        Task result = taskService.forceStatus(1L, "待交接", "OP01", null, "人为跳过质检");

        assertEquals("待交接", result.getStatus());
        assertEquals("OP01", result.getOperatorId());
        verify(equipmentService, never()).getOrCreateDevice(any(), anyInt());
        verify(historyMapper).insert(argThat((TaskStatusHistory h) -> "待交接".equals(h.getToStatus())));
    }

    // ==================== 任务不存在 ====================

    @Test
    @DisplayName("startSoak: 任务不存在抛 IllegalArgumentException")
    void testStartSoakTaskNotFound() {
        when(taskMapper.selectById(99L)).thenReturn(null);
        assertThrows(IllegalArgumentException.class, () -> taskService.startSoak(99L, "OP01"));
    }

    @Test
    @DisplayName("endSoak: 任务不存在抛 IllegalArgumentException")
    void testEndSoakTaskNotFound() {
        when(taskMapper.selectById(99L)).thenReturn(null);
        assertThrows(IllegalArgumentException.class, () -> taskService.endSoak(99L, "OP01"));
    }

    @Test
    @DisplayName("endDecoct: 任务不存在抛 IllegalArgumentException")
    void testEndDecoctTaskNotFound() {
        when(taskMapper.selectById(99L)).thenReturn(null);
        assertThrows(IllegalArgumentException.class, () -> taskService.endDecoct(99L, "OP01"));
    }

    @Test
    @DisplayName("startPour: 任务不存在抛 IllegalArgumentException")
    void testStartPourTaskNotFound() {
        when(taskMapper.selectById(99L)).thenReturn(null);
        assertThrows(IllegalArgumentException.class, () -> taskService.startPour(99L, "OP01"));
    }

    @Test
    @DisplayName("endPour: 任务不存在抛 IllegalArgumentException")
    void testEndPourTaskNotFound() {
        when(taskMapper.selectById(99L)).thenReturn(null);
        assertThrows(IllegalArgumentException.class, () -> taskService.endPour(99L, "OP01"));
    }

    @Test
    @DisplayName("endWrap: 任务不存在抛 IllegalArgumentException")
    void testEndWrapTaskNotFound() {
        when(taskMapper.selectById(99L)).thenReturn(null);
        assertThrows(IllegalArgumentException.class, () -> taskService.endWrap(99L, "OP01"));
    }

    @Test
    @DisplayName("confirmLabel: 任务不存在抛 IllegalArgumentException")
    void testConfirmLabelTaskNotFound() {
        when(taskMapper.selectById(99L)).thenReturn(null);
        assertThrows(IllegalArgumentException.class, () -> taskService.confirmLabel(99L, "OP01"));
    }

    @Test
    @DisplayName("qualityInspect: 任务不存在抛 IllegalArgumentException")
    void testQualityInspectTaskNotFound() {
        when(taskMapper.selectById(99L)).thenReturn(null);
        assertThrows(IllegalArgumentException.class, () ->
                taskService.qualityInspect(99L, InspectionResultType.PASS, "OP01", null, null));
    }

    @Test
    @DisplayName("handover: 任务不存在抛 IllegalArgumentException")
    void testHandoverTaskNotFound() {
        when(taskMapper.selectById(99L)).thenReturn(null);
        assertThrows(IllegalArgumentException.class, () ->
                taskService.handover(99L, 10, "自取", "张三", null, true));
    }

    @Test
    @DisplayName("printLabel: 任务不存在抛 IllegalArgumentException")
    void testPrintLabelTaskNotFound() {
        when(taskMapper.selectById(99L)).thenReturn(null);
        assertThrows(IllegalArgumentException.class, () ->
                taskService.printLabel(99L, "P001", "OP01"));
    }

    @Test
    @DisplayName("retryPrint: 任务不存在抛 IllegalArgumentException")
    void testRetryPrintTaskNotFound() {
        when(taskMapper.selectById(99L)).thenReturn(null);
        assertThrows(IllegalArgumentException.class, () ->
                taskService.retryPrint(99L, "P001", "OP01"));
    }

    @Test
    @DisplayName("forceStatus: 任务不存在抛 IllegalArgumentException")
    void testForceStatusTaskNotFound() {
        when(taskMapper.selectById(99L)).thenReturn(null);
        assertThrows(IllegalArgumentException.class, () ->
                taskService.forceStatus(99L, "待交接", "OP01", null, null));
    }

    @Test
    @DisplayName("bindDevice: 任务不存在抛 IllegalArgumentException")
    void testBindDeviceTaskNotFound() {
        when(taskMapper.selectByIdForUpdate(99L)).thenReturn(null);
        assertThrows(IllegalArgumentException.class, () ->
                taskService.bindDevice(99L, "D001"));
    }

    @Test
    @DisplayName("startDecoct: 任务不存在抛 IllegalArgumentException")
    void testStartDecoctTaskNotFound() {
        when(taskMapper.selectByIdForUpdate(99L)).thenReturn(null);
        assertThrows(IllegalArgumentException.class, () ->
                taskService.startDecoct(99L, "D001", "OP01"));
    }

    @Test
    @DisplayName("startWrap: 任务不存在抛 IllegalArgumentException")
    void testStartWrapTaskNotFound() {
        when(taskMapper.selectByIdForUpdate(99L)).thenReturn(null);
        assertThrows(IllegalArgumentException.class, () ->
                taskService.startWrap(99L, "W001", "OP01"));
    }

    // ==================== 消耗记录集成（V2.0） ====================

    @Test
    @DisplayName("startSoak: 有药材明细时调用消耗记录服务")
    void testStartSoakWithMedicinesTriggersConsume() {
        Task task = mockTask(1L, "待泡药");
        task.setPrescriptionId(100L);
        when(taskMapper.selectById(1L)).thenReturn(task);

        PrescriptionMedicine med1 = new PrescriptionMedicine();
        med1.setMedicineId(10L);
        med1.setMedicineName("黄芪");
        med1.setDosage(BigDecimal.valueOf(15));
        med1.setUnit("g");

        PrescriptionMedicine med2 = new PrescriptionMedicine();
        med2.setMedicineName("当归");
        med2.setDosage(BigDecimal.valueOf(10));

        when(prescriptionMedicineMapper.selectByPrescriptionId(100L)).thenReturn(java.util.Arrays.asList(med1, med2));
        when(consumeRecordService.recordConsume(any(ConsumeRecordRequest.class))).thenReturn(java.util.Arrays.asList(1L, 2L));

        Task result = taskService.startSoak(1L, "OP01");

        assertEquals("泡药中", result.getStatus());
        verify(prescriptionMedicineMapper).selectByPrescriptionId(100L);
        verify(consumeRecordService).recordConsume(argThat(req ->
                req.getTaskId().equals(1L)
                        && req.getItems() != null
                        && req.getItems().size() == 2
                        && "黄芪".equals(req.getItems().get(0).getMedicineName())
                        && 0 == BigDecimal.valueOf(15).compareTo(req.getItems().get(0).getQuantity())
        ));
    }

    @Test
    @DisplayName("startSoak: 无处方时跳过消耗记录，任务正常推进")
    void testStartSoakWithoutPrescriptionSkipsConsume() {
        Task task = mockTask(1L, "待泡药");
        when(taskMapper.selectById(1L)).thenReturn(task);

        Task result = taskService.startSoak(1L, "OP01");

        assertEquals("泡药中", result.getStatus());
        verify(consumeRecordService, never()).recordConsume(any());
    }

    @Test
    @DisplayName("startSoak: 处方无药材明细时跳过消耗记录，任务正常推进")
    void testStartSoakWithEmptyMedicinesSkipsConsume() {
        Task task = mockTask(1L, "待泡药");
        task.setPrescriptionId(100L);
        when(taskMapper.selectById(1L)).thenReturn(task);
        when(prescriptionMedicineMapper.selectByPrescriptionId(100L)).thenReturn(java.util.Collections.emptyList());

        Task result = taskService.startSoak(1L, "OP01");

        assertEquals("泡药中", result.getStatus());
        verify(consumeRecordService, never()).recordConsume(any());
    }

    @Test
    @DisplayName("startSoak: 消耗记录服务异常时任务仍正常推进（不阻断主流程）")
    void testStartSoakConsumeFailureDoesNotBlockTask() {
        Task task = mockTask(1L, "待泡药");
        task.setPrescriptionId(100L);
        when(taskMapper.selectById(1L)).thenReturn(task);

        PrescriptionMedicine med = new PrescriptionMedicine();
        med.setMedicineName("黄芪");
        med.setDosage(BigDecimal.valueOf(15));

        when(prescriptionMedicineMapper.selectByPrescriptionId(100L)).thenReturn(java.util.Collections.singletonList(med));
        when(consumeRecordService.recordConsume(any(ConsumeRecordRequest.class)))
                .thenThrow(new RuntimeException("模拟消耗服务异常"));

        Task result = taskService.startSoak(1L, "OP01");

        assertEquals("泡药中", result.getStatus());
        verify(taskMapper).updateById(task);
    }
}
