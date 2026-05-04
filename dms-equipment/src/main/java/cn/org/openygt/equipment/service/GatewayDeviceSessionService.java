package cn.org.openygt.equipment.service;

import java.util.List;
import java.util.Map;

public interface GatewayDeviceSessionService {

    List<Map<String, Object>> listOnlineManagedDevices();
}
