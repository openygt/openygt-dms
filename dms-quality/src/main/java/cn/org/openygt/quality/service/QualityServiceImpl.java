package cn.org.openygt.quality.service;

import cn.org.openygt.common.dto.InspectionResult;
import cn.org.openygt.common.enums.InspectionResultType;
import cn.org.openygt.common.service.ProductionQueryService;
import cn.org.openygt.common.service.QualityService;
import cn.org.openygt.quality.dto.InspectExecuteRequest;
import cn.org.openygt.quality.entity.Inspection;
import cn.org.openygt.quality.entity.InspectionItem;
import cn.org.openygt.quality.mapper.InspectionItemMapper;
import cn.org.openygt.quality.mapper.InspectionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
    private final InspectionItemMapper inspectionItemMapper;
    private final ProductionQueryService productionQueryService;

    @Override
    @Transactional
    public InspectionResult inspect(Long taskId, InspectionResultType result, String operatorId, String remark, String reworkNode) {
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
        ir.setNextStatus(deduceNextStatus(result, reworkNode));
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

    /**
     * 质检记录分页列表。
     */
    public Page<Inspection> listInspections(String result, String startTime, String endTime, int page, int size) {
        LambdaQueryWrapper<Inspection> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Inspection::getCreatedAt);
        if (result != null && !result.isEmpty()) {
            wrapper.eq(Inspection::getResult, result);
        }
        if (startTime != null && !startTime.isEmpty()) {
            wrapper.ge(Inspection::getCreatedAt, startTime);
        }
        if (endTime != null && !endTime.isEmpty()) {
            wrapper.le(Inspection::getCreatedAt, endTime);
        }
        return inspectionMapper.selectPage(new Page<>(page, size), wrapper);
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

    private String deduceNextStatus(InspectionResultType result, String reworkNode) {
        switch (result) {
            case PASS:
            case CONCESSION:
                return "待交接";
            case REWORK:
                return reworkNode != null && !reworkNode.isEmpty() ? reworkNode : "待煎药";
            case SCRAP:
                return "已报废";
            default:
                throw new IllegalArgumentException("未知的质检结果: " + result);
        }
    }

    /**
     * 带检查项明细的质检执行（Phase 5.5 增强）。
     */
    @Transactional
    public InspectionResult inspectWithItems(InspectExecuteRequest req) {
        Long taskId = req.getTaskId();
        String overallResult = req.getOverallResult();
        if (overallResult == null || overallResult.trim().isEmpty()) {
            throw new IllegalArgumentException("质检结果不能为空");
        }
        InspectionResultType result;
        try {
            result = InspectionResultType.valueOf(overallResult.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("无效的质检结果: " + overallResult);
        }
        String operatorId = req.getOperatorId();
        String remark = req.getRemark();
        String reworkNode = req.getReworkNode();

        // 通过 SPI 验证任务存在性
        if (productionQueryService.getTaskById(taskId) == null) {
            throw new IllegalArgumentException("任务不存在: " + taskId);
        }

        // 写入质检记录
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

        // 写入检查项明细
        if (req.getItems() != null && !req.getItems().isEmpty()) {
            int sort = 1;
            for (InspectExecuteRequest.InspectItemDTO item : req.getItems()) {
                InspectionItem ii = new InspectionItem();
                ii.setInspectionId(inspection.getId());
                ii.setItemCode(item.getItemCode());
                ii.setItemName(item.getItemName());
                ii.setResult(item.getResult());
                ii.setActualValue(item.getActualValue());
                ii.setRemark(item.getRemark());
                ii.setSortOrder(sort++);
                inspectionItemMapper.insert(ii);
            }
        }

        InspectionResult ir = new InspectionResult();
        ir.setInspectionId(inspection.getId());
        ir.setTaskId(taskId);
        ir.setResult(result);
        ir.setNextStatus(deduceNextStatus(result, reworkNode));
        ir.setOperatorId(operatorId);
        ir.setRemark(remark);
        ir.setInspectedAt(LocalDateTime.now());
        ir.setIsException(isExceptionResult(result) ? 1 : 0);
        ir.setExceptionReason(isExceptionResult(result) ? remark : null);

        log.info("质检完成(含明细): taskId={}, result={}, operatorId={}, items={}", taskId, result, operatorId,
                req.getItems() != null ? req.getItems().size() : 0);
        return ir;
    }

    /**
     * 查询质检记录的检查项明细。
     */
    public List<InspectionItem> getInspectionItems(Long inspectionId) {
        return inspectionItemMapper.selectByInspectionId(inspectionId);
    }

    private boolean isExceptionResult(InspectionResultType result) {
        return result == InspectionResultType.CONCESSION
                || result == InspectionResultType.REWORK
                || result == InspectionResultType.SCRAP;
    }
}
