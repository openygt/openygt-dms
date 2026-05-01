package cn.org.openygt.system.service.impl;

import cn.org.openygt.system.entity.InterfaceConfig;
import cn.org.openygt.system.mapper.InterfaceConfigMapper;
import cn.org.openygt.system.service.InterfaceConfigService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InterfaceConfigServiceImpl extends ServiceImpl<InterfaceConfigMapper, InterfaceConfig> implements InterfaceConfigService {

    @Override
    public Page<InterfaceConfig> listConfigs(String keyword, String interfaceType, int page, int size) {
        LambdaQueryWrapper<InterfaceConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InterfaceConfig::getDeleted, 0);
        wrapper.orderByDesc(InterfaceConfig::getCreatedAt);
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(InterfaceConfig::getInterfaceCode, keyword)
                    .or()
                    .like(InterfaceConfig::getInterfaceName, keyword));
        }
        if (interfaceType != null && !interfaceType.isEmpty()) {
            wrapper.eq(InterfaceConfig::getInterfaceType, interfaceType);
        }
        return baseMapper.selectPage(new Page<>(page, size), wrapper);
    }
}
