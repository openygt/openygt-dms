package cn.org.openygt.common.enums;

/**
 * 告警处理状态枚举
 */
public enum AlarmHandleStatus {
    UNHANDLED("未处理", "告警尚未处理"),
    HANDLING("处理中", "告警正在处理"),
    HANDLED("已处理", "告警已处理完毕"),
    IGNORED("已忽略", "告警已忽略");

    private final String label;
    private final String code;
    private String description;

    AlarmHandleStatus(String label, String description) {
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

    public static AlarmHandleStatus fromCode(String code) {
        if (code == null) return null;
        for (AlarmHandleStatus s : values()) {
            if (s.code.equals(code)) return s;
        }
        return null;
    }
}
