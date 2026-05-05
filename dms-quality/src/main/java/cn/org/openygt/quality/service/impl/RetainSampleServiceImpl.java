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
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RetainSampleServiceImpl extends ServiceImpl<RetainSampleMapper, RetainSample>
        implements RetainSampleService {

    /** 库表约定：仅使用 4 表示 7 天留样 */
    private static final int SAMPLE_TYPE_7_DAY = 4;
    private static final int DURATION_HOURS_7_DAY = 7 * 24;

    private final RetainSampleMapper retainSampleMapper;

    @Override
    @Transactional
    public List<RetainSample> createSamplesForTask(Long taskId, Long prescriptionId, Long operatorId) {
        long active = retainSampleMapper.selectCount(
                new LambdaQueryWrapper<RetainSample>()
                        .eq(RetainSample::getTaskId, taskId)
                        .in(RetainSample::getStatus, 1, 2, 3)
        );
        if (active > 0) {
            log.info("任务已存在未销毁留样，跳过重复创建: taskId={}", taskId);
            return retainSampleMapper.selectList(
                    new LambdaQueryWrapper<RetainSample>()
                            .eq(RetainSample::getTaskId, taskId)
                            .in(RetainSample::getStatus, 1, 2, 3)
                            .orderByDesc(RetainSample::getId)
                            .last("LIMIT 1")
            );
        }

        RetainSample sample = newSevenDaySample(taskId, prescriptionId, operatorId);
        // 唯一约束冲突时重试（最多 3 次）
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                retainSampleMapper.insert(sample);
                break;
            } catch (org.springframework.dao.DuplicateKeyException e) {
                log.warn("留样编号冲突，重试生成: taskId={}, attempt={}", taskId, attempt);
                sample.setSampleNo(generateSampleNo());
                if (attempt == 3) {
                    throw new IllegalStateException("留样编号生成失败，请重试", e);
                }
            }
        }
        return Collections.singletonList(sample);
    }

    private RetainSample newSevenDaySample(Long taskId, Long prescriptionId, Long operatorId) {
        LocalDateTime now = LocalDateTime.now();
        RetainSample sample = new RetainSample();
        sample.setTaskId(taskId);
        sample.setPrescriptionId(prescriptionId);
        sample.setSampleNo(generateSampleNo());
        sample.setSampleType(SAMPLE_TYPE_7_DAY);
        sample.setRetainDuration(DURATION_HOURS_7_DAY);
        sample.setRetainTime(now);
        sample.setExpireTime(now.plusHours(DURATION_HOURS_7_DAY));
        sample.setStatus(1);
        sample.setOperatorId(operatorId);
        return sample;
    }

    /**
     * 生成唯一留样编号：SMP + yyyyMMdd + HHmmss + SSS + 4位随机数。
     * 毫秒级时间戳 + 随机数，在并发场景下冲突概率极低。
     */
    private String generateSampleNo() {
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        int random = (int) (Math.random() * 10000);
        return "SMP" + ts + String.format("%04d", random);
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
