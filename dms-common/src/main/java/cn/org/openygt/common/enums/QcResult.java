package cn.org.openygt.common.enums;

/**
 * 质检结果枚举
 */
public enum QcResult {
    PASS("合格", "质检合格"),
    FAIL("不合格", "质检不合格"),
    WAIVE("让步放行", "质检让步放行");

    private final String label;
    private final String code;
    private String description;

    QcResult(String label, String description) {
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

    public static QcResult fromCode(String code) {
        if (code == null) return null;
        for (QcResult s : values()) {
            if (s.code.equals(code)) return s;
        }
        return null;
    }
}
