package cn.org.openygt.quality.service;

import cn.org.openygt.common.dto.InspectionResult;
import cn.org.openygt.common.enums.TaskStatus;
import cn.org.openygt.common.service.QualityService;
import cn.org.openygt.quality.entity.Inspection;
import cn.org.openygt.quality.entity.Task;
import cn.org.openygt.quality.mapper.InspectionMapper;
import cn.org.openygt.quality.mapper.TaskMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Slf4j
@Service
@RequiredArgsConstructor
public class QualityServiceImpl implements QualityService {

    private final TaskMapper taskMapper;
    private final InspectionMapper inspectionMapper;

    @Override
    @Transactional
    public InspectionResult inspect(Long taskId, String result, String operatorId, String remark) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在");
        }
        if (!TaskStatus.WAIT_QC.getLabel().equals(task.getStatus())) {
            throw new IllegalStateException("任务不在待质检状态，当前状态: " + task.getStatus());
        }

        String nextStatus;
        Integer isException = null;
        String exceptionReason = null;

        switch (result) {
            case "通过":
                nextStatus = TaskStatus.WAIT_HANDOVER.getLabel();
                break;
            case "让步放行":
                nextStatus = TaskStatus.WAIT_HANDOVER.getLabel();
                isException = 1;
                exceptionReason = remark;
                break;
            case "返工":
                nextStatus = TaskStatus.WAIT_DECOCT.getLabel();
                break;
            case "报废":
                nextStatus = TaskStatus.SCRAPPED.getLabel();
                isException = 1;
                exceptionReason = remark;
                break;
            default:
                throw new IllegalArgumentException("未知的质检结果: " + result);
        }

        // 更新任务状态及异常标记
        task.setStatus(nextStatus);
        task.setOperatorId(operatorId);
        if (isException != null) {
            task.setIsException(isException);
            task.setExceptionReason(exceptionReason);
        }
        taskMapper.updateById(task);

        // 写入质检记录
        Inspection inspection = new Inspection();
        inspection.setTaskId(taskId);
        inspection.setResult(result);
        inspection.setOperatorId(operatorId);
        inspection.setRemark(remark);
        inspectionMapper.insert(inspection);

        InspectionResult ir = new InspectionResult();
        ir.setInspectionId(inspection.getId());
        ir.setTaskId(taskId);
        ir.setResult(result);
        ir.setNextStatus(nextStatus);
        ir.setOperatorId(operatorId);
        ir.setRemark(remark);
        ir.setInspectedAt(new Date());
        ir.setIsException(isException);
        ir.setExceptionReason(exceptionReason);

        log.info("质检完成: taskId={}, result={}, nextStatus={}, operatorId={}", taskId, result, nextStatus, operatorId);
        return ir;
    }

    @Override
    public InspectionResult getInspectionByTaskId(Long taskId) {
        Inspection inspection = inspectionMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Inspection>()
                        .eq(Inspection::getTaskId, taskId)
                        .orderByDesc(Inspection::getCreatedAt)
                        .last("LIMIT 1")
        );
        if (inspection == null) {
            return null;
        }
        InspectionResult ir = new InspectionResult();
        ir.setInspectionId(inspection.getId());
        ir.setTaskId(inspection.getTaskId());
        ir.setResult(inspection.getResult());
        ir.setOperatorId(inspection.getOperatorId());
        ir.setRemark(inspection.getRemark());
        ir.setInspectedAt(inspection.getCreatedAt());
        return ir;
    }
}
