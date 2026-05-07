package cn.org.openygt.common.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TaskStatusTransitionTest {

    @Test
    @DisplayName("正常路径：待泡药 → 泡药中 允许")
    void testNormalSoakToSoaking() {
        assertDoesNotThrow(() ->
                TaskStatusTransition.validateNormal(TaskStatus.WAIT_SOAK.getCode(), TaskStatus.SOAKING.getCode()));
    }

    @Test
    @DisplayName("正常路径：包装中 → 待质检 允许（V30 WAIT_LABEL合并）")
    void testNormalWrapToQc() {
        assertDoesNotThrow(() ->
                TaskStatusTransition.validateNormal(TaskStatus.WRAPPING.getCode(), TaskStatus.WAIT_QC.getCode()));
    }

    @Test
    @DisplayName("正常路径：待质检 → 已暂存 允许（V30 新增）")
    void testNormalQcToStored() {
        assertDoesNotThrow(() ->
                TaskStatusTransition.validateNormal(TaskStatus.WAIT_QC.getCode(), TaskStatus.STORED.getCode()));
    }

    @Test
    @DisplayName("正常路径：已暂存 → 待交接 允许（V30 新增）")
    void testNormalStoredToHandover() {
        assertDoesNotThrow(() ->
                TaskStatusTransition.validateNormal(TaskStatus.STORED.getCode(), TaskStatus.WAIT_HANDOVER.getCode()));
    }

    @Test
    @DisplayName("正常路径：待质检 → 待二次判定 允许（V30 FAIL明确化）")
    void testNormalQcToSecondJudgement() {
        assertDoesNotThrow(() ->
                TaskStatusTransition.validateNormal(TaskStatus.WAIT_QC.getCode(), TaskStatus.SECOND_JUDGEMENT.getCode()));
    }

    @Test
    @DisplayName("正常路径：待二次判定 → 已暂存 允许")
    void testNormalSecondJudgementToStored() {
        assertDoesNotThrow(() ->
                TaskStatusTransition.validateNormal(TaskStatus.SECOND_JUDGEMENT.getCode(), TaskStatus.STORED.getCode()));
    }

    @Test
    @DisplayName("正常路径：已完成 → 待泡药 非法（跨度太大）")
    void testNormalCompletedToSoakIllegal() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                TaskStatusTransition.validateNormal(TaskStatus.COMPLETED.getCode(), TaskStatus.WAIT_SOAK.getCode()));
        assertTrue(ex.getMessage().contains("非法状态转换"));
    }

    @Test
    @DisplayName("正常路径：待贴标 → 待交接 非法（V30 已合并）")
    void testNormalLabelToHandoverIllegal() {
        assertThrows(IllegalStateException.class, () ->
                TaskStatusTransition.validateNormal(TaskStatus.WAIT_LABEL.getCode(), TaskStatus.WAIT_HANDOVER.getCode()));
    }

    @Test
    @DisplayName("回退路径：煎药中 → 待煎药 允许")
    void testRollbackDecoctingToWaitDecoct() {
        assertDoesNotThrow(() ->
                TaskStatusTransition.validateRollback(TaskStatus.DECOCTING.getCode(), TaskStatus.WAIT_DECOCT.getCode()));
    }

    @Test
    @DisplayName("回退路径：已暂存 → 待质检 允许（V30 新增）")
    void testRollbackStoredToQc() {
        assertDoesNotThrow(() ->
                TaskStatusTransition.validateRollback(TaskStatus.STORED.getCode(), TaskStatus.WAIT_QC.getCode()));
    }

    @Test
    @DisplayName("回退路径：待交接 → 已暂存 允许（V30 新增）")
    void testRollbackHandoverToStored() {
        assertDoesNotThrow(() ->
                TaskStatusTransition.validateRollback(TaskStatus.WAIT_HANDOVER.getCode(), TaskStatus.STORED.getCode()));
    }

    @Test
    @DisplayName("回退路径：已完成 → 已暂存 允许（V30 召回路径变更）")
    void testRollbackCompletedToStored() {
        assertDoesNotThrow(() ->
                TaskStatusTransition.validateRollback(TaskStatus.COMPLETED.getCode(), TaskStatus.STORED.getCode()));
    }

    @Test
    @DisplayName("回退路径：待质检 → 待泡药 非法（跨度太大）")
    void testRollbackQcToSoakIllegal() {
        assertThrows(IllegalStateException.class, () ->
                TaskStatusTransition.validateRollback(TaskStatus.WAIT_QC.getCode(), TaskStatus.WAIT_SOAK.getCode()));
    }

    @Test
    @DisplayName("状态不变：任意状态 → 自身 允许")
    void testSameStatusAllowed() {
        assertDoesNotThrow(() ->
                TaskStatusTransition.validateNormal(TaskStatus.DECOCTING.getCode(), TaskStatus.DECOCTING.getCode()));
    }

    @Test
    @DisplayName("空值校验：null 状态抛 IllegalArgumentException")
    void testNullStatusThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                TaskStatusTransition.validateNormal(null, TaskStatus.DECOCTING.getCode()));
    }

    @Test
    @DisplayName("挂起路径：运行中状态 → 已挂起 允许")
    void testSuspendFromRunning() {
        assertDoesNotThrow(() ->
                TaskStatusTransition.validateSuspend(TaskStatus.DECOCTING.getCode()));
        assertDoesNotThrow(() ->
                TaskStatusTransition.validateSuspend(TaskStatus.STORED.getCode()));
        assertDoesNotThrow(() ->
                TaskStatusTransition.validateSuspend(TaskStatus.WAIT_LABEL.getCode()));
    }

    @Test
    @DisplayName("挂起路径：终态 → 已挂起 非法")
    void testSuspendFromTerminalIllegal() {
        assertThrows(IllegalStateException.class, () ->
                TaskStatusTransition.validateSuspend(TaskStatus.COMPLETED.getCode()));
        assertThrows(IllegalStateException.class, () ->
                TaskStatusTransition.validateSuspend(TaskStatus.SCRAPPED.getCode()));
    }

    @Test
    @DisplayName("恢复路径：已挂起 → 挂起前状态 允许")
    void testResumeFromSuspended() {
        assertDoesNotThrow(() ->
                TaskStatusTransition.validateResume(TaskStatus.SUSPENDED.getCode(), TaskStatus.DECOCTING.getCode()));
    }

    @Test
    @DisplayName("恢复路径：非挂起状态 非法")
    void testResumeFromNonSuspendedIllegal() {
        assertThrows(IllegalStateException.class, () ->
                TaskStatusTransition.validateResume(TaskStatus.DECOCTING.getCode(), TaskStatus.DECOCTING.getCode()));
    }

    @Test
    @DisplayName("查询正常目标：包装中 包含 待质检（V30合并后）")
    void testGetNormalTargetsFromWrapping() {
        List<String> targets = TaskStatusTransition.getNormalTargets(TaskStatus.WRAPPING.getCode());
        assertTrue(targets.contains(TaskStatus.WAIT_QC.getCode()));
        assertFalse(targets.contains(TaskStatus.WAIT_LABEL.getCode()));
        assertFalse(targets.contains("待交付"));
    }

    @Test
    @DisplayName("查询回退目标：已暂存 包含 待质检（V30新增）")
    void testGetRollbackTargetsFromStored() {
        List<String> targets = TaskStatusTransition.getRollbackTargets(TaskStatus.STORED.getCode());
        assertTrue(targets.contains(TaskStatus.WAIT_QC.getCode()));
    }

    @Test
    @DisplayName("查询未知状态返回空列表")
    void testGetTargetsForUnknownStatus() {
        assertTrue(TaskStatusTransition.getNormalTargets("不存在").isEmpty());
    }
}
