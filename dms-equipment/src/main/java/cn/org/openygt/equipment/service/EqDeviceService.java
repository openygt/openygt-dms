package cn.org.openygt.equipment.service;

import cn.org.openygt.equipment.dto.TemperatureLogDTO;
import cn.org.openygt.equipment.entity.EqDevice;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.time.LocalDateTime;

public interface EqDeviceService {

    EqDevice getById(Long id);

    EqDevice getByCode(String deviceCode);

    EqDevice getOrCreate(String deviceCode, int defaultType);

    IPage<EqDevice> list(String keyword, Integer deviceType, String status, int page, int size);

    EqDevice create(EqDevice device);

    EqDevice update(Long id, EqDevice device);

    void delete(Long id);

    /**
     * 分页查询设备温度日志。
     */
    IPage<TemperatureLogDTO> getTemperatureLogs(Long deviceId, LocalDateTime start, LocalDateTime end, int page, int size);
}
