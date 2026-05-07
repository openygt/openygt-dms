package cn.org.openygt.common.enums;

/**
 * 任务状态枚举
 * <p>
 * code: 数据库存储值（UPPER_SNAKE_CASE）<br>
 * label: 中文展示标签<br>
 * description: 状态说明
 * </p>
 */
public enum TaskStatus {
    WAIT_SOAK("待泡药", "处方已接收，等待泡药"),
    SOAKING("泡药中", "正在进行泡药工序"),
    WAIT_DECOCT("待煎药", "泡药完成，等待煎药"),
    DECOCTING("煎药中", "正在进行煎药工序"),
    WAIT_POUR("待出液", "煎药完成，等待出液"),
    POURING("出液中", "正在进行出液工序"),
    WAIT_WRAP("待包装", "出液完成，等待包装"),
    WRAPPING("包装中", "正在进行包装工序"),
    WAIT_LABEL("待贴标", "包装完成，等待贴标"),
    WAIT_QC("待质检", "贴标完成，等待质检"),
    STORED("已暂存", "任务已暂存"),
    WAIT_HANDOVER("待交接", "质检完成，等待交接"),
    SECOND_JUDGEMENT("待二次判定", "任务需二次判定"),
    SUSPENDED("已挂起", "任务已挂起"),
    COMPLETED("已完成", "任务已完成"),
    PARTIAL_COMPLETED("已部分完成", "任务已部分完成"),
    SCRAPPED("已报废", "任务已报废"),
    REWORK("返工中", "任务返工处理中"),
    CANCELLED("已取消", "任务已取消");

    private final String label;
    private final String code;
    private String description;

    TaskStatus(String label) {
        this.label = label;
        this.code = name();
    }

    TaskStatus(String label, String description) {
        this(label);
        this.description = description;
    }

    public String getLabel() {
        return label;
    }

    /**
     * 数据库存储编码（UPPER_SNAKE_CASE）
     */
    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据编码查找枚举（用于数据库值反序列化）
     */
    public static TaskStatus fromCode(String code) {
        if (code == null) return null;
        for (TaskStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }

    /**
     * 根据中文标签查找枚举（兼容旧代码）
     */
    public static TaskStatus fromLabel(String label) {
        if (label == null) return null;
        for (TaskStatus status : values()) {
            if (status.label.equals(label)) {
                return status;
            }
        }
        return null;
    }

    /**
     * 判断是否为运行中状态（可被挂起）
     */
    public static boolean isSuspendable(String status) {
        return status != null && SUSPENDABLE_SET.contains(status);
    }

    private static final java.util.Set<String> SUSPENDABLE_SET = java.util.Collections.unmodifiableSet(
        new java.util.HashSet<>(java.util.Arrays.asList(
            WAIT_SOAK.label, SOAKING.label, WAIT_DECOCT.label, DECOCTING.label,
            WAIT_POUR.label, POURING.label, WAIT_WRAP.label, WRAPPING.label,
            WAIT_LABEL.label, WAIT_QC.label, STORED.label, WAIT_HANDOVER.label
        ))
    );
}
