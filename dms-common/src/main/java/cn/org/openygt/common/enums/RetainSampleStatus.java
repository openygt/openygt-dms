package cn.org.openygt.common.enums;

/**
 * 留样状态枚举
 */
public enum RetainSampleStatus {
    RETAINED("已留样", "样品已留存"),
    DISPOSED("已处置", "样品已处置"),
    OVERDUE("已过期", "样品已过期"),
    PENDING("待留样", "等待留样");

    private final String label;
    private final String code;
    private String description;

    RetainSampleStatus(String label, String description) {
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

    public static RetainSampleStatus fromCode(String code) {
        if (code == null) return null;
        for (RetainSampleStatus s : values()) {
            if (s.code.equals(code)) return s;
        }
        return null;
    }
}
