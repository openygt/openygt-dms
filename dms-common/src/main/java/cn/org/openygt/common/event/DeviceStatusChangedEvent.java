package cn.org.openygt.common.event;

import org.springframework.context.ApplicationEvent;

/**
 * 设备状态变化事件。
 * 当设备模拟器上报的状态发生变化时，由设备模块发布，生产模块监听并推进任务步骤。
 */
public class DeviceStatusChangedEvent extends ApplicationEvent {

    private final Long deviceId;
    private final String deviceCode;
    private final String oldStatus;
    private final String newStatus;

    public DeviceStatusChangedEvent(Object source, Long deviceId, String deviceCode, String oldStatus, String newStatus) {
        super(source);
        this.deviceId = deviceId;
        this.deviceCode = deviceCode;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public String getDeviceCode() {
        return deviceCode;
    }

    public String getOldStatus() {
        return oldStatus;
    }

    public String getNewStatus() {
        return newStatus;
    }
}
