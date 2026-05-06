package cn.org.openygt.equipment.service.impl;

import cn.org.openygt.equipment.entity.EqDeviceGroupRule;
import cn.org.openygt.equipment.mapper.EqDeviceGroupRuleMapper;
import cn.org.openygt.equipment.service.EqDeviceGroupRuleService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EqDeviceGroupRuleServiceImpl extends ServiceImpl<EqDeviceGroupRuleMapper, EqDeviceGroupRule> implements EqDeviceGroupRuleService {
}
