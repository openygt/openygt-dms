package cn.org.openygt.equipment.service.impl;

import cn.org.openygt.equipment.entity.EqDeviceStatus;
import cn.org.openygt.equipment.mapper.EqDeviceStatusMapper;
import cn.org.openygt.equipment.service.EqDeviceStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EqDeviceStatusServiceImpl implements EqDeviceStatusService {

    private final EqDeviceStatusMapper statusMapper;

    @Override
    public EqDeviceStatus getLatestByDeviceCode(String deviceCode) {
        return statusMapper.findLatestByDeviceCode(deviceCode);
    }

    @Override
    public List<EqDeviceStatus> getHistory(String deviceCode, String startTime, String endTime) {
        return statusMapper.findByDeviceCodeAndTimeRange(deviceCode, startTime, endTime);
    }

    @Override
    public void saveSnapshot(EqDeviceStatus snapshot) {
        snapshot.setSnapshotTime(LocalDateTime.now());
        snapshot.setCreatedAt(LocalDateTime.now());
        snapshot.setUpdatedAt(LocalDateTime.now());
        statusMapper.insert(snapshot);
        log.debug("设备[{}]状态快照已保存: {}", snapshot.getDeviceCode(), snapshot.getDetailStatus());
    }
}
