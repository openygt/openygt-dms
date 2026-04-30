package cn.org.openygt.equipment.service;

import cn.org.openygt.equipment.entity.AlarmConfig;

import java.util.List;

public interface AlarmConfigService {

    List<AlarmConfig> getAllConfigs();

    AlarmConfig createConfig(AlarmConfig config);

    AlarmConfig updateConfig(Long id, AlarmConfig config);

    void deleteConfig(Long id);

    List<AlarmConfig> getEnabledConfigs(String alarmType);
}
