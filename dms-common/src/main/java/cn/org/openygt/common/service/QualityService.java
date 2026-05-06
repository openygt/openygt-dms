package cn.org.openygt.common.service;

import cn.org.openygt.common.dto.InspectionItemDTO;
import cn.org.openygt.common.dto.InspectionResult;
import cn.org.openygt.common.dto.InspectionSummaryDTO;
import cn.org.openygt.common.dto.InspectionTrendDTO;
import cn.org.openygt.common.enums.InspectionResultType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 质量追溯模块对外服务接口。
 * 定义在 dms-common，由 dms-quality 实现。
 * dms-production 通过注入此接口提交质检请求。
 */
public interface QualityService {

    InspectionResult inspect(Long taskId, InspectionResultType result, String operatorId, String remark, String reworkNode);

    /**
     * 带检查项明细的质检执行（供生产模块原子调用）。
     */
    InspectionResult inspectWithItems(Long taskId, InspectionResultType result, String operatorId, String remark, String reworkNode, List<InspectionItemDTO> items);

    InspectionResult getInspectionByTaskId(Long taskId);

    // ---- 统计扩展（评审03新增，供 analytics 使用） ----

    InspectionSummaryDTO getInspectionSummary(LocalDateTime start, LocalDateTime end);

    List<InspectionTrendDTO> getInspectionTrend(LocalDateTime start, LocalDateTime end, String groupBy);

    List<Map<String, Object>> getInspectionReasonStat(LocalDateTime start, LocalDateTime end);
}
