package cn.org.openygt.common.enums;

public enum DeviceStatus {
    IDLE("空闲"),
    RUNNING("运行中"),
    FAULT("故障"),
    OFFLINE("离线"),
    MAINTENANCE("维护中");

    private final String label;

    DeviceStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
