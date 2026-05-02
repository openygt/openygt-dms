package cn.org.openygt.equipment.service.impl;

import cn.org.openygt.equipment.entity.WashStandard;
import cn.org.openygt.equipment.mapper.WashStandardMapper;
import cn.org.openygt.equipment.service.WashStandardService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WashStandardServiceImpl extends ServiceImpl<WashStandardMapper, WashStandard>
        implements WashStandardService {

    private final WashStandardMapper washStandardMapper;

    @Override
    public WashStandard getStandard(Integer deviceType, Integer washType) {
        return washStandardMapper.selectOne(
                new LambdaQueryWrapper<WashStandard>()
                        .eq(WashStandard::getDeviceType, deviceType)
                        .eq(WashStandard::getWashType, washType)
                        .eq(WashStandard::getIsActive, 1)
        );
    }
}
