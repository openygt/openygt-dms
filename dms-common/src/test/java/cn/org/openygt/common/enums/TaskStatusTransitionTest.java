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
                TaskStatusTransition.validateNormal("待泡药", "泡药中"));
    }

    @Test
    @DisplayName("正常路径：包装中 → 待交付 允许（v1.1 合并路径）")
    void testNormalWrapToDeliver() {
        assertDoesNotThrow(() ->
                TaskStatusTransition.validateNormal("包装中", "待交付"));
    }

    @Test
    @DisplayName("正常路径：待交付 → 待质检 允许（v1.1 合并路径）")
    void testNormalDeliverToQc() {
        assertDoesNotThrow(() ->
                TaskStatusTransition.validateNormal("待交付", "待质检"));
    }

    @Test
    @DisplayName("正常路径：已完成 → 待泡药 非法（跨度太大）")
    void testNormalCompletedToSoakIllegal() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                TaskStatusTransition.validateNormal("已完成", "待泡药"));
        assertTrue(ex.getMessage().contains("非法状态转换"));
    }

    @Test
    @DisplayName("正常路径：待贴标 → 待交接 非法（v1.1 已合并）")
    void testNormalLabelToHandoverIllegal() {
        assertThrows(IllegalStateException.class, () ->
                TaskStatusTransition.validateNormal("待贴标", "待交接"));
    }

    @Test
    @DisplayName("回退路径：煎药中 → 待煎药 允许")
    void testRollbackDecoctingToWaitDecoct() {
        assertDoesNotThrow(() ->
                TaskStatusTransition.validateRollback("煎药中", "待煎药"));
    }

    @Test
    @DisplayName("回退路径：待交付 → 包装中 允许（v1.1 新增）")
    void testRollbackDeliverToWrapping() {
        assertDoesNotThrow(() ->
                TaskStatusTransition.validateRollback("待交付", "包装中"));
    }

    @Test
    @DisplayName("回退路径：已完成 → 待质检 允许（召回）")
    void testRollbackCompletedToQc() {
        assertDoesNotThrow(() ->
                TaskStatusTransition.validateRollback("已完成", "待质检"));
    }

    @Test
    @DisplayName("回退路径：待质检 → 待泡药 非法（跨度太大）")
    void testRollbackQcToSoakIllegal() {
        assertThrows(IllegalStateException.class, () ->
                TaskStatusTransition.validateRollback("待质检", "待泡药"));
    }

    @Test
    @DisplayName("状态不变：任意状态 → 自身 允许")
    void testSameStatusAllowed() {
        assertDoesNotThrow(() ->
                TaskStatusTransition.validateNormal("煎药中", "煎药中"));
    }

    @Test
    @DisplayName("空值校验：null 状态抛 IllegalArgumentException")
    void testNullStatusThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                TaskStatusTransition.validateNormal(null, "煎药中"));
    }

    @Test
    @DisplayName("查询正常目标：包装中 包含 待贴标/待交付/待质检")
    void testGetNormalTargetsFromWrapping() {
        List<String> targets = TaskStatusTransition.getNormalTargets("包装中");
        assertTrue(targets.contains("待贴标"));
        assertTrue(targets.contains("待交付"));
        assertTrue(targets.contains("待质检"));
    }

    @Test
    @DisplayName("查询回退目标：待交付 包含 包装中")
    void testGetRollbackTargetsFromDeliver() {
        List<String> targets = TaskStatusTransition.getRollbackTargets("待交付");
        assertTrue(targets.contains("包装中"));
    }

    @Test
    @DisplayName("查询未知状态返回空列表")
    void testGetTargetsForUnknownStatus() {
        assertTrue(TaskStatusTransition.getNormalTargets("不存在").isEmpty());
    }
}
