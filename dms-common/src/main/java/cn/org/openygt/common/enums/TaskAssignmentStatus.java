package cn.org.openygt.common.enums;

/**
 * 任务分配状态枚举（prod_task_assignment.status，数值型）
 */
public enum TaskAssignmentStatus {
    UNASSIGNED(0, "未分配", "任务尚未分配"),
    ASSIGNED(1, "已分配", "任务已分配给员工"),
    IN_PROGRESS(2, "进行中", "任务正在执行"),
    COMPLETED(3, "已完成", "任务已完成"),
    CANCELLED(4, "已取消", "任务已取消");

    private final int value;
    private final String label;
    private final String description;

    TaskAssignmentStatus(int value, String label, String description) {
        this.value = value;
        this.label = label;
        this.description = description;
    }

    public int getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public static TaskAssignmentStatus fromValue(int value) {
        for (TaskAssignmentStatus s : values()) {
            if (s.value == value) return s;
        }
        return null;
    }
}
