package cn.org.openygt.equipment.service;

import cn.org.openygt.equipment.entity.EqDeviceOperator;

import java.util.List;

public interface EqDeviceOperatorService {
    EqDeviceOperator getCurrentByDeviceCode(String deviceCode);
    List<EqDeviceOperator> getHistoryByDeviceCode(String deviceCode);
    EqDeviceOperator shiftHandover(String deviceCode, Long newOperatorId, String newOperatorName);
    void endShift(String deviceCode);
}
