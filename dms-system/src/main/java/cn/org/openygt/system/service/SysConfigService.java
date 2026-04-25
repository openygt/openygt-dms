package cn.org.openygt.system.service;

import cn.org.openygt.system.entity.SysConfig;
import com.baomidou.mybatisplus.core.metadata.IPage;

public interface SysConfigService {

    SysConfig create(SysConfig config);

    SysConfig update(Long id, SysConfig config);

    SysConfig getById(Long id);

    SysConfig getByKey(String configKey);

    String getStringValue(String configKey, String defaultValue);

    Integer getIntValue(String configKey, Integer defaultValue);

    IPage<SysConfig> list(String keyword, int page, int size);

    void delete(Long id);
}
