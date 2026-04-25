package cn.org.openygt.quality.service;

import cn.org.openygt.common.dto.InspectionResult;
import cn.org.openygt.common.enums.InspectionResultType;
import cn.org.openygt.common.service.ProductionQueryService;
import cn.org.openygt.common.service.QualityService;
import cn.org.openygt.quality.entity.Inspection;
import cn.org.openygt.quality.mapper.InspectionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class QualityServiceImpl implements QualityService {

    private final InspectionMapper inspectionMapper;
    private final ProductionQueryService productionQueryService;

    @Override
    @Transactional
    public InspectionResult inspect(Long taskId, InspectionResultType result, String operatorId, String remark) {
        // 通过 SPI 验证任务存在性（不直接操作 prod_task 表）
        if (productionQueryService.getTaskById(taskId) == null) {
            throw new IllegalArgumentException("任务不存在: " + taskId);
        }

        // 写入质检记录（dms-quality 仅负责 qt_inspection 表）
        boolean isException = isExceptionResult(result);
        Inspection inspection = new Inspection();
        inspection.setTaskId(taskId);
        inspection.setResult(result);
        inspection.setOperatorId(operatorId);
        inspection.setRemark(remark);
        inspection.setInspectedAt(LocalDateTime.now());
        inspection.setIsException(isException ? 1 : 0);
        inspection.setExceptionReason(isException ? remark : null);
        inspectionMapper.insert(inspection);

        InspectionResult ir = new InspectionResult();
        ir.setInspectionId(inspection.getId());
        ir.setTaskId(taskId);
        ir.setResult(result);
        ir.setNextStatus(deduceNextStatus(result));
        ir.setOperatorId(operatorId);
        ir.setRemark(remark);
        ir.setInspectedAt(LocalDateTime.now());
        ir.setIsException(isExceptionResult(result) ? 1 : 0);
        ir.setExceptionReason(isExceptionResult(result) ? remark : null);

        log.info("质检完成: taskId={}, result={}, operatorId={}", taskId, result, operatorId);
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
        ir.setInspectedAt(inspection.getInspectedAt());
        ir.setIsException(inspection.getIsException());
        ir.setExceptionReason(inspection.getExceptionReason());
        return ir;
    }

    @Override
    public cn.org.openygt.common.dto.InspectionSummaryDTO getInspectionSummary(LocalDateTime from, LocalDateTime to) {
        // TODO: 实现统计聚合（Wave 2）
        log.warn("getInspectionSummary 尚未实现");
        return new cn.org.openygt.common.dto.InspectionSummaryDTO();
    }

    @Override
    public List<cn.org.openygt.common.dto.InspectionTrendDTO> getInspectionTrend(String groupBy) {
        // TODO: 实现趋势统计（Wave 2）
        log.warn("getInspectionTrend 尚未实现");
        return java.util.Collections.emptyList();
    }

    private String deduceNextStatus(InspectionResultType result) {
        switch (result) {
            case PASS:
            case CONCESSION:
                return "待交接";
            case REWORK:
                return "待煎药";
            case SCRAP:
                return "已报废";
            default:
                throw new IllegalArgumentException("未知的质检结果: " + result);
        }
    }

    private boolean isExceptionResult(InspectionResultType result) {
        return result == InspectionResultType.CONCESSION
                || result == InspectionResultType.REWORK
                || result == InspectionResultType.SCRAP;
    }
}
