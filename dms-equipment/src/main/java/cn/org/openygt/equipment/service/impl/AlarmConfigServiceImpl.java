package cn.org.openygt.equipment.service.impl;

import cn.org.openygt.equipment.entity.AlarmConfig;
import cn.org.openygt.equipment.mapper.AlarmConfigMapper;
import cn.org.openygt.equipment.service.AlarmConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlarmConfigServiceImpl implements AlarmConfigService {

    private final AlarmConfigMapper configMapper;

    @Override
    public List<AlarmConfig> getAllConfigs() {
        return configMapper.selectList(null);
    }

    @Override
    public AlarmConfig createConfig(AlarmConfig config) {
        configMapper.insert(config);
        return config;
    }

    @Override
    public AlarmConfig updateConfig(Long id, AlarmConfig config) {
        config.setId(id);
        configMapper.updateById(config);
        return config;
    }

    @Override
    public void deleteConfig(Long id) {
        configMapper.deleteById(id);
    }

    @Override
    public List<AlarmConfig> getEnabledConfigs(String alarmType) {
        return configMapper.findEnabledByType(alarmType);
    }
}
