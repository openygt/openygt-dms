package cn.org.openygt.common.enums;

/**
 * 设备业务状态枚举（用于 eq_device.status）
 * <p>
 * 与 {@link DeviceStatus}（设备运行状态：空闲/运行中/预留）区分
 * </p>
 */
public enum DeviceBizStatus {
    ONLINE("在线", "设备在线，可正常接收指令"),
    OFFLINE("离线", "设备离线，无法通信"),
    FAULT("故障", "设备发生故障，需维修"),
    MAINTENANCE("维护中", "设备正在维护");

    private final String label;
    private final String code;
    private String description;

    DeviceBizStatus(String label, String description) {
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

    public static DeviceBizStatus fromCode(String code) {
        if (code == null) return null;
        for (DeviceBizStatus s : values()) {
            if (s.code.equals(code)) return s;
        }
        return null;
    }

    public static DeviceBizStatus fromLabel(String label) {
        if (label == null) return null;
        for (DeviceBizStatus s : values()) {
            if (s.label.equals(label)) return s;
        }
        return null;
    }
}
