package cn.org.openygt.common.enums;

/**
 * 处方接收状态枚举
 */
public enum PrescriptionReceiveStatus {
    PENDING("待接收", "处方待接收"),
    RECEIVED("已接收", "处方已接收"),
    REJECTED("已拒绝", "处方已拒绝");

    private final String label;
    private final String code;
    private String description;

    PrescriptionReceiveStatus(String label, String description) {
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

    public static PrescriptionReceiveStatus fromCode(String code) {
        if (code == null) return null;
        for (PrescriptionReceiveStatus s : values()) {
            if (s.code.equals(code)) return s;
        }
        return null;
    }

    public static PrescriptionReceiveStatus fromLabel(String label) {
        if (label == null) return null;
        for (PrescriptionReceiveStatus s : values()) {
            if (s.label.equals(label)) return s;
        }
        return null;
    }
}
