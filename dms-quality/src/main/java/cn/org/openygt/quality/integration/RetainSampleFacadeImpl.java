package cn.org.openygt.quality.integration;

import cn.org.openygt.common.service.RetainSampleFacade;
import cn.org.openygt.quality.service.RetainSampleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RetainSampleFacadeImpl implements RetainSampleFacade {

    private final RetainSampleService retainSampleService;

    @Override
    public void createAfterPass(Long taskId, Long prescriptionId, String operatorId) {
        if (taskId == null || prescriptionId == null) {
            return;
        }
        long op = 0L;
        if (operatorId != null && !operatorId.trim().isEmpty()) {
            try {
                op = Long.parseLong(operatorId.trim());
            } catch (NumberFormatException ignored) {
                // 非数字工号仍写 0，与历史 QualityServiceImpl 行为一致
            }
        }
        retainSampleService.createSamplesForTask(taskId, prescriptionId, op);
    }
}
