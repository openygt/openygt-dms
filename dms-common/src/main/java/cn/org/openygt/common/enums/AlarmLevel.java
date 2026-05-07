package cn.org.openygt.common.enums;

/**
 * 告警级别枚举
 */
public enum AlarmLevel {
    LOW("低", "低级别告警"),
    MEDIUM("中", "中级别告警"),
    HIGH("高", "高级别告警"),
    CRITICAL("紧急", "紧急告警");

    private final String label;
    private final String code;
    private String description;

    AlarmLevel(String label, String description) {
        this.label = label;
        this.code = name();
        this.description = description;
    }

    public String getLabel() {
        return label;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static AlarmLevel fromCode(String code) {
        if (code == null) return null;
        for (AlarmLevel s : values()) {
            if (s.code.equals(code)) return s;
        }
        return null;
    }
}
