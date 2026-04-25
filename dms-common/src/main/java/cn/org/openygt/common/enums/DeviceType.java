package cn.org.openygt.common.enums;

public enum DeviceType {
    COOKING_MACHINE(1, "煎药机"),
    PACKING_MACHINE(2, "包装机"),
    PRINTER(4, "打印机");

    private final int code;
    private final String label;

    DeviceType(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public int getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }
}
