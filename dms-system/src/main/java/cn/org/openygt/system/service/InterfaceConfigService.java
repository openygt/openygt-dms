package cn.org.openygt.system.service;

import cn.org.openygt.system.entity.InterfaceConfig;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

public interface InterfaceConfigService extends IService<InterfaceConfig> {

    Page<InterfaceConfig> listConfigs(String keyword, String interfaceType, int page, int size);
}
