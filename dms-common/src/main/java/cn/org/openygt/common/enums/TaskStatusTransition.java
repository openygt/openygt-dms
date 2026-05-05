package cn.org.openygt.common.enums;

import java.util.*;

/**
 * 任务状态转换守卫。
 * <p>定义合法的状态流转路径，禁止任意跳转。</p>
 */
public class TaskStatusTransition {

    // 正常推进路径（不可逆）
    private static final Map<String, List<String>> NORMAL_PATH = new HashMap<>();
    static {
        NORMAL_PATH.put("待泡药", Arrays.asList("泡药中"));
        NORMAL_PATH.put("泡药中", Arrays.asList("待煎药"));
        NORMAL_PATH.put("待煎药", Arrays.asList("煎药中"));
        NORMAL_PATH.put("煎药中", Arrays.asList("待出液"));
        NORMAL_PATH.put("待出液", Arrays.asList("出液中"));
        NORMAL_PATH.put("出液中", Arrays.asList("待包装"));
        NORMAL_PATH.put("待包装", Arrays.asList("包装中"));
        NORMAL_PATH.put("包装中", Arrays.asList("待质检"));           // WAIT_LABEL 已合并，兼容旧版保留待贴标映射
        NORMAL_PATH.put("待贴标", Arrays.asList("待质检"));           // 兼容历史数据
        NORMAL_PATH.put("待质检", Arrays.asList("已暂存", "待煎药", "已报废", "待二次判定")); // PASS/返工/报废/FAIL
        NORMAL_PATH.put("已暂存", Arrays.asList("待交接"));
        NORMAL_PATH.put("待交接", Arrays.asList("已完成", "已部分完成"));
        NORMAL_PATH.put("已部分完成", Arrays.asList("已完成"));
        NORMAL_PATH.put("待二次判定", Arrays.asList("已暂存", "已报废", "待煎药")); // 主任判定：放行/报废/返工
    }

    // 回退路径（需审批）
    private static final Map<String, List<String>> ROLLBACK_PATH = new HashMap<>();
    static {
        ROLLBACK_PATH.put("泡药中", Arrays.asList("待泡药"));
        ROLLBACK_PATH.put("煎药中", Arrays.asList("待煎药"));
        ROLLBACK_PATH.put("出液中", Arrays.asList("待出液"));
        ROLLBACK_PATH.put("包装中", Arrays.asList("待包装"));
        ROLLBACK_PATH.put("待质检", Arrays.asList("待煎药"));       // 返工
        ROLLBACK_PATH.put("已暂存", Arrays.asList("待质检"));       // 退回质检
        ROLLBACK_PATH.put("待交接", Arrays.asList("已暂存"));       // 退回暂存
        ROLLBACK_PATH.put("已完成", Arrays.asList("已暂存"));       // 召回
        ROLLBACK_PATH.put("已部分完成", Arrays.asList("已暂存"));
        ROLLBACK_PATH.put("待二次判定", Arrays.asList("待质检"));   // 退回质检重新判定
    }

    // 挂起路径：运行中状态 → 已挂起
    private static final Set<String> RUNNING_STATES = Collections.unmodifiableSet(
        new HashSet<>(Arrays.asList(
            "待泡药", "泡药中", "待煎药", "煎药中", "待出液", "出液中",
            "待包装", "包装中", "待贴标", "待质检", "已暂存", "待交接"
        ))
    );

    /**
     * 校验状态转换是否合法
     */
    public static void validate(String from, String to, boolean isRollback) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("状态不能为空");
        }
        if (from.equals(to)) {
            return; // 状态不变允许
        }
        List<String> allowed = isRollback ? ROLLBACK_PATH.get(from) : NORMAL_PATH.get(from);
        if (allowed == null || !allowed.contains(to)) {
            throw new IllegalStateException(
                "非法状态转换：" + from + " → " + to +
                (isRollback ? "（回退路径未定义）" : "（正常路径未定义）")
            );
        }
    }

    /**
     * 校验正常推进路径
     */
    public static void validateNormal(String from, String to) {
        validate(from, to, false);
    }

    /**
     * 校验回退路径
     */
    public static void validateRollback(String from, String to) {
        validate(from, to, true);
    }

    /**
     * 获取某状态允许的正常推进目标
     */
    public static List<String> getNormalTargets(String from) {
        return NORMAL_PATH.getOrDefault(from, Collections.emptyList());
    }

    /**
     * 获取某状态允许的回退目标
     */
    public static List<String> getRollbackTargets(String from) {
        return ROLLBACK_PATH.getOrDefault(from, Collections.emptyList());
    }

    /**
     * 校验挂起操作是否合法
     */
    public static void validateSuspend(String from) {
        if (from == null || !RUNNING_STATES.contains(from)) {
            throw new IllegalStateException("非法挂起操作：" + from + " 不是可挂起的运行中状态");
        }
    }

    /**
     * 校验恢复操作是否合法
     */
    public static void validateResume(String from, String suspendedFrom) {
        if (!"已挂起".equals(from)) {
            throw new IllegalStateException("非法恢复操作：当前状态不是已挂起");
        }
        if (suspendedFrom == null || suspendedFrom.isEmpty()) {
            throw new IllegalStateException("恢复失败：未记录挂起前状态");
        }
    }
}
