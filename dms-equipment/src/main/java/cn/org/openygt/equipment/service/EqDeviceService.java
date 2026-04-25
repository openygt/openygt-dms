package cn.org.openygt.equipment.service;

import cn.org.openygt.equipment.entity.EqDevice;
import com.baomidou.mybatisplus.core.metadata.IPage;

public interface EqDeviceService {

    EqDevice getById(Long id);

    EqDevice getByCode(String deviceCode);

    EqDevice getOrCreate(String deviceCode, int defaultType);

    IPage<EqDevice> list(String keyword, int page, int size);

    EqDevice create(EqDevice device);

    EqDevice update(Long id, EqDevice device);

    void delete(Long id);
}
