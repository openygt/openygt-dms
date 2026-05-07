package cn.org.openygt.common.enums;

/**
 * 发药/配送记录状态枚举
 */
public enum DeliveryStatus {
    PENDING("待发药", "处方待发药"),
    DELIVERED("已发药", "处方已发药"),
    PARTIAL("部分发药", "处方部分发药");

    private final String label;
    private final String code;
    private String description;

    DeliveryStatus(String label, String description) {
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

    public static DeliveryStatus fromCode(String code) {
        if (code == null) return null;
        for (DeliveryStatus s : values()) {
            if (s.code.equals(code)) return s;
        }
        return null;
    }

    public static DeliveryStatus fromLabel(String label) {
        if (label == null) return null;
        for (DeliveryStatus s : values()) {
            if (s.label.equals(label)) return s;
        }
        return null;
    }
}
