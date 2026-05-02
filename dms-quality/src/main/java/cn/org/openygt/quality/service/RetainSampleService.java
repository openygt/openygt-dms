package cn.org.openygt.quality.service;

import cn.org.openygt.quality.entity.RetainSample;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface RetainSampleService extends IService<RetainSample> {

    List<RetainSample> createSamplesForTask(Long taskId, Long prescriptionId, Long operatorId);

    RetainSample destroySample(Long sampleId, Long destroyBy, String remark);

    List<RetainSample> findExpiringSamples(int withinHours);
}
