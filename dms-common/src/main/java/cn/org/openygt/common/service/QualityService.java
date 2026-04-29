package cn.org.openygt.common.service;

import cn.org.openygt.common.dto.InspectionResult;
import cn.org.openygt.common.dto.InspectionSummaryDTO;
import cn.org.openygt.common.dto.InspectionTrendDTO;
import cn.org.openygt.common.enums.InspectionResultType;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 质量追溯模块对外服务接口。
 * 定义在 dms-common，由 dms-quality 实现。
 * dms-production 通过注入此接口提交质检请求。
 */
public interface QualityService {

    InspectionResult inspect(Long taskId, InspectionResultType result, String operatorId, String remark, String reworkNode);

    InspectionResult getInspectionByTaskId(Long taskId);

    // ---- 统计扩展（评审03新增，供 analytics 使用） ----

    InspectionSummaryDTO getInspectionSummary(LocalDateTime start, LocalDateTime end);

    List<InspectionTrendDTO> getInspectionTrend(String groupBy);
}
