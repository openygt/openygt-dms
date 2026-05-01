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
        NORMAL_PATH.put("包装中", Arrays.asList("待贴标", "待交付", "待质检")); // 兼容旧版 + 新版合并交付
        NORMAL_PATH.put("待贴标", Arrays.asList("待交付", "待质检"));
        NORMAL_PATH.put("待交付", Arrays.asList("待质检"));
        NORMAL_PATH.put("待质检", Arrays.asList("待交接", "待煎药", "已报废")); // 通过/返工/报废
        NORMAL_PATH.put("待交接", Arrays.asList("已完成", "已部分完成"));
        NORMAL_PATH.put("已部分完成", Arrays.asList("已完成"));
    }

    // 回退路径（需审批）
    private static final Map<String, List<String>> ROLLBACK_PATH = new HashMap<>();
    static {
        ROLLBACK_PATH.put("泡药中", Arrays.asList("待泡药"));
        ROLLBACK_PATH.put("煎药中", Arrays.asList("待煎药"));
        ROLLBACK_PATH.put("出液中", Arrays.asList("待出液"));
        ROLLBACK_PATH.put("包装中", Arrays.asList("待包装"));
        ROLLBACK_PATH.put("待质检", Arrays.asList("待煎药")); // 质检返工
        ROLLBACK_PATH.put("待交付", Arrays.asList("包装中")); // 退回包装
        ROLLBACK_PATH.put("待交接", Arrays.asList("待质检")); // 退回质检
        ROLLBACK_PATH.put("已完成", Arrays.asList("待质检")); // 召回
        ROLLBACK_PATH.put("已部分完成", Arrays.asList("待质检"));
    }

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
}
