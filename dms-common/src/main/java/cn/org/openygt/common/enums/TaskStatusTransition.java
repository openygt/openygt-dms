package cn.org.openygt.common.enums;

import java.util.*;

/**
 * 任务状态转换守卫。
 * <p>定义合法的状态流转路径，禁止任意跳转。</p>
 * <p>所有状态编码统一使用英文 UPPER_SNAKE_CASE。</p>
 */
public class TaskStatusTransition {

    // 正常推进路径（不可逆）
    private static final Map<String, List<String>> NORMAL_PATH = new HashMap<>();
    static {
        NORMAL_PATH.put(TaskStatus.WAIT_SOAK.getCode(), Arrays.asList(TaskStatus.SOAKING.getCode()));
        NORMAL_PATH.put(TaskStatus.SOAKING.getCode(), Arrays.asList(TaskStatus.WAIT_DECOCT.getCode()));
        NORMAL_PATH.put(TaskStatus.WAIT_DECOCT.getCode(), Arrays.asList(TaskStatus.DECOCTING.getCode()));
        NORMAL_PATH.put(TaskStatus.DECOCTING.getCode(), Arrays.asList(TaskStatus.WAIT_POUR.getCode()));
        NORMAL_PATH.put(TaskStatus.WAIT_POUR.getCode(), Arrays.asList(TaskStatus.POURING.getCode()));
        NORMAL_PATH.put(TaskStatus.POURING.getCode(), Arrays.asList(TaskStatus.WAIT_WRAP.getCode()));
        NORMAL_PATH.put(TaskStatus.WAIT_WRAP.getCode(), Arrays.asList(TaskStatus.WRAPPING.getCode()));
        NORMAL_PATH.put(TaskStatus.WRAPPING.getCode(), Arrays.asList(TaskStatus.WAIT_QC.getCode()));
        NORMAL_PATH.put(TaskStatus.WAIT_LABEL.getCode(), Arrays.asList(TaskStatus.WAIT_QC.getCode()));
        NORMAL_PATH.put(TaskStatus.WAIT_QC.getCode(), Arrays.asList(
            TaskStatus.STORED.getCode(), TaskStatus.WAIT_DECOCT.getCode(), TaskStatus.SCRAPPED.getCode(), TaskStatus.SECOND_JUDGEMENT.getCode()));
        NORMAL_PATH.put(TaskStatus.STORED.getCode(), Arrays.asList(TaskStatus.WAIT_HANDOVER.getCode()));
        NORMAL_PATH.put(TaskStatus.WAIT_HANDOVER.getCode(), Arrays.asList(TaskStatus.COMPLETED.getCode(), TaskStatus.PARTIAL_COMPLETED.getCode()));
        NORMAL_PATH.put(TaskStatus.PARTIAL_COMPLETED.getCode(), Arrays.asList(TaskStatus.COMPLETED.getCode()));
        NORMAL_PATH.put(TaskStatus.SECOND_JUDGEMENT.getCode(), Arrays.asList(
            TaskStatus.STORED.getCode(), TaskStatus.SCRAPPED.getCode(), TaskStatus.WAIT_DECOCT.getCode()));
    }

    // 回退路径（需审批）
    private static final Map<String, List<String>> ROLLBACK_PATH = new HashMap<>();
    static {
        ROLLBACK_PATH.put(TaskStatus.SOAKING.getCode(), Arrays.asList(TaskStatus.WAIT_SOAK.getCode()));
        ROLLBACK_PATH.put(TaskStatus.DECOCTING.getCode(), Arrays.asList(TaskStatus.WAIT_DECOCT.getCode()));
        ROLLBACK_PATH.put(TaskStatus.POURING.getCode(), Arrays.asList(TaskStatus.WAIT_POUR.getCode()));
        ROLLBACK_PATH.put(TaskStatus.WRAPPING.getCode(), Arrays.asList(TaskStatus.WAIT_WRAP.getCode()));
        ROLLBACK_PATH.put(TaskStatus.WAIT_QC.getCode(), Arrays.asList(TaskStatus.WAIT_DECOCT.getCode()));
        ROLLBACK_PATH.put(TaskStatus.STORED.getCode(), Arrays.asList(TaskStatus.WAIT_QC.getCode()));
        ROLLBACK_PATH.put(TaskStatus.WAIT_HANDOVER.getCode(), Arrays.asList(TaskStatus.STORED.getCode()));
        ROLLBACK_PATH.put(TaskStatus.COMPLETED.getCode(), Arrays.asList(TaskStatus.STORED.getCode()));
        ROLLBACK_PATH.put(TaskStatus.PARTIAL_COMPLETED.getCode(), Arrays.asList(TaskStatus.STORED.getCode()));
        ROLLBACK_PATH.put(TaskStatus.SECOND_JUDGEMENT.getCode(), Arrays.asList(TaskStatus.WAIT_QC.getCode()));
    }

    // 挂起路径：运行中状态 → 已挂起
    private static final Set<String> RUNNING_STATES = Collections.unmodifiableSet(
        new HashSet<>(Arrays.asList(
            TaskStatus.WAIT_SOAK.getCode(), TaskStatus.SOAKING.getCode(),
            TaskStatus.WAIT_DECOCT.getCode(), TaskStatus.DECOCTING.getCode(),
            TaskStatus.WAIT_POUR.getCode(), TaskStatus.POURING.getCode(),
            TaskStatus.WAIT_WRAP.getCode(), TaskStatus.WRAPPING.getCode(),
            TaskStatus.WAIT_LABEL.getCode(), TaskStatus.WAIT_QC.getCode(),
            TaskStatus.STORED.getCode(), TaskStatus.WAIT_HANDOVER.getCode()
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
        if (!TaskStatus.SUSPENDED.getCode().equals(from)) {
            throw new IllegalStateException("非法恢复操作：当前状态不是已挂起");
        }
        if (suspendedFrom == null || suspendedFrom.isEmpty()) {
            throw new IllegalStateException("恢复失败：未记录挂起前状态");
        }
    }
}
