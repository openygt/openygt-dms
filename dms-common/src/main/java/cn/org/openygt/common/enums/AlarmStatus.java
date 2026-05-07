package cn.org.openygt.common.enums;

/**
 * 告警记录状态枚举（eq_device_alarm.status，数值型）
 * <p>
 * 数据库实测值：0, 1
 * </p>
 */
public enum AlarmStatus {
    UNHANDLED(0, "未处理", "告警未处理"),
    HANDLED(1, "已处理", "告警已处理");

    private final int value;
    private final String label;
    private final String description;

    AlarmStatus(int value, String label, String description) {
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

    public static AlarmStatus fromValue(int value) {
        for (AlarmStatus s : values()) {
            if (s.value == value) return s;
        }
        return null;
    }
}
