package cn.org.openygt.quality.service;

import cn.org.openygt.common.dto.InspectionItemDTO;
import cn.org.openygt.common.dto.InspectionResult;
import cn.org.openygt.common.enums.InspectionResultType;
import cn.org.openygt.common.service.ProductionQueryService;
import cn.org.openygt.common.service.QualityService;
import cn.org.openygt.quality.dto.InspectExecuteRequest;
import cn.org.openygt.quality.entity.Inspection;
import cn.org.openygt.quality.entity.InspectionItem;
import cn.org.openygt.quality.mapper.InspectionItemMapper;
import cn.org.openygt.quality.mapper.UserNameMapper;
import cn.org.openygt.quality.mapper.InspectionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class QualityServiceImpl implements QualityService {

    private final InspectionMapper inspectionMapper;
    private final InspectionItemMapper inspectionItemMapper;
    private final UserNameMapper userNameMapper;

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
        Page<Inspection> pageResult = inspectionMapper.selectPage(new Page<>(page, size), wrapper);
        // 填充操作人姓名
        java.util.Set<String> opIds = pageResult.getRecords().stream()
                .map(Inspection::getOperatorId)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
        if (!opIds.isEmpty()) {
            java.util.List<java.util.Map<String, Object>> userMaps = userNameMapper.selectNamesByIds(opIds.stream().map(Long::valueOf).collect(java.util.stream.Collectors.toList()));
            java.util.Map<Long, String> nameMap = userMaps.stream().collect(java.util.stream.Collectors.toMap(
                    m -> Long.valueOf(m.get("id").toString()),
                    m -> m.get("realName") != null ? m.get("realName").toString() : "",
                    (a, b) -> a));
            for (Inspection ins : pageResult.getRecords()) {
                if (ins.getOperatorId() != null) {
                    ins.setOperatorName(nameMap.getOrDefault(Long.valueOf(ins.getOperatorId()), ins.getOperatorId()));
                }
            }
        }
        return pageResult;
    }

    @Override
    public cn.org.openygt.common.dto.InspectionSummaryDTO getInspectionSummary(LocalDateTime from, LocalDateTime to) {
        LambdaQueryWrapper<Inspection> wrapper = new LambdaQueryWrapper<>();
        if (from != null) wrapper.ge(Inspection::getInspectedAt, from);
        if (to != null) wrapper.le(Inspection::getInspectedAt, to);
        List<Inspection> list = inspectionMapper.selectList(wrapper);

        int total = list.size();
        int pass = 0, concession = 0, rework = 0, scrap = 0;
        for (Inspection i : list) {
            if (i.getResult() == null) continue;
            switch (i.getResult()) {
                case PASS: pass++; break;
                case CONCESSION: concession++; break;
                case REWORK: rework++; break;
                case SCRAP: scrap++; break;
                default: break;
            }
        }

        cn.org.openygt.common.dto.InspectionSummaryDTO dto = new cn.org.openygt.common.dto.InspectionSummaryDTO();
        dto.setTotalCount(total);
        dto.setPassCount(pass);
        dto.setConcessionCount(concession);
        dto.setReworkCount(rework);
        dto.setScrapCount(scrap);
        dto.setPassRate(total > 0 ? round2((double) pass / total * 100) : 0.0);
        dto.setReworkRate(total > 0 ? round2((double) rework / total * 100) : 0.0);
        dto.setScrapRate(total > 0 ? round2((double) scrap / total * 100) : 0.0);
        return dto;
    }

    @Override
    public List<cn.org.openygt.common.dto.InspectionTrendDTO> getInspectionTrend(LocalDateTime from, LocalDateTime to, String groupBy) {
        LambdaQueryWrapper<Inspection> wrapper = new LambdaQueryWrapper<>();
        if (from != null) {
            wrapper.ge(Inspection::getInspectedAt, from);
        } else {
            // 默认查最近 30 天
            wrapper.ge(Inspection::getInspectedAt, LocalDateTime.now().minusDays(30));
        }
        if (to != null) wrapper.le(Inspection::getInspectedAt, to);
        List<Inspection> list = inspectionMapper.selectList(wrapper);

        java.util.Map<String, cn.org.openygt.common.dto.InspectionTrendDTO> map = new java.util.TreeMap<>();
        java.time.format.DateTimeFormatter fmt;
        if ("month".equals(groupBy)) {
            fmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM");
        } else if ("week".equals(groupBy)) {
            fmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-'W'ww");
        } else {
            fmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");
        }

        for (Inspection i : list) {
            if (i.getInspectedAt() == null || i.getResult() == null) continue;
            String period = i.getInspectedAt().format(fmt);
            cn.org.openygt.common.dto.InspectionTrendDTO dto = map.computeIfAbsent(period, k -> {
                cn.org.openygt.common.dto.InspectionTrendDTO d = new cn.org.openygt.common.dto.InspectionTrendDTO();
                d.setPeriod(k);
                d.setTotalCount(0);
                d.setPassCount(0);
                d.setConcessionCount(0);
                d.setReworkCount(0);
                d.setScrapCount(0);
                return d;
            });
            dto.setTotalCount(dto.getTotalCount() + 1);
            switch (i.getResult()) {
                case PASS: dto.setPassCount(dto.getPassCount() + 1); break;
                case CONCESSION: dto.setConcessionCount(dto.getConcessionCount() + 1); break;
                case REWORK: dto.setReworkCount(dto.getReworkCount() + 1); break;
                case SCRAP: dto.setScrapCount(dto.getScrapCount() + 1); break;
                default: break;
            }
        }
        return new java.util.ArrayList<>(map.values());
    }

    @Override
    public List<Map<String, Object>> getInspectionReasonStat(LocalDateTime from, LocalDateTime to) {
        LambdaQueryWrapper<Inspection> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Inspection::getIsException, 1);
        if (from != null) wrapper.ge(Inspection::getInspectedAt, from);
        if (to != null) wrapper.le(Inspection::getInspectedAt, to);
        wrapper.isNotNull(Inspection::getExceptionReason);
        List<Inspection> list = inspectionMapper.selectList(wrapper);

        java.util.Map<String, int[]> map = new java.util.HashMap<>();
        for (Inspection i : list) {
            String reason = i.getExceptionReason();
            if (reason == null || reason.trim().isEmpty()) continue;
            int[] counts = map.computeIfAbsent(reason.trim(), k -> new int[2]);
            counts[1]++; // fail
        }

        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (Map.Entry<String, int[]> entry : map.entrySet()) {
            Map<String, Object> row = new java.util.HashMap<>();
            row.put("item", entry.getKey());
            row.put("pass", 0);
            row.put("fail", entry.getValue()[1]);
            result.add(row);
        }
        // 按不合格数降序
        result.sort((a, b) -> Integer.compare((Integer) b.get("fail"), (Integer) a.get("pass")));
        return result;
    }

    private Double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
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

    @Override
    @Transactional
    public InspectionResult inspectWithItems(Long taskId, InspectionResultType result, String operatorId,
                                              String remark, String reworkNode, List<InspectionItemDTO> items) {
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
        if (items != null && !items.isEmpty()) {
            int sort = 1;
            for (InspectionItemDTO item : items) {
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
                items != null ? items.size() : 0);
        return ir;
    }

    /**
     * 带检查项明细的质检执行（Phase 5.5 增强，Controller 入口）。
     */
    @Transactional
    public InspectionResult inspectWithItems(InspectExecuteRequest req) {
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
        List<InspectionItemDTO> items = null;
        if (req.getItems() != null) {
            items = new java.util.ArrayList<>();
            for (InspectExecuteRequest.InspectItemDTO dto : req.getItems()) {
                InspectionItemDTO item = new InspectionItemDTO();
                item.setItemCode(dto.getItemCode());
                item.setItemName(dto.getItemName());
                item.setResult(dto.getResult());
                item.setActualValue(dto.getActualValue());
                item.setRemark(dto.getRemark());
                items.add(item);
            }
        }
        return inspectWithItems(req.getTaskId(), result, req.getOperatorId(), req.getRemark(), req.getReworkNode(), items);
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
