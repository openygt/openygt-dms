package cn.org.openygt.common.enums;

/**
 * 质检结果类型枚举。
 * 统一使用枚举名（PASS/CONCESSION/REWORK/SCRAP）进行 SPI 参数传递和 switch-case 匹配，
 * 禁止使用中文字符串，避免空格、编码等问题导致匹配失败。
 */
public enum InspectionResultType {
    PASS("通过"),
    CONCESSION("让步放行"),
    REWORK("返工"),
    SCRAP("报废");

    private final String label;

    InspectionResultType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
