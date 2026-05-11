package cn.org.openygt.equipment.enums;

/**
 * 设备精细状态枚举（24种）
 * 煎药机: 14种 | 包装机: 7种 | 通用: 3种
 */
public enum DeviceDetailStatus {

    // ===== 通用状态 =====
    IDLE("空闲", "IDLE", 0, "#52C41A"),
    BUSY("执行中", "BUSY", null, "#F5222D"),
    STANDBY("待机", "IDLE", 0, "#52C41A"),
    READY("就绪", "IDLE", 0, "#52C41A"),
    RUNNING("运行中", "BUSY", null, "#1890FF"),

    // ===== 煎药机状态 =====
    SOAKING("浸泡中", "BUSY", 1, "#722ED1"),
    PRE_DECOCTING("预热中", "BUSY", 1, "#FAAD14"),
    FIRST_DECOCTING("一煎中", "BUSY", 1, "#F5222D"),
    ADD_LATE("后下提醒", "BUSY", 1, "#FAAD14"),
    ADD_LATE_REMIND("等待后下", "BUSY", 1, "#FAAD14"),
    SECOND_DECOCTING("二煎中", "BUSY", 1, "#F5222D"),
    DRAINING("出液中", "BUSY", 1, "#1890FF"),
    COOLING("冷却中", "BUSY", 1, "#13C2C2"),

    // ===== 包装机状态 =====
    PACKAGING("包装中", "BUSY", 2, "#F5222D"),
    PACKAGE_COMPLETE("包装完成", "IDLE", 2, "#52C41A"),
    LABELING("贴标中", "BUSY", 2, "#FAAD14"),
    LABEL_COMPLETE("贴标完成", "IDLE", 2, "#52C41A"),

    // ===== 异常状态 =====
    PAUSED("暂停", "MAINTENANCE", null, "#FAAD14"),
    FAULT("故障", "FAULT", null, "#CF1322"),
    OFFLINE("离线", "OFFLINE", null, "#BFBFBF"),

    // ===== 其他 =====
    RESERVED("已预留", "RESERVED", null, "#1890FF"),
    MAINTENANCE("维护中", "MAINTENANCE", null, "#FAAD14"),
    CLEANING("清洗中", "BUSY", 1, "#13C2C2"),
    CALIBRATING("校准中", "BUSY", 1, "#722ED1");

    private final String displayName;
    private final String legacyStatus;
    private final Integer deviceType; // 1=煎药机, 2=包装机, null=通用
    private final String color;

    DeviceDetailStatus(String displayName, String legacyStatus, Integer deviceType, String color) {
        this.displayName = displayName;
        this.legacyStatus = legacyStatus;
        this.deviceType = deviceType;
        this.color = color;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getLegacyStatus() {
        return legacyStatus;
    }

    public Integer getDeviceType() {
        return deviceType;
    }

    public String getColor() {
        return color;
    }

    /**
     * 将精细状态映射为兼容的旧状态
     */
    public static String mapToLegacyStatus(String detailStatus) {
        if (detailStatus == null) return "IDLE";
        try {
            return valueOf(detailStatus).getLegacyStatus();
        } catch (IllegalArgumentException e) {
            return "IDLE";
        }
    }

    /**
     * 获取显示名称
     */
    public static String getDisplayName(String detailStatus) {
        if (detailStatus == null) return "未知";
        try {
            return valueOf(detailStatus).getDisplayName();
        } catch (IllegalArgumentException e) {
            return detailStatus;
        }
    }

    /**
     * 判断是否为运行中状态
     */
    public static boolean isRunning(String detailStatus) {
        if (detailStatus == null) return false;
        try {
            DeviceDetailStatus status = valueOf(detailStatus);
            return "BUSY".equals(status.getLegacyStatus());
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 判断是否为异常状态
     */
    public static boolean isAbnormal(String detailStatus) {
        if (detailStatus == null) return false;
        return detailStatus.equals("FAULT") || detailStatus.equals("OFFLINE");
    }

    /**
     * 获取状态颜色
     */
    public static String getColor(String detailStatus) {
        if (detailStatus == null) return "#52C41A";
        try {
            return valueOf(detailStatus).getColor();
        } catch (IllegalArgumentException e) {
            return "#52C41A";
        }
    }
}
