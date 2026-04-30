package cn.org.openygt.equipment.service;

import cn.org.openygt.equipment.entity.EqDeviceStatus;

import java.util.List;

public interface EqDeviceStatusService {
    EqDeviceStatus getLatestByDeviceCode(String deviceCode);
    List<EqDeviceStatus> getHistory(String deviceCode, String startTime, String endTime);
    void saveSnapshot(EqDeviceStatus snapshot);
}
