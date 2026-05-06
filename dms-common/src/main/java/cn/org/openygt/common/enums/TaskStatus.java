package cn.org.openygt.common.enums;

public enum TaskStatus {
    WAIT_SOAK("待泡药"),
    SOAKING("泡药中"),
    WAIT_DECOCT("待煎药"),
    DECOCTING("煎药中"),
    WAIT_POUR("待出液"),
    POURING("出液中"),
    WAIT_WRAP("待包装"),
    WRAPPING("包装中"),
    WAIT_LABEL("待贴标"),
    WAIT_QC("待质检"),
    STORED("已暂存"),
    WAIT_HANDOVER("待交接"),
    SECOND_JUDGEMENT("待二次判定"),
    SUSPENDED("已挂起"),
    COMPLETED("已完成"),
    PARTIAL_COMPLETED("已部分完成"),
    SCRAPPED("已报废");

    private final String label;

    TaskStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /**
     * 判断是否为运行中状态（可被挂起）
     */
    public static boolean isSuspendable(String status) {
        return status != null && SUSPENDABLE_SET.contains(status);
    }

    private static final java.util.Set<String> SUSPENDABLE_SET = java.util.Collections.unmodifiableSet(
        new java.util.HashSet<>(java.util.Arrays.asList(
            "待泡药", "泡药中", "待煎药", "煎药中", "待出液", "出液中",
            "待包装", "包装中", "待质检", "已暂存", "待交接"
        ))
    );

    public static TaskStatus fromLabel(String label) {
        if (label == null) return null;
        for (TaskStatus status : values()) {
            if (status.label.equals(label)) {
                return status;
            }
        }
        return null;
    }
}
