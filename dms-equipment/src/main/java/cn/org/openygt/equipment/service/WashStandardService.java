package cn.org.openygt.equipment.service;

import cn.org.openygt.equipment.entity.WashStandard;
import com.baomidou.mybatisplus.extension.service.IService;

public interface WashStandardService extends IService<WashStandard> {

    WashStandard getStandard(Integer deviceType, Integer washType);
}
