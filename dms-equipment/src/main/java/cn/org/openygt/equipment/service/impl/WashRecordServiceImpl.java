package cn.org.openygt.equipment.service.impl;

import cn.org.openygt.equipment.entity.WashRecord;
import cn.org.openygt.equipment.entity.WashStandard;
import cn.org.openygt.equipment.mapper.WashRecordMapper;
import cn.org.openygt.equipment.service.WashRecordService;
import cn.org.openygt.equipment.service.WashStandardService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class WashRecordServiceImpl extends ServiceImpl<WashRecordMapper, WashRecord>
        implements WashRecordService {

    private final WashRecordMapper washRecordMapper;
    private final WashStandardService washStandardService;

    @Override
    @Transactional
    public WashRecord startWash(Long deviceId, Long taskId, Long operatorId) {
        // 简化实现：实际应从设备/任务推断清洗类型
        WashRecord record = new WashRecord();
        record.setDeviceId(deviceId);
        record.setTaskId(taskId);
        record.setOperatorId(operatorId);
        record.setStartTime(LocalDateTime.now());
        // 默认常规清洗，实际应由任务是否含毒性药材决定
        WashStandard standard = washStandardService.getStandard(1, 1);
        if (standard != null) {
            record.setWashType(standard.getWashType());
            record.setStandardDuration(standard.getStandardDuration());
        }
        washRecordMapper.insert(record);
        return record;
    }

    @Override
    @Transactional
    public WashRecord completeWash(Long washRecordId, Long operatorId, Integer actualDurationMinutes) {
        WashRecord record = washRecordMapper.selectById(washRecordId);
        if (record == null) {
            throw new IllegalArgumentException("清洗记录不存在");
        }
        record.setEndTime(LocalDateTime.now());
        record.setOperatorId(operatorId);

        int duration = actualDurationMinutes != null ? actualDurationMinutes :
                (int) ChronoUnit.MINUTES.between(record.getStartTime(), record.getEndTime());
        record.setDurationMin(duration);

        boolean passed = record.getStandardDuration() != null && duration >= record.getStandardDuration();
        record.setResult(passed ? 1 : 0);
        washRecordMapper.updateById(record);

        if (!passed) {
            throw new IllegalStateException("WASH_DURATION_INSUFFICIENT: 清洗时长不足，标准"
                    + record.getStandardDuration() + "分钟，实际" + duration + "分钟");
        }
        return record;
    }
}
