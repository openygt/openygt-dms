package cn.org.openygt.system.service.impl;

import cn.org.openygt.system.cache.SysConfigCache;
import cn.org.openygt.system.entity.SysConfig;
import cn.org.openygt.system.mapper.SysConfigMapper;
import cn.org.openygt.system.service.SysConfigService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SysConfigServiceImpl implements SysConfigService, cn.org.openygt.common.service.SysConfigService {

    private final SysConfigMapper configMapper;
    private final SysConfigCache configCache;

    @Override
    @Transactional
    public SysConfig create(SysConfig config) {
        configMapper.insert(config);
        configCache.invalidate(config.getConfigKey());
        return config;
    }

    @Override
    @Transactional
    public SysConfig update(Long id, SysConfig config) {
        SysConfig existing = configMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("配置不存在: " + id);
        }
        config.setId(id);
        configMapper.updateById(config);
        configCache.invalidate(existing.getConfigKey());
        if (config.getConfigKey() != null && !config.getConfigKey().equals(existing.getConfigKey())) {
            configCache.invalidate(config.getConfigKey());
        }
        return configMapper.selectById(id);
    }

    @Override
    public SysConfig getById(Long id) {
        SysConfig config = configMapper.selectById(id);
        if (config == null) {
            throw new IllegalArgumentException("配置不存在: " + id);
        }
        return config;
    }

    @Override
    public SysConfig getByKey(String configKey) {
        return configCache.get(configKey, key -> {
            LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysConfig::getConfigKey, key);
            return configMapper.selectOne(wrapper);
        });
    }

    @Override
    public String getStringValue(String configKey, String defaultValue) {
        SysConfig config = getByKey(configKey);
        return (config != null && config.getConfigValue() != null) ? config.getConfigValue() : defaultValue;
    }

    @Override
    public Integer getIntValue(String configKey, Integer defaultValue) {
        String value = getStringValue(configKey, null);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            log.warn("配置项 {} 的值 {} 无法解析为整数", configKey, value);
            return defaultValue;
        }
    }

    @Override
    public IPage<SysConfig> list(String keyword, int page, int size) {
        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(SysConfig::getConfigKey, keyword)
                   .or()
                   .like(SysConfig::getDescription, keyword);
        }
        wrapper.orderByDesc(SysConfig::getCreatedAt);
        return configMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        SysConfig existing = configMapper.selectById(id);
        if (existing != null) {
            configCache.invalidate(existing.getConfigKey());
        }
        configMapper.deleteById(id);
    }
}
