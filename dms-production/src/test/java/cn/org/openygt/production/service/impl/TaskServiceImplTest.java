package cn.org.openygt.production.service.impl;

import cn.org.openygt.common.dto.EqDeviceDTO;
import cn.org.openygt.common.enums.InspectionResultType;
import cn.org.openygt.common.service.EquipmentService;
import cn.org.openygt.common.service.PrintService;
import cn.org.openygt.production.entity.*;
import cn.org.openygt.production.mapper.*;
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
    private EquipmentService equipmentService;
    @Mock
    private PrintService printService;

    @InjectMocks
    private TaskServiceImpl taskService;

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

        taskService.qualityInspect(1L, InspectionResultType.REWORK, "OP01", "温度不足");

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

        taskService.qualityInspect(1L, InspectionResultType.PASS, "OP01", null);

        assertEquals("待交接", task.getStatus());
        verify(equipmentService, never()).reserveDevice(any(), any());
    }

    @Test
    @DisplayName("qualityInspect: SCRAP 报废——状态流转 + 异常标记")
    void testQualityInspectScrap() {
        Task task = mockTask(1L, "待质检");
        when(taskMapper.selectById(1L)).thenReturn(task);

        taskService.qualityInspect(1L, InspectionResultType.SCRAP, "OP01", "污染");

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
        when(equipmentService.getDeviceId("D001")).thenReturn(10L);
        when(equipmentService.getDeviceStatus(10L)).thenReturn("idle");

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
        when(equipmentService.getDeviceId("D001")).thenReturn(10L);
        when(equipmentService.getDeviceStatus(10L)).thenReturn("running");

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
    //  测试用例骨架（方法体待架构师提交后填充）
    //  标注了预期行为，架构师请根据重构后的实体和方法签名调整断言
    // ========================================================================

    // ==================== startSoak ====================

    @Test
    @DisplayName("startSoak: 待泡药 → 泡药中，记录开始时间、工时记录、工序日志")
    void testStartSoakSuccess() {
        // TODO: 架构师提交后填充
        // Task task = mockTask(1L, "待泡药");
        // when(taskMapper.selectById(1L)).thenReturn(task);
        // Task result = taskService.startSoak(1L, "OP01");
        // assertEquals("泡药中", result.getStatus());
        // verify(taskMapper).updateById(task);
        // verify(workRecordMapper).insert(argThat(r -> "SOAK".equals(r.getAction())));
        // verify(stepLogMapper).insert(argThat(s -> "SOAK".equals(s.getStepType())));
    }

    @Test
    @DisplayName("startSoak: 状态非待泡药抛 IllegalStateException")
    void testStartSoakWrongStatus() {
        // TODO: 架构师提交后填充
        // Task task = mockTask(1L, "煎药中");
        // when(taskMapper.selectById(1L)).thenReturn(task);
        // assertThrows(IllegalStateException.class, () -> taskService.startSoak(1L, "OP01"));
    }

    // ==================== endSoak ====================

    @Test
    @DisplayName("endSoak: 泡药中 → 待煎药，计算耗时，关闭工序日志")
    void testEndSoakSuccess() {
        // TODO: 架构师提交后填充
        // Task task = mockTask(1L, "泡药中");
        // task.setSoakStartTime(LocalDateTime.now().minusMinutes(30));
        // when(taskMapper.selectById(1L)).thenReturn(task);
        // Task result = taskService.endSoak(1L, "OP01");
        // assertEquals("待煎药", result.getStatus());
        // assertEquals(30, result.getCurrentStageDuration());
        // verify(workRecordMapper).insert(argThat(r -> "SOAK".equals(r.getAction()) && r.getWorkTime() == 30));
    }

    @Test
    @DisplayName("endSoak: 状态非泡药中抛 IllegalStateException")
    void testEndSoakWrongStatus() {
        // TODO: 架构师提交后填充
        // Task task = mockTask(1L, "待泡药");
        // when(taskMapper.selectById(1L)).thenReturn(task);
        // assertThrows(IllegalStateException.class, () -> taskService.endSoak(1L, "OP01"));
    }

    // ==================== startPour ====================

    @Test
    @DisplayName("startPour: 待出液 → 出液中，记录开始时间和工序日志")
    void testStartPourSuccess() {
        // TODO: 架构师提交后填充
        // Task task = mockTask(1L, "待出液");
        // when(taskMapper.selectById(1L)).thenReturn(task);
        // Task result = taskService.startPour(1L, "OP01");
        // assertEquals("出液中", result.getStatus());
        // verify(stepLogMapper).insert(argThat(s -> "POUR".equals(s.getStepType())));
    }

    @Test
    @DisplayName("startPour: 状态非待出液抛 IllegalStateException")
    void testStartPourWrongStatus() {
        // TODO: 架构师提交后填充
        // Task task = mockTask(1L, "泡药中");
        // when(taskMapper.selectById(1L)).thenReturn(task);
        // assertThrows(IllegalStateException.class, () -> taskService.startPour(1L, "OP01"));
    }

    // ==================== endPour ====================

    @Test
    @DisplayName("endPour: 出液中 → 待包装，计算耗时，关闭工序日志")
    void testEndPourSuccess() {
        // TODO: 架构师提交后填充
        // Task task = mockTask(1L, "出液中");
        // when(taskMapper.selectById(1L)).thenReturn(task);
        // Task result = taskService.endPour(1L, "OP01");
        // assertEquals("待包装", result.getStatus());
    }

    @Test
    @DisplayName("endPour: auto 级别自动触发 startWrap（绑定包装机）")
    void testEndPourAutoTriggersWrap() {
        // TODO: 架构师提交后填充
        // Task task = mockTask(1L, "出液中");
        // task.setDecoctDeviceId(10L);
        // when(taskMapper.selectById(1L)).thenReturn(task);
        // when(equipmentService.getAutoLevel(10L)).thenReturn("auto");
        // when(equipmentService.getDeviceCode(10L)).thenReturn("D001");
        // // startWrap 需要 selectByIdForUpdate 返回待包装状态的任务
        // Task wrapReady = mockTask(1L, "待包装");
        // when(taskMapper.selectByIdForUpdate(1L)).thenReturn(wrapReady);
        // when(equipmentService.getDeviceId("D001")).thenReturn(20L);
        // when(equipmentService.getDeviceStatus(20L)).thenReturn("idle");
        // Task result = taskService.endPour(1L, "OP01");
        // assertEquals("包装中", result.getStatus());
        // assertEquals(Long.valueOf(20L), result.getPackageDeviceId());
    }

    @Test
    @DisplayName("endPour: 状态非出液中抛 IllegalStateException")
    void testEndPourWrongStatus() {
        // TODO: 架构师提交后填充
        // Task task = mockTask(1L, "出液");
        // when(taskMapper.selectById(1L)).thenReturn(task);
        // assertThrows(IllegalStateException.class, () -> taskService.endPour(1L, "OP01"));
    }

    // ==================== startWrap ====================

    @Test
    @DisplayName("startWrap: 待包装 → 包装中，绑定包装机 + 记录创建")
    void testStartWrapSuccess() {
        // TODO: 架构师提交后填充
        // Task task = mockTask(1L, "待包装");
        // when(taskMapper.selectByIdForUpdate(1L)).thenReturn(task);
        // when(equipmentService.getDeviceId("W001")).thenReturn(20L);
        // when(equipmentService.getDeviceStatus(20L)).thenReturn("idle");
        // Task result = taskService.startWrap(1L, "W001", "OP01");
        // assertEquals("包装中", result.getStatus());
        // assertEquals(Long.valueOf(20L), result.getPackageDeviceId());
        // verify(equipmentService).updateDeviceStatus(20L, "running");
    }

    @Test
    @DisplayName("startWrap: 包装机非空闲抛 IllegalStateException")
    void testStartWrapDeviceNotIdle() {
        // TODO: 架构师提交后填充
        // Task task = mockTask(1L, "待包装");
        // when(taskMapper.selectByIdForUpdate(1L)).thenReturn(task);
        // when(equipmentService.getDeviceId("W001")).thenReturn(20L);
        // when(equipmentService.getDeviceStatus(20L)).thenReturn("running");
        // assertThrows(IllegalStateException.class, () -> taskService.startWrap(1L, "W001", "OP01"));
    }

    @Test
    @DisplayName("startWrap: 状态非待包装抛 IllegalStateException")
    void testStartWrapWrongStatus() {
        // TODO: 架构师提交后填充
        // Task task = mockTask(1L, "出液中");
        // when(taskMapper.selectByIdForUpdate(1L)).thenReturn(task);
        // assertThrows(IllegalStateException.class, () -> taskService.startWrap(1L, "W001", "OP01"));
    }

    // ==================== endWrap ====================

    @Test
    @DisplayName("endWrap: 包装中 → 待贴标，释放包装机 + 记录工时")
    void testEndWrapSuccess() {
        // TODO: 架构师提交后填充
        // Task task = mockTask(1L, "包装中");
        // task.setPackageDeviceId(20L);
        // when(taskMapper.selectById(1L)).thenReturn(task);
        // Task result = taskService.endWrap(1L, "OP01");
        // assertEquals("待贴标", result.getStatus());
        // verify(equipmentService).releaseDevice(20L);
    }

    @Test
    @DisplayName("endWrap: 状态非包装中抛 IllegalStateException")
    void testEndWrapWrongStatus() {
        // TODO: 架构师提交后填充
        // Task task = mockTask(1L, "待包装");
        // when(taskMapper.selectById(1L)).thenReturn(task);
        // assertThrows(IllegalStateException.class, () -> taskService.endWrap(1L, "OP01"));
    }

    // ==================== confirmLabel ====================

    @Test
    @DisplayName("confirmLabel: 待贴标 → 待质检，创建并关闭 LABEL 工序日志")
    void testConfirmLabelSuccess() {
        // TODO: 架构师提交后填充
        // Task task = mockTask(1L, "待贴标");
        // when(taskMapper.selectById(1L)).thenReturn(task);
        // Task result = taskService.confirmLabel(1L, "OP01");
        // assertEquals("待质检", result.getStatus());
        // verify(stepLogMapper).insert(argThat(s -> "LABEL".equals(s.getStepType())));
    }

    @Test
    @DisplayName("confirmLabel: 状态非待贴标抛 IllegalStateException")
    void testConfirmLabelWrongStatus() {
        // TODO: 架构师提交后填充
        // Task task = mockTask(1L, "待质检");
        // when(taskMapper.selectById(1L)).thenReturn(task);
        // assertThrows(IllegalStateException.class, () -> taskService.confirmLabel(1L, "OP01"));
    }

    // ==================== qualityInspect 补充 ====================

    @Test
    @DisplayName("qualityInspect: CONCESSION 让步放行——待交接 + 异常标记")
    void testQualityInspectConcession() {
        // TODO: 架构师提交后填充
        // Task task = mockTask(1L, "待质检");
        // when(taskMapper.selectById(1L)).thenReturn(task);
        // taskService.qualityInspect(1L, InspectionResultType.CONCESSION, "OP01", "轻微糊味，允许放行");
        // assertEquals("待交接", task.getStatus());
        // assertEquals(Integer.valueOf(1), task.getIsException());
        // verify(taskMapper).updateById(task);
    }

    // ==================== handover ====================

    @Test
    @DisplayName("handover: 最终交接——已完成 + 记录交接时间")
    void testHandoverFinal() {
        // TODO: 架构师提交后填充
        // Task task = mockTask(1L, "待交接");
        // when(taskMapper.selectById(1L)).thenReturn(task);
        // Task result = taskService.handover(1L, 10, "自取", "张三", "10袋", true);
        // assertEquals("已完成", result.getStatus());
        // assertNotNull(result.getCompleteTime());
        // verify(handoverDetailMapper).insert(any(HandoverDetail.class));
    }

    @Test
    @DisplayName("handover: 非最终交接——已部分完成")
    void testHandoverPartial() {
        // TODO: 架构师提交后填充
        // Task task = mockTask(1L, "待交接");
        // when(taskMapper.selectById(1L)).thenReturn(task);
        // Task result = taskService.handover(1L, 5, "快递", "李四", "5袋", false);
        // assertEquals("已部分完成", result.getStatus());
        // assertNull(result.getCompleteTime());
    }

    @Test
    @DisplayName("handover: 状态不在 [待交接, 已部分完成] 抛 IllegalStateException")
    void testHandoverWrongStatus() {
        // TODO: 架构师提交后填充
        // Task task = mockTask(1L, "煎药中");
        // when(taskMapper.selectById(1L)).thenReturn(task);
        // assertThrows(IllegalStateException.class, () -> taskService.handover(1L, 10, "自取", "王五", null, true));
    }

    // ==================== pauseStep / resumeStep ====================

    @Test
    @DisplayName("pauseStep: 正常暂停工序记录")
    void testPauseStepSuccess() {
        // TODO: 架构师提交后填充
        // StepLog step = new StepLog();
        // step.setId(1L);
        // when(stepLogMapper.selectById(1L)).thenReturn(step);
        // StepLog result = taskService.pauseStep(1L, "设备故障");
        // assertEquals(Integer.valueOf(1), result.getIsPaused());
        // assertEquals("设备故障", result.getPauseReason());
        // verify(stepLogMapper).updateById(step);
    }

    @Test
    @DisplayName("pauseStep: 工序记录不存在抛 IllegalArgumentException")
    void testPauseStepNotFound() {
        // TODO: 架构师提交后填充
        // when(stepLogMapper.selectById(99L)).thenReturn(null);
        // assertThrows(IllegalArgumentException.class, () -> taskService.pauseStep(99L, "原因"));
    }

    @Test
    @DisplayName("resumeStep: 正常恢复工序，累加暂停时长")
    void testResumeStepSuccess() {
        // TODO: 架构师提交后填充
        // StepLog step = new StepLog();
        // step.setId(1L);
        // step.setPauseDuration(5);
        // when(stepLogMapper.selectById(1L)).thenReturn(step);
        // StepLog result = taskService.resumeStep(1L);
        // assertEquals(Integer.valueOf(0), result.getIsPaused());
        // // pauseDuration 应在原基础上增加 (now - updatedAt) 分钟
        // assertTrue(result.getPauseDuration() >= 5);
    }

    @Test
    @DisplayName("resumeStep: 工序记录不存在抛 IllegalArgumentException")
    void testResumeStepNotFound() {
        // TODO: 架构师提交后填充
        // when(stepLogMapper.selectById(99L)).thenReturn(null);
        // assertThrows(IllegalArgumentException.class, () -> taskService.resumeStep(99L));
    }

    // ==================== queryStepLogs / queryHandoverDetails ====================

    @Test
    @DisplayName("queryStepLogs: 按 taskId 查询工序记录，按 startedAt 升序")
    void testQueryStepLogs() {
        // TODO: 架构师提交后填充
        // taskService.queryStepLogs(1L);
        // verify(stepLogMapper).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("queryHandoverDetails: 按 taskId 查询交接明细，按 handoverTime 降序")
    void testQueryHandoverDetails() {
        // TODO: 架构师提交后填充
        // taskService.queryHandoverDetails(1L);
        // verify(handoverDetailMapper).selectList(any(LambdaQueryWrapper.class));
    }

    // ==================== updateTemperature ====================

    @Test
    @DisplayName("updateTemperature: 正常温度上报，更新当前温度 + 设备温度 + 告警检查")
    void testUpdateTemperatureSuccess() {
        // TODO: 架构师提交后填充
        // Task task = mockTask(1L, "煎药中");
        // when(equipmentService.getDeviceId("D001")).thenReturn(10L);
        // when(taskMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(task);
        // Task result = taskService.updateTemperature("D001", BigDecimal.valueOf(98.5));
        // assertEquals(0, BigDecimal.valueOf(98.5).compareTo(result.getCurrentTemp()));
        // verify(equipmentService).updateTemperature(10L, BigDecimal.valueOf(98.5));
        // verify(equipmentService).checkTemperatureAlarm(10L, BigDecimal.valueOf(98.5));
    }

    @Test
    @DisplayName("updateTemperature: 设备编码不存在返回 null")
    void testUpdateTemperatureDeviceNotFound() {
        // TODO: 架构师提交后填充
        // when(equipmentService.getDeviceId("UNKNOWN")).thenReturn(null);
        // assertNull(taskService.updateTemperature("UNKNOWN", BigDecimal.valueOf(100)));
    }

    @Test
    @DisplayName("updateTemperature: 待煎药状态—温度上报自动推进到煎药中")
    void testUpdateTemperatureAutoAdvance() {
        // TODO: 架构师提交后填充
        // Task task = mockTask(1L, "待煎药");
        // when(equipmentService.getDeviceId("D001")).thenReturn(10L);
        // when(taskMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(task);
        // Task result = taskService.updateTemperature("D001", BigDecimal.valueOf(95));
        // assertEquals("煎药中", result.getStatus());
        // verify(historyMapper).insert(argThat(h -> "待煎药".equals(h.getFromStatus()) && "煎药中".equals(h.getToStatus())));
    }

    // ==================== getById / clearAll ====================

    @Test
    @DisplayName("getById: 返回 taskMapper.selectById 结果")
    void testGetById() {
        // TODO: 架构师提交后填充
        // Task task = mockTask(1L, "待泡药");
        // when(taskMapper.selectById(1L)).thenReturn(task);
        // assertEquals(task, taskService.getById(1L));
    }

    @Test
    @DisplayName("clearAll: 委托 taskMapper.delete(null) 执行")
    void testClearAll() {
        // TODO: 架构师提交后填充
        // when(taskMapper.delete(null)).thenReturn(5);
        // assertEquals(5, taskService.clearAll());
    }

    // ==================== queryTasks ====================

    @Test
    @DisplayName("queryTasks: 按状态和设备 ID 分页查询")
    void testQueryTasksWithFilters() {
        // TODO: 架构师提交后填充
        // taskService.queryTasks("煎药中", 10L, 1, 20);
        // verify(taskMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    // ==================== queryPrintTasks ====================

    @Test
    @DisplayName("queryPrintTasks: 查询待贴标且打印状态为 PENDING/FAILED 的任务")
    void testQueryPrintTasksDefault() {
        // TODO: 架构师提交后填充
        // taskService.queryPrintTasks(null);
        // verify(taskMapper).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("queryPrintTasks: 按指定 printStatus 过滤")
    void testQueryPrintTasksWithStatus() {
        // TODO: 架构师提交后填充
        // taskService.queryPrintTasks("FAILED");
        // verify(taskMapper).selectList(any(LambdaQueryWrapper.class));
    }

    // ==================== retryPrint ====================

    @Test
    @DisplayName("retryPrint: 重试打印成功——重置状态 + 委托打印服务")
    void testRetryPrintSuccess() {
        // TODO: 架构师提交后填充
        // Task task = mockTask(1L, "待贴标");
        // task.setPrintStatus("FAILED");
        // when(taskMapper.selectById(1L)).thenReturn(task);
        // when(printService.getPrintStatus(1L)).thenReturn("PRINTED");
        // when(equipmentService.getDeviceId("P001")).thenReturn(30L);
        // Task result = taskService.retryPrint(1L, "P001", "OP01");
        // assertEquals("PRINTED", result.getPrintStatus());
        // verify(printService).retryPrint(1L, "P001", "OP01");
    }

    @Test
    @DisplayName("retryPrint: 打印状态不允许重试抛 IllegalStateException")
    void testRetryPrintWrongStatus() {
        // TODO: 架构师提交后填充
        // Task task = mockTask(1L, "待贴标");
        // task.setPrintStatus("PRINTING");
        // when(taskMapper.selectById(1L)).thenReturn(task);
        // assertThrows(IllegalStateException.class, () -> taskService.retryPrint(1L, "P001", "OP01"));
    }

    // ==================== forceStatus 补充 ====================

    @Test
    @DisplayName("forceStatus: 强制推进到包装中——绑定包装机 + 状态覆盖")
    void testForceStatusToWrap() {
        // TODO: 架构师提交后填充
        // Task task = mockTask(1L, "待包装");
        // when(taskMapper.selectById(1L)).thenReturn(task);
        // EqDeviceDTO dto = new EqDeviceDTO();
        // dto.setId(20L);
        // when(equipmentService.getOrCreateDevice("W001", 2)).thenReturn(dto);
        // Task result = taskService.forceStatus(1L, "包装中", "OP01", "W001", "强制包装");
        // assertEquals("包装中", result.getStatus());
        // assertEquals(Long.valueOf(20L), result.getPackageDeviceId());
        // verify(equipmentService).updateDeviceStatus(20L, "running");
    }

    @Test
    @DisplayName("forceStatus: 无设备编码的纯状态强制推进")
    void testForceStatusNoDevice() {
        // TODO: 架构师提交后填充
        // Task task = mockTask(1L, "待质检");
        // when(taskMapper.selectById(1L)).thenReturn(task);
        // Task result = taskService.forceStatus(1L, "待交接", "OP01", null, "人为跳过质检");
        // assertEquals("待交接", result.getStatus());
        // verify(equipmentService, never()).getOrCreateDevice(any(), anyInt());
    }

    // ==================== 任务不存在 ====================

    @Test
    @DisplayName("所有状态变更方法：任务不存在抛 IllegalArgumentException")
    void testAllMethodsThrowWhenTaskNotFound() {
        // TODO: 架构师提交后填充
        // 对每个需要查询任务的方法，验证 taskId=99 且 taskMapper.selectById 返回 null 时
        // 抛出 IllegalArgumentException("任务不存在")
    }
}
