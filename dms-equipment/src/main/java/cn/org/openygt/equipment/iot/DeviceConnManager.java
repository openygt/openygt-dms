package cn.org.openygt.equipment.iot;

import java.util.List;

public interface DeviceConnManager {

    void register(DeviceConnInfo info);

    void heartbeat(String deviceCode);

    void unregister(String deviceCode);

    DeviceConnInfo get(String deviceCode);

    List<DeviceConnInfo> listOnline();

    void updateStatus(String deviceCode, String status);

    void updateLastHeartbeat(String deviceCode);
}
