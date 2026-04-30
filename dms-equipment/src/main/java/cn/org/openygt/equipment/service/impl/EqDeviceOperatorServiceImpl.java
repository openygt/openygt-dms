package cn.org.openygt.equipment.service.impl;

import cn.org.openygt.equipment.entity.EqDeviceOperator;
import cn.org.openygt.equipment.mapper.EqDeviceOperatorMapper;
import cn.org.openygt.equipment.service.EqDeviceOperatorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EqDeviceOperatorServiceImpl implements EqDeviceOperatorService {

    private final EqDeviceOperatorMapper operatorMapper;

    @Override
    public EqDeviceOperator getCurrentByDeviceCode(String deviceCode) {
        return operatorMapper.findCurrentByDeviceCode(deviceCode);
    }

    @Override
    public List<EqDeviceOperator> getHistoryByDeviceCode(String deviceCode) {
        return operatorMapper.findByDeviceCode(deviceCode);
    }

    @Override
    @Transactional
    public EqDeviceOperator shiftHandover(String deviceCode, Long newOperatorId, String newOperatorName) {
        // 1. 结束当前班次
        operatorMapper.clearCurrentByDeviceCode(deviceCode);

        // 2. 创建新班次记录
        EqDeviceOperator record = new EqDeviceOperator();
        record.setDeviceCode(deviceCode);
        record.setOperatorId(newOperatorId);
        record.setOperatorName(newOperatorName);
        record.setShiftStartTime(LocalDateTime.now());
        record.setIsCurrent(1);
        record.setCreatedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());
        operatorMapper.insert(record);

        log.info("设备[{}]换班完成: {} -> {}", deviceCode,
                operatorMapper.findCurrentByDeviceCode(deviceCode) != null ? "上一班" : "无",
                newOperatorName);
        return record;
    }

    @Override
    @Transactional
    public void endShift(String deviceCode) {
        EqDeviceOperator current = operatorMapper.findCurrentByDeviceCode(deviceCode);
        if (current != null) {
            current.setShiftEndTime(LocalDateTime.now());
            current.setIsCurrent(0);
            current.setUpdatedAt(LocalDateTime.now());
            operatorMapper.updateById(current);
            log.info("设备[{}]班次结束", deviceCode);
        }
    }
}
