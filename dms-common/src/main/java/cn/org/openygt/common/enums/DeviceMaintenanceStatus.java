package cn.org.openygt.common.enums;

/**
 * 设备维护记录状态枚举（eq_device_maintenance.status，数值型）
 * <p>
 * 数据库实测值：0, 1, 2
 * </p>
 */
public enum DeviceMaintenanceStatus {
    PENDING(0, "待处理", "维护申请待处理"),
    COMPLETED(1, "已完成", "维护已完成"),
    CANCELLED(2, "已取消", "维护已取消");

    private final int value;
    private final String label;
    private final String description;

    DeviceMaintenanceStatus(int value, String label, String description) {
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

    public static DeviceMaintenanceStatus fromValue(int value) {
        for (DeviceMaintenanceStatus s : values()) {
            if (s.value == value) return s;
        }
        return null;
    }
}
