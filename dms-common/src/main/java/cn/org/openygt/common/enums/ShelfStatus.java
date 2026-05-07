package cn.org.openygt.common.enums;

/**
 * 货架/库位状态枚举
 */
public enum ShelfStatus {
    AVAILABLE(0, "可用", "库位可用"),
    OCCUPIED(1, "占用", "库位已占用"),
    DISABLED(2, "禁用", "库位已禁用");

    private final int value;
    private final String label;
    private final String description;

    ShelfStatus(int value, String label, String description) {
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

    public static ShelfStatus fromValue(int value) {
        for (ShelfStatus s : values()) {
            if (s.value == value) return s;
        }
        return null;
    }
}
