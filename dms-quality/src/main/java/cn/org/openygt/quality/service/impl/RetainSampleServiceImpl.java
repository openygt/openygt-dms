package cn.org.openygt.quality.service.impl;

import cn.org.openygt.quality.entity.RetainSample;
import cn.org.openygt.quality.mapper.RetainSampleMapper;
import cn.org.openygt.quality.service.RetainSampleService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RetainSampleServiceImpl extends ServiceImpl<RetainSampleMapper, RetainSample>
        implements RetainSampleService {

    private final RetainSampleMapper retainSampleMapper;

    @Override
    @Transactional
    public List<RetainSample> createSamplesForTask(Long taskId, Long prescriptionId, Long operatorId) {
        List<RetainSample> samples = new ArrayList<>();
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = retainSampleMapper.selectCount(
                new LambdaQueryWrapper<RetainSample>().likeRight(RetainSample::getSampleNo, "SMP" + dateStr)
        );

        samples.add(createSample(taskId, prescriptionId, operatorId, 1, 0, dateStr, count + 1));
        samples.add(createSample(taskId, prescriptionId, operatorId, 2, 24, dateStr, count + 2));
        samples.add(createSample(taskId, prescriptionId, operatorId, 3, 72, dateStr, count + 3));

        for (RetainSample sample : samples) {
            retainSampleMapper.insert(sample);
        }
        return samples;
    }

    private RetainSample createSample(Long taskId, Long prescriptionId, Long operatorId,
                                       int sampleType, int durationHours, String dateStr, long seq) {
        RetainSample sample = new RetainSample();
        sample.setTaskId(taskId);
        sample.setPrescriptionId(prescriptionId);
        sample.setSampleNo(String.format("SMP%s%04d", dateStr, seq));
        sample.setSampleType(sampleType);
        sample.setRetainDuration(durationHours);
        sample.setRetainTime(LocalDateTime.now());
        if (durationHours > 0) {
            sample.setExpireTime(LocalDateTime.now().plusHours(durationHours));
        }
        sample.setStatus(1);
        sample.setOperatorId(operatorId);
        return sample;
    }

    @Override
    @Transactional
    public RetainSample destroySample(Long sampleId, Long destroyBy, String remark) {
        RetainSample sample = retainSampleMapper.selectById(sampleId);
        if (sample == null) {
            throw new IllegalArgumentException("留样记录不存在");
        }
        if (Integer.valueOf(4).equals(sample.getStatus())) {
            throw new IllegalStateException("RETAIN_SAMPLE_CANNOT_DESTROY: 留样已销毁");
        }
        sample.setStatus(4);
        sample.setDestroyTime(LocalDateTime.now());
        sample.setDestroyBy(destroyBy);
        sample.setRemark(remark);
        retainSampleMapper.updateById(sample);
        return sample;
    }

    @Override
    public List<RetainSample> findExpiringSamples(int withinHours) {
        LocalDateTime threshold = LocalDateTime.now().plusHours(withinHours);
        return retainSampleMapper.selectList(
                new LambdaQueryWrapper<RetainSample>()
                        .eq(RetainSample::getStatus, 1)
                        .le(RetainSample::getExpireTime, threshold)
                        .ge(RetainSample::getExpireTime, LocalDateTime.now())
                        .orderByAsc(RetainSample::getExpireTime)
        );
    }
}
