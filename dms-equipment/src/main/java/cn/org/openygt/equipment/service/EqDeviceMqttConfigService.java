package cn.org.openygt.equipment.service;

import cn.org.openygt.equipment.entity.EqDeviceMqttConfig;

public interface EqDeviceMqttConfigService {
    EqDeviceMqttConfig getByDeviceCode(String deviceCode);
    EqDeviceMqttConfig createOrUpdate(String deviceCode, EqDeviceMqttConfig config);
    void deleteByDeviceCode(String deviceCode);
}
