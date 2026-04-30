package cn.org.openygt.equipment.service.impl;

import cn.org.openygt.equipment.entity.EqDeviceMqttConfig;
import cn.org.openygt.equipment.mapper.EqDeviceMqttConfigMapper;
import cn.org.openygt.equipment.service.EqDeviceMqttConfigService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class EqDeviceMqttConfigServiceImpl implements EqDeviceMqttConfigService {

    private final EqDeviceMqttConfigMapper mqttConfigMapper;

    @Override
    public EqDeviceMqttConfig getByDeviceCode(String deviceCode) {
        return mqttConfigMapper.findByDeviceCode(deviceCode);
    }

    @Override
    @Transactional
    public EqDeviceMqttConfig createOrUpdate(String deviceCode, EqDeviceMqttConfig config) {
        EqDeviceMqttConfig existing = mqttConfigMapper.findByDeviceCode(deviceCode);
        if (existing != null) {
            config.setId(existing.getId());
            config.setUpdatedAt(LocalDateTime.now());
            mqttConfigMapper.updateById(config);
            return config;
        } else {
            config.setDeviceCode(deviceCode);
            config.setCreatedAt(LocalDateTime.now());
            config.setUpdatedAt(LocalDateTime.now());
            mqttConfigMapper.insert(config);
            return config;
        }
    }

    @Override
    @Transactional
    public void deleteByDeviceCode(String deviceCode) {
        EqDeviceMqttConfig existing = mqttConfigMapper.findByDeviceCode(deviceCode);
        if (existing != null) {
            mqttConfigMapper.deleteById(existing.getId());
        }
    }
}
